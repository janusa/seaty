#!/usr/bin/env bash

set -euo pipefail

DB_FILE="${DB_FILE:-data/app.db}"
SCHEMA_FILE="${SCHEMA_FILE:-db/schema.sql}"
SEED_FILE="${SEED_FILE:-db/seed.sql}"

mkdir -p "$(dirname "$DB_FILE")"

if [ ! -f "$DB_FILE" ]; then
  echo "Database not found. Creating: $DB_FILE"

  sqlite3 "$DB_FILE" < "$SCHEMA_FILE"

  if [ -f "$SEED_FILE" ]; then
    echo "Seeding database..."
    sqlite3 "$DB_FILE" < "$SEED_FILE"
  fi

  echo "Database initialized."
else
  echo "Database already exists. Skipping creation."
fi