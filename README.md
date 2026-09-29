# Task API Project

This project is a backend-focused task management application built in two versions:

- Java Spring Boot backend
- Kotlin Ktor backend

The goal is to demonstrate the same backend domain in a classic Spring Boot style and a Kotlin Ktor style. Both APIs provide account registration and login, signed access tokens, rotating refresh tokens, and authenticated task CRUD. Spring persists users, refresh tokens, and tasks in SQLite; Ktor persists auth data in SQLite and currently keeps tasks in memory.

This project is useful for interview preparation, backend learning, and showing a clear understanding of API design, database integration, and project structure.

---

## What this project does

The application exposes a task API for managing daily tasks.

Features:
- Create a task
- Read all tasks
- Read one task by ID
- Update a task
- Delete a task
- Validate task title input
- Register and log in with an email and password
- Hash passwords with BCrypt
- Issue short-lived JWT access tokens and rotating refresh tokens
- Revoke refresh tokens on logout
- Protect task endpoints and scope tasks to the authenticated account
- Persist auth data in SQLite

Main endpoints:
- POST /auth/register
- POST /auth/login
- POST /auth/refresh
- POST /auth/logout
- GET /auth/me
- GET /tasks
- GET /tasks/{id}
- POST /tasks
- PUT /tasks/{id}
- DELETE /tasks/{id}

Example task JSON:

```json
{
  "title": "Buy groceries",
  "done": false
}
```

---

## Why two backends?

This repository includes both:

1. Spring Boot Java version
   - Great for enterprise-style backend applications
   - Uses Spring MVC, JPA, and repository pattern
   - Common in many production environments

2. Ktor Kotlin version
   - Great for modern Kotlin server apps
   - Lightweight and clean route-based architecture
   - Good for showing Kotlin backend skills in interviews

Both versions solve the same problem: manage tasks through API endpoints.

---

## Tech stack

### Java Spring Boot version
- Java 17
- Spring Boot 4.1.0
- Maven
- Spring Web
- Spring Security and OAuth2 Resource Server JWT
- Spring Data JPA
- Hibernate
- SQLite JDBC
- Jakarta Validation

### Kotlin Ktor version
- Kotlin
- Ktor 3.0.1
- Kotlin Serialization
- Gradle
- Netty server engine
- SQLite JDBC
- Ktor JWT authentication
- BCrypt password hashing
- Call logging and status pages

---

## Project structure

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
│       │       ├── auth/ (account and token controller, service, models, entities, repositories)
│       │       └── config/SecurityConfig.java
│       └── resources/
│           └── application.properties
├── target/
├── tasks.db
└── ktor-backend/
    ├── build.gradle.kts
    ├── settings.gradle.kts
    └── src/
        └── main/
            └── kotlin/
                └── com/example/taskapi/
                    ├── Application.kt
                    ├── Task.kt
                    ├── TaskRepository.kt
                    └── TaskRoutes.kt
```

---

## Spring Boot project details

### pom.xml
This is the Maven build file. It tells the project:
- which dependencies to download
- which Java version to compile with
- how the app should be packaged and run

Important dependencies include:
- spring-boot-starter-web for HTTP endpoints
- spring-boot-starter-security and spring-boot-starter-oauth2-resource-server for JWT protection
- spring-boot-starter-data-jpa for database access
- spring-boot-starter-validation for request validation
- sqlite-jdbc for SQLite connectivity

### src/main/resources/application.properties
This file contains the app configuration.

Key values:
- `spring.datasource.url=jdbc:sqlite:tasks.db` → points to the SQLite database file
- `spring.jpa.hibernate.ddl-auto=update` → creates tables automatically if missing
- `spring.jpa.show-sql=true` → prints SQL in the console for learning/debugging

### src/main/java/com/example/taskapi/HireSpringBootApplication.java
This is the main Spring Boot entry point.

It starts the app with:

```java
SpringApplication.run(HireSpringBootApplication.class, args);
```

This is the class that launches the Java backend.

### src/main/java/com/example/taskapi/Task.java
This is the task entity.

It maps to the database table named `tasks` and contains:
- `id` as primary key
- `title` as task text
- `done` as completion status

Important annotations:
- `@Entity` → marks it as a JPA entity
- `@Table(name = "tasks")` → maps it to a database table
- `@NotBlank` → prevents empty title values

### src/main/java/com/example/taskapi/TaskRepository.java
This repository layer handles database access.

It extends `JpaRepository<Task, Long>`, which gives built-in methods like:
- `findAll()`
- `findById()`
- `save()`
- `deleteById()`

This removes the need to write raw SQL manually.

### src/main/java/com/example/taskapi/TaskController.java
This is the REST controller that exposes the API endpoints.

It contains:
- GET /tasks → fetch all tasks
- GET /tasks/{id} → fetch one task by ID
- POST /tasks → create a task
- PUT /tasks/{id} → update task
- DELETE /tasks/{id} → delete task

The controller receives HTTP requests, reads the authenticated account from the JWT, and delegates business operations to `TaskService`. Tasks are only returned or modified for their owner.

### Authentication architecture

The Spring `auth` package separates the HTTP controller, request/response models, auth service, user and refresh-token entities, and repositories. `SecurityConfig` verifies bearer JWTs and keeps the API stateless. `TaskService` separates task business operations from the web controller and persistence layer.

### Authentication endpoints

| Method | Endpoint | Purpose | Auth required |
| --- | --- | --- | --- |
| POST | `/auth/register` | Create an account and return tokens | No |
| POST | `/auth/login` | Verify credentials and return tokens | No |
| POST | `/auth/refresh` | Rotate a refresh token and return a new token pair | No |
| POST | `/auth/logout` | Revoke a refresh token | No |
| GET | `/auth/me` | Return the authenticated account profile | Yes |

Every `/tasks` route requires `Authorization: Bearer <accessToken>`. Users can only access their own tasks.

Registration request:

```json
{
  "email": "dev@example.com",
  "password": "a-secure-password",
  "name": "Dev User"
}
```

Login request:

```json
{
  "email": "dev@example.com",
  "password": "a-secure-password"
}
```

Refresh and logout request:

```json
{
  "refreshToken": "<refreshToken from the token response>"
}
```

Registration and login return `tokenType`, `accessToken`, `refreshToken`, `expiresIn`, and a public `user` object. Passwords are BCrypt-hashed. Random refresh tokens are stored as SHA-256 hashes, expire after seven days, and rotate on use. Access tokens expire after 15 minutes. Logout revokes refresh capability; an issued access token remains valid until its short expiration.

---

## Ktor project details

### ktor-backend/build.gradle.kts
This is the Gradle build file for the Kotlin Ktor app.

It includes:
- Kotlin JVM plugin
- Kotlin serialization plugin
- Ktor server dependencies
- Netty server engine
- JSON serialization support
- logging plugins
- test dependencies

This file defines how the Ktor application is built and run.

### ktor-backend/src/main/resources/application.yaml
This supplies the SQLite URL and optional JWT secret from the environment. `JWT_SECRET` must be set before starting the service; `PORT` changes the default server port.

### ktor-backend/settings.gradle.kts
This is the Gradle settings file for the Ktor module and sets the project name.

### ktor-backend/src/main/kotlin/com/example/taskapi/Application.kt
This is the main app file.

It does several important things:
- starts the Ktor server on port 8081 or the `PORT` environment variable
- installs JSON serialization
- configures JWT verification
- installs request logging
- installs status pages for errors
- wires public auth routes and authenticated profile/task routes

The server starts with:

```kotlin
embeddedServer(Netty, port = port, host = "0.0.0.0", module = { module() })
```

### Ktor auth package

The `auth` package separates serializable API models, SQLite account and refresh-token persistence, BCrypt/JWT token logic, and auth routes. Refresh tokens are stored only as hashes and are consumed transactionally when rotated. Auth tables live in `auth.db`; task data remains in memory for this learning implementation.

### ktor-backend/src/main/kotlin/com/example/taskapi/Task.kt
This file defines the task models.

It contains:
- `Task` data class
- `TaskRequest` data class

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
