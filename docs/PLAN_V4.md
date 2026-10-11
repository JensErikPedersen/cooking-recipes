# Implementation plan, version 4

Read `CLAUDE.md` first for the requirements, technical decisions and work process. `docs/PLAN.md`
is the MVP, `docs/PLAN_V2.md` and `docs/PLAN_V3.md` the first two rounds of enhancements, all
finished and kept as the record; this file plans the third round.

Nothing proceeds from one part to the next without review and acceptance. Check off each box as it
completes, and record what was actually done underneath the part when it differs from the plan.

## Shape of the work

Three entries from `backend/docs/future_enhancements.md`. Each stands on its own, and none needs a
kind of infrastructure the stack lacks today:

| Part | Delivers | Layers | Entry |
|---|---|---|---|
| 1 | Baseline and scope | docs | - |
| 2 | Change password, in the API | backend | Password reset, step 1 (and Multiple users, point 4) |
| 3 | Change password, on a page | frontend, e2e | the same |
| 3b | Nothing typed before the page is ready is lost | frontend, e2e | - (found in Part 3) |
| 4 | One name-lookup contract | backend | Name-based lookup, points 1 and 3 |
| 5 | `?name=` on the list endpoints | backend | Name-based lookup, point 2 |
| 6 | A search field on the list pages | frontend, e2e | Name-based lookup, "no screen searches" |
| 7 | Two database accounts | Docker, backend config, docs | A single database account |

**Order.** What a user gains first, then the API clean-up and the feature built on it, and the
change to the stack last: Part 7 needs a fresh database volume, which should not interrupt the
parts before it. Parts 2-3, 4-6 and 7 are independent groups and could be taken in any order.

**Out of scope, by decision of 2026-10-10.** Ratings, role enforcement and ownership, and reset by
email. Each needs a decision of its own first: a change of scope, a change of who may do what, and
a mail container plus the collecting of email addresses. They stay in `future_enhancements.md`.

**Numbering.** Parts restart at 1. Commits say `V4 Part 2: ...`.

**Branch.** PR #2 merged `future_enhancements` into `master` on 2026-10-10. Version 4 is built on a
new branch, `enhancements_v4`, from the updated `master`.

## Decisions

Settled 2026-10-11: the recommendations of the draft, accepted as they stood. Each part refers to
them by number.

1. **A wrong current password is a 400 on the field `currentPassword`, not a 401.** A 401 already
   means "no session" in this application: `lib/api.ts` answers one by sending the browser to
   `/login`, so a mistyped password would look like being thrown out. (Part 2)
2. **The session that changes the password stays signed in, with a new session id**, as at login.
   Other sessions of the same account are not ended: that needs Spring Security's
   `SessionRegistry`, which nothing else here uses. Recorded in `future_enhancements.md` as what
   remains. (Part 2)
3. **One set of password rules.** Change takes registration's rules unchanged - at least 8
   characters, at most 72 bytes, a confirmation that matches - from one place in
   `AuthServiceImpl`, not a copy. A new password equal to the current one is allowed: refusing it
   protects nothing. (Part 2)
4. **The new hash is written by `AppUserWriter`**, with plain SQL that also sets `updated` and
   `updated_by`, so the plaintext still goes no further than that one class. Through JPA would
   work too - the user is signed in, so the `Session` holds a name - but would put the encoding in
   a second place. (Part 2)
5. **The page is `/account/password`**, reached by making the username in the header a link. After
   a change it stays on the page, clears the fields and says "Your password has been changed." -
   there is no read view to go to. (Part 3)
6. **The service contract is `findAllByNameContains` alone**, for Category, Ingredient, Tag and
   Recipe. The service-level `findByName` (Category, Ingredient) and `findTagByName` (Tag) go:
   only their own tests call them. The exact-match repository queries stay, for the
   duplicate-name checks, and Tag's is renamed `findByName`. The partial match ignores case -
   `findAllByNameContainingIgnoreCase` - because MySQL's collation does and H2 does not, the same
   reason as `existsByUsernameIgnoreCase`. Also dead and removed in the same part:
   `IngredientJpaRepository.findAllByDescriptionContains` and `RecipeService.findAllByCategoryName`
   with its repository query, both called only by tests. (Part 4)
7. **The search field calls the API's `?name=`, not a filter in the browser.** The lists are small
   enough to filter in the page, but then the endpoint would be tested and unreachable - the very
   problem this entry was written about. The field submits on Enter or its Search button, not on
   every keystroke, and the term lives in the page's state, not in the URL. (Part 6)
8. **Two accounts, created by an init script.** `recipesadmin` with all privileges on the schema,
   for Liquibase; `recipesuser` with `SELECT, INSERT, UPDATE, DELETE`, for the application. New
   `.env` variables `DB_ADMIN_USERNAME` and `DB_ADMIN_PASSWORD`; `MYSQL_USER` and `MYSQL_PASSWORD`
   leave `docker-compose.yml`, since the entrypoint would grant that account everything. An
   existing volume never runs the script, so the switch is `docker compose down -v` - every row
   reseeds and registered accounts are lost - documented in `README.md`, not migrated in place.
   (Part 7)

---

## Part 1: Baseline and scope

- [x] Branch `enhancements_v4` from the updated `master`
- [x] Root `CLAUDE.md` and `README.md` point at `docs/PLAN_V4.md` as the current plan, with
      `docs/PLAN.md`, `docs/PLAN_V2.md` and `docs/PLAN_V3.md` as the record. Ratings stay out of
      scope in version 4 too
- [x] Open questions answered and recorded under Decisions
- [x] Baseline green before any change: `./mvnw clean verify` and the whole Playwright suite.
      Version 3 ended at 263 unit, 135 integration, 107 Playwright tests

**Verify it yourself.** Read the diff, then:

```powershell
scripts\start.ps1
cd backend; .\mvnw clean verify
cd ..\e2e; npx playwright test
```

**Done (2026-10-11).** `enhancements_v4` starts at `3fc3ca5`, the merge of PR #2. Baseline: 263
unit, 135 integration, 107 Playwright tests, all green - version 3's final numbers, Playwright
against the stack as built from that code. Root `CLAUDE.md` says ratings stay out of scope in
version 4 too; its "no password change" stays until Part 3 delivers one.

---

## Part 2: Change password, in the API

One step, backend only.

- [x] `PUT /api/v1/auth/password`, taking `currentPassword`, `newPassword`, `confirmPassword`;
      204 on success. Signed in only - it is not added to `SecurityConfig`'s open paths - and
      CSRF-protected like every write
- [x] `ChangePasswordRequestDTO`: all three `@NotNull`, `newPassword` `@Size(min = 8)`, messages from
      `ValidationMessages.properties` as registration's are
- [x] `AuthServiceImpl.changePassword`: the current password checked against the stored hash
      (decision 1), then the shared rules (decision 3) - every problem in one 400, as registration
      reports them - then the new hash (decision 4) and a new session id (decision 2)
- [x] `AppUserWriter.updatePassword(username, password, updatedBy)`
- [x] Docs: `backend/CLAUDE.md` Authentication, `backend/README.md` endpoint table; in
      `future_enhancements.md` step 1 of the reset entry and point 4 of Multiple users say it is
      done, and the entry records that other sessions stay signed in

**Tests.** Authentication has no unit or slice tests today - registration is covered end to end by
`RegistrationIT` alone - and this follows it. A `PasswordChangeIT` sharing `AuthenticationIT`'s
properties, and so its context (see `backend/CLAUDE.md`, "MockMvc's `csrf()`"): register an
account, change its password over the real chain, and then the old password fails to sign in and
the new one succeeds; a wrong current password, a short, an over-long (73 bytes in fewer
characters, with `æ`) and a mismatched new one, each on its field, and all together in one 400,
the stored hash unchanged after each; without a session the change is a 401, without the CSRF
header a 403; the stored value is a bcrypt hash, never the plaintext. Registration's own tests
pass unchanged after its rules move.

**Verify it yourself.** `.\mvnw clean verify`. On the stack, in a private window: register a
throwaway account, then in the dev tools console

```js
const t = decodeURIComponent(document.cookie.match(/XSRF-TOKEN=([^;]+)/)[1]);
const r = await fetch("/api/v1/auth/password", {method: "PUT", headers: {"Content-Type": "application/json", "X-XSRF-TOKEN": t}, body: JSON.stringify({currentPassword: "...", newPassword: "...", confirmPassword: "..."})});
r.status;   // 204; a wrong currentPassword gives 400 and its message in await r.json()
```

Log out and sign in with the new password. Then delete the account with the SQL in
`e2e/support/accounts.ts`.

**Done (2026-10-11).** Deviations and findings:

- Tests first: `PasswordChangeIT`, 11 tests. Before the endpoint existed 9 failed on 404; the 401
  and the 403 passed from the start, since the security chain answers them for any path
- **Two rounds, as registration.** A missing field and a new password under 8 characters are Bean
  Validation's, answered before the service runs; a wrong current password, the 72-byte limit and
  the confirmation are the service's, and those three come in one 400 together. `confirmPassword`
  is not `@NotNull`, as at registration: an absent one is a mismatch
- The current password is checked with the `AuthenticationManager`, as login checks it, rather
  than by reading the hash and calling the encoder: no new dependency in `AuthServiceImpl`
- `checkNewPassword` and `throwIfAny` hold the rules registration had inline; `RegistrationIT`, 18
  tests, passes unchanged
- The new session id is load-bearing: with `changeSessionId()` removed,
  `shouldKeepTheSessionUnderANewId` failed
- On the stack, through the proxy: register 201, a wrong current password 400 on
  `currentPassword`, the change 204, `/me` still 200 on the same cookies, the old password 401
  and the new one 200 after logging out; the row held a new `{bcrypt}` hash, `updated` a second
  after `created`, `updated_by` the user. The probe account deleted; `app_user` holds `admin` only
- `./mvnw clean verify`: 263 unit, 146 integration (+11). Playwright: 107, against the rebuilt
  backend
- Docs: `backend/CLAUDE.md` Authentication and its test notes, `backend/README.md`;
  `future_enhancements.md` - and decision 2 here - name `SessionRegistry` as Spring Security's,
  which the draft had attributed to Spring Session

---

## Part 3: Change password, on a page

One step, frontend and e2e.

- [x] `auth.changePassword` in `lib/api.ts`
- [x] `app/(app)/account/password/page.tsx`: three password fields with `autoComplete`
      `current-password` and `new-password`, the server's errors under each, and the message of
      decision 5 after a change
- [x] The username in `AppShell`'s header becomes a link to it
- [x] Docs: `frontend/CLAUDE.md`

**Tests.** Playwright, on a throwaway account from `uniqueUsername()` - never the shared admin, whose
password every other test signs in with: the header link leads to the page; a change succeeds,
shows the message, and after logging out the new password signs in and the old one does not; a
wrong current password shows its error on that field and the session survives; a mismatched
confirmation shows its error. Each test removes its account with `deleteAccount`, as
`register.spec.ts` does. At phone width the header still fits.

**Verify it yourself.** Register an account, change its password from the header link, log out, and
sign in with the new password.

**Done (2026-10-11).** Deviations and findings:

- Tests first: `e2e/tests/password.spec.ts`, 5 tests, each on an account it registers and removes.
  Before the page existed all 5 failed at the header link. `/account/password` joined the phone
  list in `phone.spec.ts`; that one passed before the page too - an unknown URL shows not found,
  which also fits a phone - so it guards the layout, not the page's existence
- Beyond the plan's list: the session survives the change - the next page opens signed in - and a
  too-short new password is shown on its field
- The page is not an `EntityForm`, which ends in a read view; it keeps its own state, as the
  register page does. The success line is `role="status"`
- Lint and `tsc` clean. Playwright: 113 (107 + 5 + 1). Backend unchanged: 263 unit, 146 integration
- Docs: root `CLAUDE.md` Limitations (password change exists; reset does not), `frontend/CLAUDE.md`
- **Found on the way, not fixed here: sign-in can be filled before the page hydrates.** Twice, the
  first full run after the frontend container (re)started failed in `states.spec.ts`, "when the
  session ends while a page is open": still on `/login?next=%2Fcategories` after Sign in. Proven
  with a throwaway spec that delays `/_next/static/**` by 1.5 s: the same failure every time, and
  the page then shows empty fields and "Validation Exception". Playwright fills the controlled
  inputs before React has hydrated; hydration renders them from empty state, and the click, by
  then handled by React, sends an empty form. The race is version 3's, not this part's, and a
  user who types on a slow connection could meet it too. Fixed in Part 3b

---

## Part 3b: Nothing typed before the page is ready is lost

Added 2026-10-11, after the finding in Part 3. Of the three ways offered - disable the fields until
React runs, read the values from the form on submit, or only make the tests wait - the first was
chosen: the smallest change, and it protects a user as well as the tests.

- [x] `lib/use-hydrated.ts`: `useHydrated()`, false in the server's HTML and true once React runs,
      by `useSyncExternalStore` - no effect that sets state
- [x] The login and register pages wrap their fields and button in
      `<fieldset disabled={!hydrated}>`. The signed-in pages need nothing: `AppShell` renders them
      only after hydration
- [x] Docs: `frontend/CLAUDE.md`

**Tests.** One in `auth.spec.ts` and one in `register.spec.ts`: with `/_next/static/**` delayed by
a second, open the page without waiting for its load event, fill it in and submit - signed in, or
registered, all the same.

**Verify it yourself.** In Chrome's dev tools, Network, throttle to "3G" and open `/login`: the fields
take no typing until the page is ready, and what is typed after that signs in as usual.

**Done (2026-10-11).** Deviations and findings:

- The first version of each test passed before the fix: `goto()` waits for the load event, and
  by then the scripts have run. The race needs a page that is not yet loaded - as after
  `lib/api.ts`'s redirect, where the failing test met it. With `waitUntil: "commit"` all 6 runs
  (3 each) failed before the fix and pass after it
- `/login` and `/register` are still prerendered; lint and `tsc` clean
- Playwright: 115 (113 + 2), and four full runs, each straight after a restart of the frontend
  container - the condition the failure was seen under - all green. Backend unchanged

---

## Part 4: One name-lookup contract

One step, backend only. Nothing reachable over HTTP changes: this is the clean-up the filter in
Part 5 builds on.

- [x] Per decision 6: `findAllByNameContains` on `CategoryService` and `TagService`, over a
      `findAllByNameContainingIgnoreCase` in each repository; Ingredient's and Recipe's existing
      ones move to the ignore-case query
- [x] `findByName` leaves `CategoryService` and `IngredientService`, `findTagByName` leaves
      `TagService`; `TagJpaRepository.findTagByName` becomes `findByName`
- [x] `IngredientJpaRepository.findAllByDescriptionContains` and
      `RecipeService.findAllByCategoryName` with its repository query go
- [x] Docs: `backend/CLAUDE.md` Gotchas ("`CategoryJpaRepository.findAllByNameContains` is tested but
      unreachable"); `TagController`'s class comment

**Tests.** The service tests of removed methods go; new ones for Category's and Tag's partial
match. Each `*JpaRepositoryIT` proves the partial match ignores case on H2 - "BRØD" finds the three
seeded breads, "Ølandshvedebrød" among them - which the current case-sensitive query fails. The counts change by exactly
what is added and removed, listed in the Done notes.

**Verify it yourself.** `.\mvnw clean verify`. `git grep findTagByName` finds nothing outside
`docs/`.

**Done (2026-10-11).** Deviations and findings:

- Tests first: a case test in `CategoryJpaRepositoryIT` ("ER"), `IngredientJpaRepositoryIT` ("MEL")
  and `RecipeJpaRepositoryIT` ("BRØD"), run against the old `findAllByNameContains`: all three
  failed on H2. After the change they pass - `Ø` included - with the exact rows named, not a count
- Tag's partial match is new at both levels: `TagJpaRepositoryIT` ("GODT" finds both "Godt til"
  tags) and `TagServiceTest`. Category's service method is new too, in `CategoryServiceTest`
- `RecipeJpaRepositoryIT` used `findAllByCategoryName("Kager")` to count a saved recipe's category;
  it now asks `countByCategoryId`, which the delete checks already use
- Removed with their tests: service `findByName` on Category (2 tests) and Ingredient (3),
  `findTagByName` on Tag (2), `RecipeService.findAllByCategoryName` (2) and its repository query
  (1 IT), `findAllByDescriptionContains` (1 IT). Added: 2 + 2 service tests, 4 ITs
- `./mvnw clean verify`: 258 unit (263 - 9 + 4), 148 integration (146 - 2 + 4). Nothing reachable
  over HTTP changed, so the stack was not rebuilt and Playwright not run
- Found, left alone: two unused imports from before this version, `Collectors` in
  `RecipeServiceImpl` and `Optional` in `RecipeRatingJpaRepository`
- Docs: `backend/CLAUDE.md` Gotchas, `TagController`'s comment, and a status line on the
  name-lookup entry in `future_enhancements.md`, which goes with Part 6

---

## Part 5: `?name=` on the list endpoints

One step, backend only.

- [ ] `GET /api/v1/{categories,ingredients,tags,recipes}?name=...` answers the partial match, 200
      and a possibly empty list; without `name` it answers everything, as today. Units have no
      filter (the entry's exclusion stands)
- [ ] The controller only chooses between the two service calls, as the entry sketches. A blank
      `name` is not special: containing "" matches every row
- [ ] Docs: `backend/README.md` endpoint table; `TagController`'s and `UnitController`'s class
      comments; the name-lookup entry leaves `future_enhancements.md` if Part 6 is not taken, or
      with Part 6

**Tests.** Controller slice tests: with and without `name`, each calling the right service method.
One `*IT` per resource through the real context: a partial, case-differing name finds the seeded
row, and a name nothing contains answers `[]`.

**Verify it yourself.** Signed in, open `http://localhost:3000/api/v1/ingredients?name=MEL` in the
browser: the flours, and nothing else.

---

## Part 6: A search field on the list pages

One step, frontend and e2e.

- [ ] `EntityList` takes an optional search: a labelled field and a Search button above the table,
      submitting on Enter (decision 7). Its `load` receives the term
- [ ] Recipes, Categories, Ingredients and Tags use it; Units do not
- [ ] With a term and no match the page says "No <things> match "<term>"." rather than
      "No <things> yet."; clearing the field and searching again shows everything
- [ ] Docs: `frontend/CLAUDE.md`; the name-lookup entry leaves `future_enhancements.md`

**Tests.** Playwright, per list: a search narrows the list to the matching seeded rows, ignoring
case; a term nothing matches shows the empty message; clearing it brings back the whole list. One
test proves the page asked the API - the request carries `?name=` - rather than filtering what it
already had.

**Verify it yourself.** On Ingredients, search for `mel`: only the flours remain.

---

## Part 7: Two database accounts

One step, the stack and its configuration (decision 8).

- [ ] **Prove first** the problem it fixes: on the current stack, connected as `recipesuser`,
      `CREATE TABLE v4_probe (id INT)` succeeds; drop the probe
- [ ] An init script, `mysql/initdb/accounts.sh` - shell, not SQL, since it reads the passwords from
      the container's environment - mounted read-only into `/docker-entrypoint-initdb.d`. It
      creates both accounts with the grants of decision 8. `.gitattributes` already keeps `*.sh`
      at LF, which the container's shell needs
- [ ] `docker-compose.yml`: `MYSQL_USER` and `MYSQL_PASSWORD` go; the mysql service receives both
      accounts' names and passwords for the script; the backend receives `DB_ADMIN_USERNAME` and
      `DB_ADMIN_PASSWORD`, required with `:?` like the others
- [ ] `application.properties`: `spring.liquibase.user` and `.password` from them. The test
      properties replace the main file and use H2's own account, so the tests do not change
- [ ] `.env.example`: the two variables, and the paragraph that calls one account the MVP's shape
- [ ] `db/liquibase.properties`, the Maven plugin's config, names `recipesadmin`: it runs DDL too
- [ ] Docs: `README.md` - the switch needs `docker compose down -v`, and what that deletes; root
      `CLAUDE.md` and `backend/CLAUDE.md` - a host-side `spring-boot:run` exports the admin
      variables as well; `backend/README.md`; the entry leaves `future_enhancements.md`

**Tests.** The backend suite is unchanged on H2. The proof is on the stack, from a fresh volume:
the backend starts healthy, so Liquibase ran as `recipesadmin`; `SHOW GRANTS` for each account
shows what decision 8 says; as `recipesuser`, `CREATE TABLE v4_probe (id INT)` is refused; the whole
Playwright suite is green, which covers every write and registration as `recipesuser`.

**Verify it yourself.** Add the two variables to your `.env`, then `docker compose down -v` and
`scripts\start.ps1`. Then:

```powershell
docker compose exec mysql sh -c 'mysql -uroot -p"$MYSQL_ROOT_PASSWORD" -e "SHOW GRANTS FOR recipesuser; SHOW GRANTS FOR recipesadmin"'
```
