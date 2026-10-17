# Cooking Recipes

A recipe web app: Next.js frontend, Spring Boot API and MySQL, run together with Docker Compose.

## Prerequisites

Docker Desktop, or Docker Engine with the Compose plugin.

## Run

```bash
cp .env.example .env      # PowerShell: copy .env.example .env - then replace the placeholders
scripts/start.sh          # PowerShell: .\scripts\start.ps1
```

Open http://localhost:3000 and sign in with `APP_ADMIN_USERNAME` and `APP_ADMIN_PASSWORD` from
`.env`, or create an account of your own from the sign-in page. Stop with `scripts/stop.sh` or
`.\scripts\stop.ps1`.

If PowerShell refuses to run the scripts: `Set-ExecutionPolicy -Scope CurrentUser RemoteSigned`.

Mail the app sends is caught, not delivered: read it on http://localhost:8025.

The database accounts are created only on a fresh database volume. A database from before
version 4 has one account where there are now two, and the backend stops with
`Access denied for user 'recipesadmin'`: add `DB_ADMIN_USERNAME` and `DB_ADMIN_PASSWORD` to `.env`
as `.env.example` shows, then `docker compose down -v` - which deletes all data, accounts included -
and start again.

## More

`CLAUDE.md` lists every command; `docs/PLAN_V6.md` is the current plan and its progress.
`docs/PLAN.md` (the MVP) and `docs/PLAN_V2.md` to `docs/PLAN_V5.md` are finished.
