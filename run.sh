#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"
if [ -f .env ]; then
  set -a
  source .env
  set +a
fi
app_jar=target/know-engine-1.0.0-SNAPSHOT.jar
if [ ! -f "$app_jar" ]; then
  ./build.sh
fi
exec java -jar "$app_jar" "$@"
