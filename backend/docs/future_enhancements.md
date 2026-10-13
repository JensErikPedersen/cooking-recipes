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
   instead (`docs/PLAN_V5.md` decision 4): a deleted one would orphan what it owns. Removing one
   takes SQL.
2. **Two roles, in a comma-separated column.** `ROLE_ADMIN` guards account management only;
   categories, ingredients, units and tags stay open to every account by decision (`docs/PLAN_V5.md`
   decision 3). A join table, `app_user_role`, pays only with more roles than two.
3. **Nothing is owned by anyone - by decision** (`docs/PLAN_V5.md` decision 8). Every account
   edits every recipe: in a household cookbook an owner-only rule would stop one member fixing
   another's recipe. `created_by` and `updated_by` say who wrote and who last changed it, as free
   text, not a foreign key to `app_user.id`. A "My recipes" list could filter on `created_by`
   without one. Ownership becomes worth its migration only for users who should not edit each
   other's recipes; the plan for it is V5's dropped Parts 5-6, a nullable `recipe.owner_id`.
4. **No reset.** A signed-in user can change their own password since `docs/PLAN_V4.md` Part 2.
   Reset has its own entry below.

**Value.** Per-user recipe lists, private drafts, and an audit trail that can be joined and
trusted rather than read as a string.

---

## A forgotten password cannot be reset

**What is missing.** A user who forgets their password has no way back in. There is no reset. With
one bootstrapped account this hardly mattered: the operator could delete the row and let
`AdminBootstrap` recreate it. Once anyone can register (`docs/PLAN_V2.md` Part 5), a forgotten
password locks that user out for good.

**Why it was left.** A real reset needs a channel the application can reach the user through,
and the application has none. `app_user` holds no email address, and the stack has no mail server.

**What it would take.** Two pieces, in this order:

1. **Change password, signed in.** Done in `docs/PLAN_V4.md` Part 2: `PUT /api/v1/auth/password`
   with the current and the new password, to registration's rules. Since `docs/PLAN_V5.md` Part 2
   it also signs out every other session of the account.
2. **Reset by email.** An `email` column on `app_user`, collected at registration. Then
   `POST /api/v1/auth/password-reset` mails a single-use token that expires, and a second endpoint
   takes that token plus a new password. Store the token hashed, as a password is. Answer the
   request the same way whether or not the address exists, as login does for usernames. Locally,
   a Mailpit container in the stack catches the mail, so nothing is really sent.

A reset by an administrator is the alternative to email. It needs role enforcement and an
administration screen first; see the multiple-users entry above.

**Value.** Self-registration without permanent lockout.
