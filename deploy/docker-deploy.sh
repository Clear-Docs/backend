#!/bin/bash
set -e

# Docker must be installed on the server
if ! command -v docker &>/dev/null; then
  echo "Error: Docker is not installed. Install it first:"
  echo "  sudo apt update && sudo apt install -y docker.io docker-compose-plugin"
  echo "  sudo usermod -aG docker \$USER"
  exit 1
fi
if ! docker compose version &>/dev/null; then
  echo "Error: docker compose is not available. Install docker-compose-plugin."
  exit 1
fi

DEPLOY_PATH="${DEPLOY_PATH:-/opt/cleardocs-backend}"
mkdir -p "$DEPLOY_PATH"
cd "$DEPLOY_PATH"

# Login to ghcr.io if token provided (for private packages)
if [ -n "${GHCR_TOKEN}" ]; then
  echo "$GHCR_TOKEN" | docker login ghcr.io -u "$GITHUB_ACTOR" --password-stdin
fi

# Create .env from secrets
cat > .env << EOF
DB_PASSWORD=${DB_PASSWORD}
CORS_ALLOWED_ORIGINS=${CORS_ALLOWED_ORIGINS}
EOF

# Create Firebase key file
printf '%s' "$FIREBASE_SERVICE_ACCOUNT_JSON" > firebase-key.json

# Deploy
docker compose pull
docker compose up -d
