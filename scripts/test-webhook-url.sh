#!/usr/bin/env bash
# Проверка доступности URL вебхука Точки снаружи.
# Подставь свой публичный URL API (тот же, что указан в кабинете Точки).
set -e
WEBHOOK_URL="${WEBHOOK_URL:-https://your-api-domain.com/api/v1/pay/webhook/tochka}"
echo "Testing: $WEBHOOK_URL"
HTTP_CODE=$(curl -s -o /tmp/webhook-test-body.txt -w "%{http_code}" -X POST "$WEBHOOK_URL" -H "Content-Type: text/plain" -d "test" --connect-timeout 10 --max-time 15)
echo "HTTP code: $HTTP_CODE"
if [ "$HTTP_CODE" = "200" ]; then
  echo "OK — endpoint is reachable and returns 200. Tочka can send webhooks here."
else
  echo "Expected 200. Check: URL, HTTPS, firewall, nginx proxy."
  cat /tmp/webhook-test-body.txt 2>/dev/null | head -5
fi
