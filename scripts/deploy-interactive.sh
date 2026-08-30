#!/usr/bin/env bash
set -Eeuo pipefail

# Simple interactive backend deployment script for server use.
# It asks for the branch and Spring profile, then runs deploy-server.sh.

print_line() {
  echo "------------------------------------------------------------"
}

ask_with_default() {
  local label="$1"
  local default_value="$2"
  local answer

  read -r -p "${label} [${default_value}]: " answer
  echo "${answer:-${default_value}}"
}

ask_yes_no() {
  local label="$1"
  local default_value="$2"
  local answer

  read -r -p "${label} [${default_value}]: " answer
  echo "${answer:-${default_value}}"
}

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
DEPLOY_SCRIPT="${SCRIPT_DIR}/deploy-server.sh"

if [[ ! -x "${DEPLOY_SCRIPT}" ]]; then
  echo "ERROR: deploy-server.sh is missing or not executable: ${DEPLOY_SCRIPT}"
  exit 1
fi

print_line
echo "Hoppo backend deployment"
echo "This will pull code, build the jar, stop the old process, and start the new one in background."
echo "After deployment starts, logout or Ctrl+C will not stop the backend process."
print_line

DEPLOY_BRANCH="$(ask_with_default "Enter branch name" "main")"
SPRING_PROFILE="$(ask_with_default "Enter Spring profile" "prod")"
SERVER_PORT="$(ask_with_default "Enter server port" "8080")"
SKIP_TESTS="$(ask_with_default "Skip tests during build? true/false" "true")"

print_line
echo "Deployment summary"
echo "Branch         : ${DEPLOY_BRANCH}"
echo "Spring profile : ${SPRING_PROFILE}"
echo "Server port    : ${SERVER_PORT}"
echo "Skip tests     : ${SKIP_TESTS}"
print_line

CONFIRM="$(ask_yes_no "Continue deployment? y/n" "y")"
if [[ "${CONFIRM}" != "y" && "${CONFIRM}" != "Y" ]]; then
  echo "Deployment cancelled."
  exit 0
fi

echo "Starting deployment..."
export DEPLOY_BRANCH
export SPRING_PROFILE
export SERVER_PORT
export SKIP_TESTS

"${DEPLOY_SCRIPT}"

print_line
echo "Deployment command finished. Backend is running in background if startup succeeded."
echo "You can safely logout from the server console."
print_line
