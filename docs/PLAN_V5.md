# Implementation plan, version 5

Read `CLAUDE.md` first for the requirements, technical decisions and work process. `docs/PLAN.md`
is the MVP, `docs/PLAN_V2.md`, `docs/PLAN_V3.md` and `docs/PLAN_V4.md` the first three rounds of
enhancements, all finished and kept as the record; this file plans the fourth round.
`docs/PLAN_V6.md` follows it.

Nothing proceeds from one part to the next without review and acceptance. Check off each box as it
completes, and record what was actually done underneath the part when it differs from the plan.

## Shape of the work

One entry from `backend/docs/future_enhancements.md`, Multiple users, and the remainder of step 1
of another, A forgotten password cannot be reset. They share one piece of machinery: ending a
user's sessions, which a password change, a disabled account and a changed role all need - and,
in version 6, a reset. That comes first.

| Part | Delivers | Layers | Entry |
|---|---|---|---|
| 1 | Baseline and scope | docs | - |
| 2 | A password change ends the account's other sessions | backend, e2e | Password reset, step 1's remainder |
| 3 | Roles enforced, and a users API for admins | backend | Multiple users, points 1, 2 and 4 |
| 4 | A users page for admins | frontend, e2e | the same |
| 5 | Recipes have an owner, who alone may change them | Liquibase, backend | Multiple users, point 3 |
| 6 | The recipe pages follow the owner | frontend, e2e | the same |

**Order.** The sessions first, since Part 3 builds on them. Then roles before ownership, because
"the owner or an admin" needs admins to exist.

**Split, 2026-10-12.** The draft planned both entries as one version of ten parts. Reset by email -
an email address on each account, mail in the stack, the reset in the API and on pages - is
version 6, `docs/PLAN_V6.md`, built on this one.

**Out of scope.** Ratings, as in every version so far. Also: private recipes, deleting accounts, and
moving `created_by` to a foreign key - each is a decision below - and reset by email, which is
version 6. They stay in, or go to, `future_enhancements.md`.

**Numbering.** Parts restart at 1. Commits say `V5 Part 2: ...`.

**Branch.** PR #3 merged `enhancements_v4` into `master` on 2026-10-12. Version 5 is built on a new
branch, `enhancements_v5`, from the updated `master`.

## Decisions

Settled 2026-10-12: the recommendations of the draft, accepted as they stood, including the answers
to its two questions in decisions 3 and 7. Each part refers to them by number.

1. **Ending sessions uses Spring Security's `SessionRegistry`**, in memory: one backend instance,
   whose sessions already live in its memory and die with it. The alternative - a version number
   on `app_user` checked on every request - costs a query per request to cover a scale the stack
   does not have. Login is done by hand in `AuthServiceImpl`, not by a filter, so it registers the
   session itself; Part 2 proves first what Spring does and does not do here. An ended session
   answers its next request with a 401, which the frontend already turns into the sign-in page.
   (Parts 2, 3; and version 6)
2. **Who is an admin.** The first account, from `AdminBootstrap`, as today. Registration still
   makes `ROLE_USER`. An admin makes another account an admin, or not, on the users page. Roles stay
   the comma-separated column: a join table is still premature with two roles. (Part 3)
3. **What an admin may that a user may not:** manage accounts, and change or delete anyone's recipe.
   Categories, ingredients, units and tags stay open to every signed-in user, as today - deleting
   one in use is already refused with a 409, and an ordinary user should be able to fix a typo in
   an ingredient. Deleting those four does not become admin-only either. (Parts 3, 5)
4. **Accounts are disabled, never deleted.** A deleted account would orphan the recipes it owns and
   the audit trail naming it. Disabling uses the existing `enabled` column, which login already
   honours, and ends the account's sessions. A disabled account signs in with the same 401 as a
   wrong password, so it reveals nothing. (Parts 3, 4)
5. **An admin cannot change their own account on the users page** - neither disable it nor drop
   their admin role. That alone guarantees there is always an enabled admin: whoever acts is one.
   A changed role ends that user's sessions, so the new role holds from their next sign-in rather
   than their old session carrying the old one. (Part 3)
6. **Ownership is a new `recipe.owner_id`, a foreign key to `app_user`, on recipes only.** The entry
   in `future_enhancements.md` suggested turning `created_by` into the foreign key instead. That
   touches all nine tables and every DTO, and `created_by` on the seeds names `Jens` and `Majken`,
   who have no accounts. `created_by` stays what it is: an audit text. The column is nullable: the
   seeded recipes have no owner, and only an admin may change them. Categories and the other lookups
   belong to no one. (Part 5)
7. **Everyone reads every recipe**; only its owner or an admin changes or deletes it, otherwise a
   403. No private recipes: that changes every list and read, and nobody has asked for it. The
   recipe's response carries its owner's username, and the frontend hides Edit and Delete by
   comparing it with `/auth/me`. The backend's 403 stays the rule; the hiding is a convenience.
   A "My recipes" filter on the list, named as the entry's value, is left out for now: it is a small
   `?owner=` later, in the shape of `?name=`, and goes to `future_enhancements.md`. (Parts 5, 6)

---

## Part 1: Baseline and scope

- [x] Branch `enhancements_v5` from `master` once PR #3 is merged
- [x] Root `CLAUDE.md` and `README.md` point at `docs/PLAN_V5.md` as the current plan, with the
      earlier plans as the record. Ratings stay out of scope in version 5 too
- [x] Open questions answered and recorded under Decisions (2026-10-12)
- [x] Baseline green before any change: `./mvnw clean verify` and the whole Playwright suite.
      Version 4 ended at 262 unit, 156 integration, 128 Playwright tests

**Verify it yourself.** Read the diff, then:

```powershell
scripts\start.ps1
cd backend; .\mvnw clean verify
cd ..\e2e; npx playwright test
```

**Done (2026-10-12).** `enhancements_v5` starts at `5915fee`, the merge of PR #3; the two plans were
committed on it first, as `225fe01`. Baseline: 262 unit, 156 integration, 128 Playwright tests, all
green - version 4's final numbers, Playwright against the stack as built from that code. Root
`CLAUDE.md` says ratings stay out of scope in versions 2 to 5, and names `docs/PLAN_V6.md` as the
draft after this one; its limitations stay as they are until the parts deliver.

---

## Part 2: A password change ends the account's other sessions

Backend, with one e2e test. The machinery Part 3 and version 6 reuse.

- [x] **Prove first** that today a second session of the same account stays signed in after a
      password change, and whether a session signed in through `AuthServiceImpl.login` appears in
      a `SessionRegistry` at all - it is not the form-login filter that registers sessions
- [x] `SessionRegistry` and `HttpSessionEventPublisher` beans; sessions registered at login and
      at registration, and dropped at logout and on expiry (decision 1)
- [x] An expired session answers 401 in the error envelope, not Spring's default text
- [x] `changePassword` ends every other session of the account; the one that changed it stays,
      under its new id
- [x] Docs: `backend/CLAUDE.md` Authentication; the step 1 remainder leaves `future_enhancements.md`

**Tests.** In `PasswordChangeIT`: two sessions of one account, a change in one, and the other's
next request is a 401 while the first's is not. A logout leaves nothing behind in the registry.
e2e: two browser contexts signed in as one throwaway account; after a change in the first, the
second lands on the sign-in page at its next navigation.

**Verify it yourself.** `.\mvnw clean verify`. On the stack: sign in as one account in a normal
and in a private window, change the password in one, then open any page in the other - it goes to
the sign-in page.

**Done (2026-10-12).** Deviations and findings:

- **Proven first, on the stack**, with two curl sessions of a throwaway account through the
  frontend's `/api`: after a change in the first, the second still answered `/auth/me` with 200.
- **Spring registers no session of ours, proven twice.** Spring Security 7.1.1's own source:
  `maximumSessions` puts the registering strategy into a `SessionManagementFilter` that acts only on
  a request that authenticated during itself without a saved context - a login Spring performs. A
  throwaway test with the registry wired in: after our login and a request, `getAllPrincipals()`
  was empty. So `AuthServiceImpl.login` registers the session, by username, after the context is
  saved; registration goes through `login` and is covered with it.
- **Spring's default for an ended session is a 200** with a line of text
  (`ResponseBodySessionInformationExpiredStrategy`). An `expiredSessionStrategy` answers the
  envelope's 401 instead. `ConcurrentSessionFilter` borrows the logout configuration's handlers, so
  an ended session also loses its `JSESSIONID` cookie, as at logout - asserted in the IT.
- **The IT, before and after.** Without the change to `AuthServiceImpl`, of the three new tests
  only `shouldEndTheAccountsOtherSessions` failed, 200 where 401 was expected; with it, all pass.
- **"A logout leaves nothing behind" is not an IT**, as planned: MockMvc runs no servlet container,
  so the registry never hears of an id change or an end there. Proven on the stack instead, with the
  registry's debug log switched on through a throwaway Compose override: each session registered,
  moved to its new id at a password change, and removed when it was ended or logged out - the
  registry empty at the end. The same run proved what the IT cannot: after the first session's id
  changed, a change from the other session still ended it. The e2e test repeats that run.
- **The e2e test failed once, the test's fault.** Its second step navigated before the second
  sign-in had finished; the trace shows the login `POST` aborted by the navigation. It now waits
  for the start page, as its first step did, and passed three runs in a row.
- `./mvnw clean verify`: 262 unit, 159 integration (3 new). Playwright: 129 (1 new).
- Docs: `backend/CLAUDE.md` Authentication, a bullet of its own on ending sessions;
  `backend/README.md`'s password row; the step 1 remainder has left `future_enhancements.md`

---

## Part 3: Roles enforced, and a users API for admins

Backend only.

- [x] `GET /api/v1/users`: every account - id, username, enabled, roles, created - never a hash.
      Admin only, a 403 for anyone else (decision 3)
- [x] `PUT /api/v1/users/{id}` with `{"enabled", "admin"}`: an admin enables or disables an
      account and grants or withdraws the admin role (decisions 2, 4). The account's sessions end
      when either changes (decisions 1, 5). The admin's own account is a 400 (decision 5)
- [x] Written through `AppUserWriter`, with `updated` and `updated_by`, as every other write to
      `app_user`
- [x] `ROLE_ADMIN` checked in `SecurityConfig` for `/api/v1/users/**`
- [x] Docs: `backend/CLAUDE.md` Authentication, `backend/README.md` endpoint table; points 1, 2 and 4
      of Multiple users in `future_enhancements.md`

**Tests.** A `UsersIT` over the real chain: an admin lists accounts and no hash appears; a user
gets a 403 on both endpoints; disabling an account ends its session and refuses its next sign-in
with the same 401 as a wrong password; enabling it again lets it in; granting the admin role takes
effect at the account's next sign-in, not before; an admin changing their own account is a 400 and
nothing changes; an unknown id is a 404.

**Verify it yourself.** `.\mvnw clean verify`. On the stack, signed in as the admin, in the dev
tools console: `fetch("/api/v1/users").then(r => r.json()).then(console.table)` lists the accounts.
Signed in as an ordinary account, the same call answers 403.

**Done (2026-10-12).** Deviations and findings:

- **The PUT answers 204, not the account.** The change is `AppUserWriter`'s plain SQL, and the
  account read back in the same transaction would be the one JPA loaded before it. The password
  change answers 204 for the same kind of reason; the users page reloads its list.
- **A request that changes nothing writes nothing** and ends no session: no `updated` stamp for a
  change that did not happen.
- Error codes 604 `USER_ID_IS_NULL`, 605 `USER_NOT_FOUND`, 606 `OWN_ACCOUNT`, in the accounts' 600s.
  The two roles became constants on `AppUser`, also used by `AdminBootstrap` and registration
- **Tests beyond the plan's `UsersIT`**: `UserServiceTest` (8) and `UserControllerTest` (5), as
  every entity controller and service has. `UsersIT` has 11
- **The rule is load-bearing, proven**: with the `hasRole("ADMIN")` line taken out, exactly the two
  tests that expect a user's 403 failed, a user answered 200
- **On the stack**, through the frontend's `/api` with the real admin and a throwaway account: a
  user's `GET /users` 403; the admin's list carries no password; the admin's own account 400 (606);
  disabling the user answered 204, its session's next request 401, and its sign-in the wrong
  password's 401 (601); enabled again as an admin, it signed in with `ROLE_ADMIN` and read the list.
  The probe account removed afterwards
- `./mvnw clean verify`: 275 unit (13 new), 170 integration (11 new). Playwright: 129, unchanged -
  nothing in the frontend uses the endpoints yet
- Docs: `backend/CLAUDE.md` (Roles and Accounts bullets, layout, error codes, tests),
  `backend/README.md` (a users table), `future_enhancements.md` (points 1, 2 and 4 of Multiple
  users), root `CLAUDE.md` (Limitations: roles are enforced, for account management)

---

## Part 4: A users page for admins

Frontend and e2e.

- [ ] `/users`: every account, its roles, whether it is enabled, when it was created; per row,
      Disable or Enable, and Make admin or Remove admin. The admin's own row has no buttons
      (decision 5)
- [ ] "Users" in the menu for an admin only, from `/auth/me`'s roles. A user who opens `/users`
      sees the not-found page, not an error
- [ ] Docs: `frontend/CLAUDE.md`

**Tests.** e2e, as admin: the page lists a throwaway account; disabling it means it cannot sign in,
enabling it means it can; making it an admin shows it "Users" in the menu after it signs in again.
As a user: no "Users" in the menu, and `/users` is not found. The admin's own row offers nothing.
`phone.spec.ts` gains `/users`.

---

## Part 5: Recipes have an owner, who alone may change them

Liquibase and backend.

- [ ] `db.changelog_1.5.xml`: `recipe.owner_id`, nullable, a foreign key to `app_user.id`
      (decision 6). Existing rows stay without an owner
- [ ] A new recipe is owned by the account that creates it, from the signed-in user, never from the
      body
- [ ] `PUT` and `DELETE` on a recipe: its owner or an admin, otherwise a 403 in the envelope with an
      error code of its own (decision 7). A recipe without an owner: an admin only
- [ ] `RecipeDTO` carries `owner`, the username or null; a write carrying it is ignored, as the
      audit fields are
- [ ] e2e's `accounts.ts` removes an account's recipes before the account, now that a foreign key
      points at it
- [ ] Docs: `backend/CLAUDE.md`, `backend/README.md`; point 3 of Multiple users in
      `future_enhancements.md` records why `created_by` stays text

**Tests.** `RecipeIT`: a recipe created by a user is owned by them; another user's `PUT` and `DELETE`
are 403s and the recipe is unchanged; an admin's succeed; a seeded recipe is a 403 for a user and
changeable by an admin; `owner` in a request body changes nothing. Service tests for the rule.
The whole suite as before: the existing `<Entity>IT` tests run as `@WithMockUser`, which may need an
account of that name now that an owner is looked up.

---

## Part 6: The recipe pages follow the owner

Frontend and e2e.

- [ ] The recipe's read view shows its owner, and offers Edit and Delete only to its owner or an
      admin (decision 7)
- [ ] The recipe list shows each recipe's owner
- [ ] A 403 from the backend - another tab, a role just withdrawn - shows the envelope's message,
      not a crash
- [ ] Docs: `frontend/CLAUDE.md`; the "My recipes" filter left out goes to `future_enhancements.md`

**Tests.** e2e with two throwaway accounts: the second sees the first's recipe without Edit and
Delete, and its own with them; an admin sees both on every recipe; a seeded recipe offers a user
nothing.

---

## Version 5 as a whole

To be written when Part 6 is done.
