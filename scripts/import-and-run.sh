#!/usr/bin/env bash
# Installs and starts the offline bundle on the target machine (Docker only required,
# no internet access needed: all images are included in the bundle).
# Refuses to install anything whose integrity cannot be verified.
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
cd "$SCRIPT_DIR"

echo "==> Verifying bundle integrity..."
if [ -f SHA256SUMS.asc ]; then
    gpg --verify SHA256SUMS.asc SHA256SUMS
fi
if ! sha256sum --quiet -c SHA256SUMS; then
    echo "!! Integrity check FAILED - the bundle was modified or corrupted. Aborting." >&2
    exit 1
fi
echo "    All files match SHA256SUMS."

echo "==> Loading images..."
docker load -i postgres.tar
docker load -i app.tar
docker load -i frontend.tar
docker load -i keycloak.tar

if [ ! -f .env ]; then
    echo "==> Generating .env with random secrets..."
    ENV_FILE="$SCRIPT_DIR/.env" ./generate-env.sh
fi

echo "==> Starting services..."
docker compose -f docker-compose.yml --env-file .env up -d

URL="$(grep -E '^APP_PUBLIC_URL=' .env | cut -d= -f2)"
echo ""
echo "==> Done! Open: ${URL}"
