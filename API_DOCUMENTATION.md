# API Documentation — KBTU Portal

REST API для мобильного приложения. Базовый URL: `http://localhost:8080`

## Аутентификация

### JWT (рекомендуется для API v1)

1. Получить токен:
```http
POST /api/v1/auth/login
Content-Type: application/json

{"email": "a_mustafayev@kbtu.kz", "password": "student123"}
```

2. Использовать в заголовке:
```http
Authorization: Bearer <token>
```

### Сессия (legacy /api/student, /api/professor, /api/admin)

Логин через `/do-login`, затем передавать cookie `JSESSIONID`.

---

## API v1 (JWT) — Student

| Метод | Путь | Описание |
|-------|------|----------|
| GET | /api/v1/student/profile | Профиль студента |
| GET | /api/v1/student/schedule | Расписание |
| GET | /api/v1/student/journal | Журнал оценок |
| GET | /api/v1/student/transcript | Транскрипт, GPA |
| GET | /api/v1/student/attendance | Посещаемость |
| GET | /api/v1/student/financial | Финансы, баланс |
| GET | /api/v1/student/holds | Активные holds |
| GET | /api/v1/student/exam-schedule | Расписание экзаменов |
| GET | /api/v1/student/news | Новости |
| GET | /api/v1/student/announcements | Объявления (пагинация) |
| GET | /api/v1/student/notifications | Уведомления |
| GET | /api/v1/student/materials/{sectionId} | Материалы курса |
| GET | /api/v1/student/checklist | Чеклист |
| GET | /api/v1/student/mobility | Заявки на мобильность |
| GET | /api/v1/student/clearance | Clearance |
| GET | /api/v1/student/enrollments | Записи на курсы |
| GET | /api/v1/student/course-registration/available | Доступные курсы |
| POST | /api/v1/student/course-registration/submit | Отправить регистрацию |
| POST | /api/v1/student/add-drop/add | Добавить курс |
| POST | /api/v1/student/add-drop/drop | Отказаться от курса |
| GET | /api/v1/student/requests | Заявки (пагинация) |
| POST | /api/v1/student/requests | Создать заявку |
| GET | /api/v1/student/requests/{id}/messages | Сообщения заявки |
| POST | /api/v1/student/requests/{id}/messages | Добавить сообщение |
| POST | /api/v1/student/notifications/{id}/read | Отметить прочитанным |

---

## API v1 (JWT) — Teacher

| Метод | Путь | Описание |
|-------|------|----------|
| GET | /api/v1/teacher/profile | Профиль преподавателя |
| GET | /api/v1/teacher/sections | Секции курсов |
| GET | /api/v1/teacher/sections/{id}/roster | Список студентов |
| POST | /api/v1/teacher/sections/{id}/attendance | Отметить посещаемость |
| GET | /api/v1/teacher/sections/{id}/components | Компоненты оценки |
| POST | /api/v1/teacher/sections/{id}/components | Создать компонент |
| POST | /api/v1/teacher/sections/{id}/components/{cid}/publish | Опубликовать/скрыть |
| POST | /api/v1/teacher/sections/{id}/components/{cid}/lock | Заблокировать |
| POST | /api/v1/teacher/sections/{id}/grades | Сохранить оценку |
| POST | /api/v1/teacher/sections/{id}/final-grades | Сохранить итоговую оценку |
| POST | /api/v1/teacher/sections/{id}/final-grades/{sid}/publish | Опубликовать итог |
| GET | /api/v1/teacher/sections/{id}/announcements | Объявления |
| POST | /api/v1/teacher/sections/{id}/announcements | Создать объявление |
| GET | /api/v1/teacher/sections/{id}/materials | Материалы |
| POST | /api/v1/teacher/sections/{id}/materials | Загрузить материал |
| POST | /api/v1/teacher/materials/{id}/visibility | Изменить видимость |
| DELETE | /api/v1/teacher/materials/{id} | Удалить материал |
| GET | /api/v1/teacher/sections/{id}/student-notes | Заметки по студентам |
| POST | /api/v1/teacher/sections/{id}/student-notes | Добавить заметку |
| GET | /api/v1/teacher/grade-change-requests | Запросы на изменение оценок |
| POST | /api/v1/teacher/sections/{id}/grade-change-requests | Создать запрос |

---

## API v1 (JWT) — Admin

Требуются права: REGISTRAR, FINANCE, SUPPORT, CONTENT, MOBILITY, SUPER.

| Метод | Путь | Право | Описание |
|-------|------|-------|----------|
| GET | /api/v1/admin/users | SUPER | Пользователи (пагинация) |
| POST | /api/v1/admin/users/{id}/permissions | SUPER | Права админа |
| GET | /api/v1/admin/terms | REGISTRAR | Семестры |
| POST | /api/v1/admin/terms | REGISTRAR | Создать семестр |
| GET | /api/v1/admin/sections | REGISTRAR | Секции |
| POST | /api/v1/admin/sections | REGISTRAR | Создать секцию |
| POST | /api/v1/admin/sections/{id}/assign-professor | REGISTRAR | Назначить преподавателя |
| POST | /api/v1/admin/sections/{id}/meeting-times | REGISTRAR | Добавить время занятий |
| POST | /api/v1/admin/windows | REGISTRAR | Окна регистрации |
| POST | /api/v1/admin/enrollments/override | REGISTRAR | Переопределить запись |
| GET | /api/v1/admin/exams | REGISTRAR | Экзамены |
| POST | /api/v1/admin/exams | REGISTRAR | Создать экзамен |
| PUT | /api/v1/admin/exams/{id} | REGISTRAR | Обновить экзамен |
| DELETE | /api/v1/admin/exams/{id} | REGISTRAR | Удалить экзамен |
| GET | /api/v1/admin/holds | FINANCE | Holds |
| POST | /api/v1/admin/holds | FINANCE | Создать hold |
| POST | /api/v1/admin/holds/{id}/remove | FINANCE | Снять hold |
| POST | /api/v1/admin/finance/invoices | FINANCE | Создать счёт |
| POST | /api/v1/admin/finance/payments | FINANCE | Зарегистрировать платёж |
| GET | /api/v1/admin/mobility | MOBILITY | Заявки на мобильность |
| POST | /api/v1/admin/mobility/{id}/status | MOBILITY | Изменить статус |
| GET | /api/v1/admin/clearance | MOBILITY | Clearance |
| POST | /api/v1/admin/clearance/checkpoints/{id}/review | MOBILITY | Проверить checkpoint |
| GET | /api/v1/admin/surveys | CONTENT | Опросы |
| POST | /api/v1/admin/surveys | CONTENT | Создать опрос |
| POST | /api/v1/admin/surveys/{id}/close | CONTENT | Закрыть опрос |
| GET | /api/v1/admin/surveys/{id}/responses | CONTENT | Ответы опроса |
| GET | /api/v1/admin/requests | SUPPORT | Заявки |
| POST | /api/v1/admin/requests/{id}/assign | SUPPORT | Назначить |
| POST | /api/v1/admin/requests/{id}/status | SUPPORT | Обновить статус |
| GET | /api/v1/admin/grade-change-requests | REGISTRAR | Запросы на изменение оценок |
| POST | /api/v1/admin/grade-change-requests/{id}/review | REGISTRAR | Одобрить/отклонить |
| POST | /api/v1/admin/news | CONTENT | Создать новость |
| GET | /api/v1/admin/checklist-templates | REGISTRAR | Шаблоны чеклиста |
| POST | /api/v1/admin/checklist-templates | REGISTRAR | Создать шаблон |
| POST | /api/v1/admin/checklist/generate | REGISTRAR | Сгенерировать чеклисты |
| GET | /api/v1/admin/audit | SUPER | Аудит-логи |
| GET | /api/v1/admin/stats | ADMIN | Статистика |

---

## Формат ответов

### Успех (200)
JSON с данными.

### Пагинация
```json
{
  "content": [...],
  "totalElements": 100,
  "totalPages": 5,
  "number": 0,
  "size": 20
}
```

### Ошибка (ApiError)
```json
{
  "code": "BAD_REQUEST",
  "message": "Student not found",
  "details": {}
}
```

Коды: `UNAUTHORIZED`, `FORBIDDEN`, `BAD_REQUEST`, `STATE_CONFLICT`, `INTERNAL_ERROR`.

---

## Коды HTTP

| Код | Описание |
|-----|----------|
| 200 | Успех |
| 400 | Неверные параметры |
| 401 | Не авторизован |
| 403 | Нет прав |
| 404 | Не найдено |
| 409 | Конфликт состояния |
| 500 | Ошибка сервера |
