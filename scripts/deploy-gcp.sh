#!/usr/bin/env bash
set -Eeuo pipefail

# Deploy this Spring Boot backend to Google Cloud Run. A GitHub URL may be
# supplied to clone and deploy another copy of the backend repository.

PROJECT_ID="${GCP_PROJECT_ID:-}"
REGION="${GCP_REGION:-asia-southeast1}"
SERVICE_NAME="${GCP_SERVICE_NAME:-pooler-backend}"
REPOSITORY_URL="${GITHUB_URL:-}"
REPOSITORY_REF="${GITHUB_REF:-main}"
BACKEND_DIR="${BACKEND_DIR:-backend/pooler-backend}"
DATABASE="${DATABASE:-h2}"
SQL_INSTANCE="${SQL_INSTANCE:-pooler-mysql}"
SQL_DATABASE="${SQL_DATABASE:-pooler}"
SQL_USER="${SQL_USER:-pooler_app}"
SQL_TIER="${SQL_TIER:-db-f1-micro}"
ALLOW_UNAUTHENTICATED="${ALLOW_UNAUTHENTICATED:-true}"
MIN_INSTANCES="${MIN_INSTANCES:-0}"
MAX_INSTANCES="${MAX_INSTANCES:-3}"
RUNTIME_SERVICE_ACCOUNT="${RUNTIME_SERVICE_ACCOUNT:-pooler-cloud-run}"
JWT_SECRET_NAME="${JWT_SECRET_NAME:-pooler-jwt-secret}"
DB_SECRET_NAME="${DB_SECRET_NAME:-pooler-db-password}"
RESEND_SECRET_NAME="${RESEND_SECRET_NAME:-pooler-resend-api-key}"
PROFILE_MEDIA_BUCKET="${PROFILE_MEDIA_BUCKET:-}"
PROFILE_MEDIA_PRIVATE_BUCKET="${PROFILE_MEDIA_PRIVATE_BUCKET:-}"
SOURCE_DIR=""
TEMP_DIR=""

usage() {
  sed -n '2,120p' "$0" | sed -n '/^# Usage:/,/^$/p' | sed 's/^# \{0,1\}//'
}

# Usage:
#   ./scripts/deploy-gcp.sh --project PROJECT_ID [options]
#
# Options:
#   --github-url URL       Clone this GitHub repository before deploying
#   --github-ref REF       Branch/tag to clone (default: main)
#   --backend-dir PATH     Backend path inside cloned repo (default: backend/pooler-backend)
#   --database h2|mysql    H2 (default) or create/use Cloud SQL for MySQL
#   --region REGION        GCP region (default: asia-southeast1)
#   --service NAME         Cloud Run service name (default: pooler-backend)
#   --sql-instance NAME    Cloud SQL instance name (default: pooler-mysql)
#   --sql-database NAME    MySQL database name (default: pooler)
#   --sql-user USER        MySQL application user (default: pooler_app)
#   --private              Require authentication to invoke the service
#   --help                 Show this help

die() { printf 'Error: %s\n' "$*" >&2; exit 1; }
log() { printf '\n==> %s\n' "$*"; }
need() { command -v "$1" >/dev/null 2>&1 || die "Required command not found: $1"; }
cleanup() { [[ -z "$TEMP_DIR" ]] || rm -rf -- "$TEMP_DIR"; }
trap cleanup EXIT

while (($#)); do
  case "$1" in
    --project) PROJECT_ID="${2:?Missing project id}"; shift 2 ;;
    --region) REGION="${2:?Missing region}"; shift 2 ;;
    --service) SERVICE_NAME="${2:?Missing service name}"; shift 2 ;;
    --github-url) REPOSITORY_URL="${2:?Missing GitHub URL}"; shift 2 ;;
    --github-ref) REPOSITORY_REF="${2:?Missing Git ref}"; shift 2 ;;
    --backend-dir) BACKEND_DIR="${2:?Missing backend directory}"; shift 2 ;;
    --database) DATABASE="${2:?Missing database type}"; shift 2 ;;
    --sql-instance) SQL_INSTANCE="${2:?Missing SQL instance name}"; shift 2 ;;
    --sql-database) SQL_DATABASE="${2:?Missing SQL database name}"; shift 2 ;;
    --sql-user) SQL_USER="${2:?Missing SQL user}"; shift 2 ;;
    --private) ALLOW_UNAUTHENTICATED=false; shift ;;
    --help|-h) usage; exit 0 ;;
    *) die "Unknown option: $1 (use --help)" ;;
  esac
done

[[ -n "$PROJECT_ID" ]] || die "--project or GCP_PROJECT_ID is required"
[[ "$DATABASE" == h2 || "$DATABASE" == mysql ]] || die "--database must be h2 or mysql"
PROFILE_MEDIA_BUCKET="${PROFILE_MEDIA_BUCKET:-$PROJECT_ID-profile-media}"
PROFILE_MEDIA_PRIVATE_BUCKET="${PROFILE_MEDIA_PRIVATE_BUCKET:-$PROJECT_ID-private-media}"
need gcloud
need openssl

if [[ -n "$REPOSITORY_URL" ]]; then
  need git
  TEMP_DIR="$(mktemp -d)"
  log "Cloning $REPOSITORY_URL ($REPOSITORY_REF)"
  git clone --depth 1 --branch "$REPOSITORY_REF" "$REPOSITORY_URL" "$TEMP_DIR/repository"
  SOURCE_DIR="$TEMP_DIR/repository/$BACKEND_DIR"
else
  SOURCE_DIR="$(cd "$(dirname "$0")/.." && pwd)"
fi

[[ -f "$SOURCE_DIR/Dockerfile" && -f "$SOURCE_DIR/pom.xml" ]] || \
  die "No Spring backend found at $SOURCE_DIR; adjust --backend-dir"

log "Selecting project and enabling required APIs"
gcloud config set project "$PROJECT_ID" >/dev/null
gcloud services enable run.googleapis.com cloudbuild.googleapis.com artifactregistry.googleapis.com \
  secretmanager.googleapis.com sqladmin.googleapis.com storage.googleapis.com iamcredentials.googleapis.com

# Cloud Build uses the project's default compute service account on newer GCP
# projects. It needs the managed Cloud Run Builder role for source deployments.
PROJECT_NUMBER="$(gcloud projects describe "$PROJECT_ID" --format='value(projectNumber)')"
BUILD_SA_EMAIL="$PROJECT_NUMBER-compute@developer.gserviceaccount.com"
gcloud projects add-iam-policy-binding "$PROJECT_ID" \
  --member="serviceAccount:$BUILD_SA_EMAIL" --role=roles/run.builder >/dev/null

RUNTIME_SA_EMAIL="$RUNTIME_SERVICE_ACCOUNT@$PROJECT_ID.iam.gserviceaccount.com"
if ! gcloud iam service-accounts describe "$RUNTIME_SA_EMAIL" >/dev/null 2>&1; then
  gcloud iam service-accounts create "$RUNTIME_SERVICE_ACCOUNT" \
    --display-name="Pooler Cloud Run runtime"
fi

log "Creating Cloud Storage buckets for profile media"
for bucket in "$PROFILE_MEDIA_BUCKET" "$PROFILE_MEDIA_PRIVATE_BUCKET"; do
  if ! gcloud storage buckets describe "gs://$bucket" >/dev/null 2>&1; then
    gcloud storage buckets create "gs://$bucket" --location="$REGION" --uniform-bucket-level-access
  fi
done
gcloud storage buckets add-iam-policy-binding "gs://$PROFILE_MEDIA_BUCKET" \
  --member=allUsers --role=roles/storage.objectViewer >/dev/null
gcloud storage buckets add-iam-policy-binding "gs://$PROFILE_MEDIA_BUCKET" \
  --member="serviceAccount:$RUNTIME_SA_EMAIL" --role=roles/storage.objectAdmin >/dev/null
gcloud storage buckets add-iam-policy-binding "gs://$PROFILE_MEDIA_PRIVATE_BUCKET" \
  --member="serviceAccount:$RUNTIME_SA_EMAIL" --role=roles/storage.objectAdmin >/dev/null
gcloud iam service-accounts add-iam-policy-binding "$RUNTIME_SA_EMAIL" \
  --member="serviceAccount:$RUNTIME_SA_EMAIL" --role=roles/iam.serviceAccountTokenCreator >/dev/null

ensure_secret() {
  local secret_name="$1" secret_value="$2"
  if gcloud secrets describe "$secret_name" >/dev/null 2>&1; then
    printf '%s' "$secret_value" | gcloud secrets versions add "$secret_name" --data-file=- >/dev/null
  else
    printf '%s' "$secret_value" | gcloud secrets create "$secret_name" --replication-policy=automatic --data-file=- >/dev/null
  fi
  gcloud secrets add-iam-policy-binding "$secret_name" \
    --member="serviceAccount:$RUNTIME_SA_EMAIL" \
    --role=roles/secretmanager.secretAccessor >/dev/null
}

if ! gcloud secrets describe "$JWT_SECRET_NAME" >/dev/null 2>&1; then
  log "Creating JWT secret"
  ensure_secret "$JWT_SECRET_NAME" "$(openssl rand -hex 32)"
else
  gcloud secrets add-iam-policy-binding "$JWT_SECRET_NAME" \
    --member="serviceAccount:$RUNTIME_SA_EMAIL" \
    --role=roles/secretmanager.secretAccessor >/dev/null
fi

ENV_VARS="SPRING_PROFILES_ACTIVE=prod,DB_DDL_AUTO=update,PROFILE_MEDIA_BUCKET=$PROFILE_MEDIA_BUCKET,PROFILE_MEDIA_PRIVATE_BUCKET=$PROFILE_MEDIA_PRIVATE_BUCKET,PROFILE_MEDIA_PUBLIC_BASE_URL=https://storage.googleapis.com/$PROFILE_MEDIA_BUCKET"
SECRET_VARS="JWT_SECRET=$JWT_SECRET_NAME:latest"
if gcloud secrets describe "$RESEND_SECRET_NAME" >/dev/null 2>&1; then
  gcloud secrets add-iam-policy-binding "$RESEND_SECRET_NAME" \
    --member="serviceAccount:$RUNTIME_SA_EMAIL" \
    --role=roles/secretmanager.secretAccessor >/dev/null
  ENV_VARS+=",MAIL_HOST=smtp.resend.com,MAIL_PORT=587,MAIL_USERNAME=resend,MAIL_FROM=noreply@athlenaa.com,MAIL_FROM_NAME=Hoppo Auth"
  SECRET_VARS+=",MAIL_PASSWORD=$RESEND_SECRET_NAME:latest"
fi
if [[ "$DATABASE" == mysql ]]; then
  log "Provisioning Cloud SQL for MySQL"
  if ! gcloud sql instances describe "$SQL_INSTANCE" >/dev/null 2>&1; then
    gcloud sql instances create "$SQL_INSTANCE" --database-version=MYSQL_8_0 \
      --tier="$SQL_TIER" --region="$REGION" --availability-type=zonal
  fi
  gcloud sql databases describe "$SQL_DATABASE" --instance="$SQL_INSTANCE" >/dev/null 2>&1 || \
    gcloud sql databases create "$SQL_DATABASE" --instance="$SQL_INSTANCE"

  DB_PASSWORD="$(openssl rand -base64 36 | tr -d '/+=' | head -c 32)"
  if gcloud sql users list --instance="$SQL_INSTANCE" --filter="name=$SQL_USER" --format='value(name)' | grep -qx "$SQL_USER"; then
    gcloud sql users set-password "$SQL_USER" --instance="$SQL_INSTANCE" --password="$DB_PASSWORD"
  else
    gcloud sql users create "$SQL_USER" --instance="$SQL_INSTANCE" --password="$DB_PASSWORD"
  fi
  ensure_secret "$DB_SECRET_NAME" "$DB_PASSWORD"
  CONNECTION_NAME="$(gcloud sql instances describe "$SQL_INSTANCE" --format='value(connectionName)')"
  gcloud projects add-iam-policy-binding "$PROJECT_ID" \
    --member="serviceAccount:$RUNTIME_SA_EMAIL" --role=roles/cloudsql.client >/dev/null
  ENV_VARS+=",DB_URL=jdbc:mysql:///$SQL_DATABASE?cloudSqlInstance=$CONNECTION_NAME&socketFactory=com.google.cloud.sql.mysql.SocketFactory&cloudSqlRefreshStrategy=lazy&useSSL=false,DB_DRIVER=com.mysql.cj.jdbc.Driver,DB_USER=$SQL_USER,DB_DIALECT=org.hibernate.dialect.MySQLDialect"
  SECRET_VARS+=",DB_PASS=$DB_SECRET_NAME:latest"
else
  log "Using H2 (ephemeral in-memory database)"
  ENV_VARS+=",DB_URL=jdbc:h2:mem:poolerdb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE;MODE=MySQL,DB_DRIVER=org.h2.Driver,DB_USER=sa,DB_DIALECT=org.hibernate.dialect.H2Dialect"
fi

AUTH_FLAG=(--allow-unauthenticated)
[[ "$ALLOW_UNAUTHENTICATED" == true ]] || AUTH_FLAG=(--no-allow-unauthenticated)

log "Deploying $SERVICE_NAME to Cloud Run"
gcloud run deploy "$SERVICE_NAME" \
  --source "$SOURCE_DIR" \
  --region "$REGION" \
  --service-account "$RUNTIME_SA_EMAIL" \
  --set-env-vars "$ENV_VARS" \
  --set-secrets "$SECRET_VARS" \
  --min-instances "$MIN_INSTANCES" \
  --max-instances "$MAX_INSTANCES" \
  --memory 2Gi \
  --cpu 1 \
  "${AUTH_FLAG[@]}" \
  --quiet

SERVICE_URL="$(gcloud run services describe "$SERVICE_NAME" --region "$REGION" --format='value(status.url)')"
printf '\nDeployment complete: %s/pooler-backend\n' "$SERVICE_URL"
if [[ "$DATABASE" == h2 ]]; then
  printf 'Note: H2 data is reset whenever the Cloud Run instance restarts. Use --database mysql for persistent data.\n'
fi
