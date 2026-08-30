#!/usr/bin/env bash
set -Eeuo pipefail

# Deploy the Spring Boot backend from a Linux server checkout.
#
# Usage:
#   cd /path/to/pooler-backend
#   DEPLOY_BRANCH=main SPRING_PROFILE=prod SERVER_PORT=8080 ./scripts/deploy-server.sh
#
# Optional environment variables:
#   APP_NAME        Default: pooler-backend
#   DEPLOY_BRANCH   Default: main
#   GIT_REMOTE      Default: origin
#   SPRING_PROFILE  Default: prod
#   SERVER_PORT     Default: 8080
#   JAVA_OPTS       Default: -Xms256m -Xmx768m
#   SKIP_TESTS      Default: true
#   HEALTH_URL      Default: http://127.0.0.1:${SERVER_PORT}/pooler-backend/actuator/health
#   LOG_DIR         Default: <project>/logs
#   RUN_DIR         Default: <project>/run
#   PID_FILE        Default: <run-dir>/<app-name>.pid
#   JAR_FILE        Optional explicit jar path

log() {
  printf '[%s] %s\n' "$(date '+%Y-%m-%d %H:%M:%S')" "$*"
}

fail() {
  log "ERROR: $*"
  exit 1
}

require_command() {
  command -v "$1" >/dev/null 2>&1 || fail "Required command not found: $1"
}

is_running() {
  local pid="$1"
  [[ -n "${pid}" ]] && kill -0 "${pid}" >/dev/null 2>&1
}

stop_pid() {
  local pid="$1"
  local wait_seconds="${2:-15}"

  if ! is_running "${pid}"; then
    log "PID ${pid} is not running. Skipping stop."
    return 0
  fi

  log "Stopping existing ${APP_NAME} process. pid=${pid}"
  kill "${pid}" >/dev/null 2>&1 || true

  for _ in $(seq 1 "${wait_seconds}"); do
    if ! is_running "${pid}"; then
      log "Stopped process gracefully. pid=${pid}"
      return 0
    fi
    sleep 1
  done

  log "Process still running after ${wait_seconds}s. Sending force stop. pid=${pid}"
  kill -9 "${pid}" >/dev/null 2>&1 || true
}

collect_existing_pids() {
  local pid
  local pids=()

  if [[ -f "${PID_FILE}" ]]; then
    pid="$(tr -dc '0-9' < "${PID_FILE}" || true)"
    if is_running "${pid}"; then
      pids+=("${pid}")
    else
      log "PID file exists but process is not running. pid=${pid:-none}"
    fi
  fi

  while IFS= read -r pid; do
    [[ -n "${pid}" ]] && pids+=("${pid}")
  done < <(pgrep -f "java .*${APP_NAME}.*\\.jar" || true)

  if (( ${#pids[@]} == 0 )); then
    return 0
  fi

  printf '%s\n' "${pids[@]}" | sort -u
}

find_jar_file() {
  if [[ -n "${JAR_FILE:-}" ]]; then
    [[ -f "${JAR_FILE}" ]] || fail "Configured JAR_FILE does not exist: ${JAR_FILE}"
    printf '%s\n' "${JAR_FILE}"
    return 0
  fi

  local jar
  jar="$(find target -maxdepth 1 -type f -name "${APP_NAME}-*.jar" ! -name "*.original" | sort | tail -n 1)"
  [[ -n "${jar}" && -f "${jar}" ]] || fail "No runnable jar found in target for ${APP_NAME}"
  printf '%s\n' "${jar}"
}

wait_for_health() {
  if ! command -v curl >/dev/null 2>&1; then
    log "curl not found. Skipping health check."
    return 0
  fi

  log "Checking health endpoint: ${HEALTH_URL}"
  for attempt in $(seq 1 "${HEALTH_RETRIES}"); do
    if curl --silent --fail --max-time 3 "${HEALTH_URL}" >/dev/null; then
      log "Health check passed. attempt=${attempt}"
      return 0
    fi
    log "Health check not ready yet. attempt=${attempt}/${HEALTH_RETRIES}"
    sleep "${HEALTH_SLEEP_SECONDS}"
  done

  log "Health check did not pass. Check log file: ${APP_LOG_FILE}"
}

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(cd "${SCRIPT_DIR}/.." && pwd)"

APP_NAME="${APP_NAME:-pooler-backend}"
DEPLOY_BRANCH="${DEPLOY_BRANCH:-main}"
GIT_REMOTE="${GIT_REMOTE:-origin}"
SPRING_PROFILE="${SPRING_PROFILE:-prod}"
SERVER_PORT="${SERVER_PORT:-8080}"
JAVA_OPTS="${JAVA_OPTS:--Xms256m -Xmx768m}"
SKIP_TESTS="${SKIP_TESTS:-true}"
LOG_DIR="${LOG_DIR:-${PROJECT_DIR}/logs}"
RUN_DIR="${RUN_DIR:-${PROJECT_DIR}/run}"
PID_FILE="${PID_FILE:-${RUN_DIR}/${APP_NAME}.pid}"
HEALTH_URL="${HEALTH_URL:-http://127.0.0.1:${SERVER_PORT}/pooler-backend/actuator/health}"
HEALTH_RETRIES="${HEALTH_RETRIES:-20}"
HEALTH_SLEEP_SECONDS="${HEALTH_SLEEP_SECONDS:-2}"
APP_LOG_FILE="${LOG_DIR}/${APP_NAME}.out.log"

require_command git
require_command java

cd "${PROJECT_DIR}"
log "Starting ${APP_NAME} deployment."
log "Project directory: ${PROJECT_DIR}"
log "Deploy branch: ${DEPLOY_BRANCH}"
log "Spring profile: ${SPRING_PROFILE}"
log "Server port: ${SERVER_PORT}"
log "Log file: ${APP_LOG_FILE}"

git rev-parse --is-inside-work-tree >/dev/null 2>&1 || fail "Project directory is not a git repository."

current_branch="$(git rev-parse --abbrev-ref HEAD)"
log "Current branch: ${current_branch}"

if [[ "${current_branch}" != "${DEPLOY_BRANCH}" ]]; then
  log "Switching branch from ${current_branch} to ${DEPLOY_BRANCH}."
  git checkout "${DEPLOY_BRANCH}"
else
  log "Already on ${DEPLOY_BRANCH}."
fi

log "Fetching latest changes from ${GIT_REMOTE}/${DEPLOY_BRANCH}."
git fetch "${GIT_REMOTE}" "${DEPLOY_BRANCH}"

log "Pulling latest code with fast-forward only."
git pull --ff-only "${GIT_REMOTE}" "${DEPLOY_BRANCH}"

log "Preparing backend build."
if [[ "${SKIP_TESTS}" == "true" ]]; then
  log "Running Maven package with tests skipped."
  ./mvnw clean package -DskipTests
else
  log "Running Maven package with tests enabled."
  ./mvnw clean package
fi

JAR_PATH="$(find_jar_file)"
log "Build artifact selected: ${JAR_PATH}"

mkdir -p "${LOG_DIR}" "${RUN_DIR}"

mapfile -t EXISTING_PIDS < <(collect_existing_pids)
if (( ${#EXISTING_PIDS[@]} == 0 )); then
  log "No existing ${APP_NAME} process found."
else
  for pid in "${EXISTING_PIDS[@]}"; do
    stop_pid "${pid}"
  done
fi

read -r -a JAVA_OPTS_ARRAY <<< "${JAVA_OPTS}"

log "Starting ${APP_NAME} in background."
nohup java "${JAVA_OPTS_ARRAY[@]}" \
  -jar "${JAR_PATH}" \
  --spring.profiles.active="${SPRING_PROFILE}" \
  --server.port="${SERVER_PORT}" \
  > "${APP_LOG_FILE}" 2>&1 &

NEW_PID="$!"
printf '%s\n' "${NEW_PID}" > "${PID_FILE}"
disown "${NEW_PID}" >/dev/null 2>&1 || true
log "Started ${APP_NAME}. pid=${NEW_PID}"
log "PID file: ${PID_FILE}"

wait_for_health

log "Deployment finished. You can close the console; the app is running in the background."
