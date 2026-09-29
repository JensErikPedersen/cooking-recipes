# Builds and starts the stack in the background. Works from any directory.
docker compose --project-directory "$PSScriptRoot/.." up -d --build
