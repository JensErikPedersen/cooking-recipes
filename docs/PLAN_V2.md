# Implementation plan, version 2

Read `CLAUDE.md` first for the requirements, technical decisions and work process. `docs/PLAN.md`
is the finished MVP plan, kept as the record; this file is the plan for the first round of
enhancements on top of it.

Nothing proceeds from one part to the next without review and acceptance. Check off each box as it
completes, and record what was actually done underneath the part when it differs from the plan.

## Shape of the work

Four entries from `backend/docs/future_enhancements.md`, each taken all the way through - backend,
frontend where it shows, Playwright where a user can see it - before the next starts. Each entry
leaves `future_enhancements.md` in the commit that finishes it.

| Part | Delivers | Layers | `future_enhancements.md` entry |
|---|---|---|---|
| 1 | Baseline and scope | docs | - |
| 2 | Malformed ids rejected at the web boundary | backend | Validation exceptions from path variables |
| 3 | Dates with a time zone, shown in local time | backend, frontend, e2e | Dates carry no time zone |
| 4 | Recipe line errors on the line | backend, frontend, e2e | Recipe line errors cannot point at a line |
| 5 | Register a new account | backend, frontend, e2e | Multiple users (registration only) |

**Order.** Smallest and backend-only first, so Part 2 settles the error envelope work that Parts 4
and 5 build on. Registration goes last: it is the largest part, and most of the
decisions below are about it.

**Numbering.** Parts restart at 1 in this file. Commits say `V2 Part 2a: ...` so they cannot be
confused with the MVP's Parts.

**Not in this version.** Per-user ownership of recipes, role enforcement, password change or reset,
an administration screen, rate limiting of sign-up. Every account sees and edits everything, as the
one account does today. The smaller items at the end of `future_enhancements.md` are
`PLAN_V3.md`.

## Decisions

Settled 2026-10-04, from the recommendations in the first draft. Each part refers to them by number.

1. **After registering, the user is signed in**, on the Welcome page - the backend signs the new
   account in with the same code login uses. Not back on the sign-in page with "Account created",
   which would cost the user a second form for nothing. (Part 5)
2. **Password: at least 8 characters, at most 72 bytes.** The upper bound is bcrypt's: it reads
   only 72 bytes, and Spring Security refuses a longer password with an exception rather than
   silently truncating. To be proven in 5a before it is relied on, since a refused hash with no
   handler is a 500. Bytes, not characters: `æ` is two bytes. (Part 5)
3. **Username: 3-64 characters, letters, digits, `.`, `_` and `-`, unique regardless of case.**
   MySQL's collation already treats `Admin` and `admin` as one; H2 in tests does not, so the check
   must not rely on the collation alone. (Part 5)
4. **The e2e tests remove the accounts they create** with a helper in `e2e/support/` that deletes
   the row through `docker compose exec mysql`, used only by the registration specs. There is no
   endpoint to delete a user, and a "delete my account" endpoint would be a feature nobody asked
   for. (Part 5)
5. **Dates show as `yyyy-MM-dd HH:mm`, as today, in the browser's time zone, with no label.** Least
   change for the eye, and the label becomes unnecessary. (Part 3)
6. **The register form has a confirm password field, checked by the backend.** With no password
   reset, a typo at registration locks the account for good. The request carries
   `confirmPassword`, and a mismatch is a 400 on that field, shown like any other server error -
   so the frontend's rule of no validation in the browser holds. Changed 2026-10-06 from "checked
   in the browser only", which would have broken that rule. (Part 5)
7. **No email address at registration.** It would only serve a password reset by email, which is
   not in this version (`future_enhancements.md`, "A forgotten password cannot be reset"). Adding
   it then means existing accounts have none, which that entry has to handle. (Part 5)

---

## Part 1: Baseline and scope

- [x] Root `CLAUDE.md`: point Work process and Working documentation at `docs/PLAN_V2.md`, with
      `docs/PLAN.md` as the MVP record
- [x] Root `CLAUDE.md` Limitations: sign-up is now in scope; the rest - no administration screen,
      no password change or reset, no per-user ownership, roles not enforced - stays
- [x] Open questions answered and recorded under Decisions
- [x] Baseline green before any change: `./mvnw clean verify` and the whole Playwright suite
      against the stack. The MVP ended at 253 unit, 105 integration, 95 Playwright tests

**Success criteria.** The documents say what version 2 is, and the numbers every later part is
compared against are recorded here.

**Verify it yourself.** Read the diff of `CLAUDE.md`, `README.md` and `future_enhancements.md`.
Then:

```powershell
scripts\start.ps1
cd backend; .\mvnw clean verify
cd ..\e2e; npx playwright test
```

`docker compose down -v` before the start gives a fresh, reseeded database, at the cost of anything
added by hand.

**Done (2026-10-04).** Deviations and findings:

- Baseline: 253 unit, 105 integration, 95 Playwright tests, all green - the MVP's final numbers
  exactly. Run on the existing database volume rather than a fresh one, to keep its data; the
  suite assumes the seeds are intact, and passing shows they are
- `README.md` points at this plan too, and the multiple-users entry in `future_enhancements.md` no
  longer says sign-up needs roles enforced first: version 2 adds it without, by decision
- Ratings are out of scope "in the MVP and in version 2" in `CLAUDE.md`, where it said "this MVP"
- Decision 7 added: no email address at registration

---

## Part 2: Malformed ids rejected at the web boundary

One step. Backend only; nothing a user sees changes, because a malformed id is already a 400 today,
from `ServiceArguments.toUuid`, one layer in. What changes is where it is caught and the shape of
the answer.

- [x] Prove the starting point first: with `@UUID` on one `@PathVariable` and no new handler,
      a malformed id answers 400 with code `REQUEST_REJECTED` (10) and Spring's generic detail, no
      `validationExceptions[]`. The 2026-09-29 note in `future_enhancements.md` says so; a failing
      test makes it fact
- [x] Handler for `HandlerMethodValidationException` in `ApplicationExceptionHandler`: 400, code
      `VALIDATION_EXCEPTION` (20), one `validationExceptions[]` entry per violation, `objectName`
      the parameter name (`id`, `ingredientId`)
- [x] `@UUID` on every `@PathVariable` id in the five controllers, the recipe ingredient
      sub-resource included - whether those endpoints stay is a `PLAN_V3.md` decision
- [x] **PUT changes route.** Once a parameter carries a constraint, Spring validates the
      `@Valid @RequestBody` as part of method validation too, and a bad body raises
      `HandlerMethodValidationException` instead of `MethodArgumentNotValidException`. The handler
      must therefore also turn body field errors into entries named by field (`name`, not
      `categoryDTO.name`), or every PUT validation message moves off its form field. The guard is new:
      no test covered a PUT's validation errors, see below. POST has no path variable and keeps
      `MethodArgumentNotValidException`, so both handlers stay
- [x] `ServiceArguments.toUuid` stays: ids inside bodies (category, tags, ingredient lines) still
      go through it
- [x] Docs: `backend/CLAUDE.md` Errors table gains the row, the `validator` row and the Gotchas
      line about the unreferenced `@UUID` go; the entry leaves `future_enhancements.md`

**Tests.** Controller slice tests: a malformed id on GET, PUT and DELETE answers 400 with the entry
on `id`, and the service mock sees no call. A PUT with a malformed id **and** an invalid body
reports both. One `*IT` through the real context, since slices have hidden differences from it
before.

**Verify it yourself.**

```powershell
cd backend; .\mvnw clean verify
```

Against the stack, signed in, in the browser's dev tools console:

```js
await fetch("/api/v1/categories/not-a-uuid").then(async r => [r.status, await r.json()]);
// 400, errorCode 20, validationExceptions: [{objectName: "id", ...}]
```

Then edit a category, clear its name and save: the message still appears under Name.

**Done (2026-10-04).** Deviations and findings:

- Proven in three runs of `PathIdValidationTest`, written first. Before any change, the 6 PUT body
  tests passed and the 21 malformed-id tests reached the mock services. With `@UUID` added and no
  handler, all 27 failed with code 10 and Spring's "Validation failure": the malformed id as the
  plan expected, and **the 6 PUT body tests too** - the route change was real, not theoretical. With
  the handler, all 27 pass
- **The plan's guard did not exist.** No controller slice test, `*IT` or Playwright test checked a
  validation error on a PUT; every one used POST, which keeps the old route. Now covered three
  ways: `PathIdValidationTest` for every PUT endpoint, `CategoryIT.shouldReportPutBodyErrorOnTheField`
  through the real context, and a Playwright test that empties a seeded category's name on the
  edit form. That test was proven load-bearing: against a backend built without the handler it
  fails, with no message under Name
- `PathIdValidationTest` is one class over all five controllers, rather than the same tests copied
  into each controller test. It replaces the four `shouldReturn400ForMalformedId` tests, which
  stubbed the service to throw - a service the request no longer reaches
- The message is the validator's own, interpolated: "not-a-uuid is not a valid UUID"
- The envelope's header (code 20, "Validation Exception") moved into one helper, shared with the
  `ConstraintViolationException` handler
- Verified on the stack through the proxy, signed in: `GET /api/v1/categories/not-a-uuid` is 400
  on `id`; a `PUT` there with an empty name is 400 on `id` and `name` together
- `./mvnw clean verify`: 276 unit (253 - 4 + 27), 107 integration (105 + 2). Playwright: 96 (95 + 1)
- `code-review.md`: the `@UUID` validator struck off its dead-code list

---

## Part 3: Dates with a time zone, shown in local time

One step, because it changes the API contract and the frontend reads it: a backend-only step would
leave the app showing a raw ISO string with "UTC" after it.

- [x] `BaseDTO`: drop the `@JsonFormat` pattern. Dates serialize as ISO-8601 with an offset, for
      example `2026-10-01T14:51:00Z`, and read back - assert the exact form in `JsonContractIT`
      rather than assume it, since Jackson 3's defaults decide fractional seconds and `Z` versus
      `+00:00`
- [x] Round trip: a test that GETs a category and PUTs the response unchanged gets a 200, not
      today's 400
- [x] `ENV TZ=UTC` in the backend image stays, but **not for the reason planned**: storage proved
      to be UTC whatever the JVM's zone (below). Its comment and `backend/CLAUDE.md` now say it is
      not load-bearing
- [x] Frontend: one formatting function in `lib/`, browser time zone, the format of decision 5;
      the edit form's "Created" row uses it and loses " UTC". The comment on `Audited` in
      `lib/api.ts` is rewritten
- [x] Docs: `backend/CLAUDE.md` (DTOs paragraph, Docker), `frontend/CLAUDE.md` (the dates line),
      `backend/README.md` if it shows a date; the entry leaves `future_enhancements.md`

**Tests.** `JsonContractIT`: the serialized form and the round trip. Playwright: the helper
`expectCreatedNowBy` changes its pattern, and one test runs the browser in `Europe/Copenhagen`
(`test.use({ timezoneId })`) and asserts the shown time is the server's UTC time plus the offset -
the only test that proves conversion happens, since in UTC the two look the same.

**Verify it yourself.**

```powershell
cd backend; .\mvnw clean verify
scripts\start.ps1; cd e2e; npx playwright test
```

In the browser, `/api/v1/categories` shows `created` ending in `Z`. Edit any category: "Created"
shows your own clock's time, with no "UTC".

**Done (2026-10-04).** Deviations and findings:

- Tests first. `JsonContractIT` now pins the exact form, `2023-01-25T14:25:15+01:00` and
  `2023-01-25T13:25:15Z` - the value's own offset, `Z` for UTC, no fraction when there is none -
  and a round trip. `CategoryIT.shouldAcceptItsOwnResponseAsARequest` PUTs a GET's response back
  unchanged. Before the change: 400, an assertion failure and a deserialization error; after it,
  all three pass
- **The plan's reason for `TZ=UTC` was wrong.** Proven on the stack: a category created with the
  backend in UTC read `15:27:58` in MySQL and `15:27:58Z` from the API. The backend restarted with
  `TZ=Europe/Copenhagen` read that same row back unchanged, and a category it created at 17:28
  CEST was stored as `15:28:21`. Dates are stored in UTC whatever the JVM's zone, so changing it
  shifts nothing. The line stays - the base image is UTC anyway, and it keeps logs in UTC - with a
  comment that says it is not load-bearing. Both probe rows deleted
- MySQL `datetime` keeps whole seconds and rounds: a POST's response says `15:27:57.520806704Z`,
  the GET after it `15:27:58Z`. Harmless - the form always shows a fresh GET, to the minute - and
  noted in `backend/CLAUDE.md`
- The conversion test runs in `Asia/Tokyo` rather than `Europe/Copenhagen`: nine hours from UTC,
  no daylight saving, so a page that skipped the conversion cannot pass by coincidence. It compares
  with Node's own `Intl` formatting of the API's value, not with the app's function. Both it and
  the reworked `expectCreatedNowBy` proven load-bearing: with a formatter that used UTC, built into
  the stack, both failed - Tokyo expected `2026-10-05 00:31` and got `2026-10-04 15:31`
- `expectCreatedNowBy` lets the browser parse what it shows, so it holds in any time zone; the five
  audit tests lost "in UTC" from their names
- The four `*IT` helpers lost a comment saying the DTOs cannot read their own dates
- `./mvnw clean verify`: 276 unit, 109 integration (107 + 2). Lint clean. Playwright: 97 (96 + 1)

---

## Part 4: Recipe line errors on the line

Two steps. The first changes the payload's type and nothing a user sees; the second changes the
error contract and the form together, since either alone breaks the form's error display.

### 4a: Lines as a list

- [x] `RecipeDTO.recipeIngredients` from `Set` to `List`. The entity keeps its `Set`: the order
      matters only in a request, so it can be pointed into
- [x] The mapper writes lines sorted by ingredient name, so a GET answers in a stable order rather
      than the set's
- [x] No behaviour change: every existing test passes unchanged, apart from tests that build the
      DTO with a `Set` - **but one change found, below**

**Verify it yourself.** `.\mvnw clean verify`, and `npx playwright test recipes.spec.ts` against
the rebuilt stack: the recipe pages behave as before.

**Done (2026-10-04).** Deviations and findings:

- **One behaviour change, not foreseen.** Two *identical* lines - same ingredient, amount and unit -
  were merged by Jackson into the `Set` before the service saw them: shown on the stack before the
  rebuild, a POST with two came back 201 with one line. In a `List` both arrive, and the service's
  existing duplicate check refuses them: 400, "Hvedemel is listed more than once.", as it already
  did for the same ingredient with different amounts. Kept, as the consistent rule:
  `RecipeIT.shouldRejectIdenticalLines` pins it, and the stack now refuses the same request
- Danish order, by `Collator`, not `String` order: plain order puts Å before Æ and Ø, and the app
  sorts Danish everywhere. `RecipeMapperTest.shouldListIngredientLinesInDanishOrder` failed first,
  on the set's arbitrary order; on the stack the seeded recipes answer `Fuldkorns hvedemel, Gær,
  Hvedemel, Salt, Surdej, Vand`
- The frontend still sorts lines itself, in the read view and the form. Now redundant, left alone:
  4a changes no frontend code
- `./mvnw clean verify`: 277 unit (+1), 110 integration (+1). Playwright: 97

### 4b: Errors per line

- [x] `ServiceException` carries a list of field errors instead of one optional `field`; the
      handler writes one `validationExceptions[]` entry each. Every existing single-field use keeps
      its shape
- [x] `handleRecipeIngredients` reports each problem on its line and input:
      `recipeIngredients[2].ingredientId`, `.amount` or `.unitId`. The message no longer needs to
      name the ingredient, since it sits on the row: "Needs an amount above zero.". A duplicate is
      reported on the later line's `ingredientId`. All problems still come back together in one 400,
      and nothing is written when one is refused
- [x] Still in the service, not Bean Validation: existence, duplicates and the maximum amount need
      the database, and splitting the checks would report a payload's problems in two rounds
- [x] Form: `IngredientLines` shows each error under its input, linked with `aria-describedby`; the
      single message under the section goes. The form already sends lines in the order it shows
      them, so index `n` is row `n`
- [x] Docs: `backend/CLAUDE.md` (the "Recipe writes carry their relations" paragraph),
      `frontend/CLAUDE.md` if it describes the form's errors; the entry leaves
      `future_enhancements.md`

**Tests.** `RecipeServiceTest` and `RecipeIT`: each problem kind on its index, several problems in
one response, a duplicate on the second line. Playwright: the two existing line-error tests in
`recipes.spec.ts` move from the section's description to the rows' inputs, and one more asserts an
error on line 3 of 3 is shown on line 3.

**Verify it yourself.** In the browser: a new recipe with three lines - the second without an
amount, the third repeating the first ingredient. Save: the message is under the second line's
amount and the third line's ingredient, and nowhere else.

**Done (2026-10-06).** Deviations and findings:

- `ServiceException` holds one list, `fieldErrors`. The builder's `field("name")` still works and
  becomes a one-entry list carrying the exception's message, so the six existing callers did not
  change. Ten service-test assertions read the old `field` property by name; they now compare the
  whole entry, message included, which needed `equals` on `ValidationExceptionEnvelope`
- A line now reports **every** problem it has, one per input, where it used to stop at the first:
  a blank line is three entries. Checked in the order the form shows the inputs - ingredient,
  amount, unit - so the response reads in the same order as the page
- Messages: "Choose an ingredient.", "This ingredient does not exist.", "Hvedemel is already on
  line 1." (the repeat keeps the name, and the line it collides with, numbered from 1 as a reader
  counts), "Needs an amount above zero.", "At most 9999.99.", "Choose a unit.", "This unit does not
  exist.". The exception's own message is "One or more ingredient lines are invalid"; the form does
  not show it, since field errors are present
- Tests first: with `ServiceException` changed and the check not yet, the handler test passed and
  the six reworked `RecipeIT` line tests failed, still answered on `recipeIngredients`.
  `RecipeIT.shouldPointAtTheLineAtFault` is the line-3-of-3 case: two problems on the third line,
  nothing on the other two
- Found on the way: my first version found an existing line by the id of the ingredient entity the
  repository returned rather than the id in the request. Equal in production, but
  `RecipeServiceTest.shouldUpdateRecipe` stubs every lookup with one ingredient, and Mockito's
  strict stubs reported two lookups never made. Back to the request's id, as before
- Playwright: the one line-error test became "each line problem is shown under its own input, on
  its own line", asserting the message on each input and an empty description on every other
  input of the three lines. Checked by screenshot at 1000 and 375 px: each message under its input;
  "Needs an amount above zero." wraps to three lines in the narrow amount column, readable
- `./mvnw clean verify`: 278 unit (+1), 111 integration (+1). Lint and `tsc` clean. Playwright: 97

---

## Part 5: Register a new account

Two steps: the backend, proven by an `*IT` and the API, then the frontend.

### 5a: Backend registration

- [x] `POST /api/v1/auth/register`, open like login (`permitAll`), CSRF-protected like login
- [x] `RegisterRequestDTO`: username, password and `confirmPassword`, Bean Validation per
      decisions 2 and 3; a confirmation that differs is a 400 on `confirmPassword` (decision 6).
      `toString` never prints either password
- [x] The new account: `ROLE_USER` only, enabled, password through the existing
      `PasswordEncoder`. `created_by` is the new username. **Not through JPA**: the request is
      anonymous, so `BaseEntityListener` would write `anonymous`. Plain SQL, as `AdminBootstrap`
      already does - the insert moves into one place both use
- [x] A taken username, in any case: 409, a new code `USERNAME_TAKEN` (603), on the field
      `username`. The unique index stays the backstop. This does tell a caller which usernames
      exist; with open sign-up that is unavoidable, and login's single message stays as it is
- [x] Prove the 72-byte boundary before relying on it: what a 73-byte password does today, then
      the 400 that replaces it
- [x] Answer 201 and sign the user in, by the same path as login - session id changed, context
      saved (decision 1)
- [x] `AdminBootstrap` unchanged in behaviour: it still creates the first account only while the
      table is empty - now through `AppUserWriter`, the shared insert
- [x] Docs: `backend/CLAUDE.md` Authentication ("One account" goes), `backend/README.md` endpoint
      list, `AppUser`'s "read-only" comment

**Tests.** `RegistrationIT`, real context and real security chain, like `AuthenticationIT`:
registers and is signed in; the new user's next request is authenticated and `/me` names them;
the stored password is a `{bcrypt}` hash, never the plaintext; roles are `ROLE_USER`; `created_by`
is the username; a POST of a category by the new user has `createdBy` the new username; taken
username, and the same in other case, 409 on `username`; each rule 400 on its field; a mismatched
confirmation 400 on `confirmPassword` and no account created; no CSRF token 403; the registered user
can sign out and in again.

**Verify it yourself.**

```powershell
cd backend; .\mvnw clean test -Dtest=RegistrationIT
```

Against the stack, signed out, in the console of `http://localhost:3000/login`:

```js
await fetch("/api/v1/auth/me");   // 401, and the XSRF-TOKEN cookie
const t = decodeURIComponent(document.cookie.match(/XSRF-TOKEN=([^;]+)/)[1]);
await fetch("/api/v1/auth/register", {method: "POST", headers: {"Content-Type": "application/json", "X-XSRF-TOKEN": t}, body: JSON.stringify({username: "tester", password: "long enough", confirmPassword: "long enough"})}).then(async r => [r.status, await r.json()]);
```

```powershell
docker compose exec mysql sh -c 'mysql -uroot -p"$MYSQL_ROOT_PASSWORD" recipes -e "select username, roles, created_by, left(password, 8) from app_user"'
```

**Done (2026-10-07).** Deviations and findings:

- **The 72-byte boundary, proven first** with a throwaway test on the application's encoder: 72
  bytes encodes; 73 bytes, and 37 Danish letters (74 bytes), throw `IllegalArgumentException`
  ("password cannot be more than 72 bytes") - an unhandled 500. Also found: when *checking*, the
  encoder silently ignores everything past 72 bytes - a 73-byte password matched the hash of its
  first 72. The cap at registration means a stored password is always checked whole
- Which rules live where: those a value can be checked against alone are Bean Validation on
  `RegisterRequestDTO`; the byte limit and the confirmation are in `AuthServiceImpl.register`, both
  in one 400 through the field-error list from 4b, code 20 like any validation failure. A request
  breaking both kinds gets the Bean Validation errors first, the others on the next try
- "Letters" are any alphabet's (`\p{L}`), so `Søren` is a valid username; a test pins it
- The insert lives in `repository/AppUserWriter`, which also hashes, so the plaintext goes no
  further than one method. `AdminBootstrap` calls it; its behaviour is unchanged
- **Found: MockMvc's `csrf()` changes the cached context.** `RegistrationIT` passed alone and lost
  17 of 18 tests in the full suite: every one found no `XSRF-TOKEN` cookie. Proven by running
  `CategoryIT` then `RegistrationIT` - 17 errors - against `RegistrationIT` alone - none. `csrf()`
  replaces the CSRF token repository inside the shared context's filter with a test one that sets
  no cookie, and it stays replaced. Fixed by giving `RegistrationIT` `AuthenticationIT`'s
  properties, so the two share a context no `csrf()` test runs in. Recorded in `backend/CLAUDE.md`
- The case-insensitive check proven load-bearing: with a case-sensitive lookup swapped in, H2
  accepted the uppercase copy of a taken name, 201
- On the stack, signed out, through the proxy: register 201 and signed in, `/me` names the new
  user; the row is `ROLE_USER`, enabled, created by itself, a `{bcrypt}` hash; the same name in
  capitals is 409 (MySQL); 37 Danish letters plus a wrong confirmation is one 400 with both
  fields. The probe account deleted with SQL afterwards. No error in the backend log
- `./mvnw clean verify`: 278 unit, 129 integration (+18). Playwright: 97

### 5b: Register in the browser

- [x] `/register` page: the user chooses their username and password - and confirms the password
      (decision 6) - then Register, and a link back to sign in. Server errors shown on their
      fields, as the entity forms do
- [x] The sign-in page links to it: "Create an account"
- [x] `proxy.ts` lets `/register` through without a session, as it does `/login`
- [x] On success, signed in on the Welcome page (decision 1)
- [x] Docs: `frontend/CLAUDE.md`; root `CLAUDE.md` Business Requirements gains the register line

**Tests.** Playwright, starting signed out: from sign-in to register and back; register, land
signed in with the new name in the header; sign out and sign in again with it; a taken username
shown on the field; a mismatched confirmation shown on the confirm field; the new user creates a
category and its edit form says "Created by" the new name. Each test removes its account
(decision 4).

**Success criteria for Part 5.** A visitor can create an account from the sign-in page, use the
app with it, and every row they write carries their username - asserted by a test.

**Verify it yourself.** In a private browser window: sign-in page, Create an account, register,
use the app, sign out, sign in again. Then try the same username in capitals.

**Done (2026-10-07).** Deviations and findings for 5b:

- The form uses `TextField`, which gained `type` and `autoComplete`, so each server error sits
  under its field as on every entity form. Its labels are bold, as on those forms; the sign-in
  page keeps its own plain ones - a small visual difference, left alone
- Rules break in two rounds: Bean Validation answers before the service runs, so a short password
  hides a taken name and a wrong confirmation until it is fixed. Seen on a screenshot; it follows
  from 5a's split, left as it is unless you want one round
- `e2e/support/accounts.ts` deletes test accounts with SQL through `docker compose exec mysql`, and
  refuses any name that is not `e2e-` plus eight hex digits, since the name goes into SQL. Tests
  that create a category remove it through the API first. After the suite, `app_user` holds only
  `admin`
- The taken-name test uses the seeded account's name in capitals, so it proves case on MySQL and
  needs no account of its own
- Lint and `tsc` clean. Playwright: 103 (97 + 6), all six new ones failing before the page existed

### Part 5 as a whole

**Done.** A visitor creates an account from the sign-in page, lands signed in on the Welcome page,
and everything they write carries their username - asserted from the backend in `RegistrationIT`
and from the browser in `register.spec.ts`. `./mvnw clean verify`: 278 unit, 129 integration.
Playwright: 103.
