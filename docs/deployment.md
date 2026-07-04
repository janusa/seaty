# Deployment

How Seaty runs on the droplet, how it was provisioned, and how new builds ship. Ongoing deploys are
automated — a merge to `main` ships to production automatically via a poll-based pull (see
**Continuous deployment** below). The box was set up by hand following **First-time provisioning**.

## Topology

- **Droplet:** Ubuntu 24.04, `s-1vcpu-512mb` (AMS3, 512 MB RAM / 10 GB disk), public IP
  `164.92.222.230`. Domain **`tharsagan.no`** (A record only — no AAAA; the site is IPv4-only).
- **Users:** `ssh seaty` logs in as **`deploy`** (ships files, runs `sudo` — sudo requires a
  password). The app runs as the unprivileged **`appuser`** (systemd `User=`), which never has sudo.
- **Process model:** a fat JAR under systemd, bound to **loopback only**. **Caddy** terminates TLS
  on 443 and reverse-proxies to `127.0.0.1:8080`. TLS is required because the auth cookie is `Secure`.

## Layout on the box

| Path                                | Owner / mode       | Purpose                                                                                   |
|-------------------------------------|--------------------|-------------------------------------------------------------------------------------------|
| `/opt/seaty/seaty.jar`              | root:root `644`    | the executable fat jar                                                                    |
| `/var/lib/seaty/data/app.db`        | root:root `644`    | read-only SQLite DB (`WorkingDirectory=/var/lib/seaty`, so `./data/app.db` resolves here) |
| `/etc/seaty/seaty.env`              | root:appuser `640` | holds `SEATY_AUTH_SECRET`                                                                 |
| `/var/log/seaty/`                   | appuser            | rolling prod log (`seaty.log`)                                                            |
| `/etc/systemd/system/seaty.service` | root               | the unit (source of truth: `deploy/seaty.service`)                                        |
| `/etc/caddy/Caddyfile`              | root               | reverse proxy (source: `deploy/Caddyfile`)                                                |
| `/swapfile`                         | root `600`         | 2 GB swap (backstop for the 512 MB box)                                                   |

## Runtime facts

- Requires a **JRE 25** (`java -version`).
- Required env:
    - `SEATY_AUTH_SECRET` (no default; app won't start without it).
    - `SPRING_PROFILES_ACTIVE=prod`
    - `LOG_PATH=/var/log/seaty`
    - `SERVER_ADDRESS=127.0.0.1` (loopback bind — only Caddy should reach the app).
- **512 MB box:** the unit pins `-Xmx192m -XX:+UseSerialGC -Xss512k` and `MemoryMax=420M`, plus a
  2 GB swap file.
- The systemd unit (`deploy/seaty.service`) is heavily sandboxed — full filesystem protection
  (`ProtectSystem=strict`), seccomp system-call filtering, capability dropping, etc. It also sets
  `WorkingDirectory`, the prod/log/loopback env, the small-heap `ExecStart`, `MemoryMax`, and
  `ReadWritePaths=/var/log/seaty` (the one path the app writes, for the rolling log). Note: systemd
  silently drops a directive if it has a trailing inline comment, so keep comments on their own line.

## Database

Read-only, built out-of-band. `scripts/init_db.sh` no-ops if its target file already exists, so a
local `data/app.db` can be stale — build a fresh copy to a temp path (from the current
`db/schema.sql` + `db/seed.sql`) and ship that:

```bash
TMPDB=$(mktemp -d)/app.db
DB_FILE="$TMPDB" ./scripts/init_db.sh        # builds from db/schema.sql + db/seed.sql; validates FKs + integrity
scp "$TMPDB" seaty:/tmp/seaty-app.db
ssh seaty 'sudo install -o root -g root -m 644 /tmp/seaty-app.db /var/lib/seaty/data/app.db'
```

## First-time provisioning

Run on the box as `deploy` (each `sudo` prompts for a password). `scp` the referenced files to
`/tmp` first.

```bash
# Swap (2 GB) — safety net for the 512 MB box
sudo fallocate -l 2G /swapfile && sudo chmod 600 /swapfile && sudo mkswap /swapfile && sudo swapon /swapfile
echo '/swapfile none swap sw 0 0' | sudo tee -a /etc/fstab

# Temurin JRE 25 (Ubuntu 24.04 repos don't carry JDK 25)
sudo mkdir -p /etc/apt/keyrings
sudo apt-get update && sudo apt-get install -y wget apt-transport-https gnupg
wget -qO- https://packages.adoptium.net/artifactory/api/gpg/key/public | sudo tee /etc/apt/keyrings/adoptium.asc >/dev/null
echo "deb [signed-by=/etc/apt/keyrings/adoptium.asc] https://packages.adoptium.net/artifactory/deb $(. /etc/os-release && echo $VERSION_CODENAME) main" | sudo tee /etc/apt/sources.list.d/adoptium.list
sudo apt-get update && sudo apt-get install -y temurin-25-jre

# Dirs + artifacts
sudo install -d -o root -g root -m 755 /opt/seaty /var/lib/seaty /var/lib/seaty/data
sudo install -o root -g root -m 644 /tmp/seaty.jar /opt/seaty/seaty.jar
sudo install -o root -g root -m 644 /tmp/seaty-app.db /var/lib/seaty/data/app.db
sudo install -d -o appuser -g appuser -m 755 /var/log/seaty

# Secret (typed, not echoed — keeps it out of shell history)
sudo install -d -o root -g root -m 755 /etc/seaty
read -rsp 'SEATY_AUTH_SECRET: ' S; echo
printf 'SEATY_AUTH_SECRET=%s\n' "$S" | sudo tee /etc/seaty/seaty.env >/dev/null; unset S
sudo chown root:appuser /etc/seaty/seaty.env && sudo chmod 640 /etc/seaty/seaty.env

# Service
sudo install -o root -g root -m 644 /tmp/seaty.service /etc/systemd/system/seaty.service
sudo systemctl daemon-reload && sudo systemctl enable --now seaty

# Caddy (official apt repo)
sudo apt-get install -y debian-keyring debian-archive-keyring apt-transport-https curl
curl -1sLf 'https://dl.cloudsmith.io/public/caddy/stable/gpg.key' | sudo gpg --dearmor -o /usr/share/keyrings/caddy-stable-archive-keyring.gpg
curl -1sLf 'https://dl.cloudsmith.io/public/caddy/stable/debian.deb.txt' | sudo tee /etc/apt/sources.list.d/caddy-stable.list
sudo apt-get update && sudo apt-get install -y caddy
sudo install -o root -g root -m 644 /tmp/Caddyfile /etc/caddy/Caddyfile
sudo systemctl restart caddy
```

### TLS certificate issuance (and the renewal caveat)

Let's Encrypt validates by connecting to the box from its own (global) servers, but the firewall is
Norway-only. To let the **HTTP-01** challenge through on port 80, temporarily open it at the host
layer (the provider's cloud firewall already allows 80 for all), then re-lock it once the cert is
obtained:

```bash
sudo iptables -I INPUT -p tcp --dport 80 -j ACCEPT   # open 80 for issuance
sudo systemctl restart caddy                         # Caddy obtains the cert (may retry once)
# ... confirm "certificate obtained successfully" for tharsagan.no ...
sudo iptables -D INPUT -p tcp --dport 80 -j ACCEPT   # re-lock 80
```

> **⚠️ Renewal:** Caddy auto-renews ~30 days before expiry. While port 80 is Norway-locked, renewal
> **will fail** — reopen port 80 briefly (as above) when a renewal is due. Check the current expiry
> with `ssh seaty 'sudo caddy list-certificates'` or by inspecting the cert in a browser.

## Firewall hardening (host iptables)

The host firewall is iptables + an ipset `norway` (region allowlist): it accepts 22/80/443 from
Norway IPs and drops them otherwise. Two gaps sit on top of that base and must be closed. The INPUT
chain's **default policy is `ACCEPT`**, so without a catch-all any *other* port (e.g. the app's 8080)
falls through and is accepted at the host layer; and the **IPv6 ruleset starts empty** (wide open — a
bypass of the IPv4-only Norway allowlist). The rules below add loopback + ICMP and a catch-all `DROP`
(kept additive, leaving `-P ACCEPT` as an anti-lockout cushion), give IPv6 a minimal safe ruleset,
then persist everything with `netfilter-persistent save`:

```bash
# IPv4: loopback (so Caddy can reach 127.0.0.1:8080) + ICMP, then default-deny
sudo iptables -I INPUT 1 -i lo -j ACCEPT
sudo iptables -A INPUT -p icmp -j ACCEPT
sudo iptables -A INPUT -j DROP

# IPv6: base is empty/open — add a minimal safe ruleset
sudo ip6tables -A INPUT -i lo -j ACCEPT
sudo ip6tables -A INPUT -m conntrack --ctstate RELATED,ESTABLISHED -j ACCEPT
sudo ip6tables -A INPUT -p ipv6-icmp -j ACCEPT
sudo ip6tables -A INPUT -j DROP

sudo netfilter-persistent save   # persists /etc/iptables/rules.v4 and rules.v6
```

To verify from an allowlisted host: SSH still works, HTTPS still serves (`401`), port 8080 is
blocked, and IPv6 `:443` times out (bypass closed). Rules persist across reboot; the `norway` ipset
persists separately via `/etc/ipset.rules`.

Possible cleanups: any explicit `--dport 22/80/443 DROP` rules are redundant with the catch-all and
could be pruned; a proper `-P DROP` default policy (with a tested rollback) would be cleaner than the
additive catch-all. Granting IPv6 or non-Norway access is a deliberate access-policy change at both
the iptables and cloud-firewall layers.

## Continuous deployment

A merge to `main` reaches production automatically. Because the firewall is a Norway-only allowlist
(a GitHub-hosted runner's global IP can't reach the box), CD is **pull-based and outbound-only** —
the droplet polls GitHub and self-installs. Nothing inbound is opened; there is no SSH key or deploy
credential in GitHub.

**How it flows:**

1. Push to `main` → the **Verify** workflow (`.github/workflows/maven.yml`) runs `./mvnw -B verify`
   and, only if it passes, uploads the fat jar as an artifact named `seaty-jar` (tied to that
   commit).
2. On the box, `seaty-deploy.timer` fires `seaty-deploy.service` every ~2 min → runs
   `/opt/seaty/deploy-poll.sh` (source: `deploy/deploy-poll.sh`).
3. The poller asks the GitHub API for the newest successful `push` run on `main`; if its commit
   differs from `/var/lib/seaty-deploy/deployed.sha`, it downloads that run's `seaty-jar`, installs
   it to `/opt/seaty/seaty.jar` (saving the old one as `seaty.jar.prev`), restarts `seaty`, and
   records the new SHA.

The jar is built **and verified** by CI, so the 512 MB box never builds. **No post-deploy health
check** — if a deploy leaves the app unhealthy, recover manually (below) or push a fix to `main`.

### One-time setup (as `deploy`)

```bash
# GitHub token: a CLASSIC PAT with the `public_repo` scope. A fine-grained PAT won't work here — it
# can only target repos owned by the token's own account or org — and Actions artifact downloads
# require authentication even on a public repo. Note the expiry and diarise rotation; rotating it is
# just re-running this one command with the new value.
read -rsp 'GitHub deploy token: ' T; echo
printf '%s\n' "$T" | sudo tee /etc/seaty/deploy.token >/dev/null; unset T
sudo chown root:root /etc/seaty/deploy.token && sudo chmod 600 /etc/seaty/deploy.token

sudo apt-get install -y jq unzip
sudo install -d -o root -g root -m 755 /var/lib/seaty-deploy

# Ship deploy/deploy-poll.sh, deploy/seaty-deploy.service, deploy/seaty-deploy.timer to /tmp first.
sudo install -o root -g root -m 755 /tmp/deploy-poll.sh /opt/seaty/deploy-poll.sh
sudo install -o root -g root -m 644 /tmp/seaty-deploy.service /etc/systemd/system/seaty-deploy.service
sudo install -o root -g root -m 644 /tmp/seaty-deploy.timer   /etc/systemd/system/seaty-deploy.timer
sudo systemctl daemon-reload && sudo systemctl enable --now seaty-deploy.timer
```

### Operate

```bash
ssh seaty 'sudo systemctl start seaty-deploy'          # force a deploy check now (don't wait for the timer)
ssh seaty 'journalctl -u seaty-deploy -f'              # watch poll/deploy activity
ssh seaty 'systemctl list-timers seaty-deploy.timer'   # when it last ran / next runs
ssh seaty 'cat /var/lib/seaty-deploy/deployed.sha'     # currently deployed commit
# Manual rollback to the previous jar:
ssh seaty 'sudo install -o root -g root -m 644 /opt/seaty/seaty.jar.prev /opt/seaty/seaty.jar && sudo systemctl restart seaty'
```

> Rolling back the jar does **not** change `deployed.sha`, so the next poll will re-deploy the newest
> `main` build. To stop that, push a fix (or revert) to `main`, or `sudo systemctl stop
> seaty-deploy.timer` while you investigate.

## Deploy a new build (manual fallback)

CD (above) is the normal path. Use this to deploy by hand — e.g. the timer is stopped, or you need a
build that isn't on `main`:

```bash
./mvnw clean package                                     # build the fat jar (needs JDK 25)
scp target/seaty-0.0.1-SNAPSHOT.jar seaty:/tmp/seaty.jar
ssh seaty 'sudo install -o root -g root -m 644 /tmp/seaty.jar /opt/seaty/seaty.jar && sudo systemctl restart seaty'
```

## Verify

```bash
ssh seaty 'systemctl status seaty --no-pager'

# Unauthenticated checks (everything except /auth is guarded; a 401 means the app is serving):
curl -s -o /dev/null -w 'bad secret:  %{http_code}\n' 'https://tharsagan.no/auth?secret=WRONG'    # expect 401
curl -s -o /dev/null -w 'no session:  %{http_code}\n' 'https://tharsagan.no/api/guests?name=smi'  # expect 401

# Authenticated flow (needs the real secret): sign in, capture the session cookie, then search:
JAR=$(mktemp)
curl -s -c "$JAR" -o /dev/null 'https://tharsagan.no/auth?secret=<the-secret>'                    # sets session cookie
curl -s -b "$JAR" -o /dev/null -w 'search:      %{http_code}\n' 'https://tharsagan.no/api/guests?name=smi'  # expect 200
curl -s -b "$JAR" -o /dev/null -w 'short name:  %{http_code}\n' 'https://tharsagan.no/api/guests?name=ab'   # expect 400
rm -f "$JAR"
```

## Logs

Prod writes a rolling file (see `docs/logging.md`); startup/crash output also lands in journald:

```bash
ssh seaty 'sudo tail -f /var/log/seaty/seaty.log'
ssh seaty 'sudo journalctl -u seaty -f'
```
