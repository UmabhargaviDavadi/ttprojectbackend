# Mintwise Finance Tracker API

Spring Boot REST API for the sibling React application. The API uses Java 17, Spring Boot 3.4.5, Maven, Spring Data JPA, Spring Security, JWT, and MySQL 8.

## Prerequisites

- JDK 17 or newer (`java -version`)
- MySQL 8 listening on `localhost:3306` for the default runtime profile
- No globally installed Maven is required; use `mvnw.cmd` on Windows or `./mvnw` on macOS/Linux

Create the database in MySQL:

```sql
CREATE DATABASE finance_tracker CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

Set the MySQL password in the `DB_PASSWORD` environment variable. The local default is `change-me-local`, intentionally a placeholder. The datasource URL and username can also be overridden with `DB_URL` and `DB_USERNAME`. Do not commit real credentials.

If startup reports `Access denied for user 'root'@'localhost'`, set `DB_PASSWORD` to the password configured for your local MySQL root account. If you use a different MySQL account, also set `DB_USERNAME` to that account and grant it access to `finance_tracker`.

## Run

### Option 1: Local MySQL via Docker

From the project root:

```powershell
docker compose up -d mysql
```

Then start the backend with the real database configuration:

```powershell
Copy-Item .env.example .env
$env:DB_URL = "jdbc:mysql://localhost:3306/finance_tracker?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC"
$env:DB_USERNAME = "finance_app"
$env:DB_PASSWORD = "finance_app"
$env:JWT_SECRET = "replace-with-a-long-random-secret-at-least-32-chars"
.\mvnw.cmd clean install
.\mvnw.cmd spring-boot:run
```

### Option 2: Local MySQL installed directly on the machine

```powershell
$env:DB_PASSWORD = "your-local-mysql-password"
.\mvnw.cmd clean install
.\mvnw.cmd spring-boot:run
```

The API base is `http://localhost:8080/api`. The Spring servlet context path is `/api`, so controller route `/auth/login` is served at `http://localhost:8080/api/auth/login`.

The default `app.seed-demo=true` seeds categories and a demo account with sample activity. Demo credentials:

- Email: `demo@example.com`
- Password: `Demo@1234`

To disable demo records, set `SEED_DEMO=false`. Categories are always seeded.

Swagger UI: `http://localhost:8080/api/swagger-ui/index.html`  
OpenAPI JSON: `http://localhost:8080/api/v3/api-docs`

### H2 profile

H2 is test-scoped, keeping it out of the normal runtime artifact. To run the optional in-memory profile through the Maven plugin, include the test classpath:

```powershell
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.useTestClasspath=true" "-Dspring-boot.run.arguments=--spring.profiles.active=h2"
```

This starts the same API on port 8080 using an in-memory H2 database. The database is lost when the process exits. Automated tests use the separate `test` profile and `src/test/resources/application-test.properties`.

## Environment variables

The repository now includes a ready-to-use example file at `TT Backend/.env.example` and a root `docker-compose.yml` for local MySQL startup.

| Variable | Default | Purpose |
| --- | --- | --- |
| `SERVER_PORT` | `8080` | HTTP port |
| `DB_URL` | `jdbc:mysql://localhost:3306/finance_tracker?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC` | JDBC URL |
| `DB_USERNAME` | `finance_app` | MySQL user |
| `DB_PASSWORD` | `finance_app` | Local MySQL password for the finance_app user |
| `JWT_SECRET` | Local development key (over 32 bytes) | HMAC signing key; replace with a random secret in deployments |
| `JWT_EXPIRATION_MS` | `86400000` | Token validity |
| `SEED_DEMO` | `true` | Seed sample demo user/activity |

Hibernate schema update is enabled for local development; SQL logging and Open Session in View are disabled.

## Endpoints

Every route except `/auth/**` and Swagger/OpenAPI requires `Authorization: Bearer <JWT>`. Transaction types are lowercase in requests and responses. Dates use ISO `YYYY-MM-DD`. Amounts are decimal numbers.

| Method | Route | Description |
| --- | --- | --- |
| POST | `/api/auth/register` | Register and return JWT/user |
| POST | `/api/auth/login` | Sign in and return JWT/user |
| GET | `/api/transactions` | Filtered, sorted, paginated transactions |
| POST | `/api/transactions` | Create transaction; infer category if omitted |
| PUT | `/api/transactions/{id}` | Update owned transaction |
| DELETE | `/api/transactions/{id}` | Delete owned transaction |
| GET | `/api/budgets?month=&year=` | List monthly budgets with spent, remaining, percentage |
| POST | `/api/budgets` | Create budget |
| PUT | `/api/budgets/{id}` | Update owned budget |
| DELETE | `/api/budgets/{id}` | Delete owned budget |
| GET | `/api/reports/monthly?month=&year=` | Monthly totals, categories and previous-month comparison |
| GET | `/api/reports/category-summary?month=&year=` | Monthly expense category breakdown |
| GET | `/api/reports/trend?months=6` | Income/expense totals for the latest N months |
| GET | `/api/suggestions/savings` | User-specific saving ideas |
| GET | `/api/suggestions/investments` | Basic investment guidance and disclaimer |
| GET | `/api/goals` | List owned savings goals |
| POST | `/api/goals` | Create a goal |
| PUT | `/api/goals/{id}` | Update owned goal |
| DELETE | `/api/goals/{id}` | Delete owned goal |
| GET | `/api/notifications` | Newest-first notification array |
| PUT | `/api/notifications/{id}/read` | Mark owned notification read |
| GET | `/api/users/profile` | Get current user's profile |
| PUT | `/api/users/profile` | Update profile or change password |

Transactions accept `page` (zero-based), `size`, `category`, `type`, `from`, `to`, `search`, `minAmount`, `maxAmount`, and `sort` (for example `date,desc`, `amount,asc`, `description,asc`). Success pagination is `{ "content": [], "totalElements": 0, "totalPages": 0, "page": 0, "size": 10 }`.

Budget category `null` represents the single overall budget and is returned as `"Overall"`; duplicate user/category/month/year budgets are rejected. After expense transactions, threshold alerts are persisted once per budget/month at 80% and 100%. Notifications retain the contract's array response; `X-Unread-Count` supplies the unread count without changing that response shape.

Validation and service errors use `{ "timestamp": "...", "status": 400, "message": "...", "fieldErrors": {} }`. Unauthorized and forbidden responses use status 401 and 403 respectively; missing resources are 404 and conflicts are 409.

## Contract notes

`API_CONTRACT.md` is the source of truth for existing frontend request and response fields. A few additional backend requirements conflict with it, so this implementation preserves frontend-compatible shapes and adds optional fields rather than replacing them:

- Budgets remain a JSON array and retain `limit`; `spent`, `remaining`, and `percentage` are added. The frontend also derives spend from transactions.
- Notifications remain a JSON array as consumed by the frontend. Unread count is provided in `X-Unread-Count`.
- Saving tips retain `title`, `description`, `impact`, and `category`, with `message` and `priority` added.
- Investment tips retain `title`, `description`, `riskLevel`, and `horizon`; `name`, suggested amount, and a top-level disclaimer are added. Contract risk labels are `Low`, `Moderate`, and `High`.
- Goal responses retain `currentAmount`; `percentage` and `amountNeeded` are added while the entity stores `savedAmount`.
- The frontend sends optional `month`/`year` to `/reports/trend`; the endpoint accepts and ignores them, and uses the contract-supported `months` parameter (default six).

## CORS and security

CORS allows `http://localhost:5173`, `http://127.0.0.1:5173`, and `https://ttprojectfrontend-n9mv.vercel.app`, credentials, Authorization/Content-Type headers, and GET/POST/PUT/DELETE/OPTIONS. Sessions are stateless, passwords use BCrypt, CSRF is disabled for the API, and JWTs are validated by the request filter. Every data service scopes access to the authenticated user's ID. Transaction indexes cover `(user_id, date)` and `(user_id, category_id)`.

## Verification

Run the complete build and tests with:

```powershell
.\mvnw.cmd clean install
```

Tests use in-memory H2 and do not require MySQL. `mvnw.cmd` is the checked-in Windows Maven wrapper; `mvnw` is included for Unix-like environments.

With the API running on localhost:8080, verify the HTTP contract and CORS using:

```powershell
powershell.exe -ExecutionPolicy Bypass -NoProfile -File .\scripts\verify-api.ps1
```

## Frontend adjustments

No frontend changes are required for existing endpoint paths and contract shapes. To use this backend instead of mock data, set the frontend's `VITE_USE_MOCK=false` and `VITE_API_BASE_URL=http://localhost:8080/api`, then restart Vite. The backend's extra `X-Unread-Count` is optional because the current frontend derives its badge count from the returned notification array. The `DANGER` severity is represented by `severity: "danger"`; the current bell displays its generic info icon for that value, though it still shows the notification text.
