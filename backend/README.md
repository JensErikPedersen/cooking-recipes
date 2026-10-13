# recipes

A RESTful recipe application — Spring Boot 4.1.1 on Java 25, MySQL, Liquibase-managed schema.

## Running it

Normally as part of the Docker stack - see the root `README.md`. On first start the `mysql`
container creates the database and its two accounts from `.env` - `mysql/initdb/accounts.sh` - and
Liquibase builds the schema when the application starts. There is no manual database setup.

To run the backend on the host instead, against the stack's MySQL, stop the stack's backend first
(`docker compose stop backend`) - both use port 8080. Spring Boot does not read `.env`, so set the
variables yourself:

```bash
# Bash
DB_PORT=3307 DB_PASSWORD=... DB_ADMIN_PASSWORD=... ./mvnw spring-boot:run
```

```powershell
# PowerShell
$env:DB_PORT="3307"; $env:DB_PASSWORD="..."; $env:DB_ADMIN_PASSWORD="..."; ./mvnw spring-boot:run
```

Liquibase connects as `DB_ADMIN_USERNAME`, which may change the schema, and the application as
`DB_USERNAME`, which may only read and write rows. Without `DB_ADMIN_PASSWORD` the application fails
at startup with `Access denied for user 'recipesadmin'`; without `DB_PASSWORD`, with the same for
`'recipesuser'`.

| Variable | Default |
|---|---|
| `DB_HOST` / `DB_PORT` / `DB_NAME` | `localhost` / `3306` / `recipes` |
| `DB_USERNAME` | `recipesuser` |
| `DB_PASSWORD` | *none — required* |
| `DB_ADMIN_USERNAME` | `recipesadmin` |
| `DB_ADMIN_PASSWORD` | *none — required* |
| `APPLICATION_PORT` | `8080` |
| `MAIL_HOST` / `MAIL_PORT` | `localhost` / `1025` - the stack's Mailpit, published on this machine |
| `APP_BASE_URL` | `http://localhost:3000` - the address in the links of mails |

Add `-Dspring-boot.run.profiles=dev` for SQL statement and bind-value logging. Do not enable it in
production: bind-value logging writes user data to the log.

## Building and testing

```bash
./mvnw clean verify     # compile + unit tests (*Test, surefire) + integration tests (*IT, failsafe)
./mvnw clean test       # unit tests only
```

Integration tests run against in-memory H2 and need no local database. Always build with `clean`:
an IDE's Java extension may compile into the same `target/`, and Maven takes its classes as up to
date - see `CLAUDE.md`, Gotchas.

## API

All endpoints are under `/api/v1` and need a signed-in session, except login, register and the two
of a password reset. Sign-in is a session cookie, and every write - login, register, reset and
logout included - must send the `XSRF-TOKEN` cookie's value back as the `X-XSRF-TOKEN` header. The
first account is created on first start from `APP_ADMIN_*`, the rest by registering; see
`CLAUDE.md`, Authentication.

| Auth | |
|---|---|
| `POST /api/v1/auth/login` | `{"username", "password"}`; 200 with the user, 401 on bad credentials |
| `POST /api/v1/auth/register` | `{"username", "email", "password", "confirmPassword"}`; 201 with the user, signed in; 400 on a rule, 409 on a taken username or email address |
| `PUT /api/v1/auth/password` | `{"currentPassword", "newPassword", "confirmPassword"}`; 204, the session kept under a new id and every other session of the account signed out; 400 on a wrong current password or a rule |
| `PUT /api/v1/auth/email` | `{"currentPassword", "email"}`; 204, every session kept; 400 on a wrong current password or a rule, 409 on an address another account has |
| `POST /api/v1/auth/password-reset` | `{"email"}`; 202 with no body whatever the address - only an enabled account's gets a mail with a reset link, valid once for 30 minutes; 400 on a malformed address, 500 if the mail cannot be sent |
| `POST /api/v1/auth/password-reset/confirm` | `{"token", "newPassword", "confirmPassword"}`; 204, every session of the account signed out and the caller not signed in; 400 on an unknown, expired or spent token, or a rule |
| `GET /api/v1/auth/me` | the signed-in user - username, email address (`null` if none) and roles - or 401 |
| `POST /api/v1/auth/logout` | 204 |

| Users, for admins only - anyone else gets 403 | |
|---|---|
| `GET /api/v1/users` | every account: id, username, enabled, roles, audit fields; never a password |
| `PUT /api/v1/users/{id}` | `{"enabled", "admin"}`; 204, and the account's sessions end if either changed; 400 for the admin's own account, 404 for an unknown id |

Every resource offers the same five operations. A recipe's ingredient lines are written with the recipe.
`?name=` on a list answers only the rows whose name contains it, in any case, and `[]` when none
does. Names compare as Danish: `brød` finds `Brød`, `brod` does not.

| Resource | Endpoints |
|---|---|
| `/api/v1/categories` | GET (all, `?name=`, by id), POST, PUT, DELETE |
| `/api/v1/ingredients` | GET (all, `?name=`, by id), POST, PUT, DELETE |
| `/api/v1/units` | GET (all, by id), POST, PUT, DELETE |
| `/api/v1/tags` | GET (all, `?name=`, by id), POST, PUT, DELETE |
| `/api/v1/recipes` | GET (all, `?name=`, by id), POST, PUT, DELETE |

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
in · `403` missing CSRF token · `404` unknown · `409` conflict: a duplicate name, or
a delete of something still in use.

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
