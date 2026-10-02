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
`.env`. Stop with `scripts/stop.sh` or `.\scripts\stop.ps1`.

If PowerShell refuses to run the scripts: `Set-ExecutionPolicy -Scope CurrentUser RemoteSigned`.

## More

`CLAUDE.md` lists every command; `docs/PLAN.md` is the build plan and its progress.
