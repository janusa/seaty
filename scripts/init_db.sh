#!/usr/bin/env bash

set -euo pipefail

DB_FILE="${DB_FILE:-data/app.db}"
SCHEMA_FILE="${SCHEMA_FILE:-db/schema.sql}"
SEED_FILE="${SEED_FILE:-db/seed.sql}"

mkdir -p "$(dirname "$DB_FILE")"

if [ -f "$DB_FILE" ]; then
  echo "Database already exists. Skipping creation."
  exit 0
fi

echo "Database not found. Creating: $DB_FILE"

# Remove a half-built database on any failure (including the consistency checks below) so a
# re-run always starts from a clean slate. Only armed once we know we are creating the file.
success=0
trap '[ "$success" -eq 1 ] || rm -f "$DB_FILE"' EXIT

sqlite3 "$DB_FILE" < "$SCHEMA_FILE"

if [ -f "$SEED_FILE" ]; then
  echo "Seeding database..."
  sqlite3 "$DB_FILE" < "$SEED_FILE"
fi

# The application never writes, so these schema-level invariants are the only guard for the
# hand-written seed. foreign_key_check reports violations even when they were inserted with
# foreign_keys off, so it is a reliable post-load gate.
echo "Validating consistency..."

fk_violations="$(sqlite3 "$DB_FILE" "PRAGMA foreign_key_check;")"
if [ -n "$fk_violations" ]; then
  echo "Foreign key violations detected:" >&2
  echo "$fk_violations" >&2
  exit 1
fi

integrity="$(sqlite3 "$DB_FILE" "PRAGMA integrity_check;")"
if [ "$integrity" != "ok" ]; then
  echo "Integrity check failed:" >&2
  echo "$integrity" >&2
  exit 1
fi

success=1
echo "Database initialized and consistent."
