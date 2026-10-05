#!/usr/bin/env bash
# Builds the offline deployment bundle (run on a machine with internet access).
#
# Integrity: every file in the bundle is listed in SHA256SUMS. If GPG_SIGNING_KEY is set,
# SHA256SUMS is also signed (SHA256SUMS.asc) so the target site can verify the publisher.
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
ROOT_DIR="$SCRIPT_DIR/.."
BUNDLE_DIR="$ROOT_DIR/bundle"
APP_IMAGE="library-management-app:latest"
FRONTEND_IMAGE="library-management-frontend:latest"
POSTGRES_IMAGE="postgres:16-alpine"

echo "==> Creating bundle directory..."
rm -rf "$BUNDLE_DIR"
mkdir -p "$BUNDLE_DIR/deploy/postgres"

echo "==> Building images..."
docker build -t "$APP_IMAGE" "$ROOT_DIR"
docker build -t "$FRONTEND_IMAGE" "$ROOT_DIR/frontend"
docker pull "$POSTGRES_IMAGE"

echo "==> Saving images (this may take a while)..."
docker save "$APP_IMAGE" -o "$BUNDLE_DIR/app.tar"
docker save "$FRONTEND_IMAGE" -o "$BUNDLE_DIR/frontend.tar"
docker save "$POSTGRES_IMAGE" -o "$BUNDLE_DIR/postgres.tar"

echo "==> Copying run files..."
cp "$ROOT_DIR/docker-compose.release.yml" "$BUNDLE_DIR/docker-compose.yml"
cp "$ROOT_DIR/.env.example" "$BUNDLE_DIR/.env.example"
cp "$ROOT_DIR/deploy/postgres/"* "$BUNDLE_DIR/deploy/postgres/"
cp "$SCRIPT_DIR/generate-env.sh" "$BUNDLE_DIR/generate-env.sh"
cp "$SCRIPT_DIR/import-and-run.sh" "$BUNDLE_DIR/run.sh"
chmod +x "$BUNDLE_DIR/run.sh" "$BUNDLE_DIR/generate-env.sh"

echo "==> Writing checksums..."
(cd "$BUNDLE_DIR" && find . -type f ! -name 'SHA256SUMS*' -print0 | sort -z \
    | xargs -0 sha256sum > SHA256SUMS)
if [ -n "${GPG_SIGNING_KEY:-}" ]; then
    gpg --batch --yes --local-user "$GPG_SIGNING_KEY" --armor --detach-sign \
        --output "$BUNDLE_DIR/SHA256SUMS.asc" "$BUNDLE_DIR/SHA256SUMS"
    echo "    Signed with key $GPG_SIGNING_KEY"
fi

echo ""
echo "==> Bundle ready at: $BUNDLE_DIR"
ls -lh "$BUNDLE_DIR"
echo ""
echo "    Compress with:  tar -czf library-bundle.tar.gz bundle/"
echo "    Publish the SHA-256 of the archive through a separate channel:"
echo "    sha256sum library-bundle.tar.gz"
