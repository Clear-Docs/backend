#!/bin/bash
set -e

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
