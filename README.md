# Captures Converter

React/Vite frontend + Spring Boot/Maven/PostgreSQL backend.

## Запуск

```bash
docker compose up --build
```

For Docker admin account seeding, set `ADMIN_EMAIL` and `ADMIN_PASSWORD` in the same shell before starting Compose. On PowerShell:

```powershell
$env:ADMIN_EMAIL = "admin@example.com"
$env:ADMIN_PASSWORD = "your-strong-password"
docker compose up --build
```

The admin account is created only when the configured email does not already exist in the database.

Frontend: `http://localhost:5173` (локальный Vite), backend: `http://localhost:8080`,
Swagger: `http://localhost:8080/swagger-ui.html`.

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

Проект запускается без Docker и контейнеров.

## Environment

| Variable | Default |
|---|---|
| `SPRING_DATASOURCE_URL` | `jdbc:postgresql://localhost:5432/captures` |
| `SPRING_DATASOURCE_USERNAME` | `captures` |
| `SPRING_DATASOURCE_PASSWORD` | `captures` |
| `JWT_SECRET` | development value (заменить в production) |
| `JWT_ACCESS_MINUTES` | `15` |
| `JWT_REFRESH_DAYS` | `30` |
| `CORS_ORIGINS` | `http://localhost:5173` |
| `MAX_UPLOAD_SIZE` | `500MB` |
| `MAX_REQUEST_SIZE` | `500MB` |
| `VITE_API_URL` | empty (Vite `/api` proxy in development) |
| `ADMIN_EMAIL` | empty (seed disabled) |
| `ADMIN_PASSWORD` | empty (seed disabled) |

## Лимиты

Гость: 10 обработок за дату гостевого ключа (`X-Guest-Key`) и максимум 50 MB на один файл.
Клиент: максимум 150 обработок за UTC-день, максимум 500 MB на один файл и суммарно 500 MB исходных файлов за тот же день.
Размер 500 MB — сумма исходных файлов, а не одновременный размер batch. Админ без обычных лимитов.
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
```

Admin statistics include total conversions and data volume, system/process CPU load, JVM heap, host memory when supported, uptime, thread count, and runtime details. The admin dashboard refreshes these metrics every 5 seconds; the endpoint requires the `ADMIN` role.

Refresh:

```bash
curl -X POST http://localhost:8080/api/auth/refresh \
  -H 'Content-Type: application/json' -d '{"refreshToken":"REFRESH_TOKEN"}'
```

Authenticated users can view and update their profile name via `GET /api/auth/me` and `PATCH /api/auth/profile` with `{"displayName":"Your name"}`. The admin server metrics are shown in the profile dialog.

## Форматы

PNG/JPG/GIF/TIFF decode/encode поддерживаются стандартной Java ImageIO; TIFF дополнительно TwelveMonkeys.
WEBP поддерживается TwelveMonkeys плагином.
HEIC и AVIF на выходе явно возвращают HTTP 415, если подходящего native ImageIO writer отсутствует.
AVIF input не гарантируется: сервер принимает файл только если зарегистрирован совместимый reader.
Пакетная обработка выполняется множественными multipart-запросами с теми же options.
