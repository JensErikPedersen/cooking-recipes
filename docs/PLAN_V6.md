# Implementation plan, version 6

Read `CLAUDE.md` first for the requirements, technical decisions and work process. `docs/PLAN.md`
is the MVP, `docs/PLAN_V2.md` to `docs/PLAN_V5.md` the rounds of enhancements before this one, kept
as the record; this file plans the fifth round.

Nothing proceeds from one part to the next without review and acceptance. Check off each box as it
completes, and record what was actually done underneath the part when it differs from the plan.

**Status: done.** Split from the version 5 draft on 2026-10-12; started and finished 2026-10-13,
once version 5 was merged. The decisions below were settled that day.

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

- [x] A `mailpit` service in `docker-compose.yml`, its image pinned to an exact version, its web
      page on `localhost:8025` only (decision 2)
- [x] `spring-boot-starter-mail`; `MAIL_HOST`, `MAIL_PORT` and `APP_BASE_URL` in `.env.example`,
      `docker-compose.yml` and `application.properties`, none a credential
- [x] The tests send no mail. The recording mail sender moved to Part 5, the first part to send any
- [x] Docs: root `CLAUDE.md` (four containers), `README.md` (where mail appears), `backend/CLAUDE.md`

**Tests.** The backend starts healthy with the mail settings; on the stack, a mail sent from inside
the backend container shows up in Mailpit's API. The whole Playwright suite is green.

**Done 2026-10-13.**
- **Mailpit.** `axllent/mailpit:v1.31.5`, the latest stable release (2026-10-11). Its page and API
  are published on `127.0.0.1:8025` only. SMTP stays inside the network, except for
  `127.0.0.1:1025` in `docker-compose.override.yml`, for a host-side `./mvnw` run. The image has a
  healthcheck of its own, `/mailpit readyz`, so the backend `depends_on` it being healthy, as it
  does MySQL.
- **Settings.** `MAIL_HOST`, `MAIL_PORT` and `APP_BASE_URL` have defaults in `docker-compose.yml`
  (`mailpit`, `1025`, `http://localhost:FRONTEND_PORT`), so an `.env` from before V6 still starts the
  stack; `.env.example` documents them. JavaMail's timeouts are 5 seconds each, not its default of
  forever. `app.base-url` is set here, and first read in Part 5.
- **The backend's health includes mail.** Boot's mail health indicator connects to the server.
  Proven: with Mailpit stopped, `/actuator/health` answered 503, while the sign-in page was still
  served; started again, 200.
- **Proven, a mail from inside the backend container.** curl over SMTP, to the `$MAIL_HOST:$MAIL_PORT`
  of the container's own environment, `mailpit:1025`. It appeared in Mailpit's API, from and to
  the addresses sent, and was then deleted.
- **Proven, the tests have no mail sender.** A throwaway `@SpringBootTest` found no `MailSender`
  bean: the tests' `application.properties` replaces the main one and sets no `spring.mail.host`,
  without which Boot creates none. So the recording sender waits for Part 5, where
  `PasswordResetIT` first needs one.
- **A flaky search test, fixed.** The first full e2e run failed `search.spec.ts`'s "clearing the
  search ... brings back the whole list": its snapshot of every category held `E2E 2daa4c3e`, made
  by a test running alongside (`fullyParallel`), which had deleted it again before the comparison.
  Not a Part 4 change. The test now compares the categories that are not other tests' "E2E " ones,
  all back and in order. Two full runs after it: green.
- **Tests.** Backend 275 unit, 194 integration, unchanged. Playwright 143, all green, and Mailpit
  held no mail afterwards: nothing in the suite sends one.

---

## Part 5: Reset by email, in the API

Backend only.

- [x] `db.changelog_1.6.xml`: `password_reset_token` (decision 3)
- [x] The tests' mail sender: one that records, for `PasswordResetIT` to read the mail from (moved
      from Part 4)
- [x] `POST /api/v1/auth/password-reset` and `/password-reset/confirm`, both open in
      `SecurityConfig`, both CSRF-protected like login
- [x] The mail: plain text, the link, and how long it is valid
- [x] A reset ends every session of the account (V5 Part 2) and spends the token
- [x] Docs: `backend/CLAUDE.md`, `backend/README.md`; the reset entry leaves
      `future_enhancements.md`, and the rate limiting left out is recorded there

**Tests.** A `PasswordResetIT` with the recording mail sender: an enabled account's address gets a
mail; an unknown address, an account without one and a disabled account get none, and all four
answer the same 202 and body; the token from the mail resets the password, after which the old one
fails and the new one signs in; the same token again, an expired one and a made-up one are the same
400 on `token`; a second request makes the first token useless; the stored value is not the token;
the password rules apply as at registration; every session of the account has ended.

**Done 2026-10-13.**
- **Schema.** `db.changelog_1.6.xml`: `password_reset_token`, keyed by `app_user_id` - one row per
  account, so a new request replaces the old token by construction - with a unique `token_hash` and
  `expires`, and the foreign key `ON DELETE CASCADE`. No audit columns: the row is no entity, and
  lives 30 minutes. On MySQL it took the schema's Danish collation, as `app_user.id` has.
- **Code.**
  - `PasswordResetTokens`, plain SQL, makes the token and hashes it, in that class only.
  - `PasswordResetMail` writes the mail.
  - The two endpoints are on `AuthController`, and `AuthServiceImpl` holds both flows, beside the
    password rules and the session ending they share. Each flow is one transaction, so a failed
    send keeps no token.
  - The confirm spends the token only after the rules pass, with a delete whose count decides.
  - The answer to a request is 202 with no body. A malformed address is a 400: it says nothing
    about whether an account has it.
- **The tests' mail sender.** `RecordingMailSender`, a `@Component` in the test sources. The
  component scan of every `@SpringBootTest` picks it up; there was no sender at all before, and
  `AuthServiceImpl` now needs one. It keeps every mail, and `failNextSend` stands in for a server
  that is down.
- **Not tested as planned: "an account without an address".** Nothing can ask for its reset: a
  request names an address, and that account has none. An unknown address covers the same case.
- **Proven, four mutations, each caught by its test:**
  - Without the `isEnabled` filter, the disabled account got a mail:
    `shouldAnswerAlikeWhetherOrNotAMailIsSent` failed.
  - Without the transaction, a failed send kept its token: `shouldFailWhenTheMailCannotBeSent`
    failed.
  - Spending the token before the rules failed
    `shouldApplyThePasswordRulesWithoutSpendingTheToken`, among others.
  - Without ending the sessions, `shouldEndEverySessionAndSignNoOneIn` failed: 200, not 401.
- **Proven on the stack**, through the API and Mailpit:
  - A reset asked for the address in upper case sent one mail, "Reset your password", from
    `no-reply@cooking-recipes.local`. Its link starts `http://localhost:3000/reset-password?token=`
    with a 43 character token, and its text says 30 minutes.
  - The unknown address got the same 202 and no mail.
  - The confirm answered 204. The old password then got a 401, the new one signed in, and the token
    a second time got the 400.
  - The stored expiry was exactly 1800 seconds ahead of `UTC_TIMESTAMP()`: JDBC writes and compares
    it in one zone.
  - Deleting the probe account removed its token by the cascade. The probe account and the mails
    were removed.
- **Docs.**
  - `future_enhancements.md`: the "cannot be reset" entry is replaced by what the reset leaves out.
    That is the rate limit, and that the answer takes longer for a real address, the mail being sent
    before it. The plan had not named the timing.
- **Tests.** Backend 275 unit, 211 integration (17 new, all in `PasswordResetIT`). Playwright 143,
  unchanged.
- *Fixed in Part 6:* concurrent requests deadlocked on MySQL, and two for one account could answer
  409. See Part 6. H2 shows neither, and the stack probe above sent one request at a time.

---

## Part 6: Reset by email, on pages

Frontend and e2e.

- [x] "Forgot your password?" on the sign-in page, to `/forgot-password`: an address, and the same
      message afterwards whatever it was
- [x] `/reset-password?token=...`: new password and confirmation, then to the sign-in page with
      "Your password has been reset."; a bad token says so and links back to `/forgot-password`
- [x] Both pages open to a visitor without a session: `proxy.ts`'s matcher, and the fields disabled
      until hydrated, as on the sign-in page
- [x] Docs: `frontend/CLAUDE.md`

**Tests.** e2e, reading the mail from Mailpit's API: a throwaway account asks for a reset, follows
the link, sets a new password and signs in with it; the link a second time is refused; an unknown
address shows the same message and no mail arrives. `phone.spec.ts` gains both pages.

**Done 2026-10-13.**
- **Pages.**
  - `/forgot-password` says "If an account has that address, a mail with a link to choose a new
    password is on its way." after any accepted address. A malformed one is the server's message on
    its field (`noValidate`).
  - `/reset-password` reads the token from `window.location` in its submit handler, as the login
    page reads `next`, so it stays prerendered.
  - Success goes to `/login?reset`, which says "Your password has been reset. Sign in with the new
    one."
  - A bad token is an alert, "This reset link is invalid or has expired.", with "Ask for a new
    link".
  - Both new pages and the sign-in page stay static (`○`) in `next build`.
- **Found by e2e, fixed in the backend: a deadlock.** Parallel tests asking for resets got 500s:
  `CannotAcquireLockException`, "Deadlock found", on the token's `INSERT`.
  - *Reproduced on the stack:* 16 requests at once for 16 accounts answered 500 for 8, 10 and 11 of
    them in three runs.
  - *Cause, from InnoDB's deadlock report:* each transaction held `X locks gap before rec` on the
    table's primary key and waited for `insert intention` into the same gap. Its `DELETE` of a token
    not there yet had locked the gap, at MySQL's default `REPEATABLE READ`. The reset methods'
    `@Transactional` used that default: `AuthServiceImpl` has none at class level.
  - *Fix:* `READ_COMMITTED`, as every service runs. After it, 16 of 16 three times, and 40 of 40.
- **Found by probing that fix: a race for one account.** Eight requests at once for one account:
  a 409 in two of three runs, since both found no token to delete and both inserted one. A 409
  tells the address has an account.
  - *Fix:* `SELECT ... FOR UPDATE` of the account's `app_user` row first, so its requests take
    turns. After it, five runs of eight all 202, and the other probe still all 202.
  - *Abandoned:* `INSERT ... ON DUPLICATE KEY UPDATE`. It is MySQL syntax, and the tests' H2 is not
    in MySQL mode: `@AutoConfigureTestDatabase` replaces the datasource. Twelve ITs failed on bad
    grammar.
  - H2 has neither gap locks nor the race. The probe script is not kept:
    `backend/CLAUDE.md` records the finding.
- **Found by e2e, fixed in the frontend: the sign-in page's notice.** After the reset's
  `router.push`, `/login?reset` showed no notice. A full load of the same URL did.
  - *Cause:* the page read `window.location` while rendering, and after a client-side navigation it
    renders before the address bar changes.
  - *Fix:* `ResetNotice`, with `useSearchParams` inside `<Suspense>`, as Next 16's reference
    recommends for a prerendered page.
- **A test's mistake.** `getByRole("alert")` also matched Next's route announcer, an empty
  `role="alert"`; the test now looks in `main`.
- **Tests.** Playwright 152 (9 new): 7 in `reset.spec.ts`, and `/forgot-password` and
  `/reset-password` in `phone.spec.ts`. The new `support/mailpit.ts` finds the mail, reads its link
  as a path on the site under test, and deletes what the test read. Two full runs green; afterwards
  Mailpit held no mail, and the database only `admin` and `Jens` and no token. Backend 275 unit,
  211 integration.

## Version 6 as a whole

**Delivered.**
- **An email address on every account.** Asked for at registration, unique regardless of case
  (Part 2). Shown and changed on `/account`, with the current password, beside the password form
  that moved there from `/account/password` (Part 3).
- **Mailpit, a fourth container**, catching every mail on `http://localhost:8025` (Part 4).
- **A forgotten password reset by email.** The API answers alike for every address, and mails a
  single-use link that works for 30 minutes; only the token's SHA-256 is stored (Part 5). The pages
  lead there from the sign-in page and back (Part 6).

**Fixed on the way.**
- A MySQL deadlock under concurrent reset requests, and a race for one account that could answer
  409. Both are invisible on H2, and were found by e2e and the probes (Part 6).
- The sign-in page's reset notice, missing after a client-side navigation (Part 6).
- A flaky search test that compared other tests' categories (Part 4).

**Decided, and left out.** No rate limit on reset requests, and an answer that takes longer for a
real address, since the mail is sent first. Both are in `backend/docs/future_enhancements.md`. An
admin still cannot set another account's password.

**Tests.** Backend 275 unit (as in version 5: everything new is integration), 211 integration (from
171), Playwright 152 (from 136).
