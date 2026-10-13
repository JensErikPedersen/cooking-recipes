# Future enhancements

Deliberately deferred work. Each entry records what is missing, why it was left, and what it would take.

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
4. `RatingController` (five CRUD endpoints), and a recipe's ratings written either with the recipe,
   as its ingredient lines are, or under `/api/v1/recipes/{id}/ratings` - the ingredient lines'
   own sub-resource of that shape was removed in V3 Part 5 for want of a caller.

**Value.** The feature the whole `recipe_rating` table exists for. Until then the guard keeps the
limitation explicit at the API boundary rather than silent.

---

## Multiple users: the table exists, the features do not

**Status.** Part 4 added the `app_user` table, a `UserDetailsService` backed by it, and a first-run
`AdminBootstrap`, so the schema supports multiple users. `docs/PLAN_V5.md` Part 3 enforces the admin
role, for an API and a users page on which an admin disables and enables accounts and grants the
role (Parts 3-4).

**What is missing.**

1. **Accounts are created only by registering, and never deleted.** An admin disables an account
   instead (`docs/PLAN_V5.md` decision 4): a deleted one would leave `created_by` and `updated_by`
   naming an account that no longer exists. Removing one takes SQL.
2. **Two roles, in a comma-separated column.** `ROLE_ADMIN` guards account management only;
   categories, ingredients, units and tags stay open to every account by decision (`docs/PLAN_V5.md`
   decision 3). A join table, `app_user_role`, pays only with more roles than two.
3. **Nothing is owned by anyone - by decision** (`docs/PLAN_V5.md` decision 8). Every account
   edits every recipe: in a household cookbook an owner-only rule would stop one member fixing
   another's recipe. `created_by` and `updated_by` say who wrote and who last changed it, as free
   text, not a foreign key to `app_user.id`. A "My recipes" list could filter on `created_by`
   without one. Ownership becomes worth its migration only for users who should not edit each
   other's recipes; the plan for it is V5's dropped Parts 5-6, a nullable `recipe.owner_id`.
4. **Passwords.** A signed-in user changes their own since `docs/PLAN_V4.md` Part 2, and a
   forgotten one is reset by email since `docs/PLAN_V6.md` Parts 5-6 - for an account that has an
   address. What the reset leaves out has its own entry below. An admin cannot set another
   account's password.

**Value.** An audit trail that can be joined and trusted rather than read as a string, and
ownership, should users ever need to be kept from editing each other's recipes.

---

## Password resets: no rate limit, and an answer that takes longer for a real address

**Status.** A forgotten password is reset by email since `docs/PLAN_V6.md` Parts 5-6:
`POST /api/v1/auth/password-reset` mails a single-use link that works for 30 minutes, and
`/password-reset/confirm` takes its token with a new password. The answer is the same 202 whether
or not the address belongs to an enabled account. Two things were left out, by decision, for a
local application.

**What is missing.**

1. **No rate limit.** Anyone can ask for any number of resets, of any address. Each request for a
   real address sends a mail and replaces that account's token - an annoyance to its owner, not a
   way in. Nothing limits the confirm either, but a token is 32 random bytes: there is nothing to
   guess.
2. **The answer takes longer for a real address.** The mail is sent before the 202, so a caller
   who times the answers can tell an address that has an account - the body and status cannot.
   Against Mailpit the difference is milliseconds; against a real mail server it would be more.
   Registration tells a taken address outright anyway (409), as it must with open sign-up.

**What it would take.** A limit per address and per client, in memory, answering 429 - or a reverse
proxy in front of the application doing the same. For the timing, sending the mail after the
answer, from a queue or an `@Async` method; a failed send then goes to the log instead of a 500
(`docs/PLAN_V6.md` decision 3).

**Value.** Only once the application faces the internet; until then, neither is worth its code.
