#!/usr/bin/env bash
# Encrypted backup of everything the system needs to come back:
#   app database, Keycloak database (users, credentials, MFA) and uploaded files.
# Output: one AES-256 encrypted archive (PBKDF2-derived key) with an internal SHA-256 manifest.
#
# Usage: ./scripts/backup.sh [output-dir]          (default: ./backups)
#   Passphrase: prompted, or read from the file named by BACKUP_PASSPHRASE_FILE (for cron).
#   Keep the passphrase apart from the backups; without it the archive cannot be restored.
set -euo pipefail

# Project root: next to the script inside the offline bundle, its parent in the repository
ROOT_DIR="$(cd "$(dirname "$0")" && pwd)"
[ -f "$ROOT_DIR/docker-compose.yml" ] || ROOT_DIR="$(dirname "$ROOT_DIR")"
OUT_DIR="${1:-$ROOT_DIR/backups}"
compose() { docker compose --project-directory "$ROOT_DIR" "$@"; }

command -v openssl >/dev/null || { echo "openssl is required" >&2; exit 1; }

if [ -n "${BACKUP_PASSPHRASE_FILE:-}" ]; then
    PASSPHRASE="$(cat "$BACKUP_PASSPHRASE_FILE")"
else
    read -rsp "Backup passphrase: " PASSPHRASE; echo
    read -rsp "Repeat passphrase: " CONFIRM; echo
    [ "$PASSPHRASE" = "$CONFIRM" ] || { echo "Passphrases do not match" >&2; exit 1; }
fi
[ ${#PASSPHRASE} -ge 16 ] || { echo "Passphrase must be at least 16 characters" >&2; exit 1; }

umask 077
WORK="$(mktemp -d)"
trap 'rm -rf "$WORK"' EXIT

echo "Dumping databases..."
compose exec -T postgres sh -c 'pg_dump -U "$POSTGRES_USER" -d "$POSTGRES_DB" -Fc' > "$WORK/app-db.dump"
compose exec -T postgres sh -c 'pg_dump -U "$POSTGRES_USER" -d "${KC_DB_NAME:-keycloak}" -Fc' > "$WORK/keycloak-db.dump"

echo "Archiving uploaded files..."
compose exec -T app tar -C /data/library-files -cf - . > "$WORK/files.tar"

(cd "$WORK" && sha256sum app-db.dump keycloak-db.dump files.tar > SHA256SUMS)

mkdir -p "$OUT_DIR"
TARGET="$OUT_DIR/library-backup-$(date +%Y%m%d-%H%M%S).tar.enc"
tar -C "$WORK" -cf - SHA256SUMS app-db.dump keycloak-db.dump files.tar \
    | openssl enc -aes-256-cbc -pbkdf2 -iter 600000 -md sha256 -salt -pass fd:3 -out "$TARGET" 3<<<"$PASSPHRASE"
chmod 600 "$TARGET"

echo "Backup written: $TARGET"
