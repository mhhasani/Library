#!/usr/bin/env bash
set -e

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"

echo "==> Loading images..."
docker load -i "$SCRIPT_DIR/postgres.tar"
docker load -i "$SCRIPT_DIR/app.tar"
docker load -i "$SCRIPT_DIR/frontend.tar"

if [ ! -f "$SCRIPT_DIR/.env" ]; then
  cp "$SCRIPT_DIR/.env.example" "$SCRIPT_DIR/.env"
  echo ""
  echo "  .env file created from .env.example"
  echo "  Edit $SCRIPT_DIR/.env to set your passwords before continuing."
  echo ""
  read -r -p "Press Enter when ready..."
fi

echo "==> Starting services..."
docker compose -f "$SCRIPT_DIR/docker-compose.yml" --env-file "$SCRIPT_DIR/.env" up -d

echo ""
echo "==> Done! App is running at:"
echo "    Frontend : http://localhost:3000"
echo "    API      : http://localhost:8080/api"
echo "    Swagger  : http://localhost:8080/api/swagger-ui.html"
