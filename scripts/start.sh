#!/bin/sh
# Builds and starts the stack, returning once every service is healthy. Works from any directory.
exec docker compose --project-directory "$(dirname "$0")/.." up -d --build --wait
