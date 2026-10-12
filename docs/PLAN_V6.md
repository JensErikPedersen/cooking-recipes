# Implementation plan, version 6

Read `CLAUDE.md` first for the requirements, technical decisions and work process. `docs/PLAN.md`
is the MVP, `docs/PLAN_V2.md` to `docs/PLAN_V5.md` the rounds of enhancements before this one, kept
as the record; this file plans the fifth round.

Nothing proceeds from one part to the next without review and acceptance. Check off each box as it
completes, and record what was actually done underneath the part when it differs from the plan.

**Status: draft**, split from the version 5 draft on 2026-10-12. The decisions below are
recommendations, not yet settled. It starts when version 5 is done.

## Shape of the work

Step 2 of one entry from `backend/docs/future_enhancements.md`, A forgotten password cannot be
reset: reset by email. It builds on version 5 Part 2, which ends an account's sessions.

| Part | Delivers | Layers | Entry |
|---|---|---|---|
| 1 | Baseline and scope | docs | - |
| 2 | An email address on each account | Liquibase, backend, frontend, e2e | Password reset, step 2 |
| 3 | Mail in the stack | Docker, backend config, docs | the same |
| 4 | Reset by email, in the API | backend | the same |
| 5 | Reset by email, on pages | frontend, e2e | the same |

**Order.** The address first: without it there is nothing to send to. The container next, proven
on its own before anything depends on it, then the reset, API before pages.

**Out of scope.** Ratings, as in every version so far. Also: verifying an email address by mail,
and rate limiting the reset request - each is a decision below. They stay in, or go to,
`future_enhancements.md`.

**Numbering.** Parts restart at 1. Commits say `V6 Part 2: ...`.

**Branch.** `enhancements_v6`, from `master` once version 5 is merged.

## Decisions

Open. Each is the draft's recommendation; the parts refer to them by number.

1. **An email address on every new account**, at most 254 characters, unique regardless of case.
   Required at registration; optional for existing accounts, which add it on the account page -
   until then they cannot reset. Changing it asks for the current password, as changing the
   password does: the address is the way into the account. Not verified by mail: a typo means no
   reset, which an admin cannot repair either - version 5 gives admins no password change for
   others - so the account page shows the address in use.
   `APP_ADMIN_EMAIL` is not added: the bootstrapped admin sets it on the account page like anyone
   else. (Part 2)
2. **Mail through Mailpit**, a container that catches every mail and shows it on
   `http://localhost:8025` - nothing leaves the machine. `spring-boot-starter-mail`, with
   `MAIL_HOST` and `MAIL_PORT` from `.env`; Mailpit needs no credentials. The image is pinned to an
   exact version. The link in the mail needs the browser-facing address, which the backend does not
   know: a new `APP_BASE_URL`, `http://localhost:3000` by default. (Part 3)
3. **The reset flow.**
   - `POST /api/v1/auth/password-reset` with `{"email"}` always answers 202, with the same body,
     whether or not the address belongs to an enabled account - as login does for usernames.
   - The token is 32 random bytes, sent base64url in the link
     `APP_BASE_URL/reset-password?token=...`. It is stored as a SHA-256 hash, not bcrypt: the
     future-enhancements entry said "hashed, as a password is", but a token must be found by its
     hash, which bcrypt's salt prevents, and 32 random bytes have no dictionary to guess from.
   - It expires after 30 minutes, works once, and a new request replaces any earlier token of the
     account. A new table `password_reset_token`: the account, the hash, the expiry.
   - `POST /api/v1/auth/password-reset/confirm` with `{"token", "newPassword", "confirmPassword"}`:
     registration's rules, 204, every session of the account ended, and the user is **not**
     signed in - they sign in with the new password, which proves it. An unknown, expired or used
     token is a 400 on `token`, the same message for all three.
   - No rate limiting: a local application. Recorded in `future_enhancements.md`.

   (Parts 4, 5)

---

## Part 1: Baseline and scope

- [ ] Branch `enhancements_v6` from `master` once version 5 is merged
- [ ] Root `CLAUDE.md` and `README.md` point at `docs/PLAN_V6.md` as the current plan, with the
      earlier plans as the record. Ratings stay out of scope in version 6 too
- [ ] Open questions answered and recorded under Decisions
- [ ] Baseline green before any change: `./mvnw clean verify` and the whole Playwright suite, at
      version 5's final numbers

**Verify it yourself.** Read the diff, then:

```powershell
scripts\start.ps1
cd backend; .\mvnw clean verify
cd ..\e2e; npx playwright test
```

---

## Part 2: An email address on each account

Liquibase, backend, frontend, e2e.

- [ ] `db.changelog_1.6.xml`: `app_user.email`, nullable, unique (decision 1)
- [ ] Registration takes `email`, required: `@Email`, at most 254 characters; a taken one is a 409
      on `email`, regardless of case
- [ ] `PUT /api/v1/auth/email` with `{"currentPassword", "email"}`; `/auth/me` returns the address
- [ ] The register page asks for it. The account page shows the address in use and changes it; the
      password form moves beside it, and `/account/password` becomes `/account`
- [ ] Docs: `backend/CLAUDE.md`, `backend/README.md`, `frontend/CLAUDE.md`

**Tests.** `RegistrationIT`: missing, malformed, over-long and taken addresses, each on `email`.
An `EmailChangeIT` in the shape of `PasswordChangeIT`. e2e: registering with an address, changing it
on the account page, a wrong current password refused on its field.

---

## Part 3: Mail in the stack

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

## Part 4: Reset by email, in the API

Backend only.

- [ ] `db.changelog_1.7.xml`: `password_reset_token` (decision 3)
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

## Part 5: Reset by email, on pages

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

To be written when Part 5 is done.
