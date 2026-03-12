#!/usr/bin/env bash
# Проверка вебхуков в Точке: запрос к API Get Webhooks.
# Нужны TOCHKA_API_KEY и TOCHKA_CLIENT_ID (или tochka.api-key / tochka.client-id из .env).
set -e
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
BACKEND_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
if [ -f "$BACKEND_DIR/.env" ]; then
  set -a
  # shellcheck source=/dev/null
  source "$BACKEND_DIR/.env"
  set +a
fi
API_KEY="${TOCHKA_API_KEY:-}"
CLIENT_ID="${TOCHKA_CLIENT_ID:-}"
if [ -z "$API_KEY" ] || [ -z "$CLIENT_ID" ]; then
  echo "Set TOCHKA_API_KEY and TOCHKA_CLIENT_ID (or add to .env)."
  echo "Example: export TOCHKA_API_KEY='eyJ...'; export TOCHKA_CLIENT_ID='0d61fb10...'; $0"
  exit 1
fi
URL="https://enter.tochka.com/uapi/webhook/v1.0/$CLIENT_ID"
echo "GET $URL"
curl -s -w "\n\nHTTP_CODE:%{http_code}\n" -H "Authorization: Bearer $API_KEY" "$URL" | tee /tmp/tochka-webhooks-response.txt
