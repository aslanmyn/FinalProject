# KBTU University Portal

Университетский портал с ролями: **Admin**, **Professor**, **Student**. Web-интерфейс (Thymeleaf) и REST API для мобильного приложения.

## Технологии

- Java 17, Spring Boot 4.0.2
- Spring Security, JWT для API
- Spring Data JPA / Hibernate
- Thymeleaf, Maven
- H2 (по умолчанию) или PostgreSQL

## Запуск

```bash
.\mvnw.cmd spring-boot:run
```

Приложение: **http://localhost:8080**

## Тестовые аккаунты

| Роль      | Email               | Пароль    |
|-----------|---------------------|-----------|
| Admin     | admin@kbtu.kz       | admin123  |
| Professor | z.professor@kbtu.kz | prof123   |
| Student   | a_mustafayev@kbtu.kz| student123|

## Основные URL

| Раздел        | URL                          |
|---------------|------------------------------|
| Вход          | /login                       |
| Регистрация   | /register                   |
| Новости       | /news                        |
| Админ         | /admin/dashboard             |
| Профессор     | /professor/dashboard        |
| Студент       | /portal/*                    |
| H2 Console    | /h2-console                  |

## Роли и возможности

**Student** — профиль, расписание, оценки, посещаемость, финансы, заявки, материалы курсов, чеклист, мобильность, clearance.

**Professor** — секции курсов, журнал, посещаемость, gradebook, объявления, материалы, запросы на изменение оценок.

**Admin** — академическая настройка, экзамены, финансы, holds, заявки, контент, мобильность, clearance, пользователи, аудит. Права: REGISTRAR, FINANCE, SUPPORT, CONTENT, MOBILITY, SUPER.

## Структура проекта

```
src/main/java/ru/kors/finalproject/
├── config/           — SecurityConfig, JwtFilter, DataInitializer
├── entity/           — JPA-сущности (43 шт.)
├── model/            — PortalSection (модель портала)
├── repository/       — JPA-репозитории
├── service/          — бизнес-логика
├── controller/       — веб-контроллеры
│   └── api/          — REST API
│       └── v1/       — API v1 (JWT)
└── web/api/v1/       — ApiError, ApiPageResponse, обработка ошибок API
```

## API

REST API v1: `/api/v1/**` — аутентификация по JWT Bearer.

Подробнее: **API_DOCUMENTATION.md**

## Конфигурация

`application.properties`:
- H2: `jdbc:h2:mem:testdb`
- JWT: `app.jwt.secret`, `app.jwt.expiration-minutes`

## Сборка

```bash
.\mvnw.cmd clean package
java -jar target/finalProject-0.0.1-SNAPSHOT.jar
```
