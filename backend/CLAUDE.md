# Backend

Spring Boot 4.1.1 on Java 25, MySQL, Liquibase-managed schema, Maven. Package root
`dk.serik.recipes`. Project-wide rules live in the root `CLAUDE.md`; this file describes only what
is in `backend/`.

## Authentication

Session-cookie sign-in with CSRF, in `config/SecurityConfig`. Everything under `/api` needs a
session except `POST /api/v1/auth/login` and `/register`; `/actuator/health` is open for Compose.
401 and 403 come back in the error envelope (codes 600-602), never as a redirect.

- **Audit stamping depends on it.** `created_by` is NOT NULL on every table but `recipe_tag`, a
  plain `@ManyToMany` join table whose insert cannot carry it (`db.changelog_1.3.xml`). It is set by
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
- **Accounts**: the first is created by `AdminBootstrap` from `app.admin.username` /
  `app.admin.password` (env `APP_ADMIN_*`), only while `app_user` is empty, with `ROLE_ADMIN`. Any
  other comes from `POST /api/v1/auth/register`: `ROLE_USER`, created by its own username, and
  signed in on the spot through `login()`. Both insert through `AppUserWriter`, with plain SQL:
  through JPA `BaseEntityListener` would stamp `created_by` from the `Session`, which holds no
  signed-in user at startup or during an anonymous registration.
- **Roles**: `ROLE_USER` for everyone, `ROLE_ADMIN` as well for an admin - the comma-separated
  `app_user.roles`, constants on `AppUser`. Enforced only where `SecurityConfig` says so:
  `/api/v1/users/**` needs `ROLE_ADMIN`, a 403 in the envelope (602) otherwise. Everything else is
  open to any signed-in account.
- **Accounts, for admins**: `UserController` lists them and, with `PUT /api/v1/users/{id}`
  `{"enabled", "admin"}`, disables or enables another account and grants or withdraws the admin
  role - written by `AppUserWriter.updateAccess`, so the PUT answers 204, not the account JPA
  loaded before the change. Either change ends every session of that account: a session keeps the
  roles it signed in with. A disabled account signs in with a wrong password's 401. An admin's
  own account is a 400 (606) - so whoever acts is an enabled admin, and there is always one. No
  create, no delete - by decision: an account is disabled instead.
- **Registration rules**: username 3-64 characters, letters (any alphabet), digits, `.`, `_`, `-`,
  unique regardless of case - `existsByUsernameIgnoreCase`, since H2 in the tests tells case apart
  where MySQL's collation does not; taken is a 409 (603) on `username`. Password at least 8
  characters and at most 72 **bytes**: bcrypt reads no further, and Spring Security throws rather
  than encode a longer one, so the service checks before encoding - `æ` counts two. The
  confirmation is compared there too, a 400 on `confirmPassword`.
- **Changing a password**: `PUT /api/v1/auth/password`, signed in. The new password takes
  registration's rules, from the same `checkNewPassword`. A wrong current password is a 400 on
  `currentPassword`, never a 401: the frontend reads any 401 as a session that has ended and goes
  to sign in. The current one is checked with the `AuthenticationManager`, as login does, and the
  new hash written by `AppUserWriter.updatePassword` - every password is encoded in that class.
  The session gets a new id, as at login; every other session of the account is ended.
- **Ending a user's sessions**: Spring Security's `SessionRegistry`, in memory, keyed by username.
  `AuthServiceImpl.login` registers each session itself - Spring does so only for a login it
  performs, which this is not. `HttpSessionEventPublisher` tells the registry when a session
  changes its id or ends. `SessionInformation.expireNow()` ends one: Spring's
  `ConcurrentSessionFilter`, switched on by `maximumSessions(-1)`, signs it out at its next request
  with the logout handlers - the `JSESSIONID` cookie deleted - and lets that request carry on
  anonymous: an ended session is no session. A protected endpoint answers the usual 401; a login
  just works. Answering in the filter instead refused the first sign-in of a browser still holding
  the ended session's cookie, since that login was its next request. MockMvc
  runs no servlet container, so in the ITs the registry never hears of a new id or an end: what
  depends on those is proven by e2e's `password.spec.ts`.

## Layout

| Package | Holds |
|---|---|
| `controllers` | Five entity `@RestController`s under `/api/v1/`, plus `AuthController` and `UserController` |
| `service` | One interface + one `Impl` per entity, plus `ServiceArguments` |
| `repository` | `JpaRepository` per entity, derived queries - one JPQL `@Query`, `countRecipesByUnitId`, where none fits |
| `model` | JPA entities, `BaseEntity`, `BaseIdentifierEntity`, `BaseEntityListener` |
| `dto` | Request/response shapes, `BaseDTO`, `BaseIdentityDTO` |
| `mapper` | Static entity/DTO converters |
| `exceptions` | `ServiceException`, error codes, `@ControllerAdvice`, envelopes |
| `config` | `SecurityConfig`, `SessionPopulatingFilter`, `AdminBootstrap` (see Authentication) |
| `bean` | `Session` - request-scoped, holds the username (see Authentication) |
| `validator` | `@UUID` + `UUIDValidator`, on every controller path id |
| `aspect` | `JpaLoggingAspect` - wraps every `JpaRepository` call for timing |

## Conventions

**Controllers are thin.** No try/catch, no validation logic. Id parsing, existence checks and
argument validation all live in the service, which throws `ServiceException` carrying an
`HttpStatus`; the advice turns that into the envelope. The one check before the service is `@UUID`
on every path id, so a malformed one is refused before any service runs. On `PUT` the path id is
authoritative and any id in the body is overwritten, so a mismatched payload cannot update a
different row. `POST` returns 201 with a relative `Location`, `/api/v1/tags/{id}`, built from the
controller's `PATH` constant: one built from the request named `http://backend:8080`, the address
behind the Next.js proxy, which no browser can reach.

**Services** are `@Transactional` at class level (`READ_COMMITTED`, `REQUIRED`, 5s timeout), with
read methods re-annotated `readOnly = true`. Ids arrive as `String` and go through
`ServiceArguments.toUuid`, which converts a null or malformed id into a 400 - without it
`UUID.fromString` would throw a raw `IllegalArgumentException` and surface as a 500. Path ids are
already checked by `@UUID`; the ids inside bodies - category, tags, ingredient lines - are not.

**DTOs** are immutable-ish: `@Getter` only, one `@Builder @Jacksonized` constructor, fields
`private`. `BaseDTO` carries the four audit fields. Dates are Jackson's default, ISO-8601 with
the offset (`2026-10-04T15:27:58Z`), and read back as they are written - `JsonContractIT` pins
both. MySQL keeps whole seconds, so a write's response carries fractions its later GET does not.
Bean Validation messages are keys resolved from `ValidationMessages.properties`.

**Mappers** are static utility classes, `from(entity)` and `fromDto(dto)`, each null-guarded.
No MapStruct.

**Entities** extend `BaseIdentifierEntity` (UUID id stored as `varchar(36)`, `GenerationType.AUTO`)
except `RecipeIngredient`, which uses a composite `RecipeIngredientPK`. `BaseEntity` is deliberately
not `Comparable` - the comment there records why.

## Errors

Everything leaves as `ExceptionEnvelope`: `errorCode`, `message`, `description`, and optionally
`validationExceptions[]`. Codes are in `ApplicationErrorCodes`, grouped by domain in hundreds
(recipe 50-70, category 100s, ingredient 200s, unit 300s, tag 400s, rating 500s, accounts 600s).

| Exception | Result |
|---|---|
| `ServiceException` | its own `httpStatus` + envelope; its `fieldErrors` become `validationExceptions[]` entries - `field(name)` on the builder makes one with the exception's message |
| `ConstraintViolationException` | 400 + `validationExceptions[]` |
| `MethodArgumentNotValidException` | 400 + `validationExceptions[]` (separate advice class) |
| `HandlerMethodValidationException` (a `@UUID` path id, and a PUT's `@Valid` body with it) | 400 + `validationExceptions[]`, body errors by field, parameters by name |
| Spring MVC's own rejections - any `ErrorResponse`: unknown path, wrong method, wrong media type | their own status (404, 405, 415, ...), code `REQUEST_REJECTED` (10), Spring's client-safe detail as message |
| `HttpMessageNotReadableException` (malformed JSON) | 400, code 10, fixed message - not an `ErrorResponse` |
| `DataIntegrityViolationException` (a database constraint, raised at commit) | a unique or foreign key violation, or a `DuplicateKeyException`: 409, code `DATA_CONFLICT` (40), generic message, the raw text logged only. Any other kind - NOT NULL, CHECK - goes to the catch-all as a 500 |
| anything else | 500, message replaced by a random reference id that is logged server-side |

The `ErrorResponse` branch sits inside the catch-all, which checks for it first: without it every
unknown URL was a 500 with an ERROR stack trace in the log. `FrameworkErrorStatusTest` covers it.

The 500 is deliberate: raw exception text carries SQL, table and constraint names. The
consequence is that anything unhandled looks identical to the client.

A service that can say *why* a write conflicts checks before writing and throws a 409 of its own:
`CategoryServiceImpl` rejects a duplicate name (104, on field `name`) and deleting a category
recipes use (105); `UnitServiceImpl` the same (311, 312), counting recipes rather than
`recipe_ingredient` lines; `TagServiceImpl` (411, 412) and `IngredientServiceImpl` (211, 212) the
same. Tag names are unique, like the others - the schema says so, whatever older comments claimed.
The database constraint behind each stays the backstop, caught by the
`DataIntegrityViolationException` handler. `RecipeServiceImpl` rejects a duplicate recipe name
(53). The backstop tells a conflict from a server fault by Hibernate's
`ConstraintViolationException.getKind()` on the cause - the same kinds on H2 and MySQL, checked in
V3 Part 3 - so a NOT NULL column left empty is a 500, not a 409. `DataIntegrityIT` feeds the real
handler the violations H2 really raises.

**Recipe writes carry their relations.** A POST or PUT sets the category, the tags and the
ingredient lines from the body, each by id of an existing row; nothing is created through a
recipe. The lists are authoritative: a PUT's lines replace the recipe's - an existing line takes
the new amount and unit, an omitted one is deleted - and its tags replace the tags. An absent list
leaves that relation as it is. A missing category is a 400 on the field `category` (101). Bad
lines - no ingredient, no unit, no amount, an amount over 9999.99 (the column is `DECIMAL(6,2)`),
an ingredient listed twice, identical lines included - are one 400 (63), every problem an entry on
its line and input: `recipeIngredients[2].amount`, `.ingredientId`, `.unitId`, the index the line's
place in the request. A repeat is reported on the later line. Nothing is written when one is
refused. The DTO's lines are a `List`, in the request's order; a response lists them in Danish
order of ingredient name, while the entity keeps them in a `Set`. The recipe's own POST and PUT
are the only way to write lines: the `/recipes/{id}/ingredients` sub-resource was removed in V3
Part 5, and its error codes 60-62 are not reused.

## Tests

`./mvnw clean verify`. Surefire runs `*Test`, Failsafe runs `*IT`. Jacoco is wired in, and the
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
| `DataIntegrityIT` | `@SpringBootTest` | nothing - real violations from H2, native SQL in rolled-back transactions, handed to the real handler |
| `AuthenticationIT`, `RegistrationIT`, `PasswordChangeIT`, `UsersIT` | `@SpringBootTest` + MockMvc | nothing - the real security chain, session and database; one shared context |
| `CategoryIT`, `UnitIT`, `TagIT`, `IngredientIT`, `RecipeIT` | `@SpringBootTest` + MockMvc | the sign-in (`@WithMockUser`, `csrf()`); writes against the real constraints |

**`AuthenticationIT`, `RegistrationIT`, `PasswordChangeIT`, `UsersIT` and the `<Entity>IT` classes are the only ones to exercise controller to service to repository
to database**, for every entity's writes. `RecipeIT` asserts a recipe's relations on a fresh GET
and on the join tables. None of them runs in a test transaction: it would postpone the
commit, and with it any constraint violation, past the asserted response. Everything else is verified against a mock of the layer beneath it - which is how
the null `created_by` stayed invisible to a green build until Part 4, and discarded recipe tags
until `RecipeIT` in Part 7. `JsonContractIT` exists because `@WebMvcTest` builds its own Jackson mapper, so
slice tests can pass while real serialization is broken - the same class of gap.

**MockMvc's `csrf()` changes the cached context it runs in.** It replaces the CSRF token repository
inside the context's filter with a test one that sets no cookie, for every later test there. A test
that reads the real `XSRF-TOKEN` cookie therefore cannot share a context with the `<Entity>IT`
classes: `RegistrationIT`, `PasswordChangeIT` and `UsersIT` take `AuthenticationIT`'s properties to share its
context instead. Alone `RegistrationIT` passed; after `CategoryIT` every test found no cookie.

## Build and run

Normally in the Docker stack. On the host, against the stack's MySQL on port 3307 - stop the stack's
backend first, since both use 8080:

```powershell
$env:DB_PORT="3307"; $env:DB_PASSWORD="..."; $env:DB_ADMIN_PASSWORD="..."; ./mvnw spring-boot:run
```

**Two database accounts.** Liquibase connects as `DB_ADMIN_USERNAME` (`recipesadmin`), which may
change the schema; the application as `DB_USERNAME` (`recipesuser`), which may only `SELECT`,
`INSERT`, `UPDATE` and `DELETE` - `spring.liquibase.user` / `.password` give Liquibase its own
connection on the datasource's url. The stack creates both in `mysql/initdb/accounts.sh`, on a
fresh volume only. Neither password has a default. Spring does not error on an unresolved
placeholder - it passes the literal text through - so a missing one surfaces as `Access denied for
user 'recipesadmin'` from Liquibase at startup, or `'recipesuser'` after it, not as a configuration
error. The tests run on H2 as its own `sa`, untouched by either. Add
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
- `ENV TZ=UTC` is **not** load-bearing. Dates are stored in UTC whatever the JVM's zone - proven
  in V2 Part 3 by running the backend in `Europe/Copenhagen` - and leave the API with their offset.
  The line keeps the JVM's own clock, logs and a write's response, in UTC as well.

## Gotchas

- Build with `clean`. VS Code's Java extension compiles into this same `target/` with the Eclipse
  compiler, which lacks the Lombok and Jackson 3 setup Maven uses, and Maven then takes its classes
  as up to date. The symptom is "Unresolved compilation problem" in a test that should compile.

- Do not define a `@Primary JsonMapper` bean. Jackson 3 is in use (`tools.jackson`), and the
  advice classes build their own mappers; overriding the context mapper has broken write endpoints
  before while slice tests stayed green.
- Lombok is pinned **ahead** of the Spring Boot parent for JDK 25 support, and Jacoco ahead for
  Java 25 class files. Both pins carry comments in `pom.xml`. Do not "tidy" them.
- `RecipeService.addRecipeRating` throws `UnsupportedOperationException`, and a recipe write
  carrying `recipeRatings` is rejected with `RECIPE_RATING_NOT_SUPPORTED` (70). Ratings are
  readable but not writable, by decision - see `docs/future_enhancements.md`.
- Name lookup: Category, Ingredient, Tag and Recipe services expose one query,
  `findAllByNameContains`, over the repository's `findAllByNameContainingIgnoreCase`. Without
  `IgnoreCase` it ignored case on MySQL, by its collation, and not on H2. The exact `findByName`
  stays in the repositories for the duplicate-name checks and nothing else. `?name=` on the list
  endpoints reaches the partial match; a controller only picks between it and `findAll`.
- The schema's collation is Danish, `utf8mb4_da_0900_ai_ci` (`db.changelog_1.4.xml`, MySQL only):
  `æ`, `ø` and `å` are letters of their own, so `Brod` and `Brød` - or `soren` and `Søren` - are
  different names, and case is still ignored. For `=`, Danish also takes `aa` for `å`: `Aalborg` is
  taken by `Ålborg`; `LIKE` does not. H2 has no such collation, so no test sees any of this - prove a
  change to it on the stack. Before V4 Part 8 MySQL's default made `ø` equal `o`.
- A constraint on a controller parameter changes how that method's `@Valid` body is validated:
  Spring validates both in one pass and raises `HandlerMethodValidationException`, not
  `MethodArgumentNotValidException`. Every `PUT` here has a `@UUID` path id, so its body errors take
  that route; a `POST` without a path id keeps the other. `PathIdValidationTest` covers both.
