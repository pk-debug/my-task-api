# Task API Project

This project is a backend-focused task management application built in two versions:

- Java Spring Boot backend
- Kotlin Ktor backend

The goal is to demonstrate how a simple REST API can be built in both a classic enterprise style and a modern Kotlin server style. The app manages tasks with CRUD operations, validates data, and stores records in SQLite.

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
- Persist data in SQLite

Main endpoints:
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
│       │       └── TaskRepository.java
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

The controller receives HTTP requests and converts them to Java objects using Spring.

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

### ktor-backend/settings.gradle.kts
This is the Gradle settings file for the Ktor module and sets the project name.

### ktor-backend/src/main/kotlin/com/example/taskapi/Application.kt
This is the main app file.

It does several important things:
- starts the Ktor server on port 8081
- installs JSON serialization
- installs request logging
- installs status pages for errors
- sets up routing using the task routes

The server starts with:

```kotlin
embeddedServer(Netty, port = 8081, host = "0.0.0.0", module = Application::module)
```

### ktor-backend/src/main/kotlin/com/example/taskapi/Task.kt
This file defines the task models.

It contains:
- `Task` data class
- `TaskRequest` data class

The `@Serializable` annotation allows Kotlin objects to be converted to/from JSON automatically.

### ktor-backend/src/main/kotlin/com/example/taskapi/TaskRepository.kt
This is the in-memory repository used by Ktor.

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

## How to run the Spring Boot project

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

## How to run the Ktor project

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
