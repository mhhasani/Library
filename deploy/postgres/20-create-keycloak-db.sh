#!/bin/bash
# docker-entrypoint-initdb.d hook: creates Keycloak's own database and account on first start.
# For an existing volume, run scripts/db-upgrade.sh instead.
set -euo pipefail

if [ -z "${KC_DB_USER:-}" ] || [ -z "${KC_DB_PASSWORD:-}" ]; then
    echo "20-create-keycloak-db: KC_DB_USER/KC_DB_PASSWORD not set; skipping" >&2
    exit 0
fi

psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" \
     -v kc_user="$KC_DB_USER" -v kc_password="$KC_DB_PASSWORD" -v kc_db="${KC_DB_NAME:-keycloak}" \
     -f /opt/library-db/keycloak-db.sql
