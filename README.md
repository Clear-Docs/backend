# Clear-Docs Backend

## Onyx API

Base URL: http://155.212.162.11:3000/api/

Используется для получения document sets и коннекторов. Endpoint document sets возвращает список всех doc sets; backend фильтрует по `docSetId` пользователя и маппит `cc_pair_summaries` в список коннекторов.

Конфигурация в `application.properties`:
- `onyx.base-url` — base URL
- `onyx.manage-path` — базовый путь для всех Onyx endpoints (по умолчанию `/manage`, пути: `/document-set`, `/admin/document-set`, `/admin/connector/...`)
- `onyx.api-key` — Bearer API key (или через `ONYX_API_KEY` в окружении)
