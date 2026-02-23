#!/bin/bash
# Curl script to fetch Onyx document-sets API
# Requires ONYX_API_KEY - create in Onyx Admin Console or User Settings

BASE_URL="${ONYX_BASE_URL:-http://155.212.162.11:3000/api}"
MANAGE_PATH="${ONYX_MANAGE_PATH:-/manage}"
DOCUMENT_SETS_PATH="$MANAGE_PATH/document-set"
API_KEY="${ONYX_API_KEY:-}"

if [ -z "$API_KEY" ]; then
  echo "Warning: ONYX_API_KEY not set. Request will likely return 403."
  echo "Export it: export ONYX_API_KEY=your-api-key"
  echo ""
fi

echo "GET $BASE_URL$DOCUMENT_SETS_PATH"
echo "---"

curl -s -w "\n\nHTTP_STATUS:%{http_code}\n" \
  -H "Accept: application/json" \
  -H "Authorization: Bearer $API_KEY" \
  "$BASE_URL$DOCUMENT_SETS_PATH" | jq '.' 2>/dev/null || cat
