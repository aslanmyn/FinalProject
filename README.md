# KBTU Portal

University portal with three roles: `STUDENT`, `PROFESSOR`, `ADMIN`.

## Run (Docker)

```bash
docker compose up -d --build
```

If port `8080` is busy:

```bash
APP_HOST_PORT=8081 docker compose up -d --build
```

Stop:

```bash
docker compose down
```

## Environment Variables

Required for production:
- `DB_URL`
- `DB_USER`
- `DB_PASSWORD`
- `JWT_SECRET`
- `APP_STORAGE_SIGNING_SECRET`

Optional:
- `APP_PROFILE` (`postgres`)
- `APP_SEED_ENABLED` (`false` for production)
- `APP_STORAGE_ROOT`

## API

Detailed API routes and examples: `API_DOCUMENTATION.md`.
