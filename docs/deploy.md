# Деплой и CI

## GitHub Secrets

Добавь в **Settings → Secrets and variables → Actions** репозитория:

| Секрет | Обязательный | Описание |
|--------|--------------|----------|
| `SSH_HOST` | ✅ | IP сервера |
| `SSH_USER` | ✅ | Пользователь для SSH (например `ubuntu`, `root`, `deploy`) |
| `SSH_PRIVATE_KEY` | ✅ | Приватный SSH-ключ (содержимое `~/.ssh/id_rsa` или аналог) |
| `SSH_PORT` | ❌ | Порт SSH (по умолчанию `22`) |
| `DEPLOY_PATH` | ❌ | Директория на сервере (по умолчанию `/opt/cleardocs-backend`) |
| `DB_PASSWORD` | ✅ | Пароль PostgreSQL |
| `FIREBASE_SERVICE_ACCOUNT_JSON` | ✅ | Содержимое JSON-ключа Firebase (весь файл целиком) |
| `CORS_ALLOWED_ORIGINS` | ✅ | Разрешённые origins (например `https://app.cleardocs.ru`) |
| `GHCR_TOKEN` | ❌ | PAT с `read:packages` — только если образ в ghcr.io приватный |

### Как получить SSH_PRIVATE_KEY

```bash
cat ~/.ssh/id_rsa
```

Скопируй вывод целиком (включая `-----BEGIN ... KEY-----` и `-----END ... KEY-----`) и вставь в секрет.

## Поведение CI

- **При создании/обновлении PR** — запускаются тесты (`mvn test`)
- **При мерже в main/master** — тесты → сборка JAR → Docker build → push в ghcr.io → deploy на сервер (docker compose up)

## Настройка сервера

**Docker должен быть установлен на сервере до первого деплоя.**

1. Установи Docker и Docker Compose:

```bash
# Ubuntu/Debian
sudo apt update && sudo apt install -y docker.io docker-compose-plugin
sudo usermod -aG docker $USER
# Перелогинься или выполни: newgrp docker
```

2. Добавь пользователя `SSH_USER` в группу docker (чтобы он мог запускать контейнеры без root):

```bash
sudo usermod -aG docker SSH_USER
# Замени SSH_USER на реальное имя (ubuntu, deploy и т.п.)
```

3. Первый деплой создаст PostgreSQL в контейнере. Flyway выполнит миграции автоматически при старте backend.

## Вебхук Точка Банк

Эндпоинт приёма колбэков: **POST** `https://<домен-api>/api/v1/pay/webhook/tochka` (тело — JWT, `Content-Type: text/plain`). Чтобы Точка начала слать уведомления об оплатах, вебхук нужно зарегистрировать.

### Регистрация вебхука через API

URL вебхука захардкожен в коде (`TochkaPaymentService.DEFAULT_WEBHOOK_URL`). Если сменится домен API — поменять константу и задеплоить.

1. У JWT Точки (TOCHKA_API_KEY) должно быть разрешение **ManageWebhookData** (выдаётся в кабинете Точки при настройке интеграции).
2. Вызови один раз (с авторизацией Firebase): **POST** `/api/v1/pay/tochka/register-webhook`. В ответе 200 и `{"message":"Webhook registered"}` — вебхук создан в Точке с событием `acquiringInternetPayment`.

Если вернулось 403 — у токена нет ManageWebhookData. После успешной регистрации повторный вызов может вернуть ошибку от Точки (вебхук уже существует); при необходимости используй Edit/Delete в кабинете или API Точки.
