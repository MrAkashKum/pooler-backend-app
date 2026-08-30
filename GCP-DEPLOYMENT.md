# Automatic GCP backend deployment roadmap

The backend is configured for automatic deployment from GitHub to Google Cloud Run. After the one-time connection is completed, pushing backend code to the `main` branch deploys it automatically. You do not need to run the deployment script on your computer for later releases.

## What you need to provide

Send the following details to the person configuring the deployment (or provide them in this Codex task with the GitHub connection installed):

| Required item | Example | Where to find it |
| --- | --- | --- |
| GitHub repository URL | `https://github.com/account/repository` | GitHub repository page |
| GCP project ID | `hoppo-production` | GCP Console → project selector |
| GCP billing status | Enabled | GCP Console → Billing |
| Deployment region | `asia-southeast1` | Choose the region closest to users |
| GitHub production branch | `main` | GitHub repository → Branches |
| Database choice | `h2` now, `mysql` later | Your decision |
| Public or private API | Public for the mobile API | Your decision |

Do not send a GCP password, service-account JSON key, database password, or JWT secret. The workflow uses keyless Workload Identity Federation and creates application secrets in Google Secret Manager.

## How automatic deployment works

1. Backend code is pushed or merged into `main`.
2. GitHub Actions starts `.github/workflows/deploy-backend-gcp.yml` only when backend or workflow files changed.
3. GitHub authenticates to GCP through Workload Identity Federation—no downloaded JSON key is required.
4. GCP builds the backend Dockerfile with Cloud Build.
5. Cloud Run receives a new revision and moves traffic to it after a successful deployment.
6. The deployed URL appears in the GitHub Actions job summary and in GCP Console → Cloud Run.

## One-time Cloud Console setup

These steps require a GCP project owner or administrator. They are performed only once.

### 1. Create or select the GCP project

1. Open [Google Cloud Console](https://console.cloud.google.com/).
2. Use the project selector to create or select the production project.
3. Copy the **Project ID** (not only the display name).
4. Open **Billing** and link an active billing account.

### 2. Enable Google Cloud APIs

Open **APIs & Services → Library** and enable:

- Cloud Run Admin API
- Cloud Build API
- Artifact Registry API
- Secret Manager API
- Service Usage API
- IAM API and IAM Credentials API
- Security Token Service API
- Cloud Resource Manager API
- Cloud SQL Admin API (needed only when MySQL is enabled)

### 3. Create the GitHub deployment service account

1. Open **IAM & Admin → Service Accounts**.
2. Create `github-backend-deployer`.
3. Grant the permissions needed to create and deploy Cloud Run/build/runtime resources:
   - Cloud Run Admin
   - Cloud Run Source Developer
   - Cloud Build Editor
   - Artifact Registry Administrator
   - Secret Manager Admin
   - Service Account Admin
   - Service Account User
   - Service Usage Admin
   - Service Usage Consumer
   - Project IAM Admin
   - Cloud SQL Admin (optional MySQL mode)
4. Do **not** create or download a JSON key.

For stricter production governance, replace these setup-time roles later with a custom least-privilege role after the infrastructure has been created.

### 4. Connect GitHub using Workload Identity Federation

1. Open **IAM & Admin → Workload Identity Federation**.
2. Create a pool named `github-actions`.
3. Add an **OpenID Connect (OIDC)** provider named `github`.
4. Issuer URL: `https://token.actions.githubusercontent.com`.
5. Allowed audience: use the provider's default audience.
6. Attribute mappings:

```text
google.subject=assertion.sub
attribute.repository=assertion.repository
attribute.repository_owner=assertion.repository_owner
```

7. Add this attribute condition, replacing the value with the exact GitHub owner/repository:

```text
assertion.repository == 'YOUR_GITHUB_OWNER/YOUR_REPOSITORY'
```

8. Grant the pool principal access to `github-backend-deployer` with **Workload Identity User**. Restrict the principal to the same repository.
9. Copy the full provider resource name. It looks like:

```text
projects/123456789/locations/global/workloadIdentityPools/github-actions/providers/github
```

### 5. Add GitHub Actions configuration

In GitHub, open **Settings → Secrets and variables → Actions**.

Create repository **secrets**:

| Secret | Value |
| --- | --- |
| `GCP_WORKLOAD_IDENTITY_PROVIDER` | Full provider resource name copied above |
| `GCP_DEPLOY_SERVICE_ACCOUNT` | `github-backend-deployer@YOUR_PROJECT_ID.iam.gserviceaccount.com` |

Create repository **variables**:

| Variable | Required value |
| --- | --- |
| `GCP_PROJECT_ID` | Your exact GCP project ID |
| `GCP_REGION` | `asia-southeast1` or chosen region |
| `GCP_SERVICE_NAME` | `pooler-backend` |
| `DATABASE_MODE` | `h2` initially; change to `mysql` later |

Optional MySQL variables:

| Variable | Default |
| --- | --- |
| `SQL_INSTANCE` | `pooler-mysql` |
| `SQL_DATABASE` | `pooler` |
| `SQL_USER` | `pooler_app` |
| `SQL_TIER` | `db-f1-micro` |

### 6. Start the first deployment

Either merge this workflow into `main`, or open **GitHub → Actions → Deploy backend to GCP → Run workflow**. Select `h2` for the initial deployment.

Track progress in GitHub Actions. When it finishes, open **GCP Console → Cloud Run → pooler-backend**. The API base URL is:

```text
https://YOUR_CLOUD_RUN_URL/pooler-backend
```

Health endpoint:

```text
https://YOUR_CLOUD_RUN_URL/pooler-backend/api/v1/public/health
```

## Database roadmap

### Phase 1: H2 demonstration

Keep the GitHub variable `DATABASE_MODE=h2`. No Cloud SQL instance is created. H2 data is in memory and is lost whenever Cloud Run restarts, scales down, or creates another instance. Do not rely on it for real user data.

### Phase 2: MySQL production

Change `DATABASE_MODE` to `mysql`, then manually run the workflow once or push a backend change. The deployment script creates the Cloud SQL MySQL instance, application database/user, runtime IAM access, and Secret Manager password if they do not already exist.

Cloud SQL is billable. Review its region, tier, backups, high availability, deletion protection, and maintenance settings in GCP Console before accepting production traffic.

### Phase 3: Production hardening

Before launch:

1. Use Flyway or Liquibase migrations and change `DB_DDL_AUTO` from `update` to `validate`.
2. Enable Cloud SQL automated backups and point-in-time recovery.
3. Configure real SMTP credentials through Secret Manager.
4. Set production CORS and WebSocket origins.
5. Add Cloud Run error-rate and latency alerts.
6. Restrict GitHub environment approvals for production.
7. Reduce the deployment account permissions to least privilege.

## Manual fallback

The script remains available for recovery or an initial manual test:

```bash
cd backend/pooler-backend
./scripts/deploy-gcp.sh --project YOUR_PROJECT_ID --database h2
```

Normally this is unnecessary because GitHub Actions calls the same script automatically.
