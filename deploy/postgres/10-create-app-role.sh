#!/bin/bash
# docker-entrypoint-initdb.d hook: runs once, when the database volume is first created.
# For an existing volume, run scripts/db-create-app-role.sh instead.
set -euo pipefail

if [ -z "${DB_APP_USER:-}" ] || [ -z "${DB_APP_PASSWORD:-}" ]; then
    echo "10-create-app-role: DB_APP_USER/DB_APP_PASSWORD not set; the app will use the owner account" >&2
    exit 0
fi

psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" \
     -v owner="$POSTGRES_USER" -v db="$POSTGRES_DB" \
     -v app_user="$DB_APP_USER" -v app_password="$DB_APP_PASSWORD" \
     -f /opt/library-db/app-role.sql
