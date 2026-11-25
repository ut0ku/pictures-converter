# Captures Converter

React/Vite frontend + Spring Boot/Maven/PostgreSQL backend.

## Запуск

```powershell
Copy-Item .env.example .env
# Заполните секретные значения в .env
docker compose up --build
```

Compose starts PostgreSQL, the Spring Boot backend, and the built React frontend.
The new Docker database is empty; it does not import data from a PostgreSQL server
already installed on the host. Its data persists in the `captures-db-data` volume.
The database port is not published to the host, so it does not conflict with a
local PostgreSQL server. `docker compose down` keeps the data; `docker compose down
-v` deletes the database volume.

Frontend: `http://localhost:5173`, backend: `http://localhost:8080`,
Swagger: `http://localhost:8080/swagger-ui.html`.

Set `ADMIN_EMAIL` and `ADMIN_PASSWORD` in `.env` to create the administrator
account. Keep `.env` private and do not commit it. For PowerShell:

```powershell
$env:ADMIN_EMAIL = "admin@example.com"
$env:ADMIN_PASSWORD = "your-strong-password"
docker compose up --build
```

The admin account is created only when the configured email does not already exist
in the database.

Локальный frontend:

```bash
npm install
npm run dev
```

Vite проксирует `/api` на `http://localhost:8080`, поэтому в локальной разработке отдельная настройка CORS не требуется. Для production задайте `VITE_API_URL` адресом backend API.

Локальный backend без Docker:

```bash
cd backend
mvn spring-boot:run
```

Для локального запуска PostgreSQL должен быть доступен по переменным из `application.yml`.

Frontend: `http://localhost:5173`, backend: `http://localhost:8080`,
Swagger: `http://localhost:8080/swagger-ui.html`.

## Environment

| Variable | Default |
|---|---|
| `SPRING_DATASOURCE_URL` | `jdbc:postgresql://db:5432/captures` in Docker |
| `SPRING_DATASOURCE_USERNAME` | `postgres` |
| `SPRING_DATASOURCE_PASSWORD` | `POSTGRES_PASSWORD` from `.env` |
| `POSTGRES_DB` | `captures` |
| `POSTGRES_USER` | `postgres` |
| `POSTGRES_PASSWORD` | required in `.env` |
| `JWT_SECRET` | required in `.env`; use a unique value |
| `JWT_ACCESS_MINUTES` | `15` |
| `JWT_REFRESH_DAYS` | `30` |
| `CORS_ORIGINS` | `http://localhost:5173` |
| `MAX_UPLOAD_SIZE` | `500MB` |
| `MAX_REQUEST_SIZE` | `500MB` |
| `VITE_API_URL` | empty (Vite `/api` proxy in development) |
| `ADMIN_EMAIL` | `admin@example.com` |
| `ADMIN_PASSWORD` | optional; set in `.env` to seed an admin |

## API tests

The pytest suite exercises every controller endpoint against the running backend,
including account registration/login/refresh, profile, quota, image conversion,
download, history, and admin statistics.

```powershell
python -m pip install -r requirements-test.txt
python -m pytest
```

Tests use `http://localhost:8080` by default. Set `API_BASE_URL`, `ADMIN_EMAIL`,
and `ADMIN_PASSWORD` in the same shell to run admin endpoint tests; admin tests
are skipped when `ADMIN_PASSWORD` is not set.

## Лимиты

Гость: 10 обработок за дату гостевого ключа (`X-Guest-Key`) и максимум 50 MB на один файл.
Клиент: максимум 150 обработок за UTC-день, максимум 500 MB на один файл и суммарно 500 MB исходных файлов за тот же день.
Размер 500 MB — сумма исходных файлов, а не одновременный размер batch. Админ без обычных лимитов.
Администратор может выдать клиенту дополнительное число обработок и/или объём на 24 часа. Бонус складывается с активным бонусом, увеличивает дневные лимиты, а после истечения стандартные ограничения восстанавливаются.
Каждое исходное и результирующее изображение хранится в PostgreSQL как `@Lob`/bytea-equivalent JPA data; local filesystem не используется.

## API

```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H 'Content-Type: application/json' \
  -d '{"email":"client@example.com","password":"password123"}'

curl -X POST http://localhost:8080/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"email":"client@example.com","password":"password123"}'

curl -X POST http://localhost:8080/api/convert \
  -H 'Authorization: Bearer ACCESS_TOKEN' \
  -F 'file=@photo.jpg' -F 'format=WEBP' -F 'quality=82' \
  -F 'width=1920' -F 'height=1080'

curl -H 'Authorization: Bearer ACCESS_TOKEN' \
  http://localhost:8080/api/conversions

curl -H 'Authorization: Bearer ADMIN_ACCESS_TOKEN' \
  http://localhost:8080/api/admin/stats

curl -H 'Authorization: Bearer ADMIN_ACCESS_TOKEN' \
  http://localhost:8080/api/admin/users

curl -X POST http://localhost:8080/api/admin/users/USER_ID/bonus \
  -H 'Authorization: Bearer ADMIN_ACCESS_TOKEN' \
  -H 'Content-Type: application/json' \
  -d '{"extraConversions":25,"extraMegabytes":100}'
```

Admin statistics include total conversions and data volume, system/process CPU load, JVM heap, host memory when supported, uptime, thread count, and runtime details. The admin dashboard refreshes these metrics every 5 seconds; the endpoint requires the `ADMIN` role.
`GET /api/admin/users` lists registered clients, and `POST /api/admin/users/{id}/bonus` grants bonus limits for 24 hours. Both endpoints require the `ADMIN` role; each grant accepts 0–100,000 conversions and 0–102,400 MB, with at least one nonzero value.

Refresh:

```bash
curl -X POST http://localhost:8080/api/auth/refresh \
  -H 'Content-Type: application/json' -d '{"refreshToken":"REFRESH_TOKEN"}'
```

Authenticated users can view and update their profile name via `GET /api/auth/me` and `PATCH /api/auth/profile` with `{"displayName":"Your name"}`. The admin server metrics are shown in the profile dialog.

## Форматы

PNG и JPG кодируются через Java ImageIO. WEBP, GIF, TIFF, HEIC и AVIF кодируются через libvips; backend Docker устанавливает libvips и HEVC/AV1 encoder plugins.
Локальный запуск backend для этих пяти форматов требует установленного в `PATH` инструмента `vips` и соответствующих HEIF encoder plugins.
Загрузка источников использует зарегистрированные Java ImageIO readers; HEIC/AVIF input не поддерживается без совместимого reader.
Пакетная обработка выполняется множественными multipart-запросами с теми же options.
