#!/usr/bin/env bash
set -e

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
BUNDLE_DIR="$SCRIPT_DIR/../bundle"
APP_IMAGE="library-management-app:latest"
FRONTEND_IMAGE="library-management-frontend:latest"

echo "==> Creating bundle directory..."
mkdir -p "$BUNDLE_DIR"

echo "==> Building app image..."
docker build -t "$APP_IMAGE" "$SCRIPT_DIR/.."

echo "==> Building frontend image..."
docker build -t "$FRONTEND_IMAGE" "$SCRIPT_DIR/../frontend"

echo "==> Pulling postgres image..."
docker pull postgres:16-alpine

echo "==> Saving images (this may take a while)..."
docker save "$APP_IMAGE" -o "$BUNDLE_DIR/app.tar"
docker save "$FRONTEND_IMAGE" -o "$BUNDLE_DIR/frontend.tar"
docker save postgres:16-alpine -o "$BUNDLE_DIR/postgres.tar"

echo "==> Copying run files..."
cp "$SCRIPT_DIR/../docker-compose.release.yml" "$BUNDLE_DIR/docker-compose.yml"
cp "$SCRIPT_DIR/../.env.example" "$BUNDLE_DIR/.env.example"
cp "$SCRIPT_DIR/import-and-run.sh" "$BUNDLE_DIR/run.sh"
chmod +x "$BUNDLE_DIR/run.sh"

echo ""
echo "==> Bundle ready at: $BUNDLE_DIR"
echo "    Files:"
ls -lh "$BUNDLE_DIR"
echo ""
echo "    Compress with:"
echo "    tar -czf library-bundle.tar.gz bundle/"
