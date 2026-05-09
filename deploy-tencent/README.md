# Tencent Cloud Deployment

This directory captures the Tencent Cloud deployment shape used by the running production-like server.

Do not commit real `.env` files, SSL certificates, database dumps, or SpreadJS license values. Use `.env.example` as the template and keep real values only on the server or in the deployment secret store.

## Deploy

From the repository root on the Tencent Cloud server:

```bash
cp deploy-tencent/.env.example deploy-tencent/.env
# Fill deploy-tencent/.env with real values on the server.
docker compose -f deploy-tencent/docker-compose.yml --env-file deploy-tencent/.env up -d --build
```

## Notes

- `frontend` builds from `audit-ui` and receives the two SpreadJS license values as Docker build args.
- `backend` listens on localhost port `8080`; nginx proxies `/api/` to the backend container.
- MySQL is bound to localhost only.
- TLS certificates are expected under `deploy-tencent/ssl/` and are intentionally ignored by Git.
