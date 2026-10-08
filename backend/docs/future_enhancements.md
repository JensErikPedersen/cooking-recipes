# Future enhancements

Deliberately deferred work. Each entry records what is missing, why it was left, and what it would take.

---

## Name-based lookup is inconsistent across services, and unexposed at the API

**What is missing.** Every domain concept in this application is identified to a user by its
name, but the ability to look one up by name was added ad hoc, service by service. The result is
four different levels of support and one naming inconsistency:

| Service | `findByName` | `findAllByNameContains` | Repository already supports |
|---|---|---|---|
| Category | ✅ | ❌ | `findByName`, and `findAllByNameContains`, which nothing calls |
| Ingredient | ✅ | ✅ | both, plus `findAllByDescriptionContains`, which nothing calls |
| Recipe | ❌ | ✅ | `findByName`, `findAllByNameContains`, `findAllByCategoryName` |
| Tag | ⚠️ `findTagByName` | ❌ | `findTagByName` |
| Unit | ❌ | ❌ | `findByName` |
| Rating | ❌ | ❌ | none - not applicable, a rating is a 1-5 value |

Every repository `findByName` (Tag's `findTagByName`) now has a caller: the duplicate-name check
in that service's `save` and `update`, added in Parts 5a-7a. Those checks call the repository
directly, so the service-level `findByName` on Category and Ingredient, and Tag's `findTagByName`,
are called only by their own tests.

Three specific problems:

1. **Category has the repository method but not the service method.** `CategoryJpaRepository.findAllByNameContains` exists and is covered by `CategoryJpaRepositoryIT` ("two categories containing 'er'"), but `CategoryService` never exposes it, so nothing can reach it. It is working, tested, unreachable code.
2. **Tag names its method `findTagByName`** rather than `findByName` — the entity name is redundant inside `TagService`, and it breaks the pattern every other service follows.
3. **None of it is reachable over HTTP.** All five controllers expose only the five CRUD endpoints; `TagController` and `UnitController` say why in their class comments. No screen searches by name either.

**What it would take.**

1. Settle the service contract for the concepts where name lookup is meaningful — Category, Ingredient, Tag and Recipe:
   ```java
   Optional<XDTO> findByName(String name);        // exact match, one result or empty
   List<XDTO>     findAllByNameContains(String name);  // partial match, possibly empty
   ```
   Add what is missing (`Category.findAllByNameContains`, `Tag.findAllByNameContains`, and
   `Recipe.findByName` over the repository query that already exists), and rename
   `findTagByName` to `findByName` in `TagService` and `TagJpaRepository`. Whether the
   service-level `findByName` belongs in the contract at all is part of settling it - see point 3.
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
3. **Do not expose exact match over HTTP.** The two are not substitutes:
   `findAllByNameContains("Brod")` is a `LIKE` query that also matches `Rugbrod` and `Hvedebrod`, so it
   cannot answer *"is there a category called exactly Brod?"*. The API needs only the partial filter -
   these names are unique, so a full name returns a single-element list. The exact-match query
   stays in the repository, where the duplicate-name checks use it. The service-level
   `findByName` has no caller outside its tests; keep or drop it as one decision for all four.

**Deliberately excluded.** `Unit` and `Rating`. Units are a fixed handful of measures and ratings are a 1-5 lookup table; adding name search to them would be ceremony rather than capability. Unit's only query is the exact `findByName` behind its duplicate-name check. `UnitController` was built with the five CRUD endpoints only, for that reason.

**Value.** One predictable contract across the API instead of per-resource guesswork, and it makes reachable a repository method that is currently tested but dead.

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

1. **Accounts are created only by registering.** Open sign-up exists since `docs/PLAN_V2.md`
   Part 5, **without** enforcing roles, by decision: every account has the rights the first one
   has. A user administration screen - creating, disabling or deleting accounts for others - still
   needs roles enforced first. Until then the only way to remove an account is SQL.
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
   honoured on login; nothing sets it. Reset has its own entry below.

**Value.** Per-user recipe lists, private drafts, and an audit trail that can be joined and
trusted rather than read as a string.

---

## A forgotten password cannot be reset

**What is missing.** A user who forgets their password has no way back in. There is no reset, and
no change of password either. With one bootstrapped account this hardly mattered: the operator
could delete the row and let `AdminBootstrap` recreate it. Once anyone can register
(`docs/PLAN_V2.md` Part 5), a forgotten password locks that user out for good.

**Why it was left.** A real reset needs a channel the application can reach the user through,
and the application has none. `app_user` holds no email address, and the stack has no mail server.

**What it would take.** Two pieces, in this order:

1. **Change password, signed in.** `PUT /api/v1/auth/password` taking the current and the new
   password, with the same rules as registration. It needs no new infrastructure, and the reset
   reuses it.
2. **Reset by email.** An `email` column on `app_user`, collected at registration. Then
   `POST /api/v1/auth/password-reset` mails a single-use token that expires, and a second endpoint
   takes that token plus a new password. Store the token hashed, as a password is. Answer the
   request the same way whether or not the address exists, as login does for usernames. Locally,
   a Mailpit container in the stack catches the mail, so nothing is really sent.

A reset by an administrator is the alternative to email. It needs role enforcement and an
administration screen first; see the multiple-users entry above.

**Value.** Self-registration without permanent lockout.

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

---

## Smaller items deferred during Parts 2-7

Each was found on the way, judged not worth its step, and is recorded here rather than lost in a
step's notes in `docs/PLAN.md`.

- **The `/recipes/{id}/ingredients` sub-resource endpoints are unused**: the form sends the lines
  in the recipe's PUT, so Cancel can undo them (decided in Part 7). They still work and are tested;
  keep or remove them as one decision.
- **Sign-in does not return to where you were heading**: after signing in you land on the start
  page. Deliberately left out in 4b for simplicity.
