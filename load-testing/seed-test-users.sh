#!/usr/bin/env bash
# Seed test accounts for the Replime backend load test.
# Idempotent: safe to re-run. Accounts that already exist are skipped (signup returns
# an error which this script ignores).
#
# Usage:
#   ./seed-test-users.sh http://localhost:8080/api/v1
#
# Files expected in the same directory: load-users.csv, admin-users.csv

set -euo pipefail

BASE_URL="${1:-http://localhost:8080/api/v1}"
DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

signup_one() {
  local name="$1" email="$2" password="$3"
  local code
  code=$(curl -s -o /tmp/replime_signup_resp.json -w "%{http_code}" \
    -X POST "${BASE_URL}/auth/signup" \
    -H "Content-Type: application/json" \
    -d "{\"name\":\"${name}\",\"email\":\"${email}\",\"password\":\"${password}\"}")
  if [[ "$code" == "200" ]]; then
    echo "  created: ${email}"
  else
    echo "  skipped (already exists or error, HTTP ${code}): ${email}"
  fi
}

echo "== Seeding regular USER accounts (load-users.csv) against ${BASE_URL} =="
tail -n +2 "${DIR}/load-users.csv" | while IFS=',' read -r name email password; do
  [[ -z "$name" ]] && continue
  signup_one "$name" "$email" "$password"
done

echo ""
echo "== Seeding the ADMIN account (admin-users.csv) =="
echo "   Note: /auth/signup/admin only succeeds ONCE per environment (first admin wins)."
echo "   If an admin already exists, edit admin-users.csv to match its real credentials."
tail -n +2 "${DIR}/admin-users.csv" | while IFS=',' read -r name email password; do
  [[ -z "$name" ]] && continue
  code=$(curl -s -o /tmp/replime_admin_resp.json -w "%{http_code}" \
    -X POST "${BASE_URL}/auth/signup/admin" \
    -H "Content-Type: application/json" \
    -d "{\"name\":\"${name}\",\"email\":\"${email}\",\"password\":\"${password}\"}")
  if [[ "$code" == "200" ]]; then
    echo "  created admin: ${email}"
  else
    echo "  skipped (admin already exists or error, HTTP ${code}): ${email}"
  fi
done

echo ""
echo "== Influencer accounts (influencer-users.csv) =="
echo "   NOT auto-seeded: the INFLUENCER role can only be granted through the real"
echo "   /influencer/verify/request + /confirm flow, which calls the live YouTube API"
echo "   for a real channel. Manually verify at least one test account per environment"
echo "   (or ask a teammate with DB access to promote a couple of test users directly),"
echo "   then put its real credentials into influencer-users.csv before running the"
echo "   Influencer_Chatbot_Flow thread group. Until then, disable that thread group."

echo ""
echo "Done."
