# recipes

A RESTful recipe application — Spring Boot 4.1.1 on Java 25, MySQL, Liquibase-managed schema.

## Running it

Credentials are **not** in the repository. `DB_PASSWORD` has no default, so set it before starting:

```bash
# Bash
DB_USERNAME=recipesuser DB_PASSWORD=... ./mvnw spring-boot:run
```

```powershell
# PowerShell
$env:DB_USERNAME="recipesuser"; $env:DB_PASSWORD="..."; ./mvnw spring-boot:run
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

### First-time database setup

```bash
mysql -u root -p < scripts/mysql_users.sql   # edit the password placeholders first
```

That script starts with `DROP DATABASE`, so it is for a first install only. To create or repair the
accounts on a database that already has data, use the non-destructive variant:

```bash
mysql -u root -p < scripts/mysql_reset_users.sql
```

Liquibase owns the schema and applies it at startup — there is no manual DDL step. It runs as the
configured datasource user, so the **first** startup needs `DB_USERNAME=recipesadmin`, which holds
the DDL grants; `recipesuser` has DML only and is the right account once the schema exists.

## Building and testing

```bash
./mvnw clean verify     # compile + unit tests + integration tests
./mvnw test             # unit tests only  (*Test,  surefire)
./mvnw verify           # + integration tests (*IT, failsafe, H2 in-memory)
```

Integration tests run against in-memory H2 and need no local database.

## API

All endpoints are under `/api/v1`. Every resource offers the same five operations; recipes add a
nested ingredients sub-resource.

| Resource | Endpoints |
|---|---|
| `/api/v1/categories` | GET (all, by id), POST, PUT, DELETE |
| `/api/v1/ingredients` | GET (all, by id), POST, PUT, DELETE |
| `/api/v1/units` | GET (all, by id), POST, PUT, DELETE |
| `/api/v1/tags` | GET (all, by id), POST, PUT, DELETE |
| `/api/v1/recipes` | GET (all, by id), POST, PUT, DELETE |
| `/api/v1/recipes/{id}/ingredients` | POST, PUT `/{ingredientId}`, DELETE `/{ingredientId}` |

Errors return a consistent envelope:

```json
{
  "errorCode": 100,
  "message": "Category with id 913a5159-... could not be found",
  "validationExceptions": [ { "objectName": "name", "message": "Category name is required" } ]
}
```

`200` read · `201` create (with `Location`) · `204` delete · `400` validation · `404` unknown ·
`409` duplicate sub-resource.

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
| [docs/upgrade_to_java25.md](docs/upgrade_to_java25.md) | Staged plan for moving to Java 25 and Spring Boot 4.1 |
