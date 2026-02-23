#!/bin/bash
set -e

echo "==> Deploy starting..."
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
echo "==> DEPLOY_PATH=$DEPLOY_PATH"
mkdir -p "$DEPLOY_PATH"
cd "$DEPLOY_PATH"

# Validate required secrets
for var in DB_PASSWORD FIREBASE_SERVICE_ACCOUNT_JSON CORS_ALLOWED_ORIGINS; do
  if [ -z "${!var}" ]; then
    echo "Error: Required secret $var is not set"
    exit 1
  fi
done

# Login to ghcr.io if token provided (for private packages)
if [ -n "${GHCR_TOKEN}" ]; then
  echo "$GHCR_TOKEN" | docker login ghcr.io -u "$GITHUB_ACTOR" --password-stdin
fi

# Create .env from secrets
cat > .env << EOF
DB_PASSWORD=${DB_PASSWORD}
CORS_ALLOWED_ORIGINS=${CORS_ALLOWED_ORIGINS}
ONYX_API_KEY=${ONYX_API_KEY}
EOF

# Create Firebase key file
echo "==> Creating firebase-key.json..."
if [ -z "${FIREBASE_SERVICE_ACCOUNT_JSON}" ]; then
  echo "Error: FIREBASE_SERVICE_ACCOUNT_JSON is empty"
  exit 1
fi
printf '%s' "$FIREBASE_SERVICE_ACCOUNT_JSON" > firebase-key.json

# Deploy
echo "==> Pulling images..."
if ! docker compose pull; then
  echo "Error: docker compose pull failed. Check: image exists, GHCR_TOKEN set if private."
  exit 1
fi

echo "==> Starting containers..."
if ! docker compose up -d; then
  echo "Error: docker compose up failed. Container logs:"
  docker compose logs --tail=50
  exit 1
fi

echo "==> Deploy complete"
docker compose ps
