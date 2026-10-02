# Future enhancements

Deliberately deferred work. Each entry records what is missing, why it was left, and what it would take.

---

## Validation exceptions from path variables and request params are not handled

**What is missing.** The `@ControllerAdvice` layer covers three exception types today:

| Handler | Exception | Result |
|---|---|---|
| `MethodArgumentNotValidExceptionHandler` | `MethodArgumentNotValidException` | 400 + `validationExceptions[]` |
| `ApplicationExceptionHandler` | `ConstraintViolationException` | 400 + `validationExceptions[]` |
| `ApplicationExceptionHandler` | `ServiceException` | its own status + envelope |

That covers a `@Valid @RequestBody`, which raises `MethodArgumentNotValidException`. It does **not** cover constraints placed directly on `@PathVariable` or `@RequestParam` — for example the project's own `@UUID` annotation:

```java
public CategoryDTO findById(@PathVariable @UUID String id)   // not currently written this way
```

Since Spring Framework 6.1 (Spring Boot 3.2+), Spring MVC applies built-in method validation for controller parameters and raises **`HandlerMethodValidationException`**, not `ConstraintViolationException`. Nothing in the advice handles it, so it would fall through to `handleGeneralException` and be reported as **500 Internal Server Error** — the opposite of the intended 400.

**Why it was left.** Controllers currently omit `@UUID` on path variables, so the gap is not reachable. Malformed ids are already rejected with a proper 400 by `ServiceArguments.toUuid` in the service layer (see resolved finding M8), and `CategoryControllerTest.shouldReturn400ForMalformedId` proves it end to end. Adding the annotation without first adding the handler would have turned a working 400 into a 500.

**Update 2026-09-29.** No longer a 500: `HandlerMethodValidationException` implements
`ErrorResponse` (via `ResponseStatusException`), and the catch-all now passes an `ErrorResponse`'s
own status through. It would be a 400 - but with only Spring's generic detail, no
`validationExceptions[]`. The dedicated handler below is still what gives it the same shape as the
other two validation handlers.

**What it would take.**

1. Add a handler to `ApplicationExceptionHandler`:
   ```java
   @ResponseStatus(HttpStatus.BAD_REQUEST)
   @ExceptionHandler(HandlerMethodValidationException.class)
   @ResponseBody
   public ExceptionEnvelope handleHandlerMethodValidationException(HandlerMethodValidationException ex) { ... }
   ```
   mapping each `ParameterValidationResult` into a `ValidationExceptionEnvelope`, so the response shape matches the other two handlers.
2. Add `@UUID` to the `@PathVariable` parameters on every controller.
3. Test it: assert 400 and a populated `validationExceptions[]` for a malformed id, and confirm the service is never invoked.

**Value.** Rejects malformed ids at the web boundary rather than one layer in, and finally puts the `@UUID` validator (`validator/UUID.java` + `UUIDValidator.java`) to use — it is tested but otherwise unreferenced, and is the last entry on the dead-code list in `code-review.md` kept for exactly this purpose.

---

**Re-checked on Spring Boot 4.1.1 (2026-09-08).** Boot 4 brings Jakarta Validation 3.1 and
Hibernate Validator 9, so this was re-verified rather than assumed. Behaviour is unchanged: the
`@Valid @RequestBody` path still returns 400 with the full envelope (`errorCode` plus
`validationExceptions[].objectName`), asserted by the controller slice tests, and the gap described
above is neither closed nor widened by the upgrade.

## Name-based lookup is inconsistent across services, and unexposed at the API

**What is missing.** Every domain concept in this application is identified to a user by its
name, but the ability to look one up by name was added ad hoc, service by service. The result is
four different levels of support and one naming inconsistency:

| Service | `findByName` | `findAllByNameContains` | Repository already supports |
|---|---|---|---|
| Category | ✅ | ❌ | **both** — `findAllByNameContains` exists but is not exposed |
| Ingredient | ✅ | ✅ | both (plus `findAllByDescriptionContains`, also unexposed) |
| Recipe | ❌ | ✅ | `findAllByNameContains`, `findAllByCategoryName` |
| Tag | ⚠️ `findTagByName` | ❌ | `findTagByName` |
| Unit | ❌ | ❌ | none |
| Rating | ❌ | ❌ | none — not applicable, a rating is a 1-5 value |

Three specific problems:

1. **Category has the repository method but not the service method.** `CategoryJpaRepository.findAllByNameContains` exists and is covered by `CategoryJpaRepositoryIT` ("two categories containing 'er'"), but `CategoryService` never exposes it, so nothing can reach it. It is working, tested, unreachable code.
2. **Tag names its method `findTagByName`** rather than `findByName` — the entity name is redundant inside `TagService`, and it breaks the pattern every other service follows.
3. **None of it is reachable over HTTP.** `CategoryController` and `IngredientController` expose only the five CRUD endpoints, so `findByName` and `findAllByNameContains` have no route at all.

**What it would take.**

1. Settle the service contract for the concepts where name lookup is meaningful — Category, Ingredient, Tag and Recipe:
   ```java
   Optional<XDTO> findByName(String name);        // exact match, one result or empty
   List<XDTO>     findAllByNameContains(String name);  // partial match, possibly empty
   ```
   Add what is missing (`Category.findAllByNameContains`, `Recipe.findByName`, `Tag.findAllByNameContains`), and rename `TagService.findTagByName` to `findByName`.
2. Expose partial match as a filter on the collection endpoint, which is the idiomatic REST shape and needs no new path:
   ```
   GET /api/v1/ingredients            -> all
   GET /api/v1/ingredients?name=mel   -> partial match, 200 + (possibly empty) list
   ```
   ```java
   @GetMapping
   public List<IngredientDTO> findAll(@RequestParam(required = false) String name) {
       return Objects.isNull(name) ? service.findAll() : service.findAllByNameContains(name);
   }
   ```
3. **Do not expose exact match over HTTP, but keep it on the service.** The two are not substitutes:
   `findAllByNameContains("Brod")` is a `LIKE` query that also matches `Rugbrod` and `Hvedebrod`, so it
   cannot answer *"is there a category called exactly Brod?"*. The API needs only the partial filter -
   these names are unique, so a full name returns a single-element list - but the service keeps
   `findByName` because that exact-match question has a real caller: the duplicate-name pre-check
   described in the next entry. Deleting it would remove the tool needed to fix that bug.

**Deliberately excluded.** `Unit` and `Rating`. Units are a fixed handful of measures and ratings are a 1-5 lookup table; neither has a repository query, and adding name search to them would be ceremony rather than capability. `UnitController` was built with the five CRUD endpoints only, for that reason.

**Value.** One predictable contract across the API instead of per-resource guesswork, and it makes reachable a repository method that is currently tested but dead.

---

## A duplicate name returns 500 instead of 409

**Update 2026-10-02.** Unit followed in Part 6a, the same way, counting recipes rather than
`recipe_ingredient` lines, and Tag in 6b. The entry below says Tag is unaffected because tag names
are deliberately not unique: that was wrong. The schema has always made `tag.name` unique, and it
was decided to keep it so. Ingredient followed in 6c. Only Recipe, whose name is unique too, still
answers a duplicate with the generic `DATA_CONFLICT` 409 - Part 7's to decide.

**Update 2026-10-01.** Done for Category in Part 5a, as described below, plus a check that refuses
to delete a category recipes use (`CATEGORY_IN_USE`). The `DataIntegrityViolationException`
handler is in place for every entity, so a duplicate Unit or Ingredient name is already a 409 with
the generic `DATA_CONFLICT` code. What remains is their service pre-checks, for a message that names
the field; Part 6 adds them. The same goes for deleting a Unit or Ingredient a recipe uses: the
`recipe_ingredient` foreign keys make it a generic 409 today, without saying why.

**What is wrong.** `name` is declared `unique = true` on four entities - `Category`, `Ingredient`,
`Recipe` and `Unit` - but no service checks for an existing name before saving. The unique
constraint is therefore enforced only by the database, and the resulting
`DataIntegrityViolationException` is not handled by any `@ControllerAdvice`. It falls through to
`handleGeneralException`.

Verified against `CategoryController` with a throwaway `@WebMvcTest` that made the service throw
what the database would throw:

```
STATUS=500
BODY={"errorCode":0,"message":"An unexpected error occurred","description":"Reference: 7e9298d8-..."}
```

A client that posts an existing category name is told the server broke. The correct answer is
**409 Conflict**, naming the field that collided.

This was unreachable until the REST layer existed. `POST` and `PUT` on Category, Ingredient and
Unit can all reach it today; Recipe will when its controller lands. `Tag` is unaffected - tag names
are deliberately not unique.

**What it would take.**

1. Pre-check in the service, which is what `findByName` is for:
   ```java
   categoryJpaRepository.findByName(dto.getName()).ifPresent(existing -> {
       throw ServiceException.builder()
               .message(String.format("A category named '%s' already exists", dto.getName()))
               .code(ApplicationErrorCodes.CATEGORY_ALREADY_EXISTS.getCode())
               .httpStatus(HttpStatus.CONFLICT)
               .build();
   });
   ```
   with a new `X_ALREADY_EXISTS` code per domain, following the existing grouping. Note `update()`
   needs the same check but must exclude the row being updated, or renaming a category to its own
   name would be rejected.
2. Add a `DataIntegrityViolationException` handler to `ApplicationExceptionHandler` returning 409,
   as a backstop for the race between the check and the insert, and for constraints the pre-check
   does not cover. Keep the message generic - the raw exception carries table and constraint names
   (see resolved finding M5).
3. Test both paths: the pre-check returning 409, and the handler translating the exception.

**Value.** Turns a 500 into an actionable 409 on the most likely client mistake, and closes the last
route by which raw persistence errors reach the exception handler of last resort.

---

## Dates carry no time zone, and do not round-trip

**What is wrong.** `BaseDTO` formats `created` and `updated` with `@JsonFormat(pattern =
"yyyy-MM-dd HH:mm")`. Two consequences:

1. **No zone.** Jackson writes the time in the zone the `OffsetDateTime` carries, which is the
   backend JVM's. Proven in Part 5c with a throwaway test against the application's `JsonMapper`:
   16:51 at +02:00 came out as `"2026-10-01 16:51"` on a host in `Europe/Copenhagen`. The same
   moment from the container reads 14:51. The client cannot tell which.
2. **No round trip.** `@Jacksonized` deserializes through the builder, which does not carry the
   format, so the API cannot read back a date it wrote; a client that PUTs a fetched object
   unchanged gets a 400.

**Workaround in place.** The backend image sets `TZ=UTC`, and the frontend shows the value with a
"UTC" label and never sends the audit fields.

**What it would take.** Drop the pattern so the dates serialize as ISO-8601 with an offset
(`2026-10-01T14:51:00Z`), which also round-trips; have the frontend format them in the browser's
local time; drop the "UTC" label. Assert the format in `JsonContractIT`. It changes the API
contract, so frontend and backend move together.

---

## Recipe ratings: read-only, deferred to a later version

**Decision.** Ratings are out of scope for this version by choice. What exists is deliberate, not
half-finished, and the split is unusual: everything *below* the service layer is complete for both
rating concepts.

| Layer | `Rating` (the 1-5 lookup) | `RecipeRating` (recipe x rating) |
|---|---|---|
| DB schema | ✅ `rating` table, `rating TINYINT UNIQUE` | ✅ `recipe_rating`, FKs to both tables |
| Entity | ✅ | ✅ |
| Repository | ✅ (CRUD only, no derived queries) | ✅ 3 derived queries |
| DTO | ✅ (no `@Jacksonized` yet) | ✅ (no `@Jacksonized` yet) |
| Mapper | ✅ entity to DTO | ✅ entity to DTO |
| Service | ✅ **complete CRUD, 17 tests** | ❌ **does not exist** |
| Controller | ❌ | ❌ |

**Current API behaviour.** Ratings are **readable but not writable**:

* `GET /api/v1/recipes/{id}` returns `recipeRatings`, because `RecipeMapper` already maps them.
* `POST`/`PUT` on a recipe carrying `recipeRatings` is rejected with **400 `RECIPE_RATING_NOT_SUPPORTED(70)`**.
  Before that guard the field was silently discarded, which lost data without telling anyone.
* `RecipeService.addRecipeRating` throws `UnsupportedOperationException`, and no `/ratings` route is exposed.

The asymmetry is intentional: the data is real and seeded, so hiding it on read would be extra work
to conceal something about to be enabled.

**What finishing it would take** - roughly one service and one controller, since the expensive
layers are already built and tested:

1. `RecipeRatingService` + impl: `addRecipeRating`, `updateRecipeRating`, `deleteRecipeRating`,
   `findAllByRecipeId`. The repository queries exist and are covered by `RecipeRatingJpaRepositoryIT`.
2. Implement `RecipeServiceImpl.addRecipeRating`, add a `handleRecipeRatings` alongside
   `handleRecipeIngredients`, and **delete `rejectRecipeRatings`** - the guard is scaffolding for the
   deferral, and its two tests go with it.
3. `@Jacksonized` on `RatingDTO` and `RecipeRatingDTO`, plus `@Min(1) @Max(5)` on `rating`. Nothing
   above the database's `TINYINT` currently constrains the value.
4. `RatingController` (five CRUD endpoints) and `/api/v1/recipes/{id}/ratings` routes shaped like the
   existing ingredients sub-resource.

**Value.** The feature the whole `recipe_rating` table exists for. Until then the guard keeps the
limitation explicit at the API boundary rather than silent.

---

## Multiple users: the table exists, the features do not

**Status.** Part 4 added the `app_user` table, a `UserDetailsService` backed by it, and a first-run
`AdminBootstrap`, so the schema supports multiple users. What is missing is everything that would
make a second user meaningful.

**What is missing.**

1. **No way to create a second account.** `AdminBootstrap` runs only while the table is empty, so
   after first run the only route to another account is an INSERT by hand. Needs a user
   administration screen, or at minimum a sign-up endpoint, both of which need roles enforced
   first.
2. **Roles are stored but barely used.** `AppUser.roles` is a comma-separated column and the
   security chain authenticates every endpoint, but nothing authorises differently per role - an
   ordinary user can do everything an admin can. Splitting the column into an `app_user_role` join
   table is premature until a screen manages roles; enforcing `ROLE_ADMIN` on the destructive
   endpoints is not.
3. **Nothing is owned by anyone.** `created_by` records a username as free text, not a foreign key
   to `app_user.id`, so recipes cannot be filtered to their author and a renamed user orphans
   their audit trail. Turning it into a FK requires migrating every existing row and changing
   every DTO that exposes it - do not do it as a side effect of something else.
4. **No password change, no reset, no disable-in-the-UI.** The `enabled` column exists and is
   honoured on login; nothing sets it.

**Value.** Per-user recipe lists, private drafts, and an audit trail that can be joined and
trusted rather than read as a string.

---

## A single database account does all the work

**What is missing.** The stack creates one MySQL account, and both Liquibase and the running
application connect as it. That account therefore needs DDL rights permanently, because Liquibase
creates tables at every startup where a changeset is pending - which means the runtime datasource
can also drop them.

**Why it was left.** Two accounts is the correct production shape, but for a local MVP it doubles
the credentials in `.env` and adds an init script to maintain, in exchange for a boundary that
nothing local crosses.

**What it would take.** A `recipesadmin` account with DDL rights on the schema for
`spring.liquibase.user` / `.password`, and `recipesuser` restricted to `SELECT, INSERT, UPDATE,
DELETE` for the runtime datasource. MySQL's entrypoint cannot express this - setting
`MYSQL_USER` / `MYSQL_PASSWORD` grants that account `ALL PRIVILEGES` on the schema - so both
accounts have to be created by an init script mounted into
`/docker-entrypoint-initdb.d`, which runs once while the data volume is empty.

**Value.** The application can no longer drop its own tables, and a Liquibase change becomes a
deliberate act with its own credentials rather than something the runtime user could do by
accident.

