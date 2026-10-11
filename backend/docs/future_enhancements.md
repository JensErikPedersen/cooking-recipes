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
4. **No reset, no disable-in-the-UI.** A signed-in user can change their own password since
   `docs/PLAN_V4.md` Part 2. The `enabled` column exists and is honoured on login; nothing sets it.
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
   with the current and the new password, to registration's rules. It leaves other sessions of the
   account signed in; ending them - what a user who suspects a stolen password wants - needs
   Spring Security's `SessionRegistry`, which nothing here uses yet.
2. **Reset by email.** An `email` column on `app_user`, collected at registration. Then
   `POST /api/v1/auth/password-reset` mails a single-use token that expires, and a second endpoint
   takes that token plus a new password. Store the token hashed, as a password is. Answer the
   request the same way whether or not the address exists, as login does for usernames. Locally,
   a Mailpit container in the stack catches the mail, so nothing is really sent.

A reset by an administrator is the alternative to email. It needs role enforcement and an
administration screen first; see the multiple-users entry above.

**Value.** Self-registration without permanent lockout.
