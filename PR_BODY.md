## Описание

Ветка добавляет интеграцию с Onyx API и Connectors API, а также улучшает деплой через Docker.

## Изменения

### Connectors API
- Добавлен `ConnectorController` с эндпоинтом `GET /api/v1/connectors`
- `ConnectorService` получает коннекторы пользователя через Onyx API по `docSetId`
- Добавлены DTO: `GetConnectorsDto`, `EntityConnectorDto`, `OnyxDocumentSetDto`, `OnyxConnectorSummaryDto`

### Onyx API Integration
- Добавлен `OnyxClient` для работы с Onyx API (document-sets)
- Настраивается через `onyx.base-url`, `onyx.manage-path`, `onyx.api-key`
- Миграция V3: добавлено поле `doc_set_id` в таблицу `users`

### Docker & Deploy
- Добавлены `Dockerfile`, `docker-compose.yml`, `.dockerignore`
- Скрипт `deploy/docker-deploy.sh` для деплоя с системой
- CI workflow `.github/workflows/ci.yml` (сборка, тесты, деплой)

### Прочее
- Локальная конфигурация: `application-local.properties`, `LocalFirebaseConfig`
- Тесты: `ConnectorControllerTest`, мок `WithMockFirebaseUser`, `TestFirebaseConfig`

## Тестирование

- [x] Unit-тесты добавлены для ConnectorController, UserController, PlanController
- [x] Локальная разработка с application-local.properties
