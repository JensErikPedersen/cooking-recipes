# Code Review — recipes

**Reviewed:** 2026-09-06 · branch `Reviewing_with_claude` · commit `54f8d45`
**Scope:** all of `src/main` and `src/test` (68 Java files), `pom.xml`, Liquibase changelogs, property files, helper scripts.
**Last updated:** 2026-09-08 — Phases 1-3 complete, plus M1, a cleanup batch (L2, L3, M7, L9) and the REST layer (5 controllers, 28 endpoints). All Critical and High findings resolved. Findings marked ✅ are resolved; see the [Resolution log](#resolution-log).

---

## Verdict

*The verdict below describes the codebase as first reviewed. Items since resolved are marked ✅ and recorded in the [Resolution log](#resolution-log).*

The foundations are sound. The layering is clean and consistent, the Liquibase-owned schema is disciplined, and the test suite is genuinely substantial — 161 tests, all passing, 68% line coverage. This is a well-structured project that was left mid-flight.

The problems cluster in three places:

1. **The REST layer does not exist.** `controllers/` is empty. ✅ *built since - five controllers, 28 endpoints; Rating deferred.* Everything below it — services, DTOs, mappers, two `@ControllerAdvice` handlers, a custom `@UUID` validator — was built for an HTTP surface that was never written. Roughly a quarter of the exception-handling code is unreachable, which is why coverage there sits at 24%.
2. **`RecipeServiceImpl` is unfinished** in ways the tests do not reveal. Four interface methods return `null` unconditionally, and `update()` silently discards changes. ✅ *resolved*
3. **Auditing and error-status handling drifted** between the six service implementations. They were clearly written one after another, and later ones diverge from earlier ones in small, consequential ways. ✅ *resolved — correctness (H1/H2), contracts (H5/H6) and the null-argument policy (M9)*

None of this blocks the build. All of it will bite as soon as a controller calls into it.

### Health snapshot

| Area | At review | Now |
|---|---|---|
| Build | Green | Green (`mvn clean verify`) |
| Tests | 161 passing (100 unit, 61 IT) | **284 passing** (222 unit, 62 IT) |
| Line coverage | 68% | **82%** |
| Critical findings | 4 | **0 open** (4 resolved) |
| High findings | 7 | **0 open** (7 resolved) |
| Medium findings | 9 | **4 open** (5 resolved) |
| Low findings | 8 | **5 open** (4 resolved, 1 added) |

Coverage by package, from the JaCoCo report:

| Package | At review | Now |
|---|---|---|
| `service` | 89% | 88% |
| `dto` | 78% | 78% |
| `validator` | 77% | 77% |
| `mapper` | 73% | 73% |
| `model` | 51% | 51% |
| `exceptions` | **24%** | 31% |
| `aspect` | 28% | 28% |
| `config` | 27% | 27% |
| `enums` | **0%** | **0%** |

The two lowest numbers are the story: `exceptions` is unreachable without controllers, and `enums` is dead code. `service` dips a point despite the new tests because the three implemented ingredient operations added more lines than the tests reach branches in.

---

## Critical

### ✅ C1. Four `RecipeService` methods are unimplemented stubs returning `null`

`service/RecipeServiceImpl.java:122-139`

```java
public RecipeDTO addRecipeIngredient(RecipeIngredientDTO dto)    { return null; }
public RecipeDTO deleteRecipeIngredient(RecipeIngredientDTO dto) { return null; }
public RecipeDTO updateRecipeIngredient(RecipeIngredientDTO dto) { return null; }
public RecipeDTO addRecipeRating(RecipeRatingDTO dto)            { return null; }
```

These are declared on the `RecipeService` interface, so they are part of the published contract. A caller gets `null` back and no indication that nothing happened. `RecipeServiceTest` does not exercise any of them.

**Action:** implement them, or delete them from the interface until they are ready. If they must stay as placeholders, throw `UnsupportedOperationException` so a caller fails loudly instead of silently. Add tests alongside the implementation.

### ✅ C2. `RecipeServiceImpl.update()` silently loses data and never reports failure

`service/RecipeServiceImpl.java:106-120`

Three separate defects in fifteen lines:

- Returns `null` when `dto.getId()` is null (line 108) instead of throwing — inconsistent with every other service, which throws `ServiceException` with `BAD_REQUEST`.
- Never copies `instructions` onto the managed entity. `setName` and `setDescription` are called; `setInstructions` is not. **Editing a recipe silently discards its instructions.**
- When the recipe is not found, it falls through and returns the input `dto` unchanged, as though the update had succeeded. Every sibling service throws `NOT_FOUND` here.

It also returns the caller's own `dto` rather than mapping the saved entity, so the response never reflects what was actually persisted (generated ids, audit fields, normalised values).

**Action:** add `setInstructions`; throw `ServiceException` for both the null-id and not-found cases, matching `CategoryServiceImpl.update`; return `RecipeMapper.from(savedRecipe)`. Cover all three paths with tests.

### ✅ C3. `RatingServiceImpl` throws exceptions with no HTTP status, turning 400s into 500s

`service/RatingServiceImpl.java:68-71, 79-82, 102-105`

Three `ServiceException.builder()` calls set `.code()` and `.message()` but omit `.httpStatus()`. `ApplicationExceptionHandler` then does:

```java
return new ResponseEntity<>(exceptionEnvelope, ex.getHttpStatus());   // line 34
```

`ResponseEntity` rejects a null status with `IllegalArgumentException`, which is caught by the generic `Exception` handler and returned as **500 Internal Server Error**. A client sending a null rating or a bad id would get a server error instead of `400`/`404`, with the real message replaced by the `IllegalArgumentException` text.

This is latent only because no controller invokes the handler yet. It activates the moment one does.

**Action:** add the appropriate `.httpStatus(...)` to all three. Then make it unrepresentable: give `ServiceException` a non-null default (`INTERNAL_SERVER_ERROR`) or validate in the constructor, so a missing status can never reach `ResponseEntity` again.

### ✅ C4. `RecipeServiceImpl.save()` throws `NullPointerException` for a recipe with no ingredients

`service/RecipeServiceImpl.java:182`

```java
recipeDTO.getRecipeIngredients().forEach(recipeIngredientDTO -> { ... });
```

`recipeIngredients` is an ordinary nullable field on `RecipeDTO` with no default. Saving a recipe without ingredients — an entirely reasonable request — dereferences null. `handleRecipeIngredients` is called from both `save()` and `update()`, so both are affected.

**Action:** guard with `Objects.isNull(...)` and return early, or default the field to an empty set in the `RecipeDTO` builder. Add a test that saves a recipe with no ingredients.

---

## High

### ✅ H1. `UnitServiceImpl.save()` hardcodes a username

`service/UnitServiceImpl.java:61`

```java
toBeSaved.setCreatedBy("Majken");
```

Every unit ever created is attributed to "Majken" regardless of who is logged in. The `session` field is injected and used elsewhere in the same class, so this looks like leftover scaffolding.

**Action:** replace with `session.getUserName()`. Grep the other services for the same pattern before closing this out.

### ✅ H2. `UnitServiceImpl.update()` overwrites `createdBy` on every update

`service/UnitServiceImpl.java:80`

```java
managedUnit.setCreatedBy(session.getUserName());   // in update()
```

This destroys the original creator on every edit. It is also redundant — `BaseEntityListener.preUpdate` already sets `updatedBy`. The line should not exist.

**Action:** delete it. Add an assertion to `UnitJpaRepositoryIT.shouldUpdateUnit` that `createdBy` is *unchanged* after an update — the current test does not check this, which is why the bug survived.

### ✅ H3. Database credentials are committed to the repository

`src/main/resources/application.properties:10-11`, `src/main/resources/db/liquibase.properties:4-5`, `scripts/mysql_users.sql:5,8`

A MySQL root password appears in plaintext in two property files, and the `mysql_users.sql` bootstrap script creates both application users with the literal password `password`. All three are tracked in git, so the values are in the history as well as the working tree.

**Action:** move credentials to environment variables (`${DB_PASSWORD}`) or a Spring profile that is git-ignored, and rotate anything that was ever used against a real database. Purging git history is optional for a local-only project but the values should stop being read from tracked files. Change the `mysql_users.sql` passwords or parameterise them.

### ✅ H4. SQL statements and bind values are logged at TRACE in the main profile

`src/main/resources/application.properties:18-35`

```properties
spring.jpa.properties.hibernate.show_sql=true
spring.jpa.properties.hibernate.format_sql=true
logging.level.org.hibernate.orm.jdbc.bind=TRACE
logging.level.org.hibernate.SQL=DEBUG
```

There is only one profile, so these are the production settings. Every statement and every bound parameter value goes to the log — a significant throughput cost, and it writes user data into log files.

**Action:** split into `application-dev.properties` (verbose) and keep the base profile quiet. This is the natural moment to introduce profiles, which the project currently lacks entirely.

### ✅ H5. `Optional<List<T>>` repository return types never signal "empty"

`repository/IngredientJpaRepository.java:14,16`, `repository/RecipeIngredientJpaRepository.java:16,18`, `repository/RecipeRatingJpaRepository.java:12-16`

Spring Data never returns an empty `Optional` for a collection-returning derived query — it returns `Optional.of(emptyList())`. So this check in `IngredientServiceImpl.findAllByNameContains` is always true:

```java
Optional<List<Ingredient>> ingredients = repository.findAllByNameContains(name);
if(ingredients.isPresent()) { ... }     // always true, even for no matches
```

The service returns `Optional.of(emptyList())` where the caller was clearly meant to receive `Optional.empty()`. The wrapper adds a layer of indirection that communicates the opposite of what it appears to.

**Action:** change these signatures to return plain `List<T>` and let callers test `isEmpty()`.

### ✅ H6. Two different `findAll` contracts across the service layer

`CategoryService`, `IngredientService`, `UnitService`, `TagService`, `RatingService` return `Optional<List<XDTO>>`, and return `Optional.empty()` when the list is empty. `RecipeService` returns a bare `List<RecipeDTO>` and returns an empty list.

A controller layer would need two different idioms for the same operation depending on which service it called. `Optional` wrapping a collection is an anti-pattern in its own right — "no results" is what an empty list already means.

**Action:** standardise on `List<T>`, returning empty for no results. This touches five services and their tests, so it is best done in one pass before controllers are written and lock the shape in.

### ✅ H7. `RecipeDTO` fabricates a non-null `category` even when none was supplied

`dto/RecipeDTO.java:39`

```java
this.category = CategoryDTO.builder().id(categoryId).name(categoryName).build();
```

The builder unconditionally constructs a `CategoryDTO`. When neither `categoryId` nor `categoryName` is given, the result is a fully-null `CategoryDTO` rather than a null `category`. Two consequences:

- With `@JsonInclude(NON_NULL)` the field serialises as `"category":{}` — an empty object where the field should have been omitted.
- `RecipeServiceImpl.handleCategory` (line 166) reads `recipeDTO.getCategory().getId()`, finds it null, takes the "new Category" branch, and **creates a `Category` row with a null name** — which then violates the `NOT NULL` constraint with an opaque database error.

**Action:** only build the `CategoryDTO` when at least one of the two values is non-null. Then have `handleCategory` reject a null category explicitly with a `BAD_REQUEST` `ServiceException`.

---

## Medium

### ✅ M1. `handleCategory` creates categories as an undeclared side effect

`service/RecipeServiceImpl.java:165-179`

Saving a recipe with a category name but no id silently creates a new `Category` row with `description = "N/A"`. Nothing in the method name or the `RecipeService` contract suggests that saving a recipe can create a category. A typo in a category name silently produces a duplicate category rather than an error.

Separately, if a category *id* is supplied but not found, the `if(category.isPresent())` guard simply does nothing — leaving `recipe.category` null and producing a `NOT NULL` violation downstream instead of a clear 404.

**Action:** decide whether implicit creation is intended. If yes, document it on the interface and set a real description. If no, throw `CATEGORY_NOT_FOUND`. Either way, add an `else` branch for the not-found case.

### ✅ M2. `handleRecipeIngredients` swallows invalid input

`service/RecipeServiceImpl.java:181-206`

Ingredients that fail validation are logged (`log.info("RecipeIngredientDTO is not valid")`) and skipped. A missing ingredient or unit id is silently dropped. The caller receives a success response describing a recipe that is missing ingredients they asked for.

**Action:** collect the failures and throw a `ServiceException` listing them, or at minimum return them in the response. Silent partial success is the hardest kind of bug to trace back.

**Resolved 2026-09-09.** Every entry is now validated before any is attached, and all problems are
reported together in a single 400 (`RECIPE_INGREDIENTS_INVALID(63)`) rather than one at a time — a
caller fixing a bulk payload needs the whole list.

The finding understated the severity. The guard required a `recipeId` on each nested ingredient, but
**on create the recipe has no id yet**, so a realistic `POST /api/v1/recipes` carrying ingredients
dropped *all* of them and returned 201 describing a recipe with none. The nested `recipeId` is now
ignored entirely: on create there is nothing to match, and on update the recipe being edited is
authoritative — the same path-id-wins rule the controllers already apply.

Three red tests written first, all three failing as predicted (`getRecipeIngredients()` came back
`null` on the create path). Also removes the no-op `.amount(nonNull(x) ? x : null)` ternary noted in
L8.

**Two existing tests had to change, and both were passing for the wrong reason** — the fixture-shaped
blind spot this document already warns about, found twice more:

- `shouldSaveNewRecipeWithIngredients` stubbed `findByRecipeIdAndIngredientId` with the id of the
  recipe it was about to create. No real caller can supply that, so the test never exercised the
  create path it claimed to cover.
- `shouldUpdateRecipe` stubbed nothing for ingredients at all. Mockito returned empty, all three
  ingredients were silently dropped, and the test passed because it only asserted the text fields.

### ✅ M3. Malformed unbalanced parentheses in the AOP pointcut

`aspect/JpaLoggingAspect.java:19`

```java
@Pointcut("execution(* org.springframework.data.jpa.repository.JpaRepository+.*(..))))")
```

Three surplus closing parentheses. The application context starts without complaint, so this is not fatal today, but the expression is clearly not what was intended, and **nothing tests this aspect** — coverage for the package is 28%, which is the class declaration and little else. Whether the advice ever actually fires is unverified.

**Action:** fix the parentheses, then add a test that forces a repository exception and asserts the aspect logged it. Without that test there is no evidence the class does anything at all.

**Partially resolved (2026-09-08, Spring Boot 4 upgrade).** The parentheses are fixed and the
javadoc moved above the annotations, where it documents the class rather than nothing. This was
pulled forward as pre-flight for the Boot 4 parent bump: `aspectjweaver` is on the compile
classpath via `spring-aspects`, so the aspect is live, and a malformed pointcut that Boot 3
tolerated could have failed at context startup under Boot 4's reorganised AOP auto-configuration —
which would have looked like a Boot 4 problem.

**Fully resolved 2026-09-09.** `JpaLoggingAspectIT` forces a repository exception and asserts the
aspect logged it at ERROR and rethrew it unchanged. **The advice does fire** — that was a genuinely
open question, since Spring Data repositories are themselves proxies and an `execution` pointcut
against them could plausibly have matched nothing.

It is an integration test on purpose. The advice body is four lines; what needed proving was the
pointcut, and a unit test calling `logRepositoryErrors` with a mock join point would have passed
whether or not the aspect was wired to anything. `@DataJpaTest` would not do either — it is a slice
and does not scan `@Component`, so the aspect would not be registered. Package coverage 29% → 71%.

**Also found: the coverage report excluded every integration test.** The JaCoCo `report` execution
was bound to the `test` phase, so it ran before failsafe. Every figure this document has quoted was
unit-tests-only. Rebinding it to `post-integration-test` moves the real total from 83.6% to **87.0%**
— the ITs were always running, they were just never counted.

**Found while writing it:** the single-IT command documented in `CLAUDE.md` was broken.
`./mvnw verify -Dit.test=X -DskipTests` runs **zero** tests, because failsafe binds the same
`skipTests` property — the build passes having executed nothing, which is the worst possible
failure mode for a command you reach for when debugging one test. Corrected to
`-Dtest='!*' -Dsurefire.failIfNoSpecifiedTests=false`.

### ✅ M4. `BaseEntity` implements raw `Comparable` and violates its contract

`model/BaseEntity.java:17,58-64`

```java
public class BaseEntity implements Comparable, Serializable {
    public int compareTo(Object o) {
        if(o!=null && o instanceof BaseEntity && created!=null && ...) {
            return created.compareTo(...);
        }
        return -1;      // for null, wrong type, or null timestamps
    }
}
```

Returning `-1` for null breaks the contract (which requires `NullPointerException`) and is not antisymmetric: for two entities with null `created`, both `a.compareTo(b)` and `b.compareTo(a)` return `-1`. Sorting such a collection, or placing these in a `TreeSet`, produces undefined behaviour. The raw type means no compile-time checking either.

**Action:** make it `Comparable<BaseEntity>`, throw `NullPointerException` for null, and use `Comparator.nullsLast` semantics for null timestamps. If nothing sorts entities — nothing currently does — deleting the interface is the cleaner fix.

**Resolved 2026-09-09 by deletion**, the option this finding recommended. Confirmed first that
nothing depends on it: no `compareTo` call, no `TreeSet`/`TreeMap`, no `sorted()` over entities, and
no `SortedSet` or `@OrderBy` mapping in the model — all four entity collections are plain
`HashSet`. The `@SuppressWarnings("rawtypes")` the raw interface required went with it.

Both contract violations were proved with a throwaway test before removal: `compareTo(null)`
returned `-1` rather than throwing, and antisymmetry failed for two entities with null timestamps
(`signum` expected `1`, got `-1`). That test was deleted along with the interface — it existed only
to show the defect was real. A comment in `BaseEntity` records why there is no natural ordering, so
a future contributor adds a `Comparator` rather than reinstating this.

### ✅ M5. Generic exception handler leaks internal messages to clients

`exceptions/ApplicationExceptionHandler.java:38-47`

```java
@ExceptionHandler(value = { Exception.class })
public ExceptionEnvelope handleGeneralException(Exception ex) {
    ... .message(ex.getMessage()) ...
}
```

Any unhandled exception returns its raw message. For a `DataIntegrityViolationException` that means SQL fragments, table names, and constraint names reaching the client — as visible in the H2 message quoted in `RatingJpaRepositotyIT`.

**Action:** log the detail server-side, return a generic message plus a correlation id to the client.

### ✅ M6. Public mutable fields on the DTO base classes

`dto/BaseDTO.java:19-25`, `dto/BaseIdentityDTO.java:11`

`created`, `createdBy`, `updated`, `updatedBy` and `id` are all `public`, and both classes carry Lombok `@Data`, generating setters on top. Subclasses that declare only `@Getter` are therefore still fully mutable through inherited fields, so the apparent immutability of `CategoryDTO`, `TagDTO` and friends is an illusion.

**Action:** make the fields `private` and rely on the generated accessors. Consider `@Value` or records for the leaf DTOs if genuine immutability is wanted.

**Resolved 2026-09-09.** All five fields are now `private`; `@Data` was already generating the
accessors, so no caller's API changed.

Proved first with `DtoEncapsulationTest`, which failed listing exactly the five offenders. That test
is **kept**, unlike the throwaway used for M4: encapsulation is a compile-time property with no
runtime behaviour to assert, and re-widening a field is an easy mistake to make again.

The change forced 33 edits across seven leaf DTOs, whose hand-written `toString()` methods read the
inherited fields directly — they now call the getters. Worth noting for **L7**: those same seven
methods are the ones with the mislabelled fields, so replacing them with Lombok `@ToString` is now a
single contained step.

`@Value`/records were not adopted. The controllers depend on `dto.setId(id)` for the path-id-wins
rule, so genuine immutability would need a different write path — a deliberate design change rather
than a cleanup.

### ✅ M7. Missing `readOnly = true` on query paths

All six service implementations carry a class-level `@Transactional(readOnly = false, timeout = 5)`. Read methods inherit `readOnly = false`, which prevents Hibernate from skipping dirty-checking on read-only work and forfeits driver-level optimisations.

**Action:** annotate the `findAll`/`findById`/`findBy*` methods with `@Transactional(readOnly = true)`. Also revisit `timeout = 5` — five seconds is tight for a cold query on a loaded database.

### ✅ M8. Unvalidated `UUID.fromString(id)` throughout the service layer

Every service converts path ids with `UUID.fromString(id)` and no guard. A malformed id raises `IllegalArgumentException`, which bypasses the `ServiceException` path entirely and lands in the generic handler as a **500**, when it should be a **400**.

The project already contains a purpose-built `@UUID` constraint (`validator/UUID.java`) — it is simply never applied anywhere.

**Action:** apply `@UUID` to the id parameters once controllers exist, and/or catch `IllegalArgumentException` in the services and rethrow as `ServiceException` with `BAD_REQUEST` and `ID_IS_NULL`/a new invalid-id code.

### ✅ M9. Inconsistent null-argument handling across services

- `CategoryServiceImpl.save` returns `null` for a null DTO (line 84).
- `IngredientServiceImpl.save` throws `INGREDIENT_DTO_IS_NULL` / `BAD_REQUEST`.
- `TagServiceImpl.save` does not check at all — straight `NullPointerException`.
- `UnitServiceImpl.save` throws, but with `HttpStatus.NOT_FOUND` (line 68) for what is plainly a bad request.

Four services, four different behaviours for the same input.

**Action:** settle on throwing `ServiceException` with `BAD_REQUEST`, and correct the `UnitServiceImpl` status. The delete-path null checks vary in the same way and should be aligned at the same time.

---

## Low

### ✅ L1. Dead code

Confirmed unreferenced anywhere in `src`:

| Item | Note |
|---|---|
| ~~`enums/Rating.java`~~ | ✅ deleted (package removed) |
| ~~`dto/OperationResultDTO.java`~~ | ✅ deleted |
| ~~`config/SnakeCasePhysicalNamingStrategy.java`~~ | ✅ deleted, with the commented-out property |
| ~~`mapper/CategoryCategoryDTOMapper.java`~~ | ✅ removed — see below |
| `CategoryMapper.fromDto` | the only entity-direction mapper; still unused |
| `validator/UUID` + `UUIDValidator` | tested but never applied (see M8) |

**Resolved 2026-09-07** — `enums/Rating`, `OperationResultDTO` and `SnakeCasePhysicalNamingStrategy` deleted (the `enums` package is gone entirely), along with the commented-out property that referenced the naming strategy.

Two entries deliberately remain: the `@UUID` validator, which has a real use once controllers land, and `CategoryMapper.fromDto`, the last entity-direction mapper — worth keeping only if the write path will need it.

**MapStruct: decided and removed (2026-09-07).** It was adopted deliberately in March 2023 (commit `9fbed53`, closing issue #27), but when the services and mappers were actually written 15 months later (`c62cf3c`) they were all hand-written; the single `@Mapper` interface added in that same commit was never referenced. The framework, its processor path, `lombok-mapstruct-binding` and the trial mapper are all gone. Lombok remains the sole annotation processor and **must stay explicitly listed** — see the annotation-processor note in `CLAUDE.md`.

### ✅ L2. Three no-op assertions in `RatingJpaRepositotyIT`

`RatingJpaRepositotyIT.java:47, 75, 87`

```java
assertThat(opRating.isPresent());
```

AssertJ's `assertThat` builds an assertion object; with no terminal call, nothing is verified. These three lines always pass, including when the `Optional` is empty.

**Action:** append `.isTrue()`. Worth a quick scan for the pattern elsewhere — these three are the only current instances.

### ✅ L3. `@MockBean` is deprecated and marked for removal

All eight `*IT` classes. It produced 40 build warnings. Removed in a future Spring Boot release.

**Action:** replace with `@MockitoBean` from `org.springframework.test.context.bean.override.mockito`. Mechanical, one import and one annotation per class.

### ✅ L4. `liquibase-maven-plugin` points at a file that does not exist

`pom.xml:226` sets `<propertyFile>src/main/resources/liquibase.properties</propertyFile>`. There is no such file — it lives at `src/main/resources/db/liquibase.properties`. Every `mvn liquibase:*` goal fails.

That file additionally contains a typo in the changelog name (`ddb.changelog_1.0.xml`, doubled `d`) and a hardcoded absolute path to a developer's local `.m2` directory pinning a connector version (8.0.30) that does not match the project's (8.0.32).

**Action:** correct the path in `pom.xml`, fix the changelog filename, and remove the `classpath:` line so Maven resolves the driver from the reactor.

### ✅ L5. Mojibake in `messages_dk.properties`

`src/main/resources/messages_dk.properties:5` — `save.changes` contains a replacement character where `æ` belongs ("Gem ?ndringer"). The file was saved in a non-UTF-8 encoding.

Neither `messages.properties` nor `messages_dk.properties` is wired to a `MessageSource`, so nothing reads them today.

**Action:** re-save as UTF-8 and fix the character. Configure a `MessageSource` when the UI or API needs localisation, or delete both files if `ValidationMessages.properties` covers the need.

**Resolved 2026-09-09 by deleting both files** — the second option. The byte is `0xE6`, a valid
ISO-8859-1 `æ` in a file Spring would read as UTF-8; that is where the replacement character came
from, rather than a bad save. Neither file is read: no `MessageSource` bean, no
`spring.messages.*` configuration, and the exception handlers build messages from the violation
objects directly. Validation text comes from `ValidationMessages.properties` through the Jakarta
interpolator, which is a different mechanism entirely. `messages_dk.properties` held three UI labels
for a UI that does not exist, and `messages.properties` held one message already covered.
Recoverable from git history if a UI arrives; a fresh UTF-8 file plus a wired `MessageSource` would
be the way in.

### ✅ L6. Mappers log every mapping at INFO

All seven `*Mapper` classes call `log.info` on each conversion, and `RecipeMapper` (lines 35, 44, 53) does so *inside* stream operations — one line per ingredient, per rating, per tag, per recipe. Listing recipes would produce hundreds of INFO lines per request.

**Action:** drop to `debug`, or remove. The mappers are pure functions; their output is already asserted in tests.

**Resolved 2026-09-09.** Eight call sites across five mappers dropped to `debug`, keeping the trace
available without paying for it at INFO — SLF4J's parameterised form means a disabled level costs
only the check. The finding says "all seven `*Mapper` classes"; it is five of the eight
(`CategoryMapper`, `RecipeIngredientMapper` and `RecipeRatingMapper` never logged).

### ✅ L7. Cosmetic defects in `toString()` implementations

- `RatingDTO:26` — output opens with a stray leading comma: `RatingDTO{, rating=...`
- `TagDTO:24` — labels the `name` field as `label='`
- `UnitDTO:28` — labels the `name` field as `description='`
- `Category:23` / `Recipe` — omit audit fields that every sibling entity includes

These surface in logs and in test failure messages, where a mislabelled field costs real debugging time.

**Action:** fix the labels. Consider Lombok `@ToString` for the leaf classes — `RecipeDTO` already uses it — to remove the hand-maintained duplication.

**Resolved 2026-09-09.** All four defects fixed, each proved with a failing test first
(`DtoToStringTest`, `CategoryToStringTest`), which target the specific defect rather than pinning the
whole format so they do not break when a field is added.

**Four, not five:** this finding names "`Category:23` / `Recipe`" as omitting the audit fields.
`Recipe` does print all four; only `Category` omitted them.

The Lombok suggestion was **not** taken. `@ToString(callSuper = true)` across this three-level
hierarchy renders `CategoryDTO(super=BaseIdentityDTO(super=BaseDTO(...), id=...), name=...)`, and
these strings exist to be read in logs and failure messages. Removing the duplication would cost the
readability the finding is trying to protect. Worth revisiting only if the leaf DTOs ever flatten.

### ✅ L8. Minor consistency and hygiene

- `mysql:mysql-connector-java` is a relocation stub; the current coordinates are `com.mysql:mysql-connector-j`. One build warning per run.
- `pom.xml:32-40` — the `dependencyManagement` entry for `liquibase-core` references `${liquibase.version}` (inherited from the Boot parent) while the dependency itself uses the locally-defined `${liquibase-version}`. Two different properties, confusingly similar names; the managed entry is redundant.
- `RecipeService.save(RecipeDTO categoryDto)` — parameter named `categoryDto`, copy-paste from `CategoryService`.
- `IngredientServiceImpl:129` — `"Could not update Ingredient with id" + dto.getId()` is missing a trailing space.
- `RecipeServiceImpl:197` — `.amount(Objects.nonNull(x) ? x : null)` is a no-op ternary.
- `RecipeServiceImpl` `findAll*` methods guard with `if(!recipes.isEmpty())` before streaming; streaming an empty list already yields an empty list. They also mix `Collectors.toUnmodifiableList()` with a mutable `new ArrayList<>()` return, so the caller cannot rely on either.
- `CategoryMapper` uses `fromEntity`/`fromDto` while all six other mappers use `from`.
- `MethodArgumentNotValidExceptionHandler:46-52` — nested null-check on `violation.getField()`, which cannot return null; the inner branch is dead.
- `ExceptionEnvelope.toString():59` constructs a new `ObjectMapper` on every call — expensive, and the class is used on error paths.
- `.factorypath` is tracked in git despite being listed in `.gitignore` (it was committed before the rule was added).

**Resolved 2026-09-09.** Four of the ten had already been fixed in passing — the mysql coordinates
and the `dependencyManagement` duplication during the Java 25 upgrade, the no-op ternary during M2,
and `.factorypath` is no longer tracked. The remaining six:

| Item | Change |
|---|---|
| `save(RecipeDTO categoryDto)` | renamed to `recipeDTO` |
| `IngredientServiceImpl:115` | `"with id" +` → `"with id " +`, which was producing `with id5f01d434-…` |
| `RecipeServiceImpl` `findAll*` | the `isEmpty()` guard removed from all three; they now `return …stream().map(RecipeMapper::from).toList()`. The guard was redundant, and the two branches handed callers lists with **different mutability guarantees** — unmodifiable when populated, mutable when empty |
| `CategoryMapper.fromEntity` | renamed `from`, matching the other seven mappers. `fromDto` keeps its name: it is the opposite direction, so `from` would be ambiguous |
| `MethodArgumentNotValidExceptionHandler` | dead nested null-check removed (`FieldError.getField()` is never null); its log line also said "handle ConstraintViolationException", naming the wrong exception |
| `ExceptionEnvelope.toString()` | one shared `static final JsonMapper` instead of building one per call, on what is already an error path |

### ✅ L9. `RecipeIngredient.equals`/`hashCode` dereference unset back-references

`model/RecipeIngredient.java:35-52`

```java
public int hashCode() { return Objects.hash(ingredient.id, recipe.id); }
```

Both `equals` and `hashCode` read `ingredient.id` and `recipe.id` with no null guard, so any `RecipeIngredient` whose `recipe` or `ingredient` is unset throws `NullPointerException` the moment it enters a `HashSet` — including the `Set<RecipeIngredient>` on `Recipe` itself.

This is latent rather than active: `Recipe.addRecipeIngredient` sets the back-reference *before* adding to the set, so the normal path is safe. It surfaced while writing tests for `deleteRecipeIngredient`, where a detached fixture built straight from the builder crashed on `Set.remove`.

**Action:** null-guard both methods, or derive them from the `RecipeIngredientPK` fields. Note this class is one of only two entities overriding equality by field rather than inheriting the id-based implementation from `BaseIdentifierEntity` (see also `Unit`), which is itself worth reconciling.

*(Found during remediation, not in the original review pass.)*

---

## Suggested order of work

Grouped so that each phase leaves the build green.

**Phase 1 — correctness (do before writing any controller)** — ✅ **complete**
1. ✅ H1 / H2 — removed the hardcoded `"Majken"` and the `createdBy` overwrite.
2. ✅ C3 — added the missing HTTP statuses; `ServiceException` now defaults a null status.
3. ✅ C4 — null-guarded `handleRecipeIngredients`.
4. ✅ C1 — implemented the three ingredient operations; ratings deferred explicitly.
5. ✅ C2 — fixed `RecipeServiceImpl.update()`.
6. ✅ H7 — stopped fabricating an empty `CategoryDTO`.

**Phase 2 — contract consistency (cheapest before controllers exist)**
6. ✅ H6 / H5 — settled on `List<T>` across all services and repositories.
7. ✅ M9 — one null-argument policy, and fixed the `UnitServiceImpl` status code.
8. ✅ M8 — ids parsed through `ServiceArguments.toUuid`, mapping malformed values to `BAD_REQUEST`.
9. ✅ M1 — implicit category creation removed; unknown category now 404. ✅ M2 — swallowed ingredients now collected and rejected.

**Phase 3 — security and configuration** — ✅ **complete**
10. ✅ H3 — credentials removed from every tracked file. **Rotation is still outstanding and is yours to do** (see below).
11. ✅ H4 — `dev` profile added; the base profile is quiet.
12. ✅ M5 — unhandled exceptions return a reference, not the raw message.
13. ✅ L4 — Liquibase plugin configuration repaired.

**Phase 4 — cleanup**
14. ✅ L1 — dead code deleted; MapStruct decided against and removed.
15. ✅ L2 / L3 — no-op assertions fixed; migrated to `@MockitoBean` (40 build warnings → 0).
16. ✅ M7 / ✅ L9 — `readOnly` transactions; `RecipeIngredient` equality null-guarded. ✅ M4 — `Comparable` deleted. M6 still open.
17. ✅ L5–L8 — dead message bundles deleted, mapper logging at `debug`, `toString()` labels fixed, naming and hygiene items closed.

 These are cheapest now — once controllers exist, the `Optional<List<T>>` versus `List<T>` split hardens into the HTTP API.

**Then:** build the REST layer. That work will exercise `exceptions/` for the first time and should lift its coverage from 24% well up; write controller tests with `@SpringBootTest` + `MockMvc` (the repository slice tests correctly stay on `@DataJpaTest`).

---

## Resolution log

Phase 1 plus H7, applied 2026-09-06. Each change was made one at a time, with the full suite run between steps. Every fix was preceded by a **failing** test proving the defect was real and that the new assertion catches it — several of these bugs had survived a 161-test suite precisely because no test looked at the affected field.

| Finding | Change | Proof |
|---|---|---|
| H1 | `UnitServiceImpl.save()` uses `session.getUserName()` | `expected "Jens" but was "Majken"` |
| H2 | Removed `setCreatedBy` from `UnitServiceImpl.update()` | `expected "Majken" but was null` |
| C3 | Three statuses added; `ServiceException` defaults null → `INTERNAL_SERVER_ERROR` | 3 × `expected 4xx but was null` |
| C4 | Early return in `handleRecipeIngredients` for null collections | `NullPointerException` at `RecipeServiceImpl:182` |
| C1 | `addRecipeIngredient` / `deleteRecipeIngredient` / `updateRecipeIngredient` implemented | 9 new tests |
| C2 | `update()`: instructions copied, 400/404 thrown, result mapped from the saved entity | `expected "New Instructions" but was "Hæld vand i en skål..."` + 2 missing exceptions |
| H7 | `RecipeDTO` leaves an absent category null; `handleCategory` rejects it with 400 | `but was: CategoryDTO{id='null', name='null', ...}` |
| H5 / H6 | All six services return `List<T>` from `findAll`; 8 repository queries unwrapped | compiler-enforced; 173 tests still green |
| M9 | Every `save`/`update`/`delete` on all six services rejects null with 400, via `ServiceException.badRequest(...)` | 18 failing tests across 6 service tests |
| M8 | All 26 `UUID.fromString` sites routed through `ServiceArguments.toUuid`; malformed ids give 400, not 500 | 12 failing tests, 2 of which had pinned the defect |
| M5 | Unhandled exceptions log server-side and return `"An unexpected error occurred"` plus a UUID reference | 3 failing tests asserting no SQL leaks |
| H4 | SQL/bind logging moved to a new `application-dev.properties`; base profile quiet | n/a - configuration |
| H3 | Password removed from `application.properties`, `db/liquibase.properties`, `mysql_users.sql`, `export_mysqldb_data_liquibase.txt`, test config | `grep` for the literal returns 0 |
| L4 | `pom.xml` points at `db/liquibase.properties`; changelog path and stale `.m2` classpath fixed | `mvn liquibase:status` now parses the file and reaches the DB |
| L2 / L3 / M7 / L9 | 3 no-op assertions made real; `@MockBean` → `@MockitoBean` in 8 ITs; `readOnly = true` on all 18 query methods; `RecipeIngredient` equality null-guarded | 40 → 0 deprecation warnings; readOnly coverage audited |
| (new) | `ObjectMapperConfig` deleted: its `@Primary` hand-rolled mapper broke every write endpoint in production while slice tests passed | `JsonContractIT` - 4 tests against the real context mapper |
| M1 | `handleCategory` requires an existing category id: no implicit creation, unknown id now 404 | 2 failing tests; 2 more exposed as having relied on the silent-ignore |
| L1 | MapStruct dropped (trial mapper, dependency, processor path, `lombok-mapstruct-binding`); `enums/Rating`, `OperationResultDTO`, `SnakeCasePhysicalNamingStrategy` deleted | 210 tests green; 0 references remain |

**New error codes:** `RECIPE_INGREDIENT_DTO_IS_NULL(60)`, `RECIPE_INGREDIENT_NOT_FOUND(61)`, `RECIPE_INGREDIENT_ALREADY_EXISTS(62)`, `CATEGORY_IS_REQUIRED(101)`.

**Deliberately deferred:** `addRecipeRating` now throws `UnsupportedOperationException` rather than returning `null`. Ratings are out of scope for this version by decision, and the components were left out on purpose; failing loudly keeps that explicit instead of silently returning nothing.

**Also removed:** four empty placeholder tests in `RecipeServiceTest` (`addRecipeIngredient`, `deleteRecipeIngredient`, `updateRecipeIngredient`, `addRecipeRating`) whose bodies contained only a comment. They passed vacuously and inflated the count while asserting nothing; the step-4 tests replace them.

### Outstanding: credential rotation

H3 removed the password from every tracked file, but **it remains in git history** and, if it was ever used against a real database, is still live. Two actions remain that only you can take:

1. **Rotate** the MySQL password(s) that were committed, and the `recipesadmin` / `recipesuser` passwords created by `mysql_users.sql`.
2. Decide whether to **purge git history** (`git filter-repo` or BFG). For a local-only project this may not be worth the rewrite; for anything pushed to a shared remote it is.

Until the rotation happens, the value in the history is the security exposure - not the working tree.

### Two notes for whoever picks this up

**Two tests had pinned the defect.** `IngredientServiceTest` and `TagServiceTest` each asserted `assertThrows(IllegalArgumentException.class, ...)` for a malformed id — encoding the very behaviour M8 identifies as wrong. They were rewritten to expect `ServiceException`/400. Worth knowing that a green suite had been asserting the bug was correct.

**Watch for the fixture-shaped blind spot.** Several service tests mock `repository.save()` to return a fixture and then assert against that fixture — so they verify the mock, not the code. `UnitServiceTest.shouldSaveNewUnit` did exactly this and could never have caught H1. Where behaviour depends on what the service *writes*, capture the argument with `ArgumentCaptor` instead.

---

## What is already good

Worth preserving as the code grows:

- **Liquibase owns the schema outright** (`ddl-auto=none`), with a clean master/version split and a separate test changelog that layers seed data over the production DDL. This is the single best decision in the project.
- **Consistent vertical slices.** Every domain concept has the same entity → repository → service interface + impl → mapper → DTO shape. A new concept has an obvious template.
- **The audit base classes** (`BaseEntity` / `BaseIdentifierEntity` / `BaseEntityListener`) centralise cross-cutting concerns properly rather than repeating timestamps on every entity.
- **Test discipline.** 161 tests with `@DisplayName` in consistent Given/When/Then phrasing, `Mock*Util` fixture factories, and a `*Test`/`*IT` split wired to surefire/failsafe. `OffsetDateTimeProvider` pinning assertions to a fixed zone is a thoughtful touch that avoids a classic flaky-test trap.
- **Typed error codes.** `ApplicationErrorCodes` grouped in hundreds per domain, carried through `ServiceException` into a structured `ExceptionEnvelope`, is a better foundation than most projects this size have.
