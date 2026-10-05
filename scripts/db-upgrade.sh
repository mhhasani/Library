#!/usr/bin/env bash
# Upgrade helper for an EXISTING database volume (the init hooks only run on a fresh one):
#   - creates/updates the application's least-privilege runtime role (deploy/postgres/app-role.sql)
#   - creates Keycloak's own database and account (deploy/postgres/keycloak-db.sql)
# Idempotent. Usage: ./scripts/db-upgrade.sh   (values come from .env via docker compose)
set -euo pipefail

# Project root: next to the script inside the offline bundle, its parent in the repository
ROOT_DIR="$(cd "$(dirname "$0")" && pwd)"
[ -f "$ROOT_DIR/docker-compose.yml" ] || ROOT_DIR="$(dirname "$ROOT_DIR")"

docker compose --project-directory "$ROOT_DIR" up -d postgres
docker compose --project-directory "$ROOT_DIR" exec -T postgres bash -c '
    set -e
    if [ -n "${DB_APP_USER:-}" ]; then
        psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" \
             -v owner="$POSTGRES_USER" -v db="$POSTGRES_DB" \
             -v app_user="$DB_APP_USER" -v app_password="$DB_APP_PASSWORD" \
             -f /opt/library-db/app-role.sql
    fi
    psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" \
         -v kc_user="$KC_DB_USER" -v kc_password="$KC_DB_PASSWORD" -v kc_db="${KC_DB_NAME:-keycloak}" \
         -f /opt/library-db/keycloak-db.sql'

echo "Database is ready. Start everything with: docker compose up -d"
