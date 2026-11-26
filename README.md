# Конвертер изображений Captures

Веб-приложение для конвертации изображений. Клиентская часть создана на React и Vite, серверная — на Spring Boot и Maven, для хранения данных используется PostgreSQL.

## Запуск в Docker

```powershell
Copy-Item .env.example .env
# Заполните секретные переменные в файле .env
docker compose up --build
```

Docker Compose запускает PostgreSQL, серверную часть на Spring Boot и собранный интерфейс на React.
Создаваемая база данных пуста: данные из PostgreSQL, установленного на компьютере, не копируются.
Данные контейнера сохраняются в томе `captures-db-data`. Порт базы данных не публикуется на компьютере,
поэтому контейнер не конфликтует с локальным PostgreSQL. Команда `docker compose down` сохраняет данные,
а `docker compose down -v` удаляет том вместе с базой.

Интерфейс: `http://localhost:5173`, сервер API: `http://localhost:8080`,
документация API Swagger: `http://localhost:8080/swagger-ui.html`.

Чтобы создать учётную запись администратора, укажите `ADMIN_EMAIL` и `ADMIN_PASSWORD`
в файле `.env`. Не публикуйте этот файл и не добавляйте его в Git. Пример для PowerShell:

```powershell
$env:ADMIN_EMAIL = "admin@example.com"
$env:ADMIN_PASSWORD = "надежный-пароль"
docker compose up --build
```

Учётная запись администратора создаётся, только если указанный адрес ещё не зарегистрирован в базе данных.

## Локальный запуск интерфейса

```bash
npm install
npm run dev
```

Vite перенаправляет запросы `/api` на `http://localhost:8080`, поэтому при локальной разработке
дополнительная настройка CORS не нужна. Для рабочей среды укажите в `VITE_API_URL` адрес сервера API.

## Локальный запуск сервера без Docker

```bash
cd backend
mvn spring-boot:run
```

Для запуска PostgreSQL должен быть доступен, а параметры подключения — заданы в `application.yml`.

Интерфейс: `http://localhost:5173`, сервер API: `http://localhost:8080`,
документация API Swagger: `http://localhost:8080/swagger-ui.html`.

## Переменные окружения

| Переменная | Значение или описание |
|---|---|
| `SPRING_DATASOURCE_URL` | В Docker: `jdbc:postgresql://db:5432/captures` |
| `SPRING_DATASOURCE_USERNAME` | `postgres` |
| `SPRING_DATASOURCE_PASSWORD` | Значение `POSTGRES_PASSWORD` из `.env` |
| `POSTGRES_DB` | `captures` |
| `POSTGRES_USER` | `postgres` |
| `POSTGRES_PASSWORD` | Обязательная переменная в `.env` |
| `JWT_SECRET` | Обязательная переменная в `.env`; задайте уникальное значение |
| `JWT_ACCESS_MINUTES` | `15` |
| `JWT_REFRESH_DAYS` | `30` |
| `CORS_ORIGINS` | `http://localhost:5173` |
| `MAX_UPLOAD_SIZE` | `500MB` |
| `MAX_REQUEST_SIZE` | `500MB` |
| `VITE_API_URL` | Пустое значение: в режиме разработки используется прокси Vite `/api` |
| `ADMIN_EMAIL` | `admin@example.com` |
| `ADMIN_PASSWORD` | Необязательная переменная; задайте её в `.env`, чтобы создать администратора |

## Проверка API

Набор тестов pytest проверяет конечные точки контроллеров на запущенном сервере:
регистрацию и вход в аккаунт, обновление токена, профиль, квоты, конвертацию изображений,
скачивание файлов, историю и статистику администратора.

```powershell
python -m pip install -r requirements-test.txt
python -m pytest
```

По умолчанию тесты обращаются к `http://localhost:8080`. Чтобы проверить конечные точки
администратора, задайте в той же командной оболочке переменные `API_BASE_URL`, `ADMIN_EMAIL`
и `ADMIN_PASSWORD`. Если `ADMIN_PASSWORD` не задан, тесты функций администратора пропускаются.

## Ограничения и дополнительные квоты

Гость может выполнить 10 обработок за период действия гостевого ключа (`X-Guest-Key`).
Размер одного файла не должен превышать 50 МБ.
Авторизованный пользователь может выполнить до 150 обработок в день по времени UTC.
Размер одного файла и суммарный размер исходных файлов за день не должны превышать 500 МБ.
Суммарный лимит учитывает размер исходных файлов, а не размер всего пакета в памяти.
На администратора обычные ограничения не распространяются.

Администратор может на 24 часа увеличить квоту пользователя по количеству обработок и/или объёму данных.
Дополнительные квоты суммируются с ещё действующими бонусами; после истечения срока стандартные ограничения восстанавливаются.
Исходные и обработанные изображения хранятся в PostgreSQL через JPA (`@Lob`, тип `bytea`); файловая система сервера не используется.

## Примеры запросов к API

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

Статистика администратора содержит общее число конвертаций и объём данных, загрузку процессора,
использование памяти JVM и хоста (если данные доступны), время работы сервера, число потоков и сведения
о среде выполнения. Панель администратора обновляет показатели каждые 5 секунд. Для доступа к статистике
нужна роль `ADMIN`.

Запрос `GET /api/admin/users` возвращает список зарегистрированных пользователей.
Запрос `POST /api/admin/users/{id}/bonus` выдаёт дополнительные квоты на 24 часа.
Для обоих запросов нужна роль `ADMIN`. За один раз можно выдать от 0 до 100 000 обработок
и от 0 до 102 400 МБ; хотя бы одно из значений должно быть больше нуля.

Обновление токена:

```bash
curl -X POST http://localhost:8080/api/auth/refresh \
  -H 'Content-Type: application/json' -d '{"refreshToken":"REFRESH_TOKEN"}'
```

Авторизованный пользователь может посмотреть свои данные через `GET /api/auth/me`
и изменить имя запросом `PATCH /api/auth/profile`, передав `{"displayName":"Ваше имя"}`.
Метрики сервера администратора отображаются в окне профиля.

## Поддерживаемые форматы

Форматы PNG и JPG кодируются средствами Java ImageIO. Форматы WEBP, GIF, TIFF, HEIC и AVIF
кодируются с помощью libvips. Docker-образ сервера включает libvips и модули кодирования HEVC/AV1.
Для локальной конвертации в эти пять форматов установите `vips` и необходимые модули HEIF,
а также добавьте `vips` в переменную `PATH`.
Загружаемые исходные изображения читаются зарегистрированными средствами Java ImageIO.
Загрузка файлов HEIC и AVIF не поддерживается, если в среде нет совместимого средства чтения.
Пакетная обработка выполняется несколькими multipart-запросами с одинаковыми параметрами.
