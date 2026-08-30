#!/usr/bin/env bash
set -Eeuo pipefail

# Pull code from GitHub, build the backend, and run the executable WAR/JAR
# as a systemd service on an EC2 instance.
#
# The current backend builds an executable JAR. If the Maven packaging is later
# changed to WAR, this script will prefer target/*.war automatically.
#
# Usage:
#   GITHUB_REPO_URL=https://github.com/your-org/HubHop-Pooling-App.git \
#   ./scripts/aws-ec2-pull-and-run-war.sh

APP_USER="${APP_USER:-hoppo}"
APP_ROOT="${APP_ROOT:-/opt/hoppo}"
SERVICE_NAME="${SERVICE_NAME:-hoppo-pooler-backend-staging}"
REPO_DIR="${REPO_DIR:-$APP_ROOT/HubHop-Pooling-App}"
BACKEND_SUBDIR="${BACKEND_SUBDIR:-pooler-backend}"
GITHUB_REPO_URL="${GITHUB_REPO_URL:-}"
GIT_REF="${GIT_REF:-main}"
ENV_DIR="${ENV_DIR:-/etc/hoppo}"
ENV_FILE="${ENV_FILE:-$ENV_DIR/pooler-backend-staging.env}"
RELEASE_ROOT="${RELEASE_ROOT:-$APP_ROOT/releases/pooler-backend}"
CURRENT_LINK="${CURRENT_LINK:-$APP_ROOT/current/pooler-backend}"

SUDO=""
if [[ "${EUID:-$(id -u)}" -ne 0 ]]; then
  SUDO="sudo"
fi

log() { printf '\n==> %s\n' "$*"; }
die() { printf 'ERROR: %s\n' "$*" >&2; exit 1; }
need() { command -v "$1" >/dev/null 2>&1 || die "Required command not found: $1"; }

install_packages() {
  log "Installing Java 21, Git, and build tools"
  if command -v apt-get >/dev/null 2>&1; then
    $SUDO apt-get update
    $SUDO apt-get install -y git openjdk-21-jdk || $SUDO apt-get install -y git default-jdk
  elif command -v dnf >/dev/null 2>&1; then
    $SUDO dnf install -y git java-21-amazon-corretto-devel || $SUDO dnf install -y git java-21-openjdk-devel
  elif command -v yum >/dev/null 2>&1; then
    $SUDO yum install -y git java-21-amazon-corretto-devel || $SUDO yum install -y git java-21-openjdk-devel
  else
    die "Unsupported Linux package manager. Install Java 21 and git manually."
  fi
}

ensure_user_and_dirs() {
  log "Preparing application user and directories"
  if ! id "$APP_USER" >/dev/null 2>&1; then
    $SUDO useradd --system --create-home --shell /sbin/nologin "$APP_USER"
  fi
  $SUDO mkdir -p "$APP_ROOT" "$RELEASE_ROOT" "$(dirname "$CURRENT_LINK")" "$ENV_DIR" /var/log/hoppo/pooler-backend
  $SUDO chown -R "$APP_USER":"$APP_USER" "$APP_ROOT" /var/log/hoppo
}

checkout_code() {
  [[ -n "$GITHUB_REPO_URL" ]] || die "Set GITHUB_REPO_URL to your GitHub repository URL."

  log "Pulling source from $GITHUB_REPO_URL ($GIT_REF)"
  if [[ ! -d "$REPO_DIR/.git" ]]; then
    $SUDO -u "$APP_USER" git clone --branch "$GIT_REF" "$GITHUB_REPO_URL" "$REPO_DIR"
  else
    $SUDO -u "$APP_USER" git -C "$REPO_DIR" fetch origin "$GIT_REF"
    $SUDO -u "$APP_USER" git -C "$REPO_DIR" checkout "$GIT_REF"
    $SUDO -u "$APP_USER" git -C "$REPO_DIR" pull --ff-only origin "$GIT_REF"
  fi
}

create_env_file() {
  if [[ -f "$ENV_FILE" ]]; then
    log "Using existing $ENV_FILE"
    return
  fi

  log "Creating $ENV_FILE"
  $SUDO tee "$ENV_FILE" >/dev/null <<'EOF'
SPRING_PROFILES_ACTIVE=staging
SERVER_PORT=8080
DB_DDL_AUTO=validate

DB_HOST=your-rds-endpoint.ap-southeast-1.rds.amazonaws.com
DB_PORT=3306
DB_NAME=pooler_staging
DB_USER=pooler_staging
DB_PASS=change-me
DB_USE_SSL=true
DB_ALLOW_PUBLIC_KEY_RETRIEVAL=false

JWT_SECRET=replace-with-strong-base64-secret
APP_FRONTEND_BASE_URL=https://staging.hoppo.app
CORS_ORIGINS=https://staging.hoppo.app

MAIL_HOST=email-smtp.ap-southeast-1.amazonaws.com
MAIL_PORT=587
MAIL_USERNAME=change-me
MAIL_PASSWORD=change-me
MAIL_FROM=noreply@hoppo.app
MAIL_FROM_NAME=Hoppo Staging

PROFILE_MEDIA_S3_BUCKET=hoppo-profile-media-staging
PROFILE_MEDIA_S3_REGION=ap-southeast-1
PROFILE_MEDIA_KEY_PREFIX=hoppo/profile-media/staging
PROFILE_MEDIA_PUBLIC_BASE_URL=https://staging-media.hoppo.app
PROFILE_MEDIA_MAX_SIZE_MB=5

LOG_PATH=/var/log/hoppo/pooler-backend
APP_LOG_LEVEL=INFO
JAVA_OPTS=-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -Dspring.profiles.active=staging
EOF
  $SUDO chmod 600 "$ENV_FILE"
  printf '\nCreated %s. Edit it and replace placeholder secrets, then rerun this script.\n' "$ENV_FILE"
}

validate_env() {
  local missing=0
  local required=(
    DB_HOST DB_NAME DB_USER DB_PASS
    JWT_SECRET APP_FRONTEND_BASE_URL CORS_ORIGINS
    MAIL_HOST MAIL_USERNAME MAIL_PASSWORD MAIL_FROM
    PROFILE_MEDIA_S3_BUCKET PROFILE_MEDIA_S3_REGION PROFILE_MEDIA_KEY_PREFIX
  )

  for key in "${required[@]}"; do
    if ! $SUDO grep -Eq "^${key}=.+[^[:space:]]" "$ENV_FILE"; then
      printf 'Missing required value in %s: %s\n' "$ENV_FILE" "$key" >&2
      missing=1
    fi
  done

  if $SUDO grep -Eq 'change-me|replace-with|your-rds-endpoint' "$ENV_FILE"; then
    printf 'Placeholder values are still present in %s.\n' "$ENV_FILE" >&2
    missing=1
  fi

  [[ "$missing" -eq 0 ]] || die "Fill $ENV_FILE before starting the service."
}

build_artifact() {
  local backend_dir="$REPO_DIR/$BACKEND_SUBDIR"
  [[ -f "$backend_dir/pom.xml" ]] || die "Backend pom.xml not found at $backend_dir"

  log "Building backend artifact"
  $SUDO chmod +x "$backend_dir/mvnw"
  $SUDO -u "$APP_USER" bash -lc "cd '$backend_dir' && ./mvnw clean package -DskipTests"

  local artifact=""
  artifact="$(find "$backend_dir/target" -maxdepth 1 -type f -name '*.war' | sort | head -n 1)"
  if [[ -z "$artifact" ]]; then
    artifact="$(find "$backend_dir/target" -maxdepth 1 -type f -name '*.jar' \
      ! -name '*sources.jar' ! -name '*javadoc.jar' | sort | head -n 1)"
  fi
  [[ -n "$artifact" ]] || die "No executable WAR/JAR found under $backend_dir/target"

  local release_dir="$RELEASE_ROOT/$(date +%Y%m%d%H%M%S)"
  local extension="${artifact##*.}"
  $SUDO mkdir -p "$release_dir"
  $SUDO cp "$artifact" "$release_dir/app.$extension"
  $SUDO chown -R "$APP_USER":"$APP_USER" "$release_dir"
  $SUDO ln -sfn "$release_dir" "$CURRENT_LINK"
}

write_systemd_service() {
  log "Writing systemd service $SERVICE_NAME"
  $SUDO tee "/etc/systemd/system/$SERVICE_NAME.service" >/dev/null <<EOF
[Unit]
Description=Hoppo Pooler Backend Staging
After=network-online.target
Wants=network-online.target

[Service]
Type=simple
User=$APP_USER
Group=$APP_USER
WorkingDirectory=$CURRENT_LINK
EnvironmentFile=$ENV_FILE
ExecStart=/bin/sh -c 'exec /usr/bin/java \${JAVA_OPTS:-"-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -Dspring.profiles.active=staging"} -jar $CURRENT_LINK/app.jar'
Restart=always
RestartSec=10
SuccessExitStatus=143
StandardOutput=journal
StandardError=journal

[Install]
WantedBy=multi-user.target
EOF

  if [[ -f "$CURRENT_LINK/app.war" ]]; then
    $SUDO sed -i "s|$CURRENT_LINK/app.jar|$CURRENT_LINK/app.war|g" "/etc/systemd/system/$SERVICE_NAME.service"
  fi

  $SUDO systemctl daemon-reload
  $SUDO systemctl enable "$SERVICE_NAME"
}

start_service() {
  log "Starting $SERVICE_NAME"
  $SUDO systemctl restart "$SERVICE_NAME"
  $SUDO systemctl --no-pager --full status "$SERVICE_NAME" || true

  cat <<EOF

Health check:
  curl http://localhost:8080/pooler-backend/api/v1/public/health

Logs:
  sudo journalctl -u $SERVICE_NAME -f

EOF
}

install_packages
ensure_user_and_dirs
need git
checkout_code
create_env_file
validate_env
build_artifact
write_systemd_service
start_service
