# Task List API (Spring Boot + SQLite)

A beginner-friendly REST API built with **Spring Boot** that lets you
create and view a list of tasks, saved permanently in a **SQLite**
database. Built as a learning project to understand how a real backend
API works, end to end.

> **Note for future me:** if you're reading this a year from now and
> forgot everything, just read this file top to bottom. It explains
> every file, every config setting, and how to run the whole thing.

---

## What this project does

- `GET /tasks` → returns every saved task as JSON
- `POST /tasks` → saves a new task (title + done status) to the database

Data is stored in a real SQLite database file (`tasks.db`), so tasks
survive even after you restart the app.

---

## Tech stack

| Tool | Purpose |
|---|---|
| Java 17 | The programming language |
| Spring Boot 4.1.0 | Framework that handles web requests, wiring, and configuration |
| Maven | Builds the project and manages dependencies (libraries) |
| Spring Data JPA + Hibernate | Converts Java objects into database rows automatically |
| SQLite | The actual database - a single file, no server needed |
| DB Browser for SQLite | GUI tool to visually inspect/edit the database file |

---

## Project structure

```
demo/
├── pom.xml                                  Maven's "recipe" file (see below)
├── src/main/resources/
│   └── application.properties               App configuration/settings
└── src/main/java/com/example/demo/
    ├── DemoApplication.java (or HireSpringBootApplication.java)
    │                                         The app's entry point (main method)
    ├── Task.java                             Blueprint for a single task + DB table
    ├── TaskRepository.java                   Auto-generated database access layer
    └── TaskController.java                   Handles incoming GET/POST requests
```

---

## How a request flows through the app

```
Browser / curl
      │  (1) sends an HTTP request, e.g. GET /tasks
      ▼
TaskController.java
      │  (2) calls taskRepository.findAll()
      ▼
TaskRepository.java
      │  (3) Spring Data JPA auto-generates the SQL behind the scenes
      ▼
tasks.db  (SQLite file)
      │  (4) returns the raw rows
      ▼
Task.java objects
      │  (5) converted into JSON automatically by @RestController
      ▼
Browser / curl  (sees the JSON response)
```

---

## File-by-file explanation

### `pom.xml` — Maven's recipe file

Every Maven project has a `pom.xml` ("Project Object Model"). Think of
it as a recipe card that tells Maven:

- **What libraries (dependencies) this project needs.** For example,
  we added:
  - `spring-boot-starter-web` → lets us build REST APIs (`@RestController`, etc.)
  - `spring-boot-starter-data-jpa` → lets us use `@Entity`, `JpaRepository`, etc.
  - `sqlite-jdbc` → the actual driver that lets Java talk to SQLite files
  - `hibernate-community-dialects` → teaches Hibernate SQLite's specific SQL grammar
- **What Java version to compile against** (17, in our case)
- **How to package the final app** (as a runnable `.jar` file)

You never manually download these libraries — when you run
`./mvnw spring-boot:run`, Maven reads `pom.xml`, downloads whatever is
missing from the internet (into `~/.m2` on your machine), and wires it
all together.

**Rule of thumb:** if you ever add a new feature that needs a new
library (e.g. sending emails, security/login, etc.), you add a new
`<dependency>` block to `pom.xml` first.

### `application.properties` — app configuration

Lives in `src/main/resources/`. This is where we configure *how* the
app behaves — database location, logging behavior, server port, etc.
Full explanation with inline comments is in the file itself; the short
version:

- Tells Spring where the SQLite file lives (`tasks.db`)
- Tells Hibernate to auto-create/update database tables to match our
  `@Entity` classes (`spring.jpa.hibernate.ddl-auto=update`)
- Turns on SQL logging in the terminal so we can see what's happening
  under the hood (`spring.jpa.show-sql=true`)

### `Task.java` — the data blueprint

Defines what a single Task *looks like*: an `id`, a `title`, and a
`done` flag. Marked with `@Entity`, which tells Hibernate "this class
represents a database table" — so a `task` table with matching columns
gets created automatically.

### `TaskRepository.java` — the database access layer

An empty interface that extends `JpaRepository<Task, Long>`. Despite
having zero lines of actual logic, it gives us working methods like
`findAll()` and `save()` for free — Spring generates the implementation
automatically at startup.

### `TaskController.java` — the API's front door

Defines the actual URLs (`/tasks`) and what happens when someone visits
them with `GET` or `POST`. This is the only file that directly talks to
the outside world (browsers, curl, apps).

---

## How to run this project

```bash
cd demo
./mvnw spring-boot:run
```

Wait for a line like:
```
Started DemoApplication in X seconds
```

The app is now running at **http://localhost:8080**.

> If you see `Port 8080 was already in use`, an older instance of the
> app is still running in another terminal tab. Go find that tab and
> press `Ctrl+C` there first, or run `lsof -i :8080` to find and
> `kill -9 <PID>` the process using it.

---

## How to test the API

### Get all tasks (works directly in a browser)
Visit: http://localhost:8080/tasks

### Create a new task (needs a tool, since browsers can't send POST easily)
```bash
curl -X POST http://localhost:8080/tasks \
  -H "Content-Type: application/json" \
  -d '{"title": "Buy milk", "done": false}'
```

Then refresh http://localhost:8080/tasks in your browser — the new
task will appear.

---

## Viewing/editing the database with DB Browser for SQLite

1. Open **DB Browser for SQLite**
2. Click **Open Database** → select `tasks.db` from your project folder
3. Click the **Browse Data** tab → pick the `task` table from the dropdown
4. You'll see every saved task in a spreadsheet-like view

You can manually add/edit rows here too. If the app is running at the
same time, click **Write Changes** in DB Browser after any edit so the
API can see the update.

---

## Ideas for what to build next

- `GET /tasks/{id}` → fetch a single task by its ID
- `PUT /tasks/{id}` → update an existing task (e.g. mark it done)
- `DELETE /tasks/{id}` → remove a task
- Add validation (e.g. reject empty titles)
- Add a `createdAt` timestamp field to each task
