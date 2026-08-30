# Pooler / Hoppo backend

Spring Boot REST backend for Hoppo’s identity, discovery, fair Common Point, invitation, short-lived chat, live meetup, arrival, and ride-history flows.

## Run locally

Requirements: Java 21+.

```bash
./mvnw spring-boot:run
```

Default base URL: `http://localhost:8888/pooler-backend`

- Health: `/api/v1/public/health`
- Swagger: `/swagger-ui/index.html`
- OpenAPI: `/v3/api-docs`
- H2 console: `/h2-console`

The development profile uses an in-memory H2 database. Create local accounts through signup and email verification; do not commit seeded credentials or enable `ddl-auto=create-drop` in production.

## Architecture

Controllers return a consistent `ApiResponse<T>` envelope. Domain logic lives in service interfaces/implementations; repositories contain persistence queries; entities share audited timestamps through `BaseEntity`. Mobile authorization is stateless JWT plus an optional session token for sensitive actions.

Core modules:

- Auth, refresh/session tokens, Google ID-token exchange.
- Profile, rider gender, match preference, contacts, saved locations, Telegram identity.
- Discovery pings, nearby rider search with mutual `MALE`/`FEMALE`/`ANY` preference filtering, distance/midpoint/route compatibility.
- Invitation and ride state machines.
- Two-hour invitation chat, messages, read receipts, reactions, search, cleanup.
- Participant-only live location and independent physical-arrival confirmation.

## Mobile headers

| Header | Purpose |
| --- | --- |
| `Authorization` | `Bearer <accessToken>` |
| `X-Session-Token` | Required by `@ValidSession` operations |
| `X-Device-Id` | Stable app-install identifier |
| `X-Platform` | `IOS`, `ANDROID`, or `WEB` |
| `X-App-Version` | Semantic app version |
| `X-Correlation-ID` | Optional request trace ID |

## Admin monitoring with Actuator

Spring Boot Actuator is enabled for backend monitoring only. These endpoints are not integrated with the mobile frontend and must be called from Postman, a server-side monitoring tool, or an admin operations console.

Security contract:

- Base URL: `http://localhost:8888/pooler-backend`
- Required header: `Authorization: Bearer <adminAccessToken>`
- Required role: `ROLE_ADMIN`
- Anonymous requests return `401`.
- Logged-in non-admin users return `403`.

Exposed endpoints:

| Method | Path | Purpose |
| --- | --- | --- |
| GET | `/actuator/health` | Service health and component status |
| GET | `/actuator/info` | Application info |
| GET | `/actuator/metrics` | Metric names available in this runtime |
| GET | `/actuator/metrics/{metricName}` | One metric, for example `http.server.requests` |

`/actuator/health` may return HTTP `503` when a monitored dependency is down, for example SMTP or the database. That is expected monitoring behavior, not an auth failure. Auth failures still return `401` or `403` with the normal JSON security envelope.

Postman example:

```http
GET http://localhost:8888/pooler-backend/actuator/health
Authorization: Bearer <adminAccessToken>
```

Keep `management.endpoints.web.exposure.include=health,info,metrics` in properties unless operations explicitly needs more. Do not expose `env`, `beans`, `heapdump`, `threaddump`, or similar sensitive actuator endpoints publicly.

## Production trace IDs

Every backend request uses a trace id for log search:

- If the mobile app sends `X-Correlation-ID`, the backend reuses it.
- If the header is missing, the backend generates a UUID.
- The same id is returned in the `X-Correlation-ID` response header.
- Log lines include it as `trace=<id>`.

Every error response also receives a unique support reference:

```json
{
  "success": false,
  "errorCode": "AUTH-001",
  "traceId": "mobile-trace-0001",
  "errorReferenceId": "ERR-20260717-123456789012",
  "message": "Invalid email or password",
  "path": "/api/v1/auth/login"
}
```

Use `errorReferenceId` when a user reports a failed action. Search server logs for `errorReferenceId=<value>`, `errorRef=<value>`, or the response header `X-Error-Reference-ID`. Use `traceId` to see the full request lifecycle around that issue, including async mail/audit logs started by the request. These ids are random/technical only and must not contain personal information.

## Rider preference contract

`GET/PUT /api/v1/users/me` exposes safety profile fields:

| Field | Values | Default | Purpose |
| --- | --- | --- | --- |
| `gender` | `UNKNOWN`, `MALE`, `FEMALE`, `OTHER` | `UNKNOWN` | The rider's own profile category for mutual filtering. First-time setup must use an explicit rider choice, not an auto-selected default. |
| `matchPreference` | `ANY`, `MALE`, `FEMALE` | `ANY` | Who the rider wants to see in discovery. |
| `emergencyContactName` | string, max 120 | `null` | Trusted family/contact label displayed in Safety centre. |
| `emergencyContactPhone` | string, max 32 | `null` | Phone number used by the mobile app for dialler/SMS handoff. |
| `emergencyMessage` | string, max 300 | `null` | Default message text used in the mobile SMS composer. |

`POST /api/v1/discovery/nearby` filters candidates before returning them. A candidate is shown only when the requester's preference allows the candidate's gender and the candidate's preference allows the requester's gender. This keeps the default broad while giving riders, especially women, a safety control for who appears in search.

Emergency contact fields are stored only as profile metadata. The backend does not call or text the contact; mobile clients compose the call/SMS locally and include device location at the moment the rider taps the safety action.

## Profile media contract

`POST /api/v1/users/me/media?purpose=PROFILE_PHOTO` and `POST /api/v1/users/me/media?purpose=PAYMENT_QR` accept `multipart/form-data` with a `file` part. Files must be JPEG, PNG, or WebP and no larger than `profile-media.max-size-mb` (default 5 MB in each profile properties file).

The backend uploads the object to S3 using AWS SDK default credentials and stores the resulting public URL on the user:

- `profilePictureUrl`: optional account/profile photo
- `paymentQrCodeUrl`: optional payment QR for manual sharing after a match

Media URLs are intentionally not accepted by `PUT /api/v1/users/me`. A user can update these fields only through the multipart upload endpoint, so profile photo and payment QR URLs always come from the configured S3 or CDN media path.

Configuration lives in the Spring profile properties files:

```properties
profile-media.s3-bucket=your-hoppo-profile-media-bucket
profile-media.s3-region=ap-southeast-1
profile-media.key-prefix=hoppo/profile-media/prod
profile-media.public-base-url=https://cdn.yourdomain.com
profile-media.max-size-mb=5
```

Development keeps `profile-media.s3-bucket` blank so accidental local uploads fail clearly. Staging and production can resolve the same keys from deployment-provided Spring placeholders. The S3 bucket or CloudFront distribution must allow read access for returned profile-photo URLs. Hoppo does not expose payment QR during discovery. The owner may explicitly share it with the matched rider from `CAB_DISPATCHED` through the active journey, with a two-hour settlement window after completion. Missing QR setup is optional and never blocks matching, chat, booking, ride progress, or fare calculation. Ride-scoped access is revocable and produces short-lived S3 URLs.

AWS mail and S3 production setup is documented in [AWS-MAIL-S3-INTEGRATION.md](../docs/AWS-MAIL-S3-INTEGRATION.md).

## AWS EC2 staging deployment

EC2 staging setup is documented in [AWS-EC2-STAGING-DEPLOYMENT.md](../docs/AWS-EC2-STAGING-DEPLOYMENT.md).
The combined staging/production server design is documented in
[AWS-JAVA-STAGING-PRODUCTION.md](../docs/AWS-JAVA-STAGING-PRODUCTION.md).

Available backend scripts:

| Script | Purpose |
| --- | --- |
| `scripts/aws-ec2-docker-staging-setup.sh` | Install Docker on EC2, pull code from GitHub, create `scripts/aws-staging.env`, and optionally run `docker-compose.staging.yml`. |
| `scripts/aws-ec2-pull-and-run-war.sh` | Pull code from GitHub, build the backend, and run the executable WAR/JAR as a systemd service. |

The current Maven build creates an executable JAR. The non-Docker EC2 script still supports WAR packaging and will prefer `target/*.war` if the backend is later changed to WAR.

## Log retention

Backend file logs are written to `app.logging.path` and compressed hourly archives are stored under `${app.logging.path}/archive`.

The default policy keeps archive logs for 720 hours, which is exactly 30 days. Logback deletes older archives automatically on rollover or application start:

```properties
app.logging.path=/var/log/hoppo/pooler-backend
app.logging.archive.max-file-size=100MB
app.logging.archive.max-history-hours=720
app.logging.archive.total-size-cap=5GB
```

Keep these values in `application-dev.properties`, `application-staging.properties`, and `application-prod.properties` so environment changes do not require code edits.

## Fare split contract

Confirmed rides store two trip distances:

- `primaryTripDistanceKm`: longer rider, final drop-off, suggested booker
- `secondaryTripDistanceKm`: shorter rider, first drop-off

`POST /api/v1/rides/{rideId}/fare-split` accepts the real provider fare after the riders book in Grab, TADA, Gojek, or another cab app:

```json
{
  "totalFare": 30.0,
  "currency": "SGD",
  "provider": "Grab"
}
```

Validation requires `totalFare` from `0.01` to `99999.00`, a 3-letter currency code when supplied, and a non-blank provider name.

The backend stores the total fare, provider, currency, `primaryFareShare`, and `secondaryFareShare` on the ride. Shares are proportional to each rider's trip distance. Hoppo records the split for history only; it does not collect payment.

## Test

```bash
./mvnw test
```

API collections are under `doc/`, and the executable product wireframe is under `doc/productInfo/`. New endpoints are always available from generated OpenAPI even before a Postman collection refresh.

## Google OAuth note

The backend does not expose `/oauthredirect`. That route belongs to the Expo client and is registered in Google Cloud as the OAuth redirect URI. After Google redirects back to the client, the client sends the received ID token to `POST /api/v1/auth/google`, where this backend verifies the token audience against `app.google.client-ids`.

## Apple Authentication

The backend exposes `POST /api/v1/auth/apple` for Sign in with Apple. The mobile app sends the Apple identity token received from iOS, and optional `firstName` / `lastName` values when Apple returns the name on the first authorization.

```json
{
  "identityToken": "<apple-id-token>",
  "firstName": "Akash",
  "lastName": "Kumar"
}
```

Backend validation:

- verifies the Apple JWT signature with Apple's JWKS endpoint
- requires `iss=https://appleid.apple.com`
- verifies `aud` against configured Apple client IDs
- verifies `exp`, `iat`, and `email_verified`
- rejects blank, malformed, or oversized Apple identity tokens before verification
- never logs the raw identity token, email, or request body
- creates or loads the Hoppo user, then returns the normal Hoppo access, refresh, and session tokens
- auth verification/reset tokens and social-login placeholder passwords are generated with `SecureRandom`

Required backend configuration:

```properties
APPLE_CLIENT_IDS=com.yourcompany.hoppo,com.yourcompany.hoppo.service
APPLE_ISSUER=https://appleid.apple.com
APPLE_JWKS_URL=https://appleid.apple.com/auth/keys
APPLE_JWKS_REQUEST_TIMEOUT_SECONDS=5
APPLE_JWKS_CONNECT_TIMEOUT_SECONDS=3
APPLE_JWKS_CACHE_MINUTES=60
APPLE_TOKEN_ALLOWED_CLOCK_SKEW_SECONDS=60
```

`APPLE_CLIENT_IDS` is comma-separated. Use the iOS bundle ID for native iOS sign-in and the Services ID for web/Android flows, depending on what the frontend sends as the token audience.
