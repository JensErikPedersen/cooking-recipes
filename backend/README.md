# recipes

A RESTful recipe application — Spring Boot 4.1.1 on Java 25, MySQL, Liquibase-managed schema.

## Running it

Normally as part of the Docker stack - see the root `README.md`. On first start the `mysql`
container creates the database and its one account from `.env`, and Liquibase builds the schema
when the application starts. There is no manual database setup.

To run the backend on the host instead, against the stack's MySQL, stop the stack's backend first
(`docker compose stop backend`) - both use port 8080. Spring Boot does not read `.env`, so set the
variables yourself:

```bash
# Bash
DB_PORT=3307 DB_PASSWORD=... ./mvnw spring-boot:run
```

```powershell
# PowerShell
$env:DB_PORT="3307"; $env:DB_PASSWORD="..."; ./mvnw spring-boot:run
```

Without `DB_PASSWORD` the application fails at startup with `Access denied for user 'recipesuser'`.

| Variable | Default |
|---|---|
| `DB_HOST` / `DB_PORT` / `DB_NAME` | `localhost` / `3306` / `recipes` |
| `DB_USERNAME` | `recipesuser` |
| `DB_PASSWORD` | *none — required* |
| `APPLICATION_PORT` | `8080` |

Add `-Dspring-boot.run.profiles=dev` for SQL statement and bind-value logging. Do not enable it in
production: bind-value logging writes user data to the log.

## Building and testing

```bash
./mvnw clean verify     # compile + unit tests + integration tests
./mvnw test             # unit tests only  (*Test,  surefire)
./mvnw verify           # + integration tests (*IT, failsafe, H2 in-memory)
```

Integration tests run against in-memory H2 and need no local database.

## API

All endpoints are under `/api/v1` and need a signed-in session, except login. Sign-in is a session
cookie, and every write - login and logout included - must send the `XSRF-TOKEN` cookie's value
back as the `X-XSRF-TOKEN` header. The one account is created on first start from `APP_ADMIN_*`;
see `CLAUDE.md`, Authentication.

| Auth | |
|---|---|
| `POST /api/v1/auth/login` | `{"username", "password"}`; 200 with the user, 401 on bad credentials |
| `GET /api/v1/auth/me` | the signed-in user, or 401 |
| `POST /api/v1/auth/logout` | 204 |

Every resource offers the same five operations; recipes add a nested ingredients sub-resource.

| Resource | Endpoints |
|---|---|
| `/api/v1/categories` | GET (all, by id), POST, PUT, DELETE |
| `/api/v1/ingredients` | GET (all, by id), POST, PUT, DELETE |
| `/api/v1/units` | GET (all, by id), POST, PUT, DELETE |
| `/api/v1/tags` | GET (all, by id), POST, PUT, DELETE |
| `/api/v1/recipes` | GET (all, by id), POST, PUT, DELETE |
| `/api/v1/recipes/{id}/ingredients` | POST, PUT `/{ingredientId}`, DELETE `/{ingredientId}` |

A recipe's POST and PUT carry its category, tags and ingredient lines by id; a PUT replaces the
tags and the lines with the lists it sends.

Errors return a consistent envelope:

```json
{
  "errorCode": 100,
  "message": "Category with id 913a5159-... could not be found",
  "validationExceptions": [ { "objectName": "name", "message": "Category name is required" } ]
}
```

`200` read · `201` create (with `Location`) · `204` delete · `400` validation · `401` not signed
in · `403` missing CSRF token · `404` unknown · `409` conflict: a duplicate name or
sub-resource, or a delete of something still in use.

### Not in this version

Ratings are **read-only**. `GET /api/v1/recipes/{id}` returns `recipeRatings`, but a write carrying
them is rejected with `400`, and there are no rating endpoints. See
[docs/future_enhancements.md](docs/future_enhancements.md).

## Documentation

| | |
|---|---|
| [CLAUDE.md](CLAUDE.md) | Architecture, conventions and the traps worth knowing before changing code |
| [docs/code-review.md](docs/code-review.md) | Full code review, findings and what has been resolved |
| [docs/future_enhancements.md](docs/future_enhancements.md) | Deliberately deferred work, with scope notes |
