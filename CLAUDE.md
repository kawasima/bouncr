# Bouncr Project Notes

## Development Docker Services

`docker-compose.dev.yml` provides the infrastructure for local development and E2E testing. `docker-compose.yml` (default) is for production and requires `.env` with real credentials. For local dev, the `bouncr-api-server` typically runs on the host (via `mvn exec:java -Pdev`), not inside Docker.

- `redis` (`redis:7-alpine`, port 6379) — Token store (msgpack) shared between api-server and bouncr-proxy
- `db` (`postgres:16-alpine`, port 5432) — Primary database (user: `bouncr`, password: `bouncr`)
- `proxy` (built from `./bouncr-proxy`, ports 50051/8081) — Envoy ext_proc sidecar; authenticates requests, rewrites paths, sets `x-bouncr-cluster` header
- `envoy` (`envoyproxy/envoy:v1.31-latest`, ports 3000/9901) — Reverse proxy; routes traffic via `cluster_header`, calls `proxy` via ext_proc

**Typical local dev setup:**

1. `docker compose -f docker-compose.dev.yml up redis db` — starts redis, db
2. `cd bouncr-api-server && mvn exec:java -Pdev` — starts api-server on port 3005 (host)
3. `cd bouncr-ui && npm run dev` — starts UI on port 3001, proxies `/bouncr` → Envoy (port 3000)

Envoy uses `envoy/envoy-docker.yaml` (Docker) vs `envoy/envoy.yaml` (local without Docker). The `extra_hosts: host.docker.internal:host-gateway` allows Envoy to reach the api-server running on the host.

## Development: DB Reset on Startup

- Running `mvn exec:java -Pdev` (the `dev` profile) sets `CLEAR_SCHEMA=true`, which **drops and recreates the entire database on every startup**. This is intentional — development always starts from a clean state.
- Configured in `bouncr-api-server/pom.xml` under `<profile><id>dev</id>` as `<CLEAR_SCHEMA>true</CLEAR_SCHEMA>`.
- In production / docker-compose, `CLEAR_SCHEMA` defaults to `false`, so data is preserved across restarts.
