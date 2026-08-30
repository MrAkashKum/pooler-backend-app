# Pooler / Hoppo Postman assets

These files are generated from the deployed OpenAPI specification and are ready to import into Postman.

## Files

- `Pooler-API.postman_collection.json` — complete collection containing all 88 documented API operations.
- `Pooler-Auth-API.postman_collection.json` — authentication-only collection containing 11 operations.
- `Pooler-Local.postman_environment.json` — local Spring Boot environment.
- `Pooler-Staging.postman_environment.json` — deployed GCP Cloud Run environment.

## Import and use

1. In Postman, select **Import** and import one or both collections.
2. Import both environment files.
3. Select **Pooler — Local** or **Pooler — Staging (GCP)**.
4. Edit the Register/Login request body with a test account.
5. Run Login. Its test script automatically saves `accessToken`, `refreshToken`, and `sessionToken` into the selected environment.
6. Protected requests inherit `Authorization: Bearer {{accessToken}}` from the collection.

The collection also supplies `X-Device-Id`, `X-Platform`, and `X-App-Version` from the selected environment. Resource identifiers such as `rideEntityId`, `threadId`, and `invitationEntityId` can be set in the environment for path-based requests.

## Base URLs

| Environment | Base URL |
| --- | --- |
| Local | `http://localhost:8888/pooler-backend` |
| Staging (GCP) | `https://pooler-backend-663018144709.asia-southeast1.run.app/pooler-backend` |

Local Swagger UI is available at `http://localhost:8888/pooler-backend/swagger-ui/index.html`.
The staging OpenAPI document is available at `{{baseUrl}}/v3/api-docs`.

## Regenerate after API changes

Download or export the current OpenAPI JSON, then run:

```bash
node scripts/generate-postman.mjs path/to/openapi.json doc
```

The generator updates both collections and both environments. Environment secrets are intentionally empty in the exported files.
