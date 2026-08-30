#!/usr/bin/env bash
set -Eeuo pipefail

# Prepare an EC2 instance for the Hoppo backend Docker staging deployment.
# It installs Docker, clones/pulls the repository, creates staging env config,
# and optionally builds/runs docker-compose.staging.yml.
#
# Usage:
#   GITHUB_REPO_URL=https://github.com/your-org/HubHop-Pooling-App.git \
#   ./scripts/aws-ec2-docker-staging-setup.sh
#
# Run after filling env:
#   GITHUB_REPO_URL=https://github.com/your-org/HubHop-Pooling-App.git \
#   RUN_CONTAINER=true ./scripts/aws-ec2-docker-staging-setup.sh

APP_USER="${APP_USER:-hoppo}"
APP_ROOT="${APP_ROOT:-/opt/hoppo}"
REPO_DIR="${REPO_DIR:-$APP_ROOT/HubHop-Pooling-App}"
BACKEND_SUBDIR="${BACKEND_SUBDIR:-pooler-backend}"
GITHUB_REPO_URL="${GITHUB_REPO_URL:-}"
GIT_REF="${GIT_REF:-main}"
RUN_CONTAINER="${RUN_CONTAINER:-false}"
ENV_FILE_NAME="${ENV_FILE_NAME:-aws-staging.env}"

SUDO=""
if [[ "${EUID:-$(id -u)}" -ne 0 ]]; then
  SUDO="sudo"
fi

log() { printf '\n==> %s\n' "$*"; }
die() { printf 'ERROR: %s\n' "$*" >&2; exit 1; }
need() { command -v "$1" >/dev/null 2>&1 || die "Required command not found: $1"; }

install_packages() {
  log "Installing Git and Docker"
  if command -v apt-get >/dev/null 2>&1; then
    $SUDO apt-get update
    $SUDO apt-get install -y git docker.io docker-compose-plugin
  elif command -v dnf >/dev/null 2>&1; then
    $SUDO dnf install -y git docker docker-compose-plugin || $SUDO dnf install -y git docker
  elif command -v yum >/dev/null 2>&1; then
    $SUDO yum install -y git docker docker-compose-plugin || $SUDO yum install -y git docker
  else
    die "Unsupported Linux package manager. Install git, docker, and docker compose manually."
  fi

  $SUDO systemctl enable --now docker
  if [[ -n "${SUDO_USER:-}" ]]; then
    $SUDO usermod -aG docker "$SUDO_USER" || true
  fi
}

compose_cmd() {
  if docker compose version >/dev/null 2>&1; then
    printf 'docker compose'
  elif command -v docker-compose >/dev/null 2>&1; then
    printf 'docker-compose'
  else
    die "Docker Compose is not installed. Install docker-compose-plugin or docker-compose."
  fi
}

checkout_code() {
  [[ -n "$GITHUB_REPO_URL" ]] || die "Set GITHUB_REPO_URL to your GitHub repository URL."

  log "Preparing repository at $REPO_DIR"
  $SUDO mkdir -p "$APP_ROOT"
  if [[ ! -d "$REPO_DIR/.git" ]]; then
    $SUDO git clone --branch "$GIT_REF" "$GITHUB_REPO_URL" "$REPO_DIR"
  else
    $SUDO git -C "$REPO_DIR" fetch origin "$GIT_REF"
    $SUDO git -C "$REPO_DIR" checkout "$GIT_REF"
    $SUDO git -C "$REPO_DIR" pull --ff-only origin "$GIT_REF"
  fi
}

create_env_file() {
  local backend_dir="$REPO_DIR/$BACKEND_SUBDIR"
  local env_file="$backend_dir/scripts/$ENV_FILE_NAME"
  local example="$backend_dir/scripts/aws-staging.env.example"

  [[ -f "$backend_dir/docker-compose.staging.yml" ]] || die "Missing docker-compose.staging.yml at $backend_dir"
  [[ -f "$example" ]] || die "Missing env example at $example"

  if [[ ! -f "$env_file" ]]; then
    log "Creating $env_file from example"
    $SUDO cp "$example" "$env_file"
    $SUDO chmod 600 "$env_file"
    if [[ -n "${SUDO_USER:-}" ]]; then
      $SUDO chown "$SUDO_USER":"$SUDO_USER" "$env_file"
    fi
    printf '\nCreated %s. Edit it and replace placeholder secrets before running with RUN_CONTAINER=true.\n' "$env_file"
  else
    log "Using existing $env_file"
  fi
}

validate_env_for_run() {
  local env_file="$1"
  local missing=0
  local required=(
    DB_HOST DB_NAME DB_USER DB_PASS
    JWT_SECRET APP_FRONTEND_BASE_URL CORS_ORIGINS
    MAIL_HOST MAIL_USERNAME MAIL_PASSWORD MAIL_FROM
    PROFILE_MEDIA_S3_BUCKET PROFILE_MEDIA_S3_REGION PROFILE_MEDIA_KEY_PREFIX
  )

  for key in "${required[@]}"; do
    if ! grep -Eq "^${key}=.+[^[:space:]]" "$env_file"; then
      printf 'Missing required value in %s: %s\n' "$env_file" "$key" >&2
      missing=1
    fi
  done

  if grep -Eq 'change-me|replace-with|your-rds-endpoint' "$env_file"; then
    printf 'Placeholder values are still present in %s.\n' "$env_file" >&2
    missing=1
  fi

  [[ "$missing" -eq 0 ]] || die "Fill the staging env file before running the container."
}

run_container() {
  local backend_dir="$REPO_DIR/$BACKEND_SUBDIR"
  local env_file="$backend_dir/scripts/$ENV_FILE_NAME"
  local compose
  compose="$(compose_cmd)"

  validate_env_for_run "$env_file"

  log "Building and starting Hoppo backend staging container"
  cd "$backend_dir"
  # shellcheck disable=SC2086
  $compose --env-file "scripts/$ENV_FILE_NAME" -f docker-compose.staging.yml up -d --build

  log "Container status"
  # shellcheck disable=SC2086
  $compose -f docker-compose.staging.yml ps
}

install_packages
need git
need docker
checkout_code
create_env_file

if [[ "$RUN_CONTAINER" == "true" ]]; then
  run_container
else
  cat <<EOF

Docker staging setup is ready.

Next steps:
1. Edit:
   $REPO_DIR/$BACKEND_SUBDIR/scripts/$ENV_FILE_NAME

2. Run:
   cd $REPO_DIR/$BACKEND_SUBDIR
   RUN_CONTAINER=true GITHUB_REPO_URL=$GITHUB_REPO_URL $0

3. Health check:
   curl http://localhost:8080/pooler-backend/api/v1/public/health

EOF
fi
