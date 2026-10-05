#!/usr/bin/env bash
# Restores an archive made by backup.sh: verifies its manifest, then replaces the app
# database, the Keycloak database and the uploaded files. Destructive — asks first.
#
# Usage: ./scripts/restore.sh <library-backup-*.tar.enc>
#   Passphrase: prompted, or read from the file named by BACKUP_PASSPHRASE_FILE.
set -euo pipefail

# Project root: next to the script inside the offline bundle, its parent in the repository
ROOT_DIR="$(cd "$(dirname "$0")" && pwd)"
[ -f "$ROOT_DIR/docker-compose.yml" ] || ROOT_DIR="$(dirname "$ROOT_DIR")"
ARCHIVE="${1:?Usage: $0 <backup-file>}"
compose() { docker compose --project-directory "$ROOT_DIR" "$@"; }

[ -f "$ARCHIVE" ] || { echo "Not found: $ARCHIVE" >&2; exit 1; }

if [ -n "${BACKUP_PASSPHRASE_FILE:-}" ]; then
    PASSPHRASE="$(cat "$BACKUP_PASSPHRASE_FILE")"
else
    read -rsp "Backup passphrase: " PASSPHRASE; echo
fi

umask 077
WORK="$(mktemp -d)"
trap 'rm -rf "$WORK"' EXIT

openssl enc -d -aes-256-cbc -pbkdf2 -iter 600000 -md sha256 -pass fd:3 -in "$ARCHIVE" 3<<<"$PASSPHRASE" \
    | tar -C "$WORK" -xf - \
    || { echo "Decryption failed (wrong passphrase or damaged file)" >&2; exit 1; }
(cd "$WORK" && sha256sum --quiet -c SHA256SUMS) \
    || { echo "Integrity check failed: the archive was modified or is incomplete" >&2; exit 1; }
echo "Archive verified."

read -rp "This REPLACES all current data. Type 'restore' to continue: " ANSWER
[ "$ANSWER" = "restore" ] || { echo "Cancelled."; exit 1; }

compose stop frontend app keycloak
compose up -d postgres

echo "Restoring databases..."
compose exec -T postgres sh -c 'pg_restore -U "$POSTGRES_USER" -d "$POSTGRES_DB" --clean --if-exists --exit-on-error' \
    < "$WORK/app-db.dump"
compose exec -T postgres sh -c 'pg_restore -U "$POSTGRES_USER" -d "${KC_DB_NAME:-keycloak}" --clean --if-exists --exit-on-error' \
    < "$WORK/keycloak-db.dump"

echo "Restoring uploaded files..."
compose run --rm --no-deps -T --entrypoint sh app \
    -c 'find /data/library-files -mindepth 1 -delete && tar -C /data/library-files -xf -' < "$WORK/files.tar"

compose up -d
echo "Restore complete."
