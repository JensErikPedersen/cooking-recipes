# Backend

Spring Boot 4.1.1 on Java 25, MySQL, Liquibase-managed schema, Maven. Package root
`dk.serik.recipes`. Project-wide rules live in the root `CLAUDE.md`; this file describes only what
is in `backend/`.

## Authentication

Session-cookie sign-in with CSRF, in `config/SecurityConfig`. Everything under `/api` needs a
session except `POST /api/v1/auth/login`; `/actuator/health` is open for Compose. 401 and 403 come
back in the error envelope (codes 600-602), never as a redirect.

- **Audit stamping depends on it.** `created_by` is NOT NULL on all nine tables. It is set by
  `BaseEntityListener.prePersist` and by the `save` of every service except `RecipeServiceImpl`,
  all reading `Session.getUserName()`. `SessionPopulatingFilter`, inside the security chain after
  authorization, is what fills that request-scoped bean. It is created in `SecurityConfig`, not a
  `@Component`, so Boot does not register it twice and web slices do not pick it up.
- **Login does by hand what form login would** (`AuthServiceImpl`): it changes the session id, since
  an anonymous 401 already creates a session and a pre-login id must not become the signed-in one
  (session fixation), and it saves the context explicitly - Spring Security 6+ no longer does, and
  without it the next request is anonymous again.
- **CSRF**: `csrf.spa()` - token in a readable `XSRF-TOKEN` cookie, sent back as `X-XSRF-TOKEN`.
  In Security 7.1 its handler reads the token on every request, which writes the cookie; no extra
  filter is needed. Login and logout are CSRF-protected too.
- **Logout** is Spring Security's filter on `POST /api/v1/auth/logout` (204), not a controller
  method. It also deletes the `JSESSIONID` cookie, which the frontend's `proxy.ts` checks for.
- **One account**, created by `AdminBootstrap` with plain SQL from `app.admin.username` /
  `app.admin.password` (env `APP_ADMIN_*`), only while `app_user` is empty. Plain SQL because JPA
  would fire `BaseEntityListener`, which needs a request. No user administration - by decision.

## Layout

| Package | Holds |
|---|---|
| `controllers` | Five entity `@RestController`s under `/api/v1/`, plus `AuthController` |
| `service` | One interface + one `Impl` per entity, plus `ServiceArguments` |
| `repository` | `JpaRepository` per entity, derived queries - one JPQL `@Query`, `countRecipesByUnitId`, where none fits |
| `model` | JPA entities, `BaseEntity`, `BaseIdentifierEntity`, `BaseEntityListener` |
| `dto` | Request/response shapes, `BaseDTO`, `BaseIdentityDTO` |
| `mapper` | Static entity/DTO converters |
| `exceptions` | `ServiceException`, error codes, `@ControllerAdvice`, envelopes |
| `config` | `SecurityConfig`, `SessionPopulatingFilter`, `AdminBootstrap` (see Authentication) |
| `bean` | `Session` - request-scoped, holds the username (see Authentication) |
| `validator` | `@UUID` + `UUIDValidator`. Tested, but **not referenced anywhere** |
| `aspect` | `JpaLoggingAspect` - wraps every `JpaRepository` call for timing |

## Conventions

**Controllers are thin.** No try/catch, no validation logic. Id parsing, existence checks and
argument validation all live in the service, which throws `ServiceException` carrying an
`HttpStatus`; the advice turns that into the envelope. On `PUT` the path id is authoritative and
any id in the body is overwritten, so a mismatched payload cannot update a different row. `POST`
returns 201 with a `Location` header built from `ServletUriComponentsBuilder`.

**Services** are `@Transactional` at class level (`READ_COMMITTED`, `REQUIRED`, 5s timeout), with
read methods re-annotated `readOnly = true`. Ids arrive as `String` and go through
`ServiceArguments.toUuid`, which converts a null or malformed id into a 400 - without it
`UUID.fromString` would throw a raw `IllegalArgumentException` and surface as a 500.

**DTOs** are immutable-ish: `@Getter` only, one `@Builder @Jacksonized` constructor, fields
`private`. `BaseDTO` carries the four audit fields and formats timestamps as
`yyyy-MM-dd HH:mm` - note that this **does not round-trip**, since the pattern has no offset, so
assert on responses with `jsonPath` rather than deserialising back into a DTO. Bean Validation
messages are keys resolved from `ValidationMessages.properties`.

**Mappers** are static utility classes, `from(entity)` and `fromDto(dto)`, each null-guarded.
No MapStruct.

**Entities** extend `BaseIdentifierEntity` (UUID id stored as `varchar(36)`, `GenerationType.AUTO`)
except `RecipeIngredient`, which uses a composite `RecipeIngredientPK`. `BaseEntity` is deliberately
not `Comparable` - the comment there records why.

## Errors

Everything leaves as `ExceptionEnvelope`: `errorCode`, `message`, `description`, and optionally
`validationExceptions[]`. Codes are in `ApplicationErrorCodes`, grouped by domain in hundreds
(recipe 50-70, category 100s, ingredient 200s, unit 300s, tag 400s, rating 500s).

| Exception | Result |
|---|---|
| `ServiceException` | its own `httpStatus` + envelope; its optional `field` becomes a `validationExceptions[]` entry |
| `ConstraintViolationException` | 400 + `validationExceptions[]` |
| `MethodArgumentNotValidException` | 400 + `validationExceptions[]` (separate advice class) |
| Spring MVC's own rejections - any `ErrorResponse`: unknown path, wrong method, wrong media type | their own status (404, 405, 415, ...), code `REQUEST_REJECTED` (10), Spring's client-safe detail as message |
| `HttpMessageNotReadableException` (malformed JSON) | 400, code 10, fixed message - not an `ErrorResponse` |
| `DataIntegrityViolationException` (a database constraint, raised at commit) | 409, code `DATA_CONFLICT` (40), generic message; the raw text is logged only |
| anything else | 500, message replaced by a random reference id that is logged server-side |

The `ErrorResponse` branch sits inside the catch-all, which checks for it first: without it every
unknown URL was a 500 with an ERROR stack trace in the log. `FrameworkErrorStatusTest` covers it.

The 500 is deliberate: raw exception text carries SQL, table and constraint names. The
consequence is that anything unhandled looks identical to the client.

A service that can say *why* a write conflicts checks before writing and throws a 409 of its own:
`CategoryServiceImpl` rejects a duplicate name (104, on field `name`) and deleting a category
recipes use (105); `UnitServiceImpl` the same (311, 312), counting recipes rather than
`recipe_ingredient` lines. The database constraint behind each stays the backstop, caught by the
`DataIntegrityViolationException` handler. Ingredient has only the backstop until Part 6.

## Tests

`./mvnw verify`. Surefire runs `*Test`, Failsafe runs `*IT`. Jacoco is wired in, and the
`@{argLine}` it sets is load-bearing - if `prepare-agent` fails, both Surefire and Failsafe go down
with it, not just the report.

**Everything runs on in-memory H2 in MySQL mode**, not MySQL. `src/test/resources/application.properties`
points Liquibase at the production master changelog, with `spring.liquibase.contexts=test`.
Consequences worth knowing:

- H2 is not MySQL. Collation, unique-index behaviour on utf8 and error messages all differ.
- `db.changelog_1.1.xml` is the seed. Its 28 lookup rows - category, ingredient, rating, tag,
  unit - load in tests and in the application alike, from this one copy.
- Its recipe, `recipe_ingredient` and `recipe_tag` changeSets carry `contextFilter="!test"`. Tests
  load those same recipes per test with `@Sql("/db/test-data/insert_recipes.sql")` and siblings
  instead; without the filter every such test dies on a duplicate primary key. With no context set,
  as in the running application, they load.

Test layers, and what each one mocks:

| Test | Kind | Mocked |
|---|---|---|
| `*ControllerTest` | `@WebMvcTest`, `addFilters = false` | the service (`@MockitoBean`); no security chain |
| `*ServiceTest` | Mockito | the repositories |
| `*JpaRepositoryIT` | `@DataJpaTest` | nothing below it, but no controller or service |
| `JsonContractIT` | `@SpringBootTest` | serialization contract only |
| `AuthenticationIT` | `@SpringBootTest` + MockMvc | nothing - the real security chain, session and database |
| `CategoryIT`, `UnitIT` | `@SpringBootTest` + MockMvc | the sign-in (`@WithMockUser`, `csrf()`); writes against the real constraints |

**Only `AuthenticationIT`, `CategoryIT` and `UnitIT` exercise controller to service to repository
to database**, and only for category and unit writes. Neither runs in a test transaction: it would postpone the
commit, and with it any constraint violation, past the asserted response. Everything else is verified against a mock of the layer beneath it - which is how
the null `created_by` stayed invisible to a green build until Part 4. Recipe writes get the same
end-to-end test as `RecipeIT` in Part 7. `JsonContractIT` exists because `@WebMvcTest` builds its own Jackson mapper, so
slice tests can pass while real serialization is broken - the same class of gap.

## Build and run

Normally in the Docker stack. On the host, against the stack's MySQL on port 3307 - stop the stack's
backend first, since both use 8080:

```powershell
$env:DB_PORT="3307"; $env:DB_PASSWORD="..."; ./mvnw spring-boot:run
```

`DB_PASSWORD` has no default. Spring does not error on an unresolved placeholder - it passes the
literal text through - so a missing password surfaces as `Access denied for user 'recipesuser'`
from Liquibase at startup, not as a configuration error. Add
`-Dspring-boot.run.profiles=dev` for SQL and bind-value logging; do not enable it where real user
data flows.

Liquibase runs during Spring startup and **does not retry a refused connection**, so the database
must be reachable before the application starts.

## Docker

- `Dockerfile` builds with the official `maven:3.9.11-eclipse-temurin-25` image rather than
  `./mvnw`: the wrapper jar is gitignored, so a clean checkout does not have it. Keep the Maven
  version in step with `.mvn/wrapper/maven-wrapper.properties`.
- `lombok.config` must be in the build context. Without it `@Jacksonized` emits Jackson 2
  annotations and every write DTO fails to deserialise at runtime, with a green build.
- The runtime image installs `curl` solely for the Compose healthcheck on `/actuator/health`, the
  only actuator endpoint exposed. It answers only once Liquibase has finished.
- The container runs as the unprivileged `recipes` user.
- `ENV TZ=UTC` is load-bearing. Dates leave the API in the JVM's zone without an offset, and the
  frontend labels them UTC; the base image is UTC already, the line keeps it so.

## Gotchas

- Do not define a `@Primary JsonMapper` bean. Jackson 3 is in use (`tools.jackson`), and the
  advice classes build their own mappers; overriding the context mapper has broken write endpoints
  before while slice tests stayed green.
- Lombok is pinned **ahead** of the Spring Boot parent for JDK 25 support, and Jacoco ahead for
  Java 25 class files. Both pins carry comments in `pom.xml`. Do not "tidy" them.
- `RecipeService.addRecipeRating` throws `UnsupportedOperationException`, and a recipe write
  carrying `recipeRatings` is rejected with `RECIPE_RATING_NOT_SUPPORTED` (70). Ratings are
  readable but not writable, by decision - see `docs/future_enhancements.md`.
- `CategoryJpaRepository.findAllByNameContains` is tested but unreachable: no service exposes it.
  Same for the `@UUID` validator.
