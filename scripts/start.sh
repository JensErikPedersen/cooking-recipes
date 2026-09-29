#!/bin/sh
# Builds and starts the stack in the background. Works from any directory.
exec docker compose --project-directory "$(dirname "$0")/.." up -d --build
