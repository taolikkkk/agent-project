#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"
npm ci --prefix frontend --no-audit --no-fund
npm run build --prefix frontend
./mvnw -B -ntp package "$@"
