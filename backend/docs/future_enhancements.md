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

---

## Pictures: what version 8 leaves out

**Status.** `docs/PLAN_V8.md` adds a library of pictures, kept on a Docker volume in three sizes. A
recipe, an ingredient and the Welcome page can each show one picture from it. Version 7 had no
photos, by its decision 4. Three things were left out of version 8 by decision.

**What is missing.**

1. **One picture each.** A recipe or an ingredient has a nullable `picture_id`, not a gallery
   (`docs/PLAN_V8.md` decision 2). Several pictures would need join tables, `recipe_picture` and
   `ingredient_picture`, with an order column, and a gallery on the read view.
2. **No cropping or rotating in the browser.** The upload is resized as it is; the page crops it
   to the grid with CSS `object-fit: cover`, from the centre. Choosing the crop would need a focal
   point stored with the picture, or an editor before upload.
3. **JPEG and PNG only** (decision 5). Java's ImageIO reads neither WebP nor HEIC. WebP would take
   a reader plugin such as TwelveMonkeys' `imageio-webp`. HEIC has no maintained pure-Java reader,
   so it would mean a native library, or converting in the browser before upload.

**Value.** Small next to what version 8 delivers. Each is worth its work only if someone asks for
it in use.

---

## Desktop polish: the large screen could look better

**Status.** Noted on review of `docs/PLAN_V7.md` Part 5, 2026-11-08, from the screenshots: version
7 was designed phone first, and at 1280 px the pages work but look plainer than on a phone or a
tablet. Not designed yet; what follows is what the screenshots suggest, to weigh when it is.

**Where it shows.**

* A recipe's read view: the instructions column ends far short of the page, and with the seeded
  one-line instructions the page is mostly empty cream. Edit and Delete sit far from the name.
* The recipe cards: short, with rows of uneven height, and the label's tan band reads as separate
  strips at that size rather than one frame.
* The reference lists: the name column takes most of the width, with the other columns pushed to
  the far right.

**What it would take.** A pass with the frontend-design skill on the three pages above at 1280 px,
recorded in the design language of `docs/PLAN_V7.md` first, then built and reviewed by screenshot
as each V7 part was. Frontend only.

---

## OpenAPI specification: generated at build time

**Status.** Requested 2026-11-08, before `docs/PLAN_V8.md` started. The API is documented by hand,
in `backend/README.md` and `backend/CLAUDE.md`. Nothing describes it in a form a tool can read, and
nothing checks that the documentation still matches the controllers.

**What it would take.**

1. springdoc-openapi's `springdoc-openapi-starter-webmvc-api` (the spec only, no Swagger UI), at
   the latest stable release that supports Spring Boot 4. It builds an OpenAPI 3 document at
   runtime from the controllers, the DTOs and their validation annotations, served on
   `/v3/api-docs`.
2. `springdoc-openapi-maven-plugin` in the `integration-test` phase, with
   `spring-boot-maven-plugin`'s `start` and `stop` goals around it. Those goals run the
   application, and the plugin writes the document it fetches to `target/openapi.json` (or
   `.yaml`) during `./mvnw clean verify`.
3. A database for that run: the application starts with Liquibase, so the build runs it on
   in-memory H2, as the tests do, in a profile of its own. A build must not need the stack.
4. `/v3/api-docs` reachable without sign-in, in that build profile only; elsewhere it stays behind
   sign-in like the rest of `/api`, or is switched off.
5. Annotations only where the generated document is wrong or unclear, for example the session
   cookie and CSRF header as the security scheme, the error envelope as the 4xx body, and the
   multipart upload of `docs/PLAN_V8.md`. Not a description on every endpoint.
6. A decision on where the file goes: a build artifact only, or committed so a change to the API
   shows in a pull request's diff.

**Value.** An exact, current contract instead of prose that can drift. It could later produce the
frontend's TypeScript types (for example with `openapi-typescript`) in place of the hand-written
ones. That would be a step of its own.
