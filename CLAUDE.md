# The Cooking Recipe web app

## Business Requirements

This project is building a MVP version of a Cooking Recipe App. Key features:
- A user can sign in
- A user sees a Welcome page with a short description of the application and a menu showing menu items with links to different entity types (Recipes, Categories, Ingredients etc.). There should also be a logout button.
- When an entity is opened from the list, it is shown in Read mode with an edit button and a delete button.
- When the user creates a new entity (eg. Ingredient), a form is shown with Save and Cancel buttons.
- Editing an existing entity uses the same form, except that created date and created by are also displayed. When a new or existing entity has been saved on the form, the read-only version is shown.
- Opening a Recipe with Ingredients, Category, etc. displays all related entities in a readable format.

## Limitations

For the MVP there is a single user account, created on first run from environment-supplied
credentials. It lives in an `app_user` table, so the schema supports multiple users from the
start - but there is no sign-up, no user administration screen and no per-user ownership of data.
Those are future versions.

Ratings are out of scope for this MVP. The `Rating` and `RecipeRating` entities exist in the
backend but have no controller, and none will be added here.

For the MVP, this runs locally in Docker containers.

## Technical Decisions

- NextJS frontend. Use npm. Tailwind CSS for styling, no component library.
- Spring Boot backend. Maven is used already and is kept for the backend.
- Three containers: `mysql`, `backend` (Spring Boot API), `frontend` (Next.js server). The
  Next.js server owns the browser-facing origin and proxies `/api` to the backend, so there is one
  origin in the browser and no CORS configuration.
  Launched together with `docker compose`, wrapped by the start/stop scripts.
- Credentials and ports come from a gitignored `.env` at the repo root, with a committed
  `.env.example` documenting every variable. No credential is ever written into
  `application.properties`, `docker-compose.yml`, or any script.
- Start and stop scripts in `scripts/`: `.sh` for Mac/Linux and `.ps1` for Windows, thin wrappers
  over `docker compose`.
- Sign in uses `spring-boot-starter-security` with session-cookie authentication and users held
  in an `app_user` table. CSRF stays enabled. A `SessionPopulatingFilter` in the security chain
  copies the authenticated principal into the existing request-scoped `Session` bean, which is
  what finally populates `createdBy` / `updatedBy`. This is ported from the reference
  implementation rather than designed fresh - see `docs/PLAN.md` Part 4.
- e2e tests use Playwright, run against the Docker stack. Comprehensive and thorough.
- MySQL database, schema managed by Liquibase, which is already in place.

## Starting Point

A working backend is available in `backend/`. It exposes CRUD under `/api/v1/` for categories,
ingredients, recipes, tags and units. It has no security, no Docker setup, and is a pure backend.

Note that **no write works today**. `created_by` is NOT NULL on all nine tables and on
`BaseEntity`; the only thing that sets it is `BaseEntityListener` reading the `Session` bean, and
nothing in the codebase calls `setUserName()`. Every POST and PUT against real MySQL therefore
fails. The controller slice tests do not catch it because they stub `Session` with `@MockitoBean`
and run with `addFilters = false`. Sign in (Part 4) is what makes the application writable.

## Coding standards

1. Use latest versions of libraries and idiomatic approaches as of today. Upgrade to stable
   versions only - NOT RC versions. Do NOT upgrade the Spring Boot version or the Java version;
   they were upgraded recently and are deliberately left alone.
2. Keep it simple - NEVER over-engineer, ALWAYS simplify, NO unnecessary defensive programming.
   No extra features - focus on simplicity.
3. Be concise. Keep README minimal. IMPORTANT: no emojis ever.
4. When hitting issues, always identify root cause before trying a fix. Do not guess. Prove with evidence, then fix the root cause. Test the fix thoroughly.

## Working documentation

All documents for planning and executing this project are in `docs/`.
Review `docs/PLAN.md` before proceeding.

There is a `CLAUDE.md` per layer: this file holds project-wide rules, `backend/CLAUDE.md` maps the
existing backend code, and `frontend/CLAUDE.md` holds frontend conventions once that layer exists.
Keep each one to what is specific to its layer; do not duplicate this file.

## Work process

When implementing the solution, start by reading `docs/PLAN.md`.
Be critical of the plan and suggest improvements if something can be sliced better. The main goal
is structured, well-tested, robust, minor increments.
Never proceed from one step to the next automatically. Wait for review and acceptance first.
Commits to git happen after every step involving code or documentation updates.
Enrich `docs/PLAN.md` as work progresses and check off each step to show progress.
ALWAYS ask questions when in doubt.
Never make any major decision without consulting first.

