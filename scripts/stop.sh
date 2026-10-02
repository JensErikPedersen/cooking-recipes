#!/bin/sh
# Stops and removes the stack's containers. Data volumes are kept.
exec docker compose --project-directory "$(dirname "$0")/.." down
