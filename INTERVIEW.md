# Mock Interview Questions for This Project

These questions are designed for a backend interview, portfolio review, or technical discussion.

## 1) Why did you build both a Spring Boot and Ktor version?

Answer:
- It shows I can work across frameworks and compare architecture trade-offs.
- Spring is great for convention-heavy enterprise code and dependency injection.
- Ktor is a lightweight Kotlin-first server that fits modern backend work.
- The domain stays the same, which makes the comparison meaningful.

## 2) Why is MVVM not the right pattern for this backend project?

Answer:
- MVVM is mainly a UI pattern for view-model-driven frontends.
- This project is an API server, so the right model is layered backend architecture: controller/route -> service -> repository.
- The task ownership and auth rules belong in backend application logic, not in a UI view model.

## 3) What is your architecture pattern here?

Answer:
- A clean layered backend pattern.
- Web layer handles HTTP and routing.
- Service layer owns business rules.
- Repository layer accesses storage.
- Security and JWT validation sit at the edge.

## 4) Why use JWTs instead of sessions?

Answer:
- JWTs are stateless and portable across services.
- The server can validate bearer tokens without server-side session storage.
- Refresh tokens allow short-lived access tokens without forcing frequent re-authentication.

## 5) Why store refresh tokens as hashes instead of raw token values?

Answer:
- It reduces damage if the database is exposed.
- A stolen hash is less useful than a raw token.
- It matches secure token-handling best practices.

## 6) How do you protect task access from one user to another?

Answer:
- The JWT carries the user identity.
- Every task operation checks the current user against the task owner.
- The repository queries by owner identity, not just task ID.
- This ensures account isolation and prevents IDOR-style access.

## 7) What is the difference between access tokens and refresh tokens?

Answer:
- Access token: short-lived, used for API calls.
- Refresh token: longer-lived, used to mint a new access token.
- Refresh tokens rotate to reduce replay risk.
- Revoked refresh tokens prevent reuse after logout or password reset.

## 8) Why require email verification before login?

Answer:
- It prevents fake or disposable account creation from being used immediately.
- It reduces abuse and improves account ownership confidence.
- It is common in production systems and a good security practice.

## 9) How would you take this project to production?

Answer:
- Move from SQLite to PostgreSQL or MySQL.
- Use environment-driven config and secrets management.
- Add role-based authorization.
- Add rate limiting, logs, metrics, and health checks.
- Replace console mail with a proper SMTP or transactional email service.
- Add CI/CD and database migrations.

## 10) What would you improve next in this repo?

Answer:
- Role-based access for admin tasks.
- SQL persistence in the Ktor backend.
- Docker and deployment config.
- Structured logs and tracing.
- Rate limiting and validation upgrades.

---

## Follow-up mini questions

### What is the biggest design trade-off here?
- Using a lightweight layered backend is easier to reason about than over-engineering a full hexagonal setup for a small app.

### Why not use a full DDD or CQRS design?
- This project is intentionally small and educational. DDD/CQRS would add ceremony without adding much value for the current scope.

### Which framework would you choose for a greenfield production backend?
- I would choose Spring Boot for larger enterprise products and Ktor for Kotlin-first, performance-conscious services where the team prefers Kotlin end-to-end.
