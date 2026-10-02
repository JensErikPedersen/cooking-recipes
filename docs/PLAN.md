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

Category is the simplest entity: an id, a unique name and an optional description. It carries the patterns every later
slice copies, so the review of this part matters more than its size suggests. It is also the first
part in which a write can succeed, since Part 4 supplied the session that `created_by` requires.

Split into four steps, each committed and accepted on its own:

- **5a** - backend: a duplicate name and deleting a category in use answer 409, not 500
- **5b** - navigation menu, the typed API client, the list and the read view
- **5c** - the shared create/edit form, audit fields, field errors, a fresh GET after save, and
  the Edit button that leads to it
- **5d** - Delete on the read view, through an in-page confirmation dialog

### 5a: Conflicts answer 409

The UI tests below need the server's error for a duplicate name, and the backend had none: no
service checked, the unique index refused at commit, and the `DataIntegrityViolationException` fell
through to the 500 handler (`backend/docs/future_enhancements.md`). Deleting a category a recipe
uses hit the same path through `FK_recipe_category`, and every seeded category is in use.

- [x] Category POST and PUT with a name another category has: 409, code
      `CATEGORY_ALREADY_EXISTS` (104), reported on the `name` field in `validationExceptions`, the
      shape a 400 already uses. A PUT keeping its own name still succeeds
- [x] DELETE of a category recipes use: 409, code `CATEGORY_IN_USE` (105), "Category 'Brød' is
      used by 3 recipes and cannot be deleted"
- [x] Any other `DataIntegrityViolationException`: 409, code `DATA_CONFLICT` (40), generic message,
      the raw text logged only. Covers Unit and Ingredient until Part 6 gives them their own checks

**Tests.** `CategoryIT`, `@SpringBootTest` through MockMvc to H2, with no transaction around the
test - one would postpone the commit, and with it the violation, past the asserted response. Unit
tests for the service checks and for the handler.

**Verify it yourself.**

```powershell
cd backend; .\mvnw test -Dtest=CategoryIT
```

Against the stack, signed in through the browser and then in its dev tools console:

```js
const t = decodeURIComponent(document.cookie.match(/XSRF-TOKEN=([^;]+)/)[1]);
const post = (b) => fetch("/api/v1/categories", {method: "POST", headers: {"Content-Type": "application/json", "X-XSRF-TOKEN": t}, body: JSON.stringify(b)}).then(async r => [r.status, await r.json()]);
await post({name: "Dessert"});    // 409, errorCode 104, validationExceptions on name
await fetch("/api/v1/categories/14d4c0b0-46ea-498d-a3a5-56060a3d7a7c", {method: "DELETE", headers: {"X-XSRF-TOKEN": t}}).then(r => r.json());   // Brød: 409, used by 3 recipes
```

**Done.** Deviations and findings:

- Proven before fixing: `CategoryIT` failed three tests with 500, each logged as an unhandled
  `DataIntegrityViolationException` - two unique-index violations on `category.name`, one on
  `FK_recipe_category`. With only the general handler added they became 409 with code 40, which
  showed it catches the exception raised at commit; the service checks then made them 104 and 105
- `ServiceException` gained an optional `field`; the handler reports it as a `validationExceptions`
  entry, so the frontend handles a 400 and a 409 on a field the same way
- `RecipeJpaRepository.countByCategoryId` feeds the in-use check
- On MySQL, through the proxy: duplicate, a different-case duplicate (the collation is
  case-insensitive, and the check and the index agree since both use it), rename to a seeded name,
  keep own name, delete seeded Brød, delete an unused one - all as above. A duplicate Unit name is
  409 with code 40. No unhandled exception in the log
- `./mvnw clean verify` green: 242 unit, 81 integration
- Known, not changed: `BaseDTO` writes dates as `yyyy-MM-dd HH:mm` without an offset (UTC in the
  stack), and the DTOs cannot read that format back, as `backend/CLAUDE.md` records. The frontend
  therefore sends only the editable fields, and 5c decides how to label the time zone
- Found, not changed: `config/TestingConfiguration` is referenced by no test - a `@TestConfiguration`
  is excluded from component scanning - so it is dead. Its constructor call was updated to compile

### 5b: Navigation, list and read view

- [x] App shell: header, navigation menu, page layout, Tailwind base styles
- [x] `/categories` list page, reading the live API
- [x] `/categories/[id]` read-only view. Its Edit and Delete buttons come with what they do, in 5c and 5d
- [x] A typed API client module, and the error envelope shape declared once. It holds what each
      step uses, so it grows in 5c and 5d

**Tests.** Playwright, `categories.spec.ts`: the menu leads to the list, which shows the seeded
categories; opening one shows it in read mode; an unknown id shows not found.

**Verify it yourself.** At `http://localhost:3000`, click Categories in the header: the four seeded
categories are listed and the menu entry is marked. Open one: its name and description, and no
input fields. Change the id in the address bar to `00000000-0000-0000-0000-000000000000`: "Category
not found", with a link back.

**Done.** Deviations and findings:

- Pages are client components fetching in `useEffect` through `lib/api.ts`, as `AppShell` already
  did - recorded in `frontend/CLAUDE.md`. Server-side fetching would have to forward the session and
  CSRF cookies to the backend's internal address, for no gain behind a sign-in
- No data-loading hook: each page's fetch is three lines, and the shared pieces are what renders
  around it - `ErrorMessage` for a failed call and `DetailList` for a read view's labelled fields
- `AppShell` gains the menu, one entry per entity added by its slice so no link leads nowhere, with
  `aria-current` on the current section, and the `<main>` page area every page renders into
- The list sorts by name in the browser. The API returns rows in primary key order, which for the
  seed happens to be alphabetical - random UUIDs - so a test of the order waits for 5c, which
  creates categories that break it
- `globals.css`: light only. The scaffold's dark scheme inverted the text but not the components'
  fixed greys, so a hovered Log out button was light text on a light grey. Its `Arial` override is
  gone too, so the Geist font the root layout already loads is the one in use
- Verified: lint and build clean; 13 Playwright tests green; the list and read view checked in a
  screenshot

### 5c: One form for create and edit

- [x] `/categories/new` and `/categories/[id]/edit` sharing one form component with Save and Cancel
- [x] The edit form additionally displays created date and created by, populated by the session
      wired up in Part 4
- [x] After a successful save, the read-only view is shown - **populated by a fresh GET, not from
      the save response or a client cache.** This is a testability decision as much as a
      correctness one: a read view that echoes its own input cannot detect a backend that silently
      drops a field, and every later slice's e2e depends on this one being honest
- [x] Server-side validation errors from the API error envelope are surfaced on the form field
- [x] The read view's Edit button, and the list's New category button

**Tests.** Playwright, added to `categories.spec.ts`: create, with the read view's own GET awaited,
then leave through the menu and return, and reload; edit, leave and return, the old name gone from
the list; the edit form's created by is the signed-in user and its created time, read as UTC, is
now; a duplicate name and an empty name each show the server's message as the name field's
description; Cancel saves nothing; the list sorts, with the API's response served reversed.

**Verify it yourself.** At `http://localhost:3000/categories`:

- New category, a name and a description, Save: the read view. Reload - still there
- Edit, change both, Save; go to Categories and open it again - the change stuck
- Open its edit form: Created is now in UTC, Created by is the user you signed in as
- New category named `Dessert`: "A category named 'Dessert' already exists" under the Name field
- New category with no name: "Category name is required" under the Name field

The categories you create stay until 5d gives you a Delete button; remove them then.

**Done.** Deviations and findings:

- The Edit button moved here from 5d: it only links to the edit form, which is unreachable without
  it. 5d keeps Delete
- `EntityForm` is the frame every entity's form reuses: the audit fields of an existing entity,
  submit, Save and Cancel, the error that belongs to no field, and the navigation to the read view
  after a save. `CategoryForm` supplies only its fields and state; new and edit pages wrap it.
  Also shared now: `TextField` (label, input, the server's error tied by `aria-describedby`),
  `PageHeader`, `NotFound`, and the button styles in `components/styles.ts`
- No validation in the browser. The server's rules and messages are the only ones, and a 400 and
  the 5a 409 reach the field through the same `fieldErrors()`. A cleared description is sent as
  absent, so it is stored as none rather than as an empty string
- Dates: proven with a throwaway test against the application's `JsonMapper` that Jackson writes
  the time in the backend JVM's zone, not in UTC - `Europe/Copenhagen` on this host. Decided: shown
  as-is with a "UTC" label, and the backend image now sets `TZ=UTC` so the label stays true. The
  e2e test checks the label against the clock. The proper fix is in
  `backend/docs/future_enhancements.md`
- Playwright removes what it creates, through `e2e/support/api.ts` after each test - housekeeping
  only, never part of what a test proves. The stack's database held only the four seeded
  categories after the run
- The sort test proven load-bearing: with the sort removed from the list page, it fails
- Verified: lint and build clean; 20 Playwright tests green; the edit form with a field error and
  the read view checked in screenshots

### 5d: Delete

- [x] Delete button on the read view
- [x] Delete asks for confirmation using an in-page dialog, never `window.confirm`

**Tests.** Playwright, added to `categories.spec.ts`: confirm a delete, land on the list without it,
and its URL then shows not found; cancel the dialog and the category survives a reload; delete
seeded Brød and the dialog shows the server's reason with only an OK button, and Brød is still listed.

**Verify it yourself.** Open a category you created, Delete: a dialog on the page asks. Cancel
keeps it; Delete removes it and shows the list without it. Then open Brød and confirm a delete:
"Category 'Brød' is used by 3 recipes and cannot be deleted", in the dialog, with OK as
the only button, and Brød stays.

**Done.** Deviations and findings:

- `DeleteButton` is the reusable piece: the button, a native `<dialog>` opened with
  `showModal()` - modal, Escape closes it, and it is a `dialog` role named by its heading - the
  server's refusal shown inside it, and the list after a delete. It needs `m-auto`: Tailwind's
  preflight zeroes every margin, including the one the browser centres a modal with
- After a refusal the dialog shows the reason and only an OK button: offering Delete again would
  only repeat the refusal. Raised in review
- The e2e cleanup accepts a 404, since a delete test removes its own category
- Verified: lint and build clean; 23 Playwright tests green; the dialog with the in-use refusal
  checked in a screenshot. After the run the stack held the four seeded categories, plus a "Suppe"
  created by hand - not by the tests, whose names all start with "E2E"

### Part 5 as a whole

**Tests.** Playwright, asserting what a user sees rather than what the database holds - the
database is the backend tests' business. Cover: list renders seeded categories; open one and see
read mode; create one and land on its read view; **navigate away and back**, and the new category
is still listed and still reads correctly; edit it, navigate away and back, the change survived;
delete it and it is gone from the list; a duplicate name shows the server's error on the form
field; an empty name shows the validation message; deleting a category a recipe uses shows the
server's reason and leaves it listed.

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

**Done.** Every test listed above is in `categories.spec.ts`, 13 tests, and every write is checked
after leaving the page or reloading. For Part 6 to reuse as-is: `lib/api.ts` (one client object per
entity, `fieldErrors`), `EntityForm`, `TextField`, `DetailList`, `PageHeader`, `NotFound`,
`DeleteButton`, `ErrorMessage`, the button styles, and on the backend the 409 pattern of 5a. An
entity adds its client object, its `<Entity>Form`, four small pages, a menu entry and its spec.

---

## Part 6: Replication slices - Unit, Tag, Ingredient

One reviewable increment per entity, in ascending order of difficulty. Each reuses Part 5's
components. If a slice needs a new shared abstraction, that is a signal Part 5 was under-designed -
say so rather than working around it.

- [x] Unit: full CRUD, in list, read, create, edit, delete
- [x] Tag: full CRUD. ~~Note tag names are deliberately not unique, unlike the others~~ - they are
      unique: the schema has always said so, and that was decided to hold. See 6b
- [x] Ingredient: full CRUD, including its description field

**Tests.** The Category Playwright suite replicated per entity, adjusted for each entity's fields
and keeping the navigate-away-and-back assertion rather than
trusting the post-save view.

**Success criteria.** Three entities working end to end. Any duplication across the four slices is
either justified or factored out before Part 7.

**Verify it yourself.** Run the same click-through as Part 5 against `/units`, `/tags` and
`/ingredients`: create, leave and return, edit, leave and return, delete. Two things specific to
this part:

- Tag names are unique, like the others (corrected in 6b) - a second tag with the same name shows
  the server's error on the Name field
- Ingredient has a description field as well as a name; confirm it round-trips

Also worth a look: skim the diff for the three slices. If they are near-identical copies of the
Category pages, that is the signal to factor something out before Recipe makes it worse.

### 6a: Unit

**Tests.** `UnitIT` as `CategoryIT`, with the in-use case on one recipe using the unit on two
lines. Playwright `units.spec.ts`, the Category suite replicated: 13 tests.

**Verify it yourself.** At `http://localhost:3000/units` (Units in the menu), the Part 5
click-through: create with a name and a label, reload, edit, leave and return, delete. Then:

- a new unit named `Gram`: "A unit named 'Gram' already exists" under Name
- Save with both fields empty: a message under each
- open Gram, Delete, confirm: "Unit 'Gram' is used by 2 recipes and cannot be deleted", with OK

**Done.** Deviations and findings:

- Backend, the 5a pattern: 409 `UNIT_ALREADY_EXISTS` (311) on the name field, 409 `UNIT_IN_USE`
  (312). `UnitIT` first showed both as the 5a backstop's generic 409, code 40
- **The in-use count is of recipes, not lines.** One recipe can use a unit on several ingredient
  lines - seeded Gram is on 8 lines in 2 recipes - so a derived count would have said 8.
  `RecipeIngredientJpaRepository.countRecipesByUnitId` is the first JPQL `@Query` in the
  repositories; `UnitIT` proves the distinct count, and the Gram e2e test proves it on MySQL
- **Found and fixed:** the `Unit` entity declared `label` unique and `name` not, the reverse of
  the Liquibase schema. With `ddl-auto=none` it changed nothing at runtime, but it misled; the
  annotations now mirror the schema
- **Part 5 under-designed one piece, as this part warned.** The read view and edit page each held
  the same load, 404 and error handling. Rather than copy it into every entity, it is now
  `EntityLoader`, and the Category pages use it too - the Category suite guards that refactor.
  The list pages keep their three-line fetch: what differs between them is the table
- e2e: what every spec does alike moved to `support/pages.ts` (`createThroughForm`,
  `expectReadView`, `expectCreatedNowBy`, `openFromMenu`), and cleanup is `deleteById` for any
  collection. The Category spec uses them, its tests unchanged
- Verified: `./mvnw clean verify` 245 unit, 86 integration; lint and build clean; 36 Playwright
  tests green; afterwards the stack held only seeded units and categories

### 6b: Tag

**Tests.** `TagIT` as `CategoryIT`. Playwright `tags.spec.ts`, the Category suite replicated: 13
tests.

**Verify it yourself.** At `http://localhost:3000/tags` (Tags in the menu), the Part 5
click-through: create, reload, edit, leave and return, delete. Then:

- a new tag named `Spicy`: "A tag named 'Spicy' already exists" under Name
- open "Godt til kaffen", Delete, confirm: "used by 3 recipes and cannot be deleted", with OK

**Done.** Deviations and findings:

- **Tag names are unique - the plan was wrong.** It said "deliberately not unique", as did the
  `TagDTO` comment, `backend/CLAUDE.md` and `future_enhancements.md`. The Liquibase schema has
  always created `tag.name` unique, and MySQL enforces it: `show index from tag` lists a unique
  `name` index, and a second "Spicy" through the API was the 5a backstop's 409. No reason for
  non-unique names was recorded anywhere; the claim matched only the `Tag` entity's annotation.
  Decided: unique holds, which also keeps Part 7's tag multi-select unambiguous. The plan's
  "confirm both save" step is corrected, and the entity, the DTO comment and the docs with it
- Backend, the 5a pattern: 409 `TAG_ALREADY_EXISTS` (411) on the name field, 409 `TAG_IN_USE`
  (412). `RecipeJpaRepository.countByTagsId` counts through the recipes' `@ManyToMany` tags - a
  derived query, since `recipe_tag`'s key (recipe, tag) cannot count a recipe twice
- **Found: VS Code compiles into Maven's `target/`.** The first `TagIT` run failed with "Unresolved
  compilation problem: com.fasterxml.jackson.databind cannot be resolved" - the Eclipse compiler's
  message, not javac's. The Red Hat Java extension's language server rebuilt `TagDTO.class` into
  `target/classes` after an edit, without the Jackson 3 setup `lombok.config` gives Maven, and
  Maven took the class as up to date. A `clean` build is correct; the Commands in `CLAUDE.md` now
  say to use one. Earlier steps always ran `clean verify`, which is why it never showed
- Verified: `./mvnw clean verify` 248 unit, 91 integration; lint and build clean; 49 Playwright
  tests green; afterwards the stack held only seeded tags, units and categories

### 6c: Ingredient

**Tests.** `IngredientIT` as `CategoryIT`. Playwright `ingredients.spec.ts`, the Category suite
replicated plus one: a cleared description is none after a reload, not an empty value - 14 tests.

**Verify it yourself.** At `http://localhost:3000/ingredients` (Ingredients in the menu), the Part
5 click-through: create with a description, reload, edit both fields, leave and return, delete.
Then:

- edit one, clear its description, Save, reload: Description reads None
- a new ingredient named `Hvedemel`: "An ingredient named 'Hvedemel' already exists" under Name
- open Hvedemel, Delete, confirm: "used by 2 recipes and cannot be deleted", with OK

**Done.** Deviations and findings:

- Backend, the 5a pattern: 409 `INGREDIENT_ALREADY_EXISTS` (211) on the name field, 409
  `INGREDIENT_IN_USE` (212). `recipe_ingredient`'s key is (recipe, ingredient), so the derived
  `countByIngredientId` counts recipes. Schema and entity agree on the unique name this time
- **The list pages are factored out**, as this part's success criterion asks. Four were the same
  50 lines but for their columns; they are now `EntityList`, given a load call and the columns
  after Name. Every earlier spec passed unchanged on it, the sort tests included. What stays per
  entity is justified: its form, which holds its own fields, and the small read, new and edit
  pages wrapping the shared pieces
- Verified: `./mvnw clean verify` 251 unit, 96 integration; lint and build clean; 63 Playwright
  tests green; afterwards the stack held only seeded data

### Part 6 as a whole

**Done.** Three entities end to end, each with its own `<Entity>IT` and replicated Playwright suite.
Duplication is factored out where it was the same code - `EntityList`, `EntityLoader`, the e2e
`support/pages.ts` - and justified where it remains: the per-entity forms and page wrappers, a
few lines each. Found on the way: tag names were documented as not unique but are, and VS Code
compiles into Maven's `target/`. Part 7 starts from four lookups with the same 409 behaviour.

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

Split into four steps, each committed and accepted on its own:

- **7a** - `RecipeIT`, and the backend fixes it calls for
- **7b** - the recipe list and read view
- **7c** - the recipe form without ingredient lines: category, tags, delete, no ratings sent
- **7d** - ingredient lines on the form, and the full Playwright round trip

Decided with the split: the form saves its ingredient lines inside the recipe's own PUT rather than
through the sub-resource endpoints below, which would write each line before Save and leave Cancel
unable to undo it. The PUT therefore has to replace the lines - see 7a.

- [x] `RecipeIT` - `@SpringBootTest` against a real database, not a slice. POST a recipe with a
      category, two tags and three ingredients; GET it back; assert every relation survived with
      the right amount and unit. Then PUT to change the category, drop a tag and alter an
      ingredient, and GET again. Then DELETE, and assert the `recipe_ingredient` and `recipe_tag`
      rows went with it rather than being orphaned
- [x] Confirm from `RecipeIT` - not by reading the code - whether tags are writable through the
      existing API. If they are silently discarded, that is a backend fix inside this part, not a
      workaround in the frontend
- [x] Recipe list and read view, showing category, tags, and each ingredient with its quantity and
      unit in a readable layout
- [x] Recipe form: category as a dropdown of existing categories, tags as a multi-select of
      existing tags
- [x] Ingredient rows on the form: add a row, pick an ingredient from a dropdown, enter a quantity,
      pick a unit from a dropdown, remove a row
- [ ] ~~Wire to the recipe ingredient sub-resource endpoints that already exist on `RecipeController`~~
      Decided against with the split: the lines travel in the recipe's PUT. The endpoints stay in
      the API, unused by the UI
- [x] Ratings are explicitly not built. The API returns `recipeRatings` on read and rejects them on
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
cd backend; .\mvnw clean test -Dtest=RecipeIT
```

Read that test's source as well as its result. It is the only thing standing between a green build
and silently discarded relations, so it is worth confirming it asserts on data fetched back from
the API rather than on the object it just posted.

### 7a: RecipeIT and the backend fixes

**Tests.** `RecipeIT`, 5 tests: create with every relation, then a fresh GET; a PUT changing the
category, dropping a tag, changing one line's amount and unit and removing another, then a fresh
GET; delete, with the join rows counted in the database and the ingredient and tag still there;
a duplicate name; a PUT keeping its own name.

**Verify it yourself.** The command above, and read `RecipeIT`. The UI comes in 7b to 7d.

**Done.** Deviations and findings - each proven by `RecipeIT` before it was fixed:

- **Tags were silently discarded.** A POST with two tags answered 201, and a fresh GET showed
  `tags: []`: `RecipeServiceImpl` never wrote them. It now sets exactly the listed tags, each an
  existing tag by id (404 otherwise); absent tags leave them as they are
- **Writing them then failed on `recipe_tag.created_by`**, as `my-recipes` had found: Hibernate's
  join-table insert is `insert into recipe_tag (recipe_id,tag_id)`, and the column is NOT NULL.
  Decided: `db.changelog_1.3.xml` makes it nullable, ported from `my-recipes`, rather than
  promoting `recipe_tag` to an entity. The 5a backstop had reported it as "The change conflicts with
  existing data" - a 409 for what was really a server fault, the price of catching every
  `DataIntegrityViolationException` there
- **A PUT could neither change nor remove an ingredient line.** A line already on the recipe was
  kept as stored, and a line left out stayed. The payload's list is now authoritative: an existing
  line takes its amount and unit, an omitted line is deleted - explicitly, as the sub-resource
  delete already did, since the association has no `orphanRemoval`. Proven load-bearing: with the
  old keep-as-stored behaviour restored, the PUT test fails on 500 against 600
- Duplicate name: 409 `RECIPE_ALREADY_EXISTS` (53) on the name field, the 5a pattern
- The delete test only became meaningful once tags were saved: before, its `recipe_tag` count was
  zero because no row had ever been written
- `RecipeServiceTest.shouldUpdateRecipe` stubbed the old flow, which skipped resolving lines already
  on the recipe; it now stubs the ingredient and unit lookups. Two unit tests added: duplicate name,
  unknown tag
- Against MySQL through the proxy: create, update, duplicate and delete all as in `RecipeIT`; the
  1.3 changeset applied to the existing volume. `./mvnw clean verify` 253 unit, 101 integration;
  63 Playwright tests green
- **Seed data was found changed in the stack:** the unused seeded tag "Børnevenlig" had been deleted
  by hand while trying out the Tags page, and the Tag spec failed on it. Restored with one INSERT of
  its seed values. The specs assume the seeds are intact: after deleting a seed by hand, restore it
  or reseed with `docker compose down -v`
- Not done here, for 7d: the nested lines' amount is not validated - `recipeIngredients` carries
  no `@Valid`, so a line without an amount is stored without one

### 7b: Recipe list and read view

**Tests.** Playwright `recipes.spec.ts`, 6 tests: the menu leads to the list, with each recipe's
category and tags; the list sorts, the API's response served reversed; the read view of
"Fuldkorns hvedebrød" shows its category, tag, six lines as amount, unit and ingredient, and
instructions; "Brunkage", with no tags or lines, says None rather than leaving gaps; the category,
tag and ingredient link to their own pages; an unknown id shows not found.

**Verify it yourself.** At `http://localhost:3000`, Recipes is first in the menu. Open "Fuldkorns
hvedebrød": Brød, Godt til kaffen, then six lines such as "170 gr Fuldkorns hvedemel" in name
order, and the instructions. Click Brød, the tag and an ingredient - each opens its own page. Note
"Ølandshvedebrød" sorts last in the list.

**Done.** Deviations and findings:

- Read-only, as planned: the New, Edit and Delete buttons come with the form in 7c. `EntityList`'s
  New button became optional for that; 7c makes it required again
- The read view resolves nothing itself: each line already carries `ingredientName` and
  `unitLabel`. Lines read amount, unit, ingredient, sorted by ingredient; instructions keep their
  line breaks; category, tags and ingredients link to their own pages
- **Found and fixed: list order depended on the viewer's browser.** The sort test failed on
  "Ølandshvedebrød". `localeCompare` without a locale follows the runtime's: Node runs in `da-DK`
  (Windows), so the test expected Danish order, Ø after Z, while Playwright Test's browser runs in
  `en-US`, which sorts Ø beside O. A user's English browser would have done the same. Decided:
  Danish order always. `lib/sort.ts` holds the one comparator, used by `EntityList`, the read
  view's tags and lines, and the list's tags column - which had used a plain `.toSorted()`, byte
  order. The specs compare with the same comparator from `support/pages.ts`
- Verified: lint and build clean; 69 Playwright tests green; the list and a read view checked in
  screenshots

### 7c: Recipe form, without ingredient lines

**Tests.** Playwright, 9 added to `recipes.spec.ts`: create with a category and two tags, the read
view's own GET awaited, then checked again after leaving through the menu and after a reload; edit
the category and drop a tag, leave and return; editing only the text keeps the ingredient lines;
created by and now in UTC on the edit form; the `page.route` check that neither the POST nor the
PUT carries `recipeRatings`; a duplicate name, and a missing name then a missing category, each on
its field; Cancel; delete.

**Verify it yourself.** At `http://localhost:3000/recipes`: New recipe, give it a name, choose a
category, tick two tags, Save - the read view. Reload. Edit it: change the category, untick a tag,
Save, leave and return. Then:

- New recipe with nothing filled in, Save: "Recipe name is required" under Name; add a name, Save
  again: "A recipe requires a category" under Category
- open "Fuldkorns hvedebrød", Edit, Save without changes: its six ingredient lines are still there
- delete the recipe you created

**Done.** Deviations and findings:

- `RecipeForm` loads the categories and tags first and renders once it has them. The category is
  a dropdown, `SelectField` - a labelled select tied to its error like `TextField`, which 7d's
  ingredient and unit pickers reuse. The tags are a row of checkboxes: a multi-select that needs
  no instructions
- **The body is built field by field, never from the loaded recipe**, which carries
  `recipeRatings`. The ratings test checks both the POST and the PUT, since an edit form is where
  spreading the loaded object would leak them
- Ingredient lines are not sent yet, and an absent list leaves the recipe's lines alone (7a), so
  editing an existing recipe keeps them - tested on a recipe given lines through the API, since the
  form cannot add them before 7d. `e2e/support/api.ts` gained `createById` for that setup
- Backend: a missing category was a 400 with no field, so the form could only show it above Save.
  It now names the field `category`, with a message for the reader: "A recipe requires a
  category". A blank name is caught by Bean Validation before the service runs, so the two
  messages come one after the other, not together
- `EntityList`'s New button is required again, as 7b promised
- Verified: `./mvnw clean verify` 253 unit, 101 integration; lint and build clean; 78 Playwright
  tests green; afterwards the stack held exactly the seeded recipes, lines and tag links; the edit
  form checked in a screenshot

### 7d: Ingredient lines on the form

**Tests.** `RecipeIT`, 4 added: a line without an amount, an amount too large for the column, an
ingredient listed twice, and a PUT with two bad lines that leaves the recipe unchanged. Playwright,
3 added: the plan's round trip - a category, two tags and three ingredients, one amount typed with a
comma; edit the category, drop a tag, change a line's amount and unit, remove a line; delete, and
the ingredient and tag are still on their own pages - with every check made after leaving and
returning; the line messages under the Ingredients section; a removed and an added line after a
reload.

**Verify it yourself.** The plan's own steps, above, at `http://localhost:3000/recipes`. Then on a
new recipe: Add ingredient, choose Hvedemel and a unit but no amount, Save - "Hvedemel needs an
amount above zero." under Ingredients. Add Hvedemel a second time - "listed more than once".

```powershell
cd backend; .\mvnw clean test -Dtest=RecipeIT
```

**Done.** Deviations and findings:

- **The line checks were missing or let data through, proven before each fix.** A line without an
  amount was saved without one (201). The same ingredient on two lines answered 201 and silently
  kept one: `Recipe` holds its lines in a set keyed by ingredient. An amount of 10000 reached the
  database, `DECIMAL(6,2)`, and came back as the generic 409. All three are now a 400
- The problems are sentences naming the ingredient - "Hvedemel needs an amount above zero." -
  sorted and joined into one message on the field `recipeIngredients`, shown under the form's
  Ingredients section. One message, not one per line: the DTO takes the lines as a `Set`, so their
  order, and an index to point at, is lost. Noted in `backend/docs/future_enhancements.md`. The old
  messages named ids, and nothing tested them except one assertion on an id, now on the sentence
- `IngredientLines`: one row per line - ingredient, amount, unit, Remove - each a group named
  "Ingredient line N" with screen-reader labels, and an Add ingredient button. The amount is a text
  input that takes "3,5" as well as "3.5": a `type="number"` input would let the browser refuse the
  form with its own validation. Blank lines are sent too, so the server says what is missing
- The form now always sends its lines, so the 7a rule applies in full: what the form shows is what
  the recipe keeps. The 7c test that editing the text keeps the lines still holds
- Verified: `./mvnw clean verify` 253 unit, 105 integration; lint and build clean; 81 Playwright
  tests green; afterwards the stack held exactly its seeded data; the form with lines and a line
  error checked in a screenshot

### Part 7 as a whole

**Done.** `RecipeIT` proves the relations persist through create, edit and delete, asserting on a
fresh GET and on the join tables; it found tags discarded, the `recipe_tag` audit column blocking
them, edits unable to change or remove a line, and lines accepted without an amount or twice.
Playwright proves a user can produce and edit every relation and see it again after leaving the
page. Neither inspects the database from the UI layer. Decided on the way: the lines travel in the
recipe's PUT, not through the sub-resource endpoints; `recipe_tag.created_by` is nullable; lists
sort the Danish way.

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
