# TaskFlow

A small team task tracker: a **Spring Boot 3 + Kotlin** REST API and a **React 18 + TypeScript** frontend, backed by **PostgreSQL**.

Users register, create projects, and manage tasks on a three-column board (To Do / In Progress / Done) with filters, sorting, overdue highlighting and a per-project summary.

| Layer | Stack |
|---|---|
| Backend | Kotlin 2.2, Spring Boot 3.5, Spring Security (JWT), Spring Data JPA, Bean Validation, Flyway, springdoc-openapi, Maven |
| Database | PostgreSQL 16 |
| Frontend | React 18, TypeScript, Vite, React Router, TanStack Query, axios, react-hook-form + zod |
| Tests | JUnit 5, MockK, springmockk, MockMvc (`@WebMvcTest`, `@SpringBootTest`), Jest, React Testing Library |
| Tooling | Docker Compose, GitHub Actions |

---

## Quick start (Docker, one command)

Requirements: Docker Desktop.

```bash
cp .env.example .env          # then edit the passwords and JWT_SECRET (32+ characters)
docker compose up --build
```

| What | URL |
|---|---|
| App | http://localhost:5173 |
| API docs (Swagger UI) | http://localhost:8080/swagger-ui.html |
| Health | http://localhost:8080/actuator/health |

### Sample users (seeded by Flyway)

| Email | Password | Notes |
|---|---|---|
| `demo@taskflow.dev` | `Password123!` | Owns the "Website Relaunch" project with 4 tasks, one of them overdue |
| `asha@taskflow.dev` | `Password123!` | Teammate who can be assigned tasks |

You can also register a new account from the login screen.

---

## Running locally without Docker for the apps

Requirements: JDK 21, Node 20+, Docker (for PostgreSQL only).

```bash
cp .env.example .env              # edit values
docker compose up -d db           # PostgreSQL only
```

**Backend** (reads the root `.env` automatically via `spring.config.import`):

```bash
cd backend
./mvnw spring-boot:run            # Windows: mvnw.cmd spring-boot:run  (or `mvn spring-boot:run`)
```

**Frontend:**

```bash
cd frontend
npm install
npm run dev                       # http://localhost:5173
```

The frontend calls `http://localhost:8080/api` by default; override with `VITE_API_BASE_URL` (see `frontend/.env.example`).

---

## Running the tests

**Backend** (21 tests; the database from `docker compose up -d db` must be running for the integration tests):

```bash
cd backend
./mvnw test                       # or: mvn test
```

| Test | Kind | What it covers |
|---|---|---|
| `AuthServiceTest` | Unit (MockK) | Email normalisation, password hashing, duplicate email, invalid credentials |
| `JwtServiceTest` | Unit | Token round-trip, expired and tampered tokens rejected |
| `TaskServiceTest` | Unit (MockK) | Due date not in the past, default status, unknown assignee, ownership 404, summary zero-filling, invalid sort, overdue rule (fixed `Clock`) |
| `TaskControllerWebMvcTest` | `@WebMvcTest` | Real security chain + JWT filter: 401 JSON without token, field-level 400s, authenticated status change |
| `TaskFlowIntegrationTest` | `@SpringBootTest` | Full HTTP → DB flow on the real Flyway schema; another user's project returns 404. Each test is rolled back |

**Frontend** (8 tests):

```bash
cd frontend
npm test
```

| Test | What it covers |
|---|---|
| `TaskCard.test.tsx` | Renders details, overdue highlighting, status dropdown callback, open-editor callback |
| `SummaryBar.test.tsx` | Counts per status and overdue count |
| `LoginPage.test.tsx` | Client-side validation blocks the API call, valid submit, server error shown |

**CI:** `.github/workflows/ci.yml` runs both test suites (backend against a PostgreSQL service container) and the frontend production build on every push and pull request.

---

## API

All endpoints except register/login require `Authorization: Bearer <token>`. Users can only see and change their own projects and the tasks inside them.

| Method | Endpoint | Purpose |
|---|---|---|
| POST | `/api/auth/register` | Create a user (201) |
| POST | `/api/auth/login` | Returns `{ token, expiresAt, user }` |
| GET | `/api/projects` | Current user's projects |
| POST | `/api/projects` | Create a project (201) |
| GET | `/api/projects/{id}` | One project *(extra, used by the board header)* |
| GET | `/api/projects/{id}/tasks` | Filter `status`, `priority`, `assigneeId`; sort `sort=dueDate\|priority\|createdAt` + `direction=asc\|desc`; paging `page`, `size` (max 100) |
| POST | `/api/projects/{id}/tasks` | Create a task (201) |
| PUT | `/api/tasks/{id}` | Update a task |
| PATCH | `/api/tasks/{id}/status` | Change only the status |
| DELETE | `/api/tasks/{id}` | Delete a task (204) |
| GET | `/api/projects/{id}/summary` | Count per status, total, overdue |
| GET | `/api/users` | List users for the assignee picker *(extra)* |

### Error format

Every error uses one shape, produced by `GlobalExceptionHandler` (`@RestControllerAdvice`) and by the security entry point for 401s:

```json
{
  "timestamp": "2026-11-05T13:35:39Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "path": "/api/projects/1/tasks",
  "fieldErrors": [{ "field": "title", "message": "Title is required" }]
}
```

---

## Architecture

```
taskflow/
├── backend/      Spring Boot API (package-by-feature, layered inside each feature)
├── frontend/     React SPA
├── docker-compose.yml
└── .github/workflows/ci.yml
```

### Backend

```
com.taskflow
├── auth/       AuthController → AuthService            (register, login)
├── project/    ProjectController → ProjectService → ProjectRepository
├── task/       TaskController → TaskService → TaskRepository
│   ├── TaskSpecifications   (filters)
│   ├── TaskMapper           (entity → DTO)
│   └── sort/                (sort strategies + resolver)
├── user/       User entity, repository, DTOs, user list endpoint
├── security/   JwtService, JwtAuthenticationFilter
├── config/     SecurityConfig, AppProperties, OpenApiConfig, TimeConfig (Clock)
└── common/     ApiError, PageResponse, exceptions, GlobalExceptionHandler
```

Request flow: **JWT filter** validates the token and sets the principal → **controller** validates the DTO (`@Valid`) → **service** enforces ownership and business rules inside a transaction → **repository** queries PostgreSQL → **mapper** builds the response DTO. JPA entities never leave the service layer.

- **Schema** is owned by Flyway migrations (`V1__init_schema.sql`, `V2__seed_demo_data.sql`); Hibernate runs with `ddl-auto=validate`. The schema has `CHECK` constraints on the enums, foreign keys with sensible delete rules, and indexes on `(project_id, status)` and `assignee_id`.
- **Ownership** is enforced in the queries themselves (`findByIdAndOwnerId`, `findByIdAndProjectOwnerId`). Another user's resource returns **404**, not 403, so its existence is not revealed.
- **Time** comes from an injected `Clock` (UTC), so "due date not in the past" and "overdue" are deterministic in tests.
- **Stateless JWT** (HS256, configurable expiry). Passwords are hashed with BCrypt.

### Frontend

```
src/
├── api/          axios client (token + 401 interceptors), one module per resource, shared types
├── auth/         AuthContext, ProtectedRoute, tokenStorage
├── components/   Button, Modal, FormField, TaskCard, Layout, Spinner/ErrorMessage/EmptyState
├── features/     auth (login, register) · projects · board (BoardPage, SummaryBar, TaskFilters, TaskFormModal)
├── hooks/        React Query hooks (useProjects, useTasks…)
└── utils/        date helpers
```

- **API layer:** components never call axios directly; they use hooks, which call `src/api/*`.
- **Protected routes:** logged-out users are redirected to `/login`. An expired token is handled two ways: a timer logs the user out at `expiresAt`, and any 401 from the API clears the session. Either way the login page says "Your session expired".
- **Moving tasks:** each card has a status dropdown that calls `PATCH /tasks/{id}/status`, then the board and summary refetch.
- Every API call shows **loading, empty and error** states. The layout is responsive: the columns stack on mobile.

### State management: why React Query + Context

Almost all state in this app is **server state** (projects, tasks, summary). TanStack Query handles caching, loading/error flags, refetching and cache invalidation after mutations, which would otherwise be hand-written reducers in Redux. The only global client state is the **auth session**, which is small and rarely changes, so a plain React Context is enough. Redux Toolkit would add boilerplate without solving a problem this app has.

---

## Design patterns used

| Pattern | Where | Why |
|---|---|---|
| **Strategy** | `task/sort/` — `TaskSortStrategy` with `DueDateSortStrategy`, `PrioritySortStrategy`, `CreatedAtSortStrategy`, picked by `TaskSortResolver` | Each sort option owns its ordering logic (priority sorts by business rank HIGH > MEDIUM > LOW, not alphabetically; due date puts tasks without a date last). Adding a sort option means adding one `@Component`; nothing else changes |
| **Specification** | `TaskSpecifications` | Optional filters compose into one query instead of a combinatorial set of repository methods |
| **Repository** | Spring Data interfaces in each feature | Data access is isolated behind interfaces, which makes the services easy to unit test with MockK |
| **Mapper / Factory** | `TaskMapper`, `toResponse()` extensions | One place builds each DTO; entities are never exposed. `TaskMapper` also computes the derived `overdue` flag |
| **DTO** | `*Request` / `*Response` classes | Decouples the API contract from the persistence model |

---

## Configuration and secrets

- No secrets are in the code. The backend reads `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `JWT_SECRET`, `JWT_EXPIRATION_MINUTES` and `CORS_ALLOWED_ORIGINS` from the environment, or from the root `.env` for local runs.
- `.env` is gitignored; `.env.example` documents every variable.
- `JWT_SECRET` has no default, so the app refuses to start without one.

---

## Assumptions

- **Assignees** can be any registered user (there is no team/membership model in the brief). `GET /api/users` feeds the assignee dropdown.
- **"Due date not in the past"** is enforced on **create** only, so existing tasks can still be edited after their date passes. "Today" is evaluated in UTC on the backend; the frontend also blocks past dates in the user's local time.
- **Overdue** means the due date is before today and the status is not DONE.
- Only project owners can access a project; there is no sharing.
- The task list returns up to 100 tasks per page; the board requests one page, which is enough for this scope.

## Trade-offs

- **Status dropdown instead of drag-and-drop:** the brief allows either. A dropdown is accessible and works the same on mobile and desktop.
- **JWT in `localStorage`:** simple and survives reloads, but readable by scripts if the site had an XSS flaw. In production I would use a short-lived access token plus a refresh token in an `HttpOnly` cookie.
- **No refresh tokens:** when the token expires the user logs in again.
- **Integration tests use the docker-compose database** with per-test rollback rather than Testcontainers, which keeps the setup to one dependency (Docker) but means the database must be running.
- **N+1 on task assignees** is avoided with Hibernate batch fetching (`default_batch_fetch_size`) rather than fetch joins, which conflict with paginated count queries.
- Frontend `npm audit` warnings come from dev-only, transitive dependencies of Jest 29; they are not shipped to the browser.

## What I would do next

- Activity log on tasks (who changed the status and when): a `task_activity` table written in `TaskService`, shown in the task modal.
- Drag-and-drop on the board (for example with dnd-kit), keeping the dropdown as the accessible fallback.
- Project sharing/membership, so assignees are limited to project members.
- Refresh tokens with `HttpOnly` cookies.
- Testcontainers for fully self-contained integration tests, and end-to-end tests with Playwright.
