#!/usr/bin/env bash
# Upgrade helper for an EXISTING database volume (the init hook only runs on a fresh one):
# creates/updates the least-privilege runtime role defined in deploy/postgres/app-role.sql.
# Usage: ./scripts/db-create-app-role.sh   (reads DB_APP_USER / DB_APP_PASSWORD from .env)
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"

docker compose --project-directory "$ROOT_DIR" exec -T postgres bash -c '
    psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" \
         -v owner="$POSTGRES_USER" -v db="$POSTGRES_DB" \
         -v app_user="$DB_APP_USER" -v app_password="$DB_APP_PASSWORD" \
         -f /opt/library-db/app-role.sql'

echo "Runtime role is ready. Restart the application: docker compose up -d app"
