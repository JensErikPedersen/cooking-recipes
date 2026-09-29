# Builds and starts the stack, returning once every service is healthy. Works from any directory.
docker compose --project-directory "$PSScriptRoot/.." up -d --build --wait
