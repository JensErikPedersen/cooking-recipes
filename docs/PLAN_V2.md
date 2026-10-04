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
and 5 build on. Registration goes last: it is the largest part and the only one with open
decisions, which can be settled while Parts 2-4 are under way.

**Numbering.** Parts restart at 1 in this file. Commits say `V2 Part 2a: ...` so they cannot be
confused with the MVP's Parts.

**Not in this version.** Per-user ownership of recipes, role enforcement, password change or reset,
an administration screen, rate limiting of sign-up. Every account sees and edits everything, as the
one account does today. The smaller items at the end of `future_enhancements.md` are
`PLAN_V3.md`.

## Open questions

Each has a recommendation; nothing below depends on it until the part named.

1. **After registering, where does the user land?** (Part 5) Recommended: signed in, on the Welcome
   page - the backend signs the new account in with the same code login uses. The alternative is
   back on the sign-in page with "Account created", which costs the user a second form for nothing.
2. **Password rules.** (Part 5) Recommended: at least 8 characters, at most 72 bytes. The upper
   bound is bcrypt's: it reads only 72 bytes, and Spring Security refuses a longer password with an
   exception rather than silently truncating. To be proven in 5a before it is relied on, since a
   refused hash with no handler is a 500. Note bytes, not characters: `æ` is two bytes.
3. **Username rules.** (Part 5) Recommended: 3-64 characters, letters, digits, `.`, `_` and `-`,
   unique regardless of case. MySQL's collation already treats `Admin` and `admin` as one; H2 in
   tests does not, so the check must not rely on the collation alone.
4. **Removing accounts the e2e tests create.** (Part 5) There is no endpoint to delete a user, and
   the tests must leave the data as seeded. Recommended: a helper in `e2e/support/` that deletes the
   row through `docker compose exec mysql`, used only by the registration spec. The alternative, a
   "delete my account" endpoint, is a feature nobody has asked for.
5. **Date display.** (Part 3) Recommended: the same `yyyy-MM-dd HH:mm` as today, in the browser's
   time zone, without a label. Least change for the eye, and the label becomes unnecessary.
6. **Confirm password field on the register form.** (Part 5) Recommended: yes, checked in the
   browser only. With no password reset, a typo at registration locks the account for good.

---

## Part 1: Baseline and scope

- [ ] Root `CLAUDE.md`: point Work process and Working documentation at `docs/PLAN_V2.md`, with
      `docs/PLAN.md` as the MVP record
- [ ] Root `CLAUDE.md` Limitations: sign-up is now in scope; the rest - no administration screen,
      no per-user ownership, roles not enforced - stays
- [ ] Open questions above answered, the answers written in place of the recommendations
- [ ] Baseline green before any change: `./mvnw clean verify` and the whole Playwright
      suite against a fresh stack. The MVP ended at 253 unit, 105 integration, 95 Playwright tests

**Success criteria.** The documents say what version 2 is, and the numbers every later part is
compared against are recorded here.

**Verify it yourself.** Read the `CLAUDE.md` diff. Then:

```powershell
docker compose down -v; scripts\start.ps1
cd backend; .\mvnw clean verify
cd ..\e2e; npx playwright test
```

---

## Part 2: Malformed ids rejected at the web boundary

One step. Backend only; nothing a user sees changes, because a malformed id is already a 400 today
- from `ServiceArguments.toUuid`, one layer in. What changes is where it is caught and the shape of
the answer.

- [ ] Prove the starting point first: with `@UUID` on one `@PathVariable` and no new handler,
      a malformed id answers 400 with code `REQUEST_REJECTED` (10) and Spring's generic detail, no
      `validationExceptions[]`. The 2026-09-29 note in `future_enhancements.md` says so; a failing
      test makes it fact
- [ ] Handler for `HandlerMethodValidationException` in `ApplicationExceptionHandler`: 400, code
      `VALIDATION_EXCEPTION` (20), one `validationExceptions[]` entry per violation, `objectName`
      the parameter name (`id`, `ingredientId`)
- [ ] `@UUID` on every `@PathVariable` id in the five controllers, the recipe ingredient
      sub-resource included - whether those endpoints stay is a `PLAN_V3.md` decision
- [ ] **PUT changes route.** Once a parameter carries a constraint, Spring validates the
      `@Valid @RequestBody` as part of method validation too, and a bad body raises
      `HandlerMethodValidationException` instead of `MethodArgumentNotValidException`. The handler
      must therefore also turn body field errors into entries named by field (`name`, not
      `categoryDTO.name`), or every PUT validation message moves off its form field. The existing
      PUT validation tests and the e2e form tests are the guard. POST has no path variable and keeps
      `MethodArgumentNotValidException`, so both handlers stay
- [ ] `ServiceArguments.toUuid` stays: ids inside bodies (category, tags, ingredient lines) still
      go through it
- [ ] Docs: `backend/CLAUDE.md` Errors table gains the row, the `validator` row and the Gotchas
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

---

## Part 3: Dates with a time zone, shown in local time

One step, because it changes the API contract and the frontend reads it: a backend-only step would
leave the app showing a raw ISO string with "UTC" after it.

- [ ] `BaseDTO`: drop the `@JsonFormat` pattern. Dates serialize as ISO-8601 with an offset, for
      example `2026-10-01T14:51:00Z`, and read back - assert the exact form in `JsonContractIT`
      rather than assume it, since Jackson 3's defaults decide fractional seconds and `Z` versus
      `+00:00`
- [ ] Round trip: a test that GETs a category and PUTs the response unchanged gets a 200, not
      today's 400
- [ ] `ENV TZ=UTC` in the backend image **stays**, for a different reason than today: the columns are
      `datetime`, which store no zone, so the JVM's zone decides what a stored value means. Changing
      it would shift every existing row. Its comment and `backend/CLAUDE.md` are reworded to say so
- [ ] Frontend: one formatting function in `lib/`, browser time zone, the format decided in open
      question 5; the edit form's "Created" row uses it and loses " UTC". The comment on `Audited`
      in `lib/api.ts` is rewritten
- [ ] Docs: `backend/CLAUDE.md` (DTOs paragraph, Docker), `frontend/CLAUDE.md` (the dates line),
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

---

## Part 4: Recipe line errors on the line

Two steps. The first changes the payload's type and nothing a user sees; the second changes the
error contract and the form together, since either alone breaks the form's error display.

### 4a: Lines as a list

- [ ] `RecipeDTO.recipeIngredients` from `Set` to `List`. The entity keeps its `Set`: the order
      matters only in a request, so it can be pointed into
- [ ] The mapper writes lines sorted by ingredient name, so a GET answers in a stable order rather
      than the set's
- [ ] No behaviour change: every existing test passes unchanged, apart from tests that build the
      DTO with a `Set`

**Verify it yourself.** `.\mvnw clean verify`, and `npx playwright test recipes.spec.ts` against
the rebuilt stack: the recipe pages behave as before.

### 4b: Errors per line

- [ ] `ServiceException` carries a list of field errors instead of one optional `field`; the
      handler writes one `validationExceptions[]` entry each. Every existing single-field use keeps
      its shape
- [ ] `handleRecipeIngredients` reports each problem on its line and input:
      `recipeIngredients[2].ingredientId`, `.amount` or `.unitId`. The message no longer needs to
      name the ingredient, since it sits on the row: "Needs an amount above zero.". A duplicate is
      reported on the later line's `ingredientId`. All problems still come back together in one 400,
      and nothing is written when one is refused
- [ ] Still in the service, not Bean Validation: existence, duplicates and the maximum amount need
      the database, and splitting the checks would report a payload's problems in two rounds
- [ ] Form: `IngredientLines` shows each error under its input, linked with `aria-describedby`; the
      single message under the section goes. The form already sends lines in the order it shows
      them, so index `n` is row `n`
- [ ] Docs: `backend/CLAUDE.md` (the "Recipe writes carry their relations" paragraph),
      `frontend/CLAUDE.md` if it describes the form's errors; the entry leaves
      `future_enhancements.md`

**Tests.** `RecipeServiceTest` and `RecipeIT`: each problem kind on its index, several problems in
one response, a duplicate on the second line. Playwright: the two existing line-error tests in
`recipes.spec.ts` move from the section's description to the rows' inputs, and one more asserts an
error on line 3 of 3 is shown on line 3.

**Verify it yourself.** In the browser: a new recipe with three lines - the second without an
amount, the third repeating the first ingredient. Save: the message is under the second line's
amount and the third line's ingredient, and nowhere else.

---

## Part 5: Register a new account

Two steps: the backend, proven by an `*IT` and the API, then the frontend.

### 5a: Backend registration

- [ ] `POST /api/v1/auth/register`, open like login (`permitAll`), CSRF-protected like login
- [ ] `RegisterRequestDTO`: username and password, Bean Validation per open questions 2 and 3.
      `toString` never prints the password
- [ ] The new account: `ROLE_USER` only, enabled, password through the existing
      `PasswordEncoder`. `created_by` is the new username. **Not through JPA**: the request is
      anonymous, so `BaseEntityListener` would write `anonymous`. Plain SQL, as `AdminBootstrap`
      already does - the insert moves into one place both use
- [ ] A taken username, in any case: 409, a new code `USERNAME_TAKEN` (603), on the field
      `username`. The unique index stays the backstop. This does tell a caller which usernames
      exist; with open sign-up that is unavoidable, and login's single message stays as it is
- [ ] Prove the 72-byte boundary before relying on it: what a 73-byte password does today, then
      the 400 that replaces it
- [ ] Answer 201 and sign the user in, by the same path as login - session id changed, context
      saved - if open question 1 is answered that way
- [ ] `AdminBootstrap` unchanged: it still creates the first account only while the table is empty
- [ ] Docs: `backend/CLAUDE.md` Authentication ("One account" goes), `backend/README.md` endpoint
      list, `AppUser`'s "read-only" comment

**Tests.** `RegistrationIT`, real context and real security chain, like `AuthenticationIT`:
registers and is signed in; the new user's next request is authenticated and `/me` names them;
the stored password is a `{bcrypt}` hash, never the plaintext; roles are `ROLE_USER`; `created_by`
is the username; a POST of a category by the new user has `createdBy` the new username; taken
username, and the same in other case, 409 on `username`; each rule 400 on its field; no CSRF token
403; the registered user can sign out and in again.

**Verify it yourself.**

```powershell
cd backend; .\mvnw clean test -Dtest=RegistrationIT
```

Against the stack, signed out, in the console of `http://localhost:3000/login`:

```js
await fetch("/api/v1/auth/me");   // 401, and the XSRF-TOKEN cookie
const t = decodeURIComponent(document.cookie.match(/XSRF-TOKEN=([^;]+)/)[1]);
await fetch("/api/v1/auth/register", {method: "POST", headers: {"Content-Type": "application/json", "X-XSRF-TOKEN": t}, body: JSON.stringify({username: "tester", password: "long enough"})}).then(async r => [r.status, await r.json()]);
```

```powershell
docker compose exec mysql sh -c 'mysql -uroot -p"$MYSQL_ROOT_PASSWORD" recipes -e "select username, roles, created_by, left(password, 8) from app_user"'
```

### 5b: Register in the browser

- [ ] `/register` page: the user chooses their username and password - plus a confirm password
      field if open question 6 says so - then Register, and a link back to sign in. Server errors shown on their fields, as the entity forms do
- [ ] The sign-in page links to it: "Create an account"
- [ ] `proxy.ts` lets `/register` through without a session, as it does `/login`
- [ ] On success, per open question 1
- [ ] Docs: `frontend/CLAUDE.md`; root `CLAUDE.md` Business Requirements gains the register line

**Tests.** Playwright, starting signed out: from sign-in to register and back; register, land
signed in with the new name in the header; sign out and sign in again with it; a taken username
shown on the field; mismatched confirmation refused without a request; the new user creates a
category and its edit form says "Created by" the new name. Each test removes its account per open
question 4.

**Success criteria for Part 5.** A visitor can create an account from the sign-in page, use the
app with it, and every row they write carries their username - asserted by a test.

**Verify it yourself.** In a private browser window: sign-in page, Create an account, register,
use the app, sign out, sign in again. Then try the same username in capitals.
