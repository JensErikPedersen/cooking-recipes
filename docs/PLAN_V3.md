# Implementation plan, version 3

Read `CLAUDE.md` first for the requirements, technical decisions and work process. `docs/PLAN.md`
is the MVP and `docs/PLAN_V2.md` the first round of enhancements, both finished and kept as the
record; this file plans the second round.

Nothing proceeds from one part to the next without review and acceptance. Check off each box as it
completes, and record what was actually done underneath the part when it differs from the plan.

## Shape of the work

The six items under "Smaller items deferred during Parts 2-7" at the end of
`backend/docs/future_enhancements.md`. Each is small; together they are tidying, two corrections to
what the API answers, one removal and one convenience for the user. Each item leaves that list in
the commit that finishes it, and the heading goes with the last one.

| Part | Delivers | Layers | Item |
|---|---|---|---|
| 1 | Baseline and scope | docs | - |
| 2 | Tidy-up, no behaviour change | backend | Dead code; `RecipeIngredientJpaRepository`'s id type |
| 3 | Only a real conflict is a 409 | backend | The handler answers 409 for every constraint |
| 4 | A `Location` the browser can use | backend | `Location` names the internal address |
| 5 | The unused recipe ingredient endpoints go | backend | The sub-resource endpoints are unused |
| 6 | Sign-in returns to where you were heading | frontend, e2e | Sign-in lands on the start page |

**Order.** The part that cannot change behaviour first, then the backend corrections, the removal,
and the one change a user sees last. Parts 3-5 are independent of each other and could be taken in
any order.

**Numbering.** Parts restart at 1. Commits say `V3 Part 2: ...`.

## Decisions

Settled 2026-10-07: the recommendations of the draft, accepted as they stood. Each part refers to
them by number.

1. **The recipe ingredient sub-resource is removed.** Nothing has called it since Part 7 of the
   MVP - the form sends its lines in the recipe's PUT, so Cancel can undo them - yet every rule
   about lines lived twice, once per write path, and 4b of version 2 had to keep both in step. Its
   three endpoints, three service methods, their tests and error codes 60-62 go; the line handling
   the form uses stays. (Part 5)
2. **`Location` becomes relative**, `/api/v1/categories/{id}`, which HTTP allows and the browser
   resolves against its own origin - not `server.forward-headers-strategy=framework`. It needs no
   configuration, does not depend on what the proxy forwards, and does not make the backend trust
   `X-Forwarded-*` headers from whoever reaches its port, which `docker-compose.override.yml`
   publishes to the host. (Part 4)
3. **409 only for UNIQUE and FOREIGN_KEY**, which a client can act on, and for
   `DuplicateKeyException`, which `AppUserWriter`'s plain SQL raises without Hibernate in between.
   NOT_NULL, CHECK and anything unrecognised go to the general handler as a 500 with a log
   reference: they are server faults. (Part 3)
4. **"Return to" applies to both ways a signed-out browser reaches `/login`** - `proxy.ts` (no
   session cookie) and `lib/api.ts` (a session that ended while a page was open) - carrying the
   path in `?next=`. Logging out does not: you chose to leave. Only a path on this site is
   followed, so a crafted link cannot send a user elsewhere after signing in. (Part 6)
5. **Of the leftovers from version 2, only the stale `recipesadmin` comment** in
   `backend/src/main/resources/db/liquibase.properties` is taken, in Part 2. The others stay as
   they are: registration rules in two rounds, the sign-in page's plain labels, the frontend's own
   sorting of recipe lines, and the amount message wrapping on a phone. (Part 2)

---

## Part 1: Baseline and scope

- [x] Root `CLAUDE.md` and `README.md` point at `docs/PLAN_V3.md` as the current plan, with
      `docs/PLAN.md` and `docs/PLAN_V2.md` as the record
- [x] Open questions answered and recorded under Decisions
- [x] Baseline green before any change: `./mvnw clean verify` and the whole Playwright suite.
      Version 2 ended at 278 unit, 129 integration, 103 Playwright tests

**Verify it yourself.** Read the diff, then:

```powershell
scripts\start.ps1
cd backend; .\mvnw clean verify
cd ..\e2e; npx playwright test
```

**Done (2026-10-07).** Baseline: 278 unit, 129 integration, 103 Playwright tests, all green -
version 2's final numbers. Root `CLAUDE.md` also says ratings stay out of scope in version 3.

---

## Part 2: Tidy-up

One step, backend only, and nothing a test or a user can see may change.

- [x] Delete `src/test/java/dk/serik/recipes/config/TestingConfiguration.java`. Confirm first that
      nothing references it: a `@TestConfiguration` is not picked up by component scanning, and no
      test imports it
- [x] Delete `backend/HELP.md`, the Spring Initializr's generated help. Confirm no document links it
- [x] `RecipeIngredientJpaRepository` extends `JpaRepository<RecipeIngredient, RecipeIngredientPK>`.
      Nothing calls `findById` or `deleteById` on it today, so no caller changes; the point is that
      one could not be called with a `UUID` and silently find nothing
- [x] The stale `recipesadmin` comment in `db/liquibase.properties` (decision 5)
- [x] Docs: the two items leave `future_enhancements.md`; `backend/CLAUDE.md` if it mentions either

**Tests.** None new: the point is that the suite does not move. The repository's own `*IT` covers
its derived queries.

**Verify it yourself.** `.\mvnw clean verify` with the same counts as Part 1's baseline.
`git grep TestingConfiguration HELP.md` finds nothing outside `docs/`.

**Done (2026-10-08).** Deviations and findings:

- Both files were referenced only by their own `future_enhancements.md` entry. The test `config`
  package held nothing else, so it is gone too
- **`liquibase.properties` was stale twice, not once.** Besides naming `recipesadmin`, its `url`
  pointed at port 3306, where nothing listens since the local MySQL was removed in Part 0 of the
  MVP. Proven: `./mvnw liquibase:status` with the stack's `recipesuser` failed on 3306 and
  reported "is up to date" on 3307, the stack's `DB_HOST_PORT`. The url is now 3307, and the
  comment names `recipesuser` and says where the port comes from. Run as configured afterwards:
  "is up to date"
- `./mvnw clean verify`: 278 unit, 129 integration - the baseline, unchanged

---

## Part 3: Only a real conflict is a 409

One step, backend only.

- [x] **Prove first** what each constraint actually raises, on H2 and on MySQL - Hibernate decides
      the kind from the database's error code, so the two can differ. A unique name, a foreign key
      on delete, a NOT NULL column: for each, the `DataIntegrityViolationException`'s cause and its
      `getKind()`. On MySQL through the stack, as a throwaway; on H2 as the test below
- [x] `ApplicationExceptionHandler.handleDataIntegrityViolation`: 409 `DATA_CONFLICT` (40) for
      UNIQUE and FOREIGN_KEY, and for `DuplicateKeyException`; anything else passes to the general
      handler, which logs it with a reference and answers 500 (decision 3)
- [x] The 409 message stays generic and the raw text stays in the log, as today
- [x] Docs: `backend/CLAUDE.md` Errors table and the paragraph that warns "a NOT NULL column left
      empty also comes back as a 409"; the item leaves `future_enhancements.md`

**Tests.** `ApplicationExceptionHandlerTest`: one case per kind, built from real Hibernate
exceptions. One `*IT` per real path: a duplicate name still 409 - the services' own checks answer
first, so this reaches the handler only through a race, which the IT provokes by inserting with
`JdbcTemplate` - and a NOT NULL violation provoked the same way answers 500, not 409.

**Verify it yourself.** `.\mvnw clean verify`. In the stack, the app behaves as before: every
conflict a user can cause is caught by a service's own check first.

**Done (2026-10-08).** Deviations and findings:

- **Proven first**, with a throwaway probe: native SQL through Hibernate, every statement in a
  transaction set to roll back, run on H2 and then on the stack's MySQL with Liquibase off.

  | Violation | H2 | MySQL | Hibernate kind |
  |---|---|---|---|
  | duplicate category name | 23505 | 1062 | UNIQUE |
  | recipe with no such category | 23506 | 1452 | FOREIGN_KEY |
  | delete a category a recipe uses | 23503 | 1451 | FOREIGN_KEY |
  | category with `created_by` NULL | 23502 | 1048 | NOT_NULL |

  The same kinds on both, so one rule serves both. Spring translates every one to a plain
  `DataIntegrityViolationException` with Hibernate's exception as the cause - never to
  `DuplicateKeyException` on this path. MySQL's category and recipe counts were the same before
  and after the run. The schema has no CHECK constraint to probe
- **The plan's integration test could not have worked as written.** A violation provoked with
  `JdbcTemplate` never passes through Hibernate, so it carries no kind at all. `DataIntegrityIT`
  provokes each one the probe's way instead and hands what Spring would hand over to the real
  handler, plus a duplicate through `AppUserWriter`, which is the plain-JDBC path and does arrive as
  `DuplicateKeyException`. Before the change only the NOT NULL case failed - it answered 409
- Unit tests in `ApplicationExceptionHandlerTest`: each kind built from Hibernate's own exception,
  UNIQUE and FOREIGN_KEY 409, NOT_NULL, CHECK and OTHER 500, `DuplicateKeyException` 409, and a
  violation with no kind 500. Before the change the four 500 cases failed
- A 500 goes through the existing catch-all, so it gets the same generic message and logged
  reference as any unhandled exception
- `./mvnw clean verify`: 284 unit (+6), 134 integration (+5). Playwright: 103, against the rebuilt
  backend - the conflicts users can cause are still caught by the services' own checks

---

## Part 4: A `Location` the browser can use

One step, backend only. Nothing reads the header today - the frontend takes the id from the body -
so this is correctness of the API, not a visible change.

- [ ] Show the problem first, on the stack: a POST through the proxy answers
      `Location: http://backend:8080/api/v1/...`, a host only the containers can resolve
- [ ] Per decision 2: either a relative `Location` built from the path alone, in every
      controller that answers 201, or `server.forward-headers-strategy=framework` after checking
      that the Next.js proxy sends `X-Forwarded-Host` and `-Proto`
- [ ] Docs: `backend/CLAUDE.md` (Controllers: "a `Location` header built from
      `ServletUriComponentsBuilder`"); the item leaves `future_enhancements.md`

**Tests.** The controller slice tests assert each 201's `Location` with `endsWith(...)`, which
passes for `http://backend:8080/...` and for the fix alike - so it could never have caught this.
They change to the exact value. One `*IT` asserts it through the real context. `AuthenticationIT`
and `RegistrationIT` GET the `Location` they are given, and work with either form.

**Verify it yourself.** Signed in, in the browser's dev tools console:

```js
const t = decodeURIComponent(document.cookie.match(/XSRF-TOKEN=([^;]+)/)[1]);
const r = await fetch("/api/v1/tags", {method: "POST", headers: {"Content-Type": "application/json", "X-XSRF-TOKEN": t}, body: JSON.stringify({name: "Location probe"})});
r.headers.get("Location");   // no "backend:8080"; then delete the tag from the Tags page
```

---

## Part 5: The unused recipe ingredient endpoints go

One step, backend only (decision 1).

- [ ] Confirm nothing calls them: no frontend code, no e2e spec, no document other than the
      backend's own
- [ ] Remove `POST /recipes/{id}/ingredients`, `PUT` and `DELETE /recipes/{id}/ingredients/{ingredientId}`
      from `RecipeController`, and `addRecipeIngredient`, `updateRecipeIngredient`,
      `deleteRecipeIngredient` with their private helpers from the service
- [ ] Error codes 60-62 go if nothing else uses them; the codes are not reused
- [ ] Their tests go: `RecipeControllerTest`, `RecipeServiceTest`, the rows in
      `PathIdValidationTest`, any `RecipeIT` case
- [ ] Docs: `backend/README.md` ("recipes add a nested ingredients sub-resource"),
      `backend/CLAUDE.md` (the sentence that they "still work, but the UI does not use them"); the
      item leaves `future_enhancements.md`

**Tests.** The suite shrinks by the removed tests and nothing else fails. `RecipeIT`'s line tests,
which use the recipe's own PUT, still pass unchanged.

**Verify it yourself.** `.\mvnw clean verify`; the counts drop by exactly the tests removed, listed
in the Done notes. Playwright unchanged. A `POST /api/v1/recipes/{id}/ingredients` now answers 405
or 404 - say which.

---

## Part 6: Sign-in returns to where you were heading

One step, frontend and e2e.

- [ ] `proxy.ts` redirects to `/login?next=<the path and query asked for>`
- [ ] `lib/api.ts`, on a 401 while a page is open, does the same with the current page
- [ ] The login page signs in and goes to `next` if it is a path on this site - starts with `/`,
      not `//` - and to `/` otherwise. A link to `/login?next=https://elsewhere.example` must land
      on `/`
- [ ] The register page keeps going to the Welcome page: a new account has nowhere it was heading
- [ ] Note for the implementation: `useSearchParams` in a page Next prerenders needs a
      `<Suspense>` boundary; check the Next 16 guide in `node_modules/next/dist/docs/` first, as
      `frontend/AGENTS.md` asks
- [ ] Docs: `frontend/CLAUDE.md`; the item leaves `future_enhancements.md`, and with it the
      "Smaller items" heading

**Tests.** Playwright, starting signed out: opening a recipe's URL leads to sign-in and, after it,
to that recipe; `/login` alone still lands on `/`; `?next=https://elsewhere.example` and
`?next=//elsewhere.example` land on `/`. The expired-session path: clear the session cookie on an
open page, act, sign in, and arrive back on that page.

**Verify it yourself.** In a private window, open `http://localhost:3000/recipes` directly: you are
asked to sign in, and afterwards you are on Recipes, not the Welcome page.
