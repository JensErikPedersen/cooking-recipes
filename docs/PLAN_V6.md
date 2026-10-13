# Implementation plan, version 6

Read `CLAUDE.md` first for the requirements, technical decisions and work process. `docs/PLAN.md`
is the MVP, `docs/PLAN_V2.md` to `docs/PLAN_V5.md` the rounds of enhancements before this one, kept
as the record; this file plans the fifth round.

Nothing proceeds from one part to the next without review and acceptance. Check off each box as it
completes, and record what was actually done underneath the part when it differs from the plan.

**Status: in progress.** Split from the version 5 draft on 2026-10-12; started 2026-10-13, once
version 5 was merged. The decisions below were settled that day.

## Shape of the work

Step 2 of one entry from `backend/docs/future_enhancements.md`, A forgotten password cannot be
reset: reset by email. It builds on version 5 Part 2, which ends an account's sessions.

| Part | Delivers | Layers | Entry |
|---|---|---|---|
| 1 | Baseline and scope | docs | - |
| 2 | An email address at registration | Liquibase, backend, frontend, e2e | Password reset, step 2 |
| 3 | Changing the address, on the account page | backend, frontend, e2e | the same |
| 4 | Mail in the stack | Docker, backend config, docs | the same |
| 5 | Reset by email, in the API | backend | the same |
| 6 | Reset by email, on pages | frontend, e2e | the same |

**Order.** The address first: without it there is nothing to send to. The container next, proven
on its own before anything depends on it, then the reset, API before pages. The address was one
part in the draft, split in Part 1 because it was the largest: by feature, not by layer, since an
address the API requires before the register page sends one fails every e2e test that registers.

**Out of scope.** Ratings, as in every version so far. Also: verifying an email address by mail,
and rate limiting the reset request - each is a decision below. They stay in, or go to,
`future_enhancements.md`.

**Numbering.** Parts restart at 1. Commits say `V6 Part 2: ...`.

**Branch.** `enhancements_v6`, from `master` once version 5 is merged.

## Decisions

Settled 2026-10-13, as the draft recommended, with the additions marked. The parts refer to them
by number.

1. **An email address on every new account**, at most 254 characters, unique regardless of case.
   Required at registration; optional for existing accounts, which add it on the account page -
   until then they cannot reset. Changing it asks for the current password, as changing the
   password does: the address is the way into the account. Not verified by mail: a typo means no
   reset, which an admin cannot repair either - version 5 gives admins no password change for
   others - so the account page shows the address in use.
   `APP_ADMIN_EMAIL` is not added: the bootstrapped admin sets it on the account page like anyone
   else. The column takes the table's Danish collation, as usernames do: case is ignored, as it
   should be, but `aase@x.dk` also equals `åse@x.dk` and `jose@x.dk` equals `josé@x.dk` - checked
   on the stack. Only addresses with non-ASCII letters can clash, and a clash is a 409 "taken".
   *Added in Part 1.* (Parts 2, 3)
2. **Mail through Mailpit**, a container that catches every mail and shows it on
   `http://localhost:8025` - nothing leaves the machine. `spring-boot-starter-mail`, with
   `MAIL_HOST` and `MAIL_PORT` from `.env`; Mailpit needs no credentials. The image is pinned to an
   exact version. The link in the mail needs the browser-facing address, which the backend does not
   know: a new `APP_BASE_URL`, `http://localhost:3000` by default. (Part 4)
3. **The reset flow.**
   - `POST /api/v1/auth/password-reset` with `{"email"}` always answers 202, with the same body,
     whether or not the address belongs to an enabled account - as login does for usernames.
   - The token is 32 random bytes, sent base64url in the link
     `APP_BASE_URL/reset-password?token=...`. It is stored as a SHA-256 hash, not bcrypt: the
     future-enhancements entry said "hashed, as a password is", but a token must be found by its
     hash, which bcrypt's salt prevents, and 32 random bytes have no dictionary to guess from.
   - It expires after 30 minutes, works once, and a new request replaces any earlier token of the
     account. A new table `password_reset_token`: the account, the hash, the expiry. Its foreign
     key to `app_user` is `ON DELETE CASCADE`: e2e removes its accounts with SQL, and a token
     would otherwise block that. *Added in Part 1.*
   - A mail that cannot be sent - Mailpit down - is a 500, not swallowed. That answer tells an
     address that exists from one that does not, but only while mail is broken; a 202 would tell
     the user to wait for a mail that never comes. *Added in Part 1.*
   - `POST /api/v1/auth/password-reset/confirm` with `{"token", "newPassword", "confirmPassword"}`:
     registration's rules, 204, every session of the account ended, and the user is **not**
     signed in - they sign in with the new password, which proves it. An unknown, expired or used
     token is a 400 on `token`, the same message for all three.
   - No rate limiting: a local application. Recorded in `future_enhancements.md`.

   (Parts 5, 6)

---

## Part 1: Baseline and scope

- [x] Branch `enhancements_v6` from `master` once version 5 is merged
- [x] Root `CLAUDE.md` and `README.md` point at `docs/PLAN_V6.md` as the current plan, with the
      earlier plans as the record. Ratings stay out of scope in version 6 too
- [x] Open questions answered and recorded under Decisions
- [x] Baseline green before any change: `./mvnw clean verify` and the whole Playwright suite, at
      version 5's final numbers

**Verify it yourself.** Read the diff, then:

```powershell
scripts\start.ps1
cd backend; .\mvnw clean verify
cd ..\e2e; npx playwright test
```

**Done 2026-10-13.** `enhancements_v6` was branched from `master` at the merge of version 5
(`67ebf14`). Baseline on a stack rebuilt from it: 275 unit, 171 integration, 136 Playwright tests,
all green - version 5's final numbers. The decisions were settled as drafted, with three additions
from reviewing the plan: the email column's collation (decision 1), and the token's cascade and a
failed send as a 500 (decision 3). Part 2 was split in two, so the version has six parts and the
later ones moved up by one. Asked as API then pages, it became registration then changing the
address: split by layer, the API would require an address the register page does not yet send,
and every e2e test that registers an account would fail between the two parts.

---

## Part 2: An email address at registration

Liquibase, backend, frontend, e2e.

- [x] `db.changelog_1.5.xml`: `app_user.email`, nullable, unique (decision 1)
- [x] Registration takes `email`, required: `@Email`, at most 254 characters; a taken one is a 409
      on `email`, regardless of case
- [x] The register page asks for it
- [x] Docs: `backend/CLAUDE.md`, `backend/README.md`, `frontend/CLAUDE.md`

**Tests.** `RegistrationIT`: missing, malformed, over-long and taken addresses, each on `email`.
e2e: registering with an address, and a taken one refused on its field; every test that registers
an account gives it an address.

**Done 2026-10-13.**
- **Schema.** `db.changelog_1.5.xml`, not 1.6 as drafted: 1.4 was the last. `email VARCHAR(254)`,
  nullable, unique as `uk_app_user_email`, after `username`. On the stack's existing volume it
  migrated on start: the column took the table's Danish collation, and `admin` and `Jens` have no
  address.
- **Backend.** `RegisterRequestDTO.email` is `@NotBlank`, not `@NotNull`: `@Email` passes an empty
  string. A taken address is a 409, `EMAIL_TAKEN(607)`, on `email`, checked after the username by
  `existsByEmailIgnoreCase`. `AppUserWriter.insert` takes the address, `null` for the bootstrapped
  admin.
- **Register page.** An "Email address" field, `type="email"` for the phone keyboard and autofill.
  The form has `noValidate`, so the server's message shows, not the browser's. Proven: without it,
  the malformed-address e2e test failed, the field had no message.
- **Proven, the service check.** With the check removed, `RegistrationIT` failed twice. A duplicate
  fell to the database backstop, code 40, not 607. The same address in upper case was a 201, since
  H2 tells case apart.
- **Proven on the stack, decision 1's collation.** Through the API: `aase` is taken by `åse`,
  `jose` by `josé`, and `ÅSE` by `åse`, each as the service's own 607. `ASE` and `åse` stay
  distinct. The probe accounts were removed.
- **Tests.** Backend 275 unit, 180 integration (9 new): 8 in `RegistrationIT`, and one in
  `DataIntegrityIT` for the unique column. `PasswordChangeIT` and `UsersIT` register with an address.
  Playwright 138 (2 new): a taken address in another case, and a malformed then a missing one.
  `password.spec.ts` and `users.spec.ts` fill the address in, with `emailOf()` from
  `support/accounts.ts`.

---

## Part 3: Changing the address, on the account page

Backend, frontend, e2e.

- [x] `PUT /api/v1/auth/email` with `{"currentPassword", "email"}`; `/auth/me` returns the address
- [x] The account page shows the address in use and changes it; the password form moves beside
      it, and `/account/password` becomes `/account`
- [x] Docs: `backend/CLAUDE.md`, `backend/README.md`, `frontend/CLAUDE.md`

**Tests.** An `EmailChangeIT` in the shape of `PasswordChangeIT`. e2e: an account without an
address adds one, an address changed, a wrong current password refused on its field.

**Done 2026-10-13.**
- **API.** `PUT /api/v1/auth/email` answers 204. A wrong current password is a 400 on its field,
  checked as for a password change by a shared `checkCurrentPassword`. An address another account
  has, in any case, is the 409 (607). The account's own address in another case is not taken:
  `existsByEmailIgnoreCaseAndUsernameNot`. The taken check comes only after the password, so a
  session cannot probe addresses with it. No session ends or changes id.
- **The address in every user answer.** Login, registration and `/auth/me` all answer with
  `AuthenticatedUserDTO`, so all three carry `email`, read from the table, `null` while there is
  none. Only `/auth/me` was planned; the other two would otherwise have said `null` for an account
  that has one.
- **One set of address messages.** The keys moved from `register.email.*` to `email.*`, shared by
  registration and the change.
- **Account page.** `/account` replaces `/account/password`, which is gone: no redirect. It has two
  sections, "Email address" and "Password", each with its own form. Both forms have a "Current
  password" field, so the e2e tests reach each through its section. The page asks `/auth/me` for the
  address itself, as `/users` asks for its list, because `AppShell`'s user is fetched once per visit.
- **A stale comment fixed.** `AuthService.changePassword`'s Javadoc still said other sessions stay
  signed in, though V5 Part 2 ended them.
- **Proven.** Checking the address against every account, the account's own included, failed
  `shouldAcceptOwnAddressInAnotherCase` (409, not 204). Checking whether it is taken before the
  password failed `shouldNotTellATakenAddressWithoutThePassword` (409, not 400).
- **Tests.** Backend 275 unit, 194 integration (14 new): `EmailChangeIT` has 13 methods, one of
  them parameterised twice. `AuthenticationIT` checks that the bootstrapped admin has no address,
  and `RegistrationIT` that a new account's address is in its answers. Playwright 143 (5 new, in
  `email.spec.ts`). `password.spec.ts` works in the page's Password section. `registerThroughPage`
  and `removeEmail` in `support/accounts.ts` replace the copies of the register steps.

---

## Part 4: Mail in the stack

Docker, backend config, docs. Like V4 Part 7, a change to the stack.

- [ ] A `mailpit` service in `docker-compose.yml`, its image pinned to an exact version, its web
      page on `localhost:8025` only (decision 2)
- [ ] `spring-boot-starter-mail`; `MAIL_HOST`, `MAIL_PORT` and `APP_BASE_URL` in `.env.example`,
      `docker-compose.yml` and `application.properties`, none a credential
- [ ] The tests send no mail: H2 tests get a mail sender that records instead
- [ ] Docs: root `CLAUDE.md` (four containers), `README.md` (where mail appears), `backend/CLAUDE.md`

**Tests.** The backend starts healthy with the mail settings; on the stack, a mail sent from inside
the backend container shows up in Mailpit's API. The whole Playwright suite is green.

---

## Part 5: Reset by email, in the API

Backend only.

- [ ] `db.changelog_1.6.xml`: `password_reset_token` (decision 3)
- [ ] `POST /api/v1/auth/password-reset` and `/password-reset/confirm`, both open in
      `SecurityConfig`, both CSRF-protected like login
- [ ] The mail: plain text, the link, and how long it is valid
- [ ] A reset ends every session of the account (V5 Part 2) and spends the token
- [ ] Docs: `backend/CLAUDE.md`, `backend/README.md`; the reset entry leaves
      `future_enhancements.md`, and the rate limiting left out is recorded there

**Tests.** A `PasswordResetIT` with the recording mail sender: an enabled account's address gets a
mail; an unknown address, an account without one and a disabled account get none, and all four
answer the same 202 and body; the token from the mail resets the password, after which the old one
fails and the new one signs in; the same token again, an expired one and a made-up one are the same
400 on `token`; a second request makes the first token useless; the stored value is not the token;
the password rules apply as at registration; every session of the account has ended.

---

## Part 6: Reset by email, on pages

Frontend and e2e.

- [ ] "Forgot your password?" on the sign-in page, to `/forgot-password`: an address, and the same
      message afterwards whatever it was
- [ ] `/reset-password?token=...`: new password and confirmation, then to the sign-in page with
      "Your password has been reset."; a bad token says so and links back to `/forgot-password`
- [ ] Both pages open to a visitor without a session: `proxy.ts`'s matcher, and the fields disabled
      until hydrated, as on the sign-in page
- [ ] Docs: `frontend/CLAUDE.md`

**Tests.** e2e, reading the mail from Mailpit's API: a throwaway account asks for a reset, follows
the link, sets a new password and signs in with it; the link a second time is refused; an unknown
address shows the same message and no mail arrives. `phone.spec.ts` gains both pages.

## Version 6 as a whole

To be written when Part 6 is done.
