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

**Branch.** PR #3 merges `enhancements_v4` into `master`. Version 5 is built on a new branch,
`enhancements_v5`, from the updated `master`. This draft stays uncommitted until then, so it does
not ride along in PR #3.

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

- [ ] Branch `enhancements_v5` from `master` once PR #3 is merged
- [ ] Root `CLAUDE.md` and `README.md` point at `docs/PLAN_V5.md` as the current plan, with the
      earlier plans as the record. Ratings stay out of scope in version 5 too
- [x] Open questions answered and recorded under Decisions (2026-10-12)
- [ ] Baseline green before any change: `./mvnw clean verify` and the whole Playwright suite.
      Version 4 ended at 262 unit, 156 integration, 128 Playwright tests

**Verify it yourself.** Read the diff, then:

```powershell
scripts\start.ps1
cd backend; .\mvnw clean verify
cd ..\e2e; npx playwright test
```

---

## Part 2: A password change ends the account's other sessions

Backend, with one e2e test. The machinery Part 3 and version 6 reuse.

- [ ] **Prove first** that today a second session of the same account stays signed in after a
      password change, and whether a session signed in through `AuthServiceImpl.login` appears in
      a `SessionRegistry` at all - it is not the form-login filter that registers sessions
- [ ] `SessionRegistry` and `HttpSessionEventPublisher` beans; sessions registered at login and
      at registration, and dropped at logout and on expiry (decision 1)
- [ ] An expired session answers 401 in the error envelope, not Spring's default text
- [ ] `changePassword` ends every other session of the account; the one that changed it stays,
      under its new id
- [ ] Docs: `backend/CLAUDE.md` Authentication; the step 1 remainder leaves `future_enhancements.md`

**Tests.** In `PasswordChangeIT`: two sessions of one account, a change in one, and the other's
next request is a 401 while the first's is not. A logout leaves nothing behind in the registry.
e2e: two browser contexts signed in as one throwaway account; after a change in the first, the
second lands on the sign-in page at its next navigation.

---

## Part 3: Roles enforced, and a users API for admins

Backend only.

- [ ] `GET /api/v1/users`: every account - id, username, enabled, roles, created - never a hash.
      Admin only, a 403 for anyone else (decision 3)
- [ ] `PUT /api/v1/users/{id}` with `{"enabled", "admin"}`: an admin enables or disables an
      account and grants or withdraws the admin role (decisions 2, 4). The account's sessions end
      when either changes (decisions 1, 5). The admin's own account is a 400 (decision 5)
- [ ] Written through `AppUserWriter`, with `updated` and `updated_by`, as every other write to
      `app_user`
- [ ] `ROLE_ADMIN` checked in `SecurityConfig` for `/api/v1/users/**`
- [ ] Docs: `backend/CLAUDE.md` Authentication, `backend/README.md` endpoint table; points 1, 2 and 4
      of Multiple users in `future_enhancements.md`

**Tests.** A `UsersIT` over the real chain: an admin lists accounts and no hash appears; a user
gets a 403 on both endpoints; disabling an account ends its session and refuses its next sign-in
with the same 401 as a wrong password; enabling it again lets it in; granting the admin role takes
effect at the account's next sign-in, not before; an admin changing their own account is a 400 and
nothing changes; an unknown id is a 404.

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
