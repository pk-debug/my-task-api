# Task API Project

A backend-focused task management API built in two implementations:

- Java Spring Boot
- Kotlin Ktor

The project demonstrates the same business domain in two common backend styles: a classic enterprise MVC + JPA stack and a lightweight, route-based Kotlin server. The goal is to showcase API design, authentication, database access, JWT handling, task ownership rules, and clean project structure for interviews and learning.

---

## Why this project exists

This repository is designed to answer a common backend interview question:

"Can you build the same API with more than one framework and explain the differences in architecture?"

It includes:

- task CRUD APIs
- email + password authentication
- JWT access tokens
- rotating refresh tokens
- user-scoped task access
- SQLite persistence
- verification and password reset workflows
- a lightweight clean layered structure

---

## Core features

- Create, read, update, and delete tasks
- Validate task payloads and business rules
- Register a new account
- Log in with email and password
- Require email verification before full account use
- Send password reset links with one-time tokens
- Issue short-lived access tokens and rotating refresh tokens
- Revoke refresh tokens on logout or reset
- Restrict task access to the authenticated owner
- Store auth data in SQLite

---

## API summary

| Method | Endpoint | Purpose | Auth |
| --- | --- | --- | --- |
| POST | /auth/register | Create an account | No |
| POST | /auth/login | Log in | No |
| POST | /auth/verify-email | Confirm email with one-time token | No |
| POST | /auth/resend-verification | Re-send verification token | No |
| POST | /auth/forgot-password | Request reset email | No |
| POST | /auth/reset-password | Change password using reset token | No |
| POST | /auth/refresh | Rotate refresh token | No |
| POST | /auth/logout | Revoke refresh token | No |
| GET | /auth/me | Fetch authenticated user profile | Yes |
| GET | /tasks | List current user tasks | Yes |
| GET | /tasks/{id} | Fetch one task by ID | Yes |
| POST | /tasks | Create a task | Yes |
| PUT | /tasks/{id} | Update a task | Yes |
| DELETE | /tasks/{id} | Delete a task | Yes |

Example request bodies:

```json
{
  "email": "dev@example.com",
  "password": "a-secure-password",
  "name": "Dev User"
}
```

```json
{
  "email": "dev@example.com",
  "password": "a-secure-password"
}
```

```json
{
  "title": "Buy groceries",
  "done": false
}
```

---

## Architecture choice

This project follows a thin layered backend style instead of MVVM:

- HTTP layer: controllers/routes
- Application layer: auth and task services
- Domain layer: task/user models and validation rules
- Persistence layer: repositories + SQLite/JPA
- Security layer: JWT verification and bearer-auth enforcement

This is closer to a clean backend architecture than MVVM, which is primarily a UI pattern. It keeps responsibilities separated without over-engineering a small service.

---

## Tech stack

### Spring Boot version
- Java 17
- Spring Boot 4.1.0
- Maven
- Spring Web MVC
- Spring Security
- Spring Data JPA
- Hibernate
- SQLite JDBC
- JWT resource server support

### Ktor version
- Kotlin
- Ktor 3.0.1
- Kotlin Serialization
- Gradle
- Netty
- SQLite JDBC
- BCrypt password hashing
- JWT auth using Ktor auth JWT

---

## Repository structure

```text
my-task-api/
├── README.md
├── pom.xml
├── mvnw
├── mvnw.cmd
├── .mvn/
├── src/
│   └── main/
│       ├── java/
│       │   └── com/example/taskapi/
│       │       ├── HireSpringBootApplication.java
│       │       ├── Task.java
│       │       ├── TaskController.java
│       │       ├── TaskService.java
│       │       ├── TaskRepository.java
│       │       ├── auth/
│       │       └── config/
│       └── resources/
│           └── application.properties
├── target/
├── tasks.db
├── ktor-backend/
│   ├── build.gradle.kts
│   ├── settings.gradle.kts
│   └── src/
│       └── main/
│           └── kotlin/
│               └── com/example/taskapi/
│                   ├── Application.kt
│                   ├── Task.kt
│                   ├── TaskRepository.kt
│                   ├── TaskRoutes.kt
│                   └── auth/
└── src/test/java/com/example/taskapi/DemoApplicationTests.java
```

---

## Quick start

### 1) Spring Boot

From the project root:

```bash
./mvnw spring-boot:run
```

Required environment variables:

```bash
export JWT_SECRET="replace-with-a-random-32-byte-secret"
```

Optional mail settings for real SMTP delivery:

```bash
export MAIL_HOST="smtp.gmail.com"
export MAIL_PORT="587"
export MAIL_USERNAME="you@example.com"
export MAIL_PASSWORD="your-password"
export MAIL_FROM="no-reply@example.com"
export APP_PUBLIC_BASE_URL="http://localhost:8080"
```

If `MAIL_HOST` is not set, the app falls back to a console-style development email sender.

### 2) Ktor

From the Ktor module:

```bash
cd ktor-backend
export JWT_SECRET="replace-with-a-random-32-byte-secret"
export APP_PUBLIC_BASE_URL="http://localhost:8081"
gradle run
```

Optional mail settings:

```bash
export MAIL_HOST="smtp.gmail.com"
export MAIL_PORT="587"
export MAIL_USERNAME="you@example.com"
export MAIL_PASSWORD="your-password"
export MAIL_FROM="no-reply@example.com"
```

---

## Security and auth model

The auth flow is intentionally simple but production-aware:

- password hashing via BCrypt
- JWT access tokens with a short lifetime
- refresh tokens stored as hashes
- refresh-token rotation on use
- logout revocation
- email verification before full login access
- password reset tokens with expiry and one-time use
- user ownership checks for all task operations

Important rule:

- a user can only access tasks they own
- failed or stale refresh tokens return 401 Unauthorized
- verifying or resetting an account invalidates old one-time action tokens

---

## Why two implementations matter

This project is useful for discussing real backend trade-offs:

- Spring Boot is great for enterprise apps, convention-heavy structure, dependency injection, and a mature ecosystem.
- Ktor is great for Kotlin-first, lightweight, expressive server-side apps with route-based architecture.

The business logic remains similar, but the project structure and server style differ significantly.

---

## Validation status

The project has been verified with real test runs:

- Spring Boot test suite passes
- Ktor test suite passes

This makes it a solid example for backend interviews, portfolio work, or learning architecture trade-offs.

---

## Recommended next upgrades

If you want to continue beyond this project, the most valuable production-ready additions are:

1. Role-based authorization (admin/user)
2. SQL-backed task persistence in the Ktor backend
3. Rate limiting and request throttling
4. Structured logging and metrics
5. Docker + PostgreSQL setup
6. CI pipeline and deployment config

---

## Final note

This project is intentionally not just a CRUD app; it demonstrates how a backend should handle auth, token lifecycle, ownership boundaries, and system design choices. That makes it useful both as a learning project and as a portfolio item.

The `@Serializable` annotation allows Kotlin objects to be converted to/from JSON automatically.

### ktor-backend/src/main/kotlin/com/example/taskapi/TaskRepository.kt
This is the in-memory repository used by Ktor for tasks. It scopes operations to the authenticated user ID. Ktor account and refresh-token persistence is separate in `auth/AuthRepository.kt` and SQLite.

It stores tasks in a `linkedMapOf` and exposes methods like:
- `getAll()`
- `getById()`
- `create()`
- `update()`
- `delete()`

This is a simple version of a repository pattern suitable for learning and interviews.

### ktor-backend/src/main/kotlin/com/example/taskapi/TaskRoutes.kt
This file defines the route logic.

It handles:
- GET /tasks
- GET /tasks/{id}
- POST /tasks
- PUT /tasks/{id}
- DELETE /tasks/{id}

It validates input and returns clear HTTP status codes such as:
- 400 Bad Request for invalid or empty titles
- 404 Not Found for missing tasks
- 201 Created for successful creation

---

## Run locally

Set a strong secret before starting either backend. The value must contain at least 32 bytes and must not be committed.

```bash
export JWT_SECRET="$(openssl rand -base64 48)"
```

### Spring Boot

From the project root:

```bash
cd /Users/pawankumar/AndroidStudioProjects/my-task-api
./mvnw clean test
./mvnw spring-boot:run
```

Then open:

```text
http://localhost:8080/tasks
```

---

### Ktor

From the Ktor project folder:

```bash
cd /Users/pawankumar/AndroidStudioProjects/my-task-api/ktor-backend
gradle clean test
gradle run
```

Then open:

```text
http://localhost:8081/tasks
```

The Ktor server defaults to port `8081`; set `PORT` to override it. Spring Boot defaults to port `8080`.

## Example flow

1. Call `POST /auth/register` or `POST /auth/login` and retain the returned tokens.
2. Send `Authorization: Bearer <accessToken>` on `/auth/me` and every `/tasks` request.
3. When the access token expires, call `POST /auth/refresh` with the refresh token and replace both tokens with the rotated response.
4. Call `POST /auth/logout` with the current refresh token when finished.

Spring stores its database in `tasks.db`. Ktor stores accounts and refresh-token state in `auth.db`; Ktor task data is process-local and resets when the app restarts.

---

## Example API calls

### Get all tasks

```bash
curl http://localhost:8080/tasks
```

### Create a task

```bash
curl -X POST http://localhost:8080/tasks \
  -H "Content-Type: application/json" \
  -d '{"title":"Buy milk","done":false}'
```

### Update a task

```bash
curl -X PUT http://localhost:8080/tasks/1 \
  -H "Content-Type: application/json" \
  -d '{"title":"Buy milk and bread","done":true}'
```

### Delete a task

```bash
curl -X DELETE http://localhost:8080/tasks/1
```

---

## Notes for interview/demo purposes

This project is a good demonstration of:
- REST API design
- CRUD operations
- Java backend development
- Kotlin backend development
- database persistence
- validation
- controller and repository patterns
- project folder structure and modular backend architecture

It is intentionally simple, easy to understand, and structured in a way that is helpful for learning and presenting in interviews.

---

## Final summary

This repository shows two ways to build the same backend idea:

- Spring Boot version: classic Java enterprise-style API
- Ktor version: modern Kotlin server-side API

Both are focused on task management and can be used as a strong interview project to explain backend fundamentals, API routes, validation, and persistence.
