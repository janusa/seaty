#!/usr/bin/env bash
#
# Seaty continuous-deploy poller. Runs on the droplet (as root, via seaty-deploy.timer) and pulls
# the latest CI-verified fat jar from GitHub Actions, installs it, and restarts the app.
#
# Why pull, not push: the droplet's firewall is a Norway-only allowlist (default-deny), so a
# GitHub-hosted runner cannot reach it. This script only makes OUTBOUND HTTPS calls, so the
# firewall stays fully locked. See docs/deployment.md ("Continuous deployment").
#
# Flow: find the newest successful push-run of the Verify workflow on main -> if its commit differs
# from what we last deployed, download that run's `seaty-jar` artifact -> install to /opt/seaty and
# restart seaty. The jar was already built AND verified by CI (./mvnw -B verify), so we do not build
# on this 512 MB box. There is no post-deploy health check by design; recover manually if needed
# (restore ${APP_JAR}.prev and restart, or push a fix to main).
#
# Requires: curl, jq, unzip, and a GitHub token at TOKEN_FILE (classic PAT, public_repo scope --
# janusa/seaty is a public repo owned by another user, so a fine-grained PAT can't target it, and
# artifact downloads need auth even on public repos). All output goes to journald
# (journalctl -u seaty-deploy). The token is never printed.

set -euo pipefail

# --- config ---------------------------------------------------------------------------------------
REPO="janusa/seaty"                       # owner/repo
WORKFLOW="maven.yml"                       # the CI workflow file that publishes the artifact
BRANCH="main"                             # only deploy commits that landed on main
ARTIFACT="seaty-jar"                      # artifact name uploaded by the workflow
APP_JAR="/opt/seaty/seaty.jar"           # where seaty.service loads the jar from
STATE_DIR="/var/lib/seaty-deploy"        # holds deployed.sha (last successfully deployed commit)
TOKEN_FILE="/etc/seaty/deploy.token"     # classic PAT, public_repo scope (600 root:root)
API="https://api.github.com/repos/${REPO}"

log() { printf '%s %s\n' "$(date -u +%FT%TZ)" "$*"; }
die() { printf '%s ERROR %s\n' "$(date -u +%FT%TZ)" "$*" >&2; exit 1; }

# --- read the token -------------------------------------------------------------------------------
[ -r "$TOKEN_FILE" ] || die "token file $TOKEN_FILE is missing or unreadable"
TOKEN="$(tr -d '\r\n' < "$TOKEN_FILE")"
[ -n "$TOKEN" ] || die "token file $TOKEN_FILE is empty"

# Common curl invocation for the JSON API. --retry rides out transient blips; --fail turns HTTP
# errors into a non-zero exit. The auth header is passed via -H; curl drops it on cross-host
# redirects (e.g. the artifact's signed storage URL), so the token never leaves api.github.com.
gh_api() {
	curl -fsS --retry 3 --retry-delay 2 --max-time 30 \
		-H "Authorization: Bearer ${TOKEN}" \
		-H "Accept: application/vnd.github+json" \
		-H "X-GitHub-Api-Version: 2022-11-28" \
		"$@"
}

# --- find the newest verified build on main -------------------------------------------------------
runs_json="$(gh_api "${API}/actions/workflows/${WORKFLOW}/runs?branch=${BRANCH}&event=push&status=success&per_page=1")" \
	|| die "failed to query workflow runs"

head_sha="$(jq -r '.workflow_runs[0].head_sha // empty' <<<"$runs_json")"
run_id="$(jq -r '.workflow_runs[0].id // empty' <<<"$runs_json")"
[ -n "$head_sha" ] && [ -n "$run_id" ] || { log "no successful ${BRANCH} build yet; nothing to do"; exit 0; }

deployed=""
[ -f "${STATE_DIR}/deployed.sha" ] && deployed="$(tr -d '\r\n' < "${STATE_DIR}/deployed.sha")"
if [ "$head_sha" = "$deployed" ]; then
	log "up to date (${head_sha}); nothing to do"
	exit 0
fi

log "new build detected: ${deployed:-<none>} -> ${head_sha} (run ${run_id}); deploying"

# --- resolve + download the artifact --------------------------------------------------------------
artifacts_json="$(gh_api "${API}/actions/runs/${run_id}/artifacts")" || die "failed to list artifacts for run ${run_id}"
art_id="$(jq -r --arg n "$ARTIFACT" 'first(.artifacts[] | select(.name == $n) | .id) // empty' <<<"$artifacts_json")"
if [ -z "$art_id" ]; then
	# Expected before this feature is merged to main (older runs predate the upload step), or if a
	# run's artifact has since expired. Not fatal — just nothing to deploy this cycle.
	log "run ${run_id} (${head_sha}) has no ${ARTIFACT} artifact; nothing to deploy"
	exit 0
fi

workdir="$(mktemp -d)"
trap 'rm -rf "$workdir"' EXIT

gh_api -L -o "${workdir}/artifact.zip" "${API}/actions/artifacts/${art_id}/zip" \
	|| die "failed to download artifact ${art_id}"
unzip -q -o "${workdir}/artifact.zip" -d "${workdir}/extract" || die "failed to unzip artifact"

new_jar="$(find "${workdir}/extract" -maxdepth 1 -name 'seaty-*.jar' | head -n1)"
[ -n "$new_jar" ] || die "no seaty-*.jar found inside the artifact"
[ -s "$new_jar" ] || die "downloaded jar is empty"

# --- install + restart ----------------------------------------------------------------------------
if [ -f "$APP_JAR" ]; then
	cp -p "$APP_JAR" "${APP_JAR}.prev"   # kept for manual rollback
fi
install -m 644 -o root -g root "$new_jar" "$APP_JAR"
log "installed new jar to ${APP_JAR}; restarting seaty"
systemctl restart seaty

# Only record success after the restart command returns 0. systemd's Restart=on-failure keeps the
# service supervised from here; we intentionally do not health-probe (see header).
printf '%s\n' "$head_sha" > "${STATE_DIR}/deployed.sha"
log "deployed ${head_sha}"
