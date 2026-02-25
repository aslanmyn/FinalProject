# KBTU Portal

Учебный портал с ролями `STUDENT`, `PROFESSOR`, `ADMIN`.

## Запуск (Docker)

```bash
docker compose up -d --build
```

Если порт 8080 занят:

```bash
APP_HOST_PORT=8081 docker compose up -d --build
```

Остановить:

```bash
docker compose down
```

## Конфигурация (env)

Обязательные для продакшена переменные:
- `DB_URL`
- `DB_USER`
- `DB_PASSWORD`
- `JWT_SECRET`
- `APP_STORAGE_SIGNING_SECRET`

Опционально:
- `APP_PROFILE` (`postgres`)
- `APP_SEED_ENABLED` (`false` для production)
- `APP_STORAGE_ROOT`

## API

Подробные API-роуты и примеры: `API_DOCUMENTATION.md`.
