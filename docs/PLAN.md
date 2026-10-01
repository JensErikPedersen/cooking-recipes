# Implementation plan

Read `CLAUDE.md` first for the requirements, technical decisions and work process.

Nothing proceeds from one part to the next without review and acceptance. Check off each box as it
completes, and record what was actually done underneath the part when it differs from the plan.

## Shape of the work

Parts 0-4 build the stack and the way in. From Part 5 onwards the work is sliced **vertically**:
one entity is taken all the way from the database through the API to the UI and its Playwright
coverage before the next one starts. The first slice is the expensive one because it establishes the patterns;
the ones after it are replication.

Auth lands in Part 4, **before** the first slice. This reverses an earlier decision, and the
reason is a hard technical constraint rather than a preference: `created_by` is NOT NULL on all
nine tables and on `BaseEntity` itself, and the only thing that populates it is
`BaseEntityListener` reading the request-scoped `Session` bean - which nothing in the codebase
ever writes to. Until sign in fills that bean in, **every INSERT through the API fails**. A CRUD
slice built before auth could not create a single row, so it could not pass its own success
criteria.

| Part | Delivers | Entity |
|---|---|---|
| 0 | Version check, report only | - |
| 1 | This plan, `backend/CLAUDE.md` | - |
| 2 | Docker stack, Next.js scaffold, scripts | - |
| 3 | Backend in the stack, schema, seed data | - |
| 4 | Sign in and sign out | - |
| 5 | First vertical slice | Category |
| 6 | Replication slices | Unit, Tag, Ingredient |
| 7 | Composite slice | Recipe |
| 8 | Welcome page and full e2e sweep | - |

---

## Part 0: Verify software and versions

Mostly check and report. The one thing actually decided here is the MySQL image tag, because
Part 3 cannot write a compose file without it. Do not upgrade anything that already works, and do
not touch the Java application's own versions at all - Spring Boot 4.1.1 and Java 25 were upgraded
recently and are out of scope, as are the Lombok and Jacoco pins, which carry comments in
`pom.xml` explaining why they sit where they do.

- [x] Report installed versions: Docker and Docker Compose, Node and npm, Java, Maven wrapper, MySQL client if present
- [x] Report the latest stable versions to target for the new components only: Next.js, React,
      Tailwind CSS, Playwright

**MySQL, which is the one real version decision in this part.**

- [x] Report the local server's version and, for the application accounts, which authentication
      plugin they use: `SELECT user, host, plugin FROM mysql.user WHERE user LIKE 'recipes%';`
- [x] Choose the `mysql` image tag. **Prefer the LTS line over an innovation release** - innovation
      releases are superseded every few months and are not what a database should sit on. Verify
      what the current LTS actually is against the official tags rather than assuming; at the time
      this plan was written that was 8.4, but confirm it
- [x] Record the authentication-plugin consequence, because it decides whether the existing
      scripts still work. `mysql_native_password` is disabled by default from 8.4 and removed in
      9.x, and `backend/scripts/mysql_users.sql` and `mysql_reset_users.sql` both say
      `IDENTIFIED WITH mysql_native_password`. On a current image those scripts fail outright.
      The fix is plain `IDENTIFIED BY`, which yields `caching_sha2_password` - the default, and
      fully supported by mysql-connector-j. Fix the scripts here or record that Part 3's init
      script supersedes them
- [x] Note what the upgrade does **not** involve: no data migration and no in-place server
      upgrade. The container starts from an empty volume and Liquibase builds the schema, so the
      version choice is just a tag. The laptop's own MySQL is a separate instance and Part 3 does
      not touch it
- [x] Confirm `./mvnw clean verify` passes, as the pre-change baseline. Note it needs no database:
      the suite runs on in-memory H2 in MySQL mode. That makes the baseline cheap, but it also
      means a green build says nothing about whether MySQL is reachable - Part 3 is the first
      thing that proves that

**Success criteria.** A single report, ending in a chosen `mysql` tag with a reason. The only
file that may change in `backend/` is the pair of account scripts, and only if the plugin change
above applies.

**Verify it yourself.** Nothing is built in this part, so the check is that nothing changed:
`git status` shows no modified files under `backend/`. Spot-check two numbers in the report against
your own machine, for example `docker --version` and `node --version`, so the report is confirmed
rather than trusted.

### Result (2026-09-28)

| Installed | Version |
|---|---|
| Docker / Compose | 29.8.1 / v5.5.1, upgraded from 20.10.17 / v2.10.2 (2022) before Part 2 |
| Node / npm | v22.23.2 / 10.8.3 - Next.js 16 needs Node 20.9 or newer |
| Java | OpenJDK 25 (25+36) |
| Maven wrapper | 3.9.11 |
| MySQL client | not installed - SQL checks go through `docker compose exec mysql` |
| Local MySQL server | removed, so the plugin query did not apply |

Targets for the new components: `next` / `create-next-app` 16.3.6, `react` 19.3.0, `tailwindcss`
4.3.3, `@playwright/test` 1.63.0. The latest `typescript` is 7.0.2, the native rewrite; Part 2 takes
the version `create-next-app` installs rather than forcing 7.

**MySQL tag: `mysql:9.7`.** The plan's assumption was stale: 9.7 became the LTS on 2026-04-21,
superseding 8.4, and the official image's `lts` tag points at 9.7.2 while `latest` points at the
26.7 innovation release. 9.7 has premier support to 2031-04 against 8.4's 2029-04, and Spring Boot
4.1.1 manages mysql-connector-j 9.7.0, the matching line. Pinned to the minor version so patch
releases arrive but the next LTS does not arrive silently, as it would with `mysql:lts`.

**Account scripts.** `mysql_native_password` is removed in 9.x, so both account scripts fail on
9.7. Not fixed: nothing will run them, since the local server is gone and the container creates the
account from `.env`. They are deleted in Part 3.

**Baseline.** `./mvnw clean verify` green: 230 unit tests, 63 integration tests, 0 failures.

---

## Part 1: Plan

- [x] Agree the technical decisions and record them in `CLAUDE.md`
- [x] Rename `AGENTS.md` to `CLAUDE.md`
- [x] Write this plan
- [x] Record the future multi-user schema extension in `backend/docs/future_enhancements.md`
- [x] Write `backend/CLAUDE.md` describing the existing backend: package layout, the DTO and mapper
      convention, the `ServiceException` plus `@ControllerAdvice` error envelope, the `Session`
      bean and `BaseEntityListener` audit path, the Liquibase layout, and how to build and run
- [x] User reviews and approves before any implementation starts

Done beyond the plan: `backend/.gitignore` deleted in favour of a single root `.gitignore`. The
backend copy had regained the `scripts/` rule that `f2bb1ed` removed, and two of its rules only
worked because they were anchored to `backend/`. History rewritten to remove
`backend/docs/mvn_cmd.md`, which held a database password, from the root commit; the GitHub repo
was deleted and is recreated from the rewritten history.

**Success criteria.** The user has accepted the plan. `backend/CLAUDE.md` describes code that
actually exists, with no aspirational content.

**Verify it yourself.** Read `CLAUDE.md` and this plan and confirm they say what you meant. Then
pick two claims from `backend/CLAUDE.md` at random and check them against the code - for example,
that the package layout listed really matches `backend/src/main/java/dk/serik/recipes/`, and that
the error envelope it describes matches `ApplicationExceptionHandler`. If a claim is aspirational
rather than true, say so; that is the failure mode worth catching here.

---

## Part 2: Scaffolding

Infrastructure and a hello-world frontend. No backend and no database in this part - the point is
to prove the container topology and the scripts before anything real depends on them.

Split into three steps, each committed and accepted on its own, so that a failure points at one
layer: the app, the container, or the test harness.

- [x] `.env.example` committed, documenting every variable; `.env` gitignored. Pulled forward
      during Part 1 while the credential decisions were fresh. It is the plan of record, not yet
      proven: each part that consumes a variable confirms it is actually read, and anything
      unreferenced by the end of Part 4 should be deleted from it

### 2a: Next.js app on the host

- [x] `frontend/` scaffolded with `create-next-app`: TypeScript, App Router, Tailwind, ESLint
- [x] A placeholder page at `/` and a placeholder route handler the page calls from the browser,
      so the hello-world proves both a render and a fetch. The route lives outside `/api`, which
      Part 3 hands to the backend in its entirety
- [x] Extend the root `.gitignore` - the only one in the repository, already covering `target/`,
      `.env` and the IDE files - with the frontend entries `node_modules/` and `.next/`. Fold in
      whatever else the `.gitignore` written by `create-next-app` needs, then delete that file

**Verify it yourself.** From `frontend/`: `npm run dev`, then `http://localhost:3000` shows the
placeholder page and the value it fetched. `npm run lint` and `npm run build` pass.

**Done.** `create-next-app@16.3.6` with its defaults: no `src/` directory, `@/*` import alias,
Turbopack, no React Compiler. Deviations and findings:

- React raised from the scaffold's 19.2.8 to the latest 19.3.0; Next.js accepts any 19.x
- ESLint stays on 9 despite npm flagging it unsupported: the plugins inside `eslint-config-next`
  (`eslint-plugin-react`, `jsx-a11y`, `import`) do not accept ESLint 10 even at their latest
- TypeScript is the scaffold's 5.9.3, as decided in Part 0
- The placeholder is `app/hello/route.ts` returning a fixed JSON message, fetched by `app/page.tsx`
  in the browser. Both are replaced when the real pages arrive
- Removed: the scaffold's `README.md`, its `.gitignore` (merged into the root file, except `.env*`,
  which would have ignored `.env.example`), and the five demo SVGs in `public/`, which left it empty
- Kept: the generated `AGENTS.md` and a `CLAUDE.md` that imports it. They point agents at the
  Next.js docs bundled in `node_modules/next/dist/docs/`, and `next dev` re-creates the block
  anyway. 2c adds the frontend conventions to that `CLAUDE.md`
- Proven, not assumed: served HTML shows `Loading...`, and the DOM after JavaScript runs (headless
  Edge) shows the fetched message. `/hello` builds as dynamic, so the value is fetched at request
  time rather than frozen at build time

### 2b: The frontend in Docker

- [x] `frontend/Dockerfile`, multi-stage, producing a Next.js standalone build
- [x] `docker-compose.yml` with the `frontend` service only for now, plus a named network
- [x] `scripts/start.sh`, `scripts/stop.sh`, `scripts/start.ps1`, `scripts/stop.ps1` - thin
      wrappers over `docker compose up -d --build` and `docker compose down`
- [x] Minimal root `README.md`: prerequisites, copy `.env.example`, run the start script, the URL

**Success criteria.** From a clean checkout, the two documented commands - copy `.env.example`,
run the start script - bring up a page at `http://localhost:3000`. The stop script leaves nothing
running. The start script works on Windows PowerShell and on a POSIX shell.

**Verify it yourself.**

```powershell
copy .env.example .env      # then fill in the values
.\scripts\start.ps1
```

- `http://localhost:3000` renders the placeholder page, and the value it fetched is visible on it
- `docker compose ps` lists the frontend service as running
- `.\scripts\stop.ps1`, then `docker ps` lists nothing from this project

The point of this step is the topology, not the page. If the page renders but the stop script
leaves a container behind, the step is not done.

**Done.** Deviations and findings:

- Image based on `node:24-slim`, following the official Next.js `with-docker` example reduced to
  npm. Node 24 is the current LTS; the host stays on Node 22 for `npm run dev`, which Next.js 16
  accepts equally. The server runs as the unprivileged `node` user; the image is 287 MB
- `next.config.ts` sets `output: "standalone"`. `frontend/.dockerignore` keeps the host's
  `node_modules` out of the build context - it holds Windows binaries
- `docker-compose.yml` requires `FRONTEND_PORT` with `:?`, so starting without `.env` fails at
  once with "copy .env.example to .env" instead of a confusing port error
- The scripts use `--project-directory` rather than `-f`, so they work from any directory and
  Compose still picks up `docker-compose.override.yml` automatically once Part 3 adds it
- `.gitattributes` keeps `*.sh` at LF, and both `.sh` scripts are committed executable
- Verified: all four scripts, each run from a different directory; page, `/hello` and CSS served
  from the container; each stop leaves no container or network behind
- Found: the start script returns when the container starts, about a second before the server
  answers. Harmless by hand, but a Playwright run straight after a start can race it. Part 3's
  healthchecks plus `up --wait` close it

### 2c: Playwright

- [x] Playwright installed and configured at `e2e/` in the repo root - not inside `frontend/`,
      since it tests the whole stack rather than one service. `baseURL` from
      `PLAYWRIGHT_BASE_URL`, defaulting to the Docker stack's `http://localhost:3000`
- [x] `frontend/CLAUDE.md` recording the frontend conventions established in 2a and 2b

**Tests.** A Playwright smoke spec that loads `/` and asserts the placeholder content and the
fetched value both render.

**Verify it yourself.** With the stack up, `npx playwright test` from `e2e/` is green. Then stop
the stack and run it again: it must fail, which proves it tests the stack rather than passing
regardless.

**Done.** Deviations and findings:

- `e2e/` is its own npm package: `@playwright/test` 1.63.0 and `@types/node` 22, matching the host
  Node. `npm test` runs `playwright test`
- Chromium only for now. Firefox and WebKit are one line each in `playwright.config.ts` if wanted
- The config never starts the stack itself - it tests whatever is running, which is the point.
  Traces are kept for failed tests; the HTML report is written but not opened automatically.
  `playwright-report/` and `test-results/` are gitignored
- The smoke spec asserts the heading by role and the fetched message by its text. The server HTML
  says `Loading...` (proven in 2a), so the second assertion can only pass after the browser fetch
- Verified: green against the running stack; with the stack stopped it fails on
  `net::ERR_CONNECTION_REFUSED`
- `frontend/CLAUDE.md` keeps its generated `@AGENTS.md` import. `next dev` only refreshes the
  block inside `AGENTS.md` and leaves `CLAUDE.md` alone - checked in
  `next/dist/server/lib/generate-agent-files.js`, so the conventions added below the import survive

---

## Part 3: Backend and database in the stack

Split into three steps, each committed and accepted on its own: the containers and schema, then
the proxy, then the seed data.

### 3a: MySQL and backend in the stack

- [x] `spring-boot-starter-actuator` added to `pom.xml`, exposing `/actuator/health` only. It
      earns its place here: a backend container that reports healthy only once Liquibase has
      finished is worth more than the dependency costs, and Part 4 needs an open health endpoint
      to exclude from authentication
- [x] `backend/Dockerfile`, multi-stage, Maven build then a JRE runtime image
- [x] `mysql` and `backend` services added to `docker-compose.yml`
- [x] MySQL healthcheck, and `backend` set to `depends_on: condition: service_healthy` - Liquibase
      runs during Spring startup and does not retry a refused connection, so "container started"
      is not sufficient
- [x] Backend healthcheck on `/actuator/health`, with `frontend` depending on it the same way.
      "The JVM started" is not the same as "Liquibase finished and the API answers"
- [x] Start scripts use `up --wait`, so they return once every service is healthy rather than
      merely started - closing the race found in 2b
- [x] A single application database account created by the MySQL entrypoint from `.env`. Liquibase
      runs as that account. A separate DDL account is deliberately not introduced for the MVP; it
      is recorded as a future enhancement instead
- [x] `docker-compose.override.yml` for local development: publishes the backend and MySQL ports
      to the host, and switches the backend to the `dev` profile
- [x] MySQL host port defaults to something other than 3306, since a local MySQL is usually
      already bound there and the clash fails the whole stack
- [x] Delete `backend/scripts/mysql_users.sql` and `mysql_reset_users.sql`. They use
      `mysql_native_password`, which 9.7 no longer has, and the MySQL entrypoint now creates the
      account (Part 0)
- [x] `backend/README.md` brought in line with the stack: drop the `recipesadmin` first-startup
      instruction and the manual account scripts, both superseded by the single account the MySQL
      entrypoint creates, and remove the link to the non-existent `docs/upgrade_to_java25.md`.
      Kept minimal - the root `README.md` covers running the stack

**Tests.** `./mvnw clean verify` still passes. The existing Playwright smoke test stays green,
now behind a frontend that waits for the backend.

**Verify it yourself.**

```powershell
docker compose down -v      # deliberately destroy the data volume
.\scripts\start.ps1         # returns only once all three services are healthy
docker compose ps           # three services, mysql and backend marked healthy
Invoke-RestMethod http://localhost:8080/api/v1/categories      # empty: schema, no seed yet
docker compose exec mysql sh -c 'mysql -uroot -p"$MYSQL_ROOT_PASSWORD" recipes -e "show tables"'
```

The last command lists the nine application tables plus Liquibase's two, proving Liquibase ran
against real MySQL - the first time anything has.

**Done.** Deviations and findings:

- `backend/Dockerfile` builds with `maven:3.9.11-eclipse-temurin-25`, matching the wrapper, rather
  than `./mvnw` - the wrapper jar is gitignored, so a clean checkout lacks it. Runtime is
  `eclipse-temurin:25-jre` plus `curl` for the healthcheck, as the unprivileged `recipes` user. The
  fat jar runs as is; the layered extraction `my-recipes` used is an optimisation left out
- MySQL healthcheck pings over TCP (`-h 127.0.0.1`), so the temporary server the entrypoint runs
  during first-run initialisation, with networking off, cannot report healthy early. It uses
  `CMD-SHELL` so the password is actually expanded: `my-recipes` used exec form, where
  `$MYSQL_ROOT_PASSWORD` stays literal and the check only passed because `mysqladmin ping` returns
  success even on access denied
- `start_interval: 2s` polls fast while a service starts, then settles to every 5s
- Server default collation kept: `utf8mb4` / `utf8mb4_0900_ai_ci`. The old account script created
  the schema as `utf8mb4_unicode_ci`; nothing in the changelog depends on either
- Verified from an empty volume: all three services up in 1m45 including image builds, mysql and
  backend healthy, backend connected first time (0 restarts, no connection errors), Liquibase ran
  all 23 changesets, `GET :8080/api/v1/categories` returns `[]`, the account uses
  `caching_sha2_password`. Stop and start keeps the volume and runs 0 changesets; a warm start takes
  16s. The Playwright smoke test passes straight after a start, so the 2b race is closed
- Verified the host-side run documented in `backend/README.md`: `./mvnw spring-boot:run` with
  `DB_PORT=3307` against the stack's MySQL, with the stack's backend stopped
- Found, pre-existing and not fixed here: **every unknown URL returns 500**, not 404. Spring raises
  `NoResourceFoundException` for a path nothing handles, and `ApplicationExceptionHandler`'s
  catch-all turns it into a 500 and an ERROR log line with a stack trace. Seen on
  `/actuator/info`, `/api/v1/nope` and `/nothing-here`

### 3a.1: Correct status for rejected requests

Added after 3a found every unknown URL returning 500.

- [x] Root cause: `ApplicationExceptionHandler`'s catch-all on `Exception` also caught Spring
      MVC's own rejections, which already carry their status. `FrameworkErrorStatusTest`, written
      first, proved four cases all returned 500: unknown path (404), wrong method (405), wrong
      media type (415) and malformed JSON (400)
- [x] Fix at that level, not per status: the catch-all first checks for `ErrorResponse`, the
      interface those exceptions share, and returns its status with Spring's client-safe detail
      under the new code `REQUEST_REJECTED` (10). Malformed JSON is not an `ErrorResponse`, so
      `HttpMessageNotReadableException` gets its own 400 handler with a fixed message
- [x] Side effect, recorded in `future_enhancements.md`: `HandlerMethodValidationException` is an
      `ErrorResponse` too, so that known case would now be a 400 instead of a 500

**Verified.** `./mvnw clean verify` green, 235 unit and 63 integration tests. Against the stack:
`/api/v1/nope` 404, `DELETE /api/v1/categories` 405, malformed JSON 400, all in the envelope, and no
ERROR lines in the backend log.

### 3b: The `/api` proxy

- [x] `frontend` proxies `/api/*` to the backend via `next.config.ts` rewrites, so the browser only
      ever talks to the Next.js origin

**Tests.** A Playwright spec asserts `GET /api/v1/categories` through the frontend origin returns
200 with a JSON array. 3c tightens it to seeded rows.

**Verify it yourself.** `http://localhost:3000/api/v1/categories` in the browser returns the same
JSON as `http://localhost:8080/api/v1/categories`. The first proves the proxy, the second the
backend on its own.

**Done.** Deviations and findings:

- `next.config.ts` rewrites `/api/:path*` to `${BACKEND_URL}/api/:path*`. The destination is fixed
  at build time, as `my-recipes` recorded: proven here by the built image's
  `.next/routes-manifest.json` holding `http://backend:8080`, while `BACKEND_URL` is not set in the
  running container at all. So Compose passes it as a build argument, and `npm run dev` on the host
  falls back to `http://localhost:8080` - verified against the stack's published backend
- `BACKEND_URL` is not in `.env`: `docker-compose.yml` derives it as `http://backend:${APPLICATION_PORT}`,
  so it cannot drift from the port the backend listens on
- A second spec asserts an unknown `/api` path gets the backend's JSON 404 (`errorCode` 10), not a
  Next.js page - proving all of `/api` goes to the backend
- Verified: 3 Playwright tests green. With only the backend stopped, both proxy specs fail - the
  frontend answers 500 after about 8s - so they genuinely depend on the backend

### 3c: Seed data

- [x] Seed data, in two halves. The lookup half already exists and is proven: the test suite loads
      `src/test/resources/db/changelog/db.dml-base-data.xml` - 28 consistent inserts covering
      category, ingredient, rating, tag and unit - on every run. Promote it to a main-source
      changeset rather than rewriting it, and keep exactly one copy so the tests and the running
      application cannot drift apart
- [x] The recipe half is the part that needs work. `backend/scripts/db.data-snapshot-2023-08-07.xml`
      holds the same 28 lookup rows plus 33 more - 7 recipes, 12 `recipe_ingredient`, 10
      `recipe_rating`, 4 `recipe_tag` - and is not wired into Liquibase. Those 33 are almost
      certainly where the known inconsistency lives, since they are the half nothing has ever
      loaded. Validate just them against the DDL - foreign key targets, missing parents, column
      drift - and **report what is actually wrong before changing anything**. This is a stop
      point: the repair approach is agreed before any data changes
- [x] Cross-check against the second known-good source before repairing by hand:
      `src/test/resources/db/test-data/insert_recipes.sql` and its siblings
      (`insert_recipe_ingredients.sql`, `insert_recipe_tags.sql`, `insert_recipe_ratings.sql`) are
      loaded per-test with `@Sql` and do work. Where the snapshot and the SQL fixtures disagree,
      the fixtures are the ones with a passing test behind them
- [x] Drop the `recipe_rating` rows from the seed. Ratings are out of scope for this MVP and have
      no controller, so seeding 10 rows nothing can read or write is dead data
- [x] Result: one `db.changelog_1.1.xml` included from the master changelog, carrying the lookup
      rows plus repaired recipes, `recipe_ingredient` and `recipe_tag`. If the recipe half turns
      out to be unsalvageable, ship the lookup half alone and hand-write two or three recipes -
      and say so rather than quietly shipping empty tables

**Tests.** `./mvnw clean verify` passes. Watch for one specific breakage: `db.changelog-master-test.xml`
includes the production master changelog **and then** `db.dml-base-data.xml`. Once the lookup rows
move into a main-source changeset, that test changelog loads them twice and every test run dies on
a duplicate primary key. Fix it by removing the second include, not by duplicating the data under a
different id.

Note also that tests run on in-memory H2 in MySQL mode, so a green suite does not prove the seed
loads against real MySQL. Only the stack check below proves that.

The 3b Playwright spec is tightened to assert the proxied call returns seeded rows.

**Success criteria - this is the Docker definition of done for Part 3.** From a clean checkout
with an empty Docker volume, one start script produces a working stack: schema created by
Liquibase, seed data present, API reachable through the proxy. Running the start script a second
time preserves the data. `docker compose down -v` followed by a start rebuilds from empty without
manual steps.

**Verify it yourself.**

```powershell
docker compose down -v      # deliberately destroy the data volume
.\scripts\start.ps1
```

- `http://localhost:3000/api/v1/categories` in the browser returns a JSON array of seeded
  categories
- the data survives a restart:

```powershell
.\scripts\stop.ps1
.\scripts\start.ps1         # seeded rows are still there, not recreated
```

To look at the database directly at any point:

```powershell
docker compose exec mysql sh -c 'mysql -uroot -p"$MYSQL_ROOT_PASSWORD" recipes -e "select count(*) from category; select count(*) from recipe; select count(*) from recipe_ingredient;"'
```

Seed data is the one thing worth checking in SQL here, because a half-loaded seed is invisible from
the UI until Part 7. Expect four categories, and a non-zero recipe count with matching
`recipe_ingredient` rows. Zero recipes with four categories means the lookup half loaded and the
recipe half silently did not.

**Done.** Deviations and findings:

- **The report, before any change.** The snapshot has one defect: Liquibase exported it in
  alphabetical table order, so `recipe_ingredient` precedes `unit` and `recipe_tag` precedes `tag`.
  Proven on MySQL 9.7 in a scratch copy of the schema: loaded as-is, all 12 `recipe_ingredient` rows
  fail `FK_recipe_ingredient_unit` and all 4 `recipe_tag` rows fail `FK_recipe_tag_tag`; reordered,
  all 51 rows load. No missing parents, no column drift. Its lookup half is identical to
  `db.dml-base-data.xml`, and its recipe half matches the `@Sql` fixtures on every business column -
  they differ only in audit metadata. Content is thin but valid: 2 of 7 recipes have ingredients and
  the instructions are one-line stubs. Kept as-is, by decision, metadata included
- **Found: the plan's approach would have broken ~25 tests.** They load the same 7 recipes with
  `@Sql`, and the test suite runs the production changelog, so seeding recipes there collides on the
  primary key. Agreed fix (option A): the recipe changeSets carry `contextFilter="!test"`, and the
  test properties set `spring.liquibase.contexts=test`. Proven load-bearing: with the context
  switched off, all 13 tests in `RecipeJpaRepositoryIT` error on a duplicate primary key
- `db.dml-base-data.xml` moved with `git mv` to `db.changelog_1.1.xml`, changeSet ids unchanged,
  followed by the snapshot's changeSets -4, -5 and -7, byte-identical apart from the filter. -6,
  `recipe_rating`, is left out
- `db.changelog-master-test.xml` deleted rather than trimmed: without its second include it only
  wrapped the production master, so the test properties now point at that directly
- `my-recipes` has no seed data; the "Reference implementation" section below is corrected
- Verified: `./mvnw clean verify` green, 235 unit and 63 integration tests; each test context runs 28
  changeSets and filters out 3. From an empty volume the stack runs all 31 and holds 4 categories,
  7 recipes, 12 `recipe_ingredient` and 4 `recipe_tag` rows; a recipe reads back through the proxy
  with its category, tag and every ingredient with amount and unit. A restart runs 0 changeSets and
  keeps the rows. The proxy spec now asserts the four seeded categories; 3 Playwright tests green
- Heads-up for Part 7: `my-recipes` had to make `recipe_tag.created_by` nullable because Hibernate's
  `@ManyToMany` insert has no value for it, so every tag write was a 500. `RecipeIT` should confirm

---

## Part 4: Sign in and sign out

Ported from `my-recipes`, which already solved this. Read its `SecurityConfig`, `AuthController`,
`SessionPopulatingFilter` and `AuthenticationIT` before writing anything - each carries comments
recording a specific failure, and re-deriving them costs a day each.

Split into two steps, each committed and accepted on its own: the backend, proven by
`AuthenticationIT` and the API, then the frontend.

No user administration: one account, created on first run, as `CLAUDE.md` Limitations states. The
backend needs a lookup by username and the bootstrap insert, and no CRUD endpoints for users.

### 4a: Backend authentication

- [x] `spring-boot-starter-security` added to `pom.xml`
- [x] `app_user` table as `db.changelog_1.2.xml`, after Part 3's seed changeset: id, username
      unique, password hash, enabled, roles as a single comma-separated column, plus the standard
      audit columns
- [x] `AppUser` entity, repository, and a `UserDetailsService` backed by it
- [x] `AdminBootstrap` creating the first account from environment-supplied credentials, and
      **only while the table is empty**. No password hash is committed anywhere in this repository
- [x] `AuthController` with JSON login, logout and current-user endpoints
- [x] `SecurityConfig`: session cookie auth, `/api/v1/auth/login` and `/actuator/health` open
      (the latter added in Part 3, and compose depends on it answering unauthenticated),
      everything else authenticated, 401 and 403 returned as this application's own error envelope
      rather than a redirect to a login page
- [x] CSRF stays ON, with `CookieCsrfTokenRepository.withHttpOnlyFalse()` and a `CsrfCookieFilter`
      so the token cookie actually reaches the browser. Boot 4 / Security 7 extends CSRF to API
      endpoints, and this app authenticates with a cookie the browser attaches automatically -
      which is exactly the condition CSRF protection exists for. Do not disable it
- [x] `SessionPopulatingFilter`, registered after `AuthorizationFilter`, copying the authenticated
      principal into the request-scoped `Session` bean. **This is the piece that makes any write
      work at all.** It falls back to a non-null constant for unauthenticated paths, so no request
      can ever put a null into the NOT NULL `created_by`
- [x] Existing controller slice tests kept green now that security is on the classpath
- [x] The Playwright proxy specs sign in through the API first, since `/api` now requires a
      session. This is the seed of the login fixture 4b completes

**Tests.** An `AuthenticationIT` against the real application context with `@SpringBootTest` plus
`@AutoConfigureMockMvc` - **not** a `@WebMvcTest` slice. The existing slice tests run with
`addFilters = false` and stub `Session` with `@MockitoBean`, which is precisely why the null
`created_by` bug is invisible to them today. Cover: anonymous request rejected with 401; CSRF
cookie issued to an anonymous caller; bad credentials rejected without revealing whether the user
exists; login succeeds and the session survives into the next request; current-user endpoint
reports the logged-in user; a write without a CSRF token rejected; and the one that matters -
**POST a category with a logged-in session and assert `createdBy` is the authenticated username**.
Admin credentials come in as `@SpringBootTest(properties = ...)`, so `AdminBootstrap` is covered
as a side effect.

**Verify it yourself.**

```powershell
cd backend; .\mvnw test -Dtest=AuthenticationIT
```

Green means the session bean, the filter chain, CSRF and the audit stamping all hold together.
What is worth reading rather than running is the test source: confirm each test asserts something
you actually care about, and that none of them stubs `Session`.

Against the stack: `http://localhost:3000/api/v1/categories` now answers 401, and the first account
exists without its password being in the repo:

```powershell
docker compose exec mysql sh -c 'mysql -uroot -p"$MYSQL_ROOT_PASSWORD" recipes -e "select username, enabled, roles from app_user"'
git grep -nE '\$2[aby]\$[0-9]{2}\$'          # a bcrypt hash; expect no output
```

**Done.** Deviations and findings:

- Ported from `my-recipes`, simplified where it was defensive: no null-body check (a missing body is
  already a 400 since 3a.1), no separate "account disabled" message (it would reveal the account
  exists), no configurable roles property, no unreachable 401 branch in `currentUser()`
- **No `CsrfCookieFilter`.** `csrf.spa()` in Spring Security 7.1.1 replaces the hand-built
  repository and handler, and its handler reads the token on every request - proven by a stack
  trace from the repository's `saveToken`: `CsrfFilter` -> `SpaCsrfTokenRequestHandler.handle` ->
  `getParameterName()` -> the cookie is written. With the filter removed all 12 tests still pass;
  `shouldIssueCsrfCookieToAnonymousCaller` stays as the guard should a later version change it
- `SessionPopulatingFilter` is built inside `SecurityConfig` rather than being a `@Component`: Boot
  would register a component filter a second time outside the security chain, and web slices would
  pick it up
- `AuthenticationIT` obtains the CSRF token the way a browser does - the `XSRF-TOKEN` cookie from an
  earlier response, sent back as `X-XSRF-TOKEN` - not with MockMvc's `csrf()` helper, which writes
  straight into the repository and would hide a missing cookie. 12 tests: the plan's list plus
  health staying open, login itself needing a CSRF token, and logout ending the session. The
  `created_by` assertion reads the row back with a separate GET
- Each guard proven load-bearing: without `SessionPopulatingFilter` the category POST is a 500
  (`createdBy is set to: null`); without saving the context the session does not survive
- The 6 `@WebMvcTest` classes failed 67 tests on the default security (401 on reads, 403 on writes)
  and now run with `addFilters = false`, as in `my-recipes`
- The Playwright proxy specs sign in through `e2e/support/api.ts` - fetch the cookie, POST the
  credentials with the header - reading the account from the repository's `.env` via
  `process.loadEnvFile`. A new spec asserts an anonymous `/api` call is a 401
- Verified against the stack: `AdminBootstrap` created `admin` with a `{bcrypt}` hash and skipped on
  restart; anonymous `/api` is 401, health 200; through the proxy, sign in, POST a category, read it
  back and the MySQL row says `created_by = admin`; logout makes the session a 401. `./mvnw clean
  verify` green: 235 unit, 75 integration. 4 Playwright tests green
- Found, not fixed: a POST's `Location` header says `http://backend:8080/...`, the internal address,
  because the backend builds it from the `Host` the proxy sends. The response body carries the id,
  so nothing needs the header yet. `server.forward-headers-strategy=framework` would fix it if Part 5
  wants it
- `backend/CLAUDE.md` gains an Authentication section; its claim that the slice tests mock `Session`
  was never true and is corrected. The root `CLAUDE.md` "no write works today" is now past tense

### 4b: Sign in and sign out in the browser

- [x] Login page and a route guard. The logout button goes in a minimal header created here -
      the full app shell with navigation is Part 5, which expands this one rather than replacing
      it

**Tests.** Playwright: unauthenticated lands on login, wrong credentials show an error, correct
credentials enter, logout returns to login, back button does not re-enter. The login fixture is
completed here and every later slice builds on it.

**Success criteria for Part 4.** The app cannot be used without signing in, the API cannot be
called without a session, and a row written through the API carries the authenticated username in
`created_by` - asserted by a test, not by inspection.

**Verify it yourself.** In the browser: the app redirects you to login, the right credentials get
you in, and logout puts you back.

**Done.** Deviations and findings:

- Ported in shape from `my-recipes` - a `proxy.ts` redirect, a `/me` check, a Playwright setup
  project - and simplified: no "return to where you were heading" after login, no component library,
  no auth context. English UI, matching the existing page
- `proxy.ts` (Next 16's name for middleware) redirects when `JSESSIONID` is missing, expressed as a
  matcher condition, so the function is a single redirect. `AppShell`, the layout of the
  `app/(app)/` route group, renders nothing until `/api/v1/auth/me` confirms the session and sends
  a 401 to `/login`. The header shows the username and a Log out button
- `lib/api.ts` holds only what sign-in needs; Part 5 grows it into the full client. Writes send the
  CSRF header, and fetch a fresh token when none is present: logout clears the `XSRF-TOKEN` cookie,
  and `my-recipes` only avoided a 403 on the next login by calling `/me` on every page load
- Backend: logout also deletes the `JSESSIONID` cookie, so `proxy.ts` sees a signed-out browser
  rather than a dead cookie. `AuthenticationIT` asserts it
- **Found and fixed: session fixation.** An anonymous 401 creates a session (Spring stores the
  request for a redirect this API never makes), and login kept that id - proven with `curl`: the
  pre-login id, used alone, answered 200 after sign-in. `AuthServiceImpl` now changes the session id
  on login, as Spring's own form login does. `shouldChangeTheSessionIdOnLogin` failed before the fix;
  after it the old id gets 401 and only the new one works
- Playwright: a setup project signs in through the form and saves the session to `e2e/.auth/`
  (gitignored - a live cookie); every test starts from it, and the sign-in tests opt out. The 4a
  API sign-in helper is gone, replaced by the saved session. The `.env` loading moved into
  `support/auth.ts`, since imports run before the config's own code
- Verified: lint and build clean; `./mvnw clean verify` 235 unit, 76 integration; 10 Playwright
  tests green. The back-button test proven load-bearing: with `AppShell`'s 401 redirect disabled it
  fails on `/`

---

## Part 5: First vertical slice - Category

Category is the simplest entity: an id and a unique name. It carries the patterns every later
slice copies, so the review of this part matters more than its size suggests. It is also the first
part in which a write can succeed, since Part 4 supplied the session that `created_by` requires.

- [ ] App shell: header, navigation menu, page layout, Tailwind base styles
- [ ] `/categories` list page, reading the live API
- [ ] `/categories/[id]` read-only view with Edit and Delete buttons
- [ ] `/categories/new` and `/categories/[id]/edit` sharing one form component with Save and Cancel
- [ ] The edit form additionally displays created date and created by, populated by the session
      wired up in Part 4
- [ ] After a successful save, the read-only view is shown - **populated by a fresh GET, not from
      the save response or a client cache.** This is a testability decision as much as a
      correctness one: a read view that echoes its own input cannot detect a backend that silently
      drops a field, and every later slice's e2e depends on this one being honest
- [ ] Delete asks for confirmation using an in-page dialog, never `window.confirm`
- [ ] Server-side validation errors from the API error envelope are surfaced on the form field
- [ ] A typed API client module, and the error envelope shape declared once

**Tests.** Playwright, asserting what a user sees rather than what the database holds - the
database is the backend tests' business. Cover: list renders seeded categories; open one and see
read mode; create one and land on its read view; **navigate away and back**, and the new category
is still listed and still reads correctly; edit it, navigate away and back, the change survived;
delete it and it is gone from the list; a duplicate name shows the server's error on the form
field; an empty name shows the validation message.

The audit fields are asserted here too - open the edit form of a category created during the test
and confirm created by shows the logged-in user. That is the UI-level expression of the Part 4
session wiring, so no SQL is needed to prove it.

**Success criteria.** Every Category operation works against the real database through the full
stack, proven by a re-fetch rather than by the view rendered immediately after the write. The
form, list, read view, API client and error handling are reusable as-is by the next slice.

**Verify it yourself.** Click the whole thing through at `http://localhost:3000/categories`. The
rule throughout: what the screen shows straight after a save proves nothing, because it may be
showing you your own input back. Navigate away and return, or reload, and then believe it.

- create a category, land on its read view, reload - still there
- edit it, leave the page, come back - the new name stuck
- open its edit form - created by shows the user you logged in as
- try to save a second category with the same name; the server's error appears on the form field,
  not as a crash or a silent no-op
- try to save an empty name; the validation message appears
- delete it, and it is gone from the list

No database queries in this part. If the UI and the database could disagree here, that is a backend
bug, and it is the backend tests' job to catch it - not something to paper over by checking both.

---

## Part 6: Replication slices - Unit, Tag, Ingredient

One reviewable increment per entity, in ascending order of difficulty. Each reuses Part 5's
components. If a slice needs a new shared abstraction, that is a signal Part 5 was under-designed -
say so rather than working around it.

- [ ] Unit: full CRUD, in list, read, create, edit, delete
- [ ] Tag: full CRUD. Note tag names are deliberately not unique, unlike the others
- [ ] Ingredient: full CRUD, including its description field

**Tests.** The Category Playwright suite replicated per entity, adjusted for each entity's fields
and for Tag's non-unique names, and keeping the navigate-away-and-back assertion rather than
trusting the post-save view.

**Success criteria.** Three entities working end to end. Any duplication across the four slices is
either justified or factored out before Part 7.

**Verify it yourself.** Run the same click-through as Part 5 against `/units`, `/tags` and
`/ingredients`: create, leave and return, edit, leave and return, delete. Two things specific to
this part:

- Tag names are deliberately not unique - create two tags with the same name and confirm both save.
  If the app rejects the second, the Category pattern was copied too literally
- Ingredient has a description field as well as a name; confirm it round-trips

Also worth a look: skim the diff for the three slices. If they are near-identical copies of the
Category pages, that is the signal to factor something out before Recipe makes it worse.

---

## Part 7: Composite slice - Recipe

The only genuinely hard slice: a Recipe has a Category, many Tags, and many Ingredients that each
carry a Unit and a quantity.

**The coverage gap this part must close first.** Recipe writes are currently tested at every layer
against a mock of the layer beneath: `RecipeControllerTest` is a `@WebMvcTest` with the service
mocked, `RecipeServiceTest` mocks the repositories, and `RecipeJpaRepositoryIT` skips the
controller and service entirely. Nothing exercises controller to service to repository to database
for a write, which is precisely the seam where a DTO's ingredients and tags get assembled into
`recipe_ingredient` and `recipe_tag` rows. That failure mode is silent: every layer's test passes,
the POST returns 200, and the relations are gone. `my-recipes` shipped exactly that bug with tags.
Write `RecipeIT` before building any UI on top.

- [ ] `RecipeIT` - `@SpringBootTest` against a real database, not a slice. POST a recipe with a
      category, two tags and three ingredients; GET it back; assert every relation survived with
      the right amount and unit. Then PUT to change the category, drop a tag and alter an
      ingredient, and GET again. Then DELETE, and assert the `recipe_ingredient` and `recipe_tag`
      rows went with it rather than being orphaned
- [ ] Confirm from `RecipeIT` - not by reading the code - whether tags are writable through the
      existing API. If they are silently discarded, that is a backend fix inside this part, not a
      workaround in the frontend
- [ ] Recipe list and read view, showing category, tags, and each ingredient with its quantity and
      unit in a readable layout
- [ ] Recipe form: category as a dropdown of existing categories, tags as a multi-select of
      existing tags
- [ ] Ingredient rows on the form: add a row, pick an ingredient from a dropdown, enter a quantity,
      pick a unit from a dropdown, remove a row
- [ ] Wire to the recipe ingredient sub-resource endpoints that already exist on `RecipeController`
- [ ] Ratings are explicitly not built. The API returns `recipeRatings` on read and rejects them on
      write with `RECIPE_RATING_NOT_SUPPORTED`; ensure the frontend never sends them

**Tests.** Three layers, each owning what only it can see.

1. **`RecipeIT`** carries relation integrity, as above. This is the load-bearing test of the part.
2. **Playwright** covers the user's path: fill the form with a category, two tags and three
   ingredients, save, land on the read-only view - then **navigate away and come back** and assert
   every relation still renders. Edit to change the category, remove a tag, change one
   ingredient's quantity and unit, and remove an ingredient; navigate away and back; assert. Then
   delete. The navigate-away step is what makes this test worth having: read the page straight
   after the save and it may just be echoing the form's own state, which would stay green over a
   backend that dropped every tag.
3. **One `page.route()` assertion** that the POST body carries no `recipeRatings`. A round-trip
   cannot distinguish "the frontend did not send it" from "it was sent and ignored", so this is
   the one request-shape assertion that earns its place. It does not justify adding Vitest - keep
   it in Playwright.

**Success criteria.** `RecipeIT` proves the relations persist; Playwright proves a user can produce
and edit them through the UI and see them again on a fresh page load. Neither test inspects the
database from the UI layer.

**Verify it yourself.** Create a recipe with a category, two tags, and three ingredients that each
have a quantity and a unit. Leave the page - go to the recipe list, or reload - then open it again.
Every relation should still be there. Then edit it: change the category, remove one tag, change one
ingredient's quantity and unit, remove another ingredient. Leave and return again.

Then delete the recipe, and confirm the ingredients and tags it referenced still exist on their own
pages - deleting a recipe must not take its ingredients with it.

```powershell
cd backend; .\mvnw test -Dtest=RecipeIT
```

Read that test's source as well as its result. It is the only thing standing between a green build
and silently discarded relations, so it is worth confirming it asserts on data fetched back from
the API rather than on the object it just posted.

---

## Part 8: Welcome page and full sweep

- [ ] Welcome page with a hardcoded short description of the application
- [ ] Navigation menu linking all five entity types, with the signed-in user and logout visible
- [ ] Empty states, loading states and a not-found page
- [ ] Responsive check at phone width
- [ ] Full Playwright suite runs green against a freshly built stack from an empty volume
- [ ] `README.md` final pass, minimal
- [ ] `backend/docs/future_enhancements.md` updated with anything deferred during Parts 2-7

**Success criteria.** `docker compose down -v`, then the start script, then the full Playwright
suite passes with no manual intervention.

**Verify it yourself.** The full cold start, exactly as a new machine would do it:

```powershell
docker compose down -v
.\scripts\start.ps1
npx playwright test
```

Everything green, no manual step in between. Then read `README.md` and follow it literally, doing
only what it says - if it is missing a step, you will find out here rather than in six months.

---

## Reference implementation

`C:\projects\my-recipes` is an earlier, substantially complete attempt at this same application.
It is a reference to borrow from. It was abandoned over process, not code: it was built too fast to
verify step by step. Consult it for solved problems, do not copy its pace.

Specifically worth consulting: the three-container compose topology, `.env.example`, the whole
Spring Security setup ported wholesale in Part 4, and the `next.config.ts` rewrites. It has no seed
data: checked in 3c, its `db.changelog_1.1.xml` creates `app_user` and relaxes
`recipe_tag.created_by`, and nothing else - an earlier note here calling it a repaired seed was wrong.

Consulting it does not shorten the review of any part. A borrowed solution still has to be
explained and accepted like any other.

## Resolved

- Reference implementation: `my-recipes` is an earlier attempt, approved for use as reference.
- The multi-user schema note lives in `backend/docs/future_enhancements.md` in this repo.
