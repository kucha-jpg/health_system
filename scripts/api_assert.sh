#!/usr/bin/env bash
set -euo pipefail

BASE_URL="${BASE_URL:-http://127.0.0.1:9090/api}"
ASSERT_ONLY=1 BASE_URL="$BASE_URL" "$(dirname "$0")/api_test.sh"
