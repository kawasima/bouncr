# bouncr-proxy

A reverse proxy for Bouncr, built as an Envoy [ext_proc](https://www.envoyproxy.io/docs/envoy/latest/configuration/http/http_filters/ext_proc_filter) (External Processing) sidecar in Go. It provides authentication, authorization, and dynamic multi-application routing.

## Architecture

```
Client / Browser
  |  (Authorization: Bearer <token>  or  Cookie: BOUNCR_TOKEN=<token>)
  v
Envoy Proxy (port 3000)
  |  ext_proc filter calls bouncr-proxy (gRPC, port 50051)
  |
  |  bouncr-proxy:
  |    1. Matches request path to a Realm (DB-driven regex patterns)
  |    2. Resolves the Application and rewrites :path for the backend
  |    3. Sets x-bouncr-cluster header (Envoy cluster_header routing)
  |    4. Looks up token in Redis and builds HS256 JWT
  |    5. Sets x-bouncr-credential header with signed JWT
  |
  v
Backend Application (e.g., bouncr-api-server on port 3005)
  |  reads x-bouncr-credential JWT for user identity & permissions
```

### Dynamic Multi-Application Routing

The `applications` table in the database defines `virtual_path` and `pass_to` for each backend. When bouncr-proxy matches a realm, it:

1. **Rewrites `:path`** — strips the `virtual_path` prefix and prepends the backend path from `pass_to`
2. **Sets `x-bouncr-cluster`** — Envoy uses `cluster_header` routing to forward the request to the named cluster

Cluster names are derived from the `pass_to` host and port (e.g., `http://api:3005/bouncr/api` becomes cluster `api_3005`).

## Prerequisites

- Go 1.25+
- PostgreSQL (for realm/application data)
- Redis (for session token store)
- Envoy Proxy

## Build

```bash
go build -o bouncr-proxy ./cmd/bouncr-proxy/
```

Or with Docker:

```bash
docker build -t bouncr-proxy .
```

## Usage

### Run the server

```bash
export DB_DSN="postgres://user:pass@localhost:5432/bouncr?sslmode=disable"
export JWT_SECRET="your-secret-key"
export REDIS_URL="redis://localhost:6379"

./bouncr-proxy
```

This starts:
- **gRPC ext_proc server** on port 50051 (for Envoy)
- **Admin HTTP server** on port 8081 (for operations)

### Generate Envoy configuration

Generate a complete `envoy.yaml` from the database:

```bash
export DB_DSN="postgres://user:pass@localhost:5432/bouncr?sslmode=disable"

./bouncr-proxy gen-envoy-config > envoy.yaml
```

This reads all applications from the DB and produces an Envoy config with:
- Dynamic cluster definitions (one per application `pass_to`)
- `cluster_header` routing via ext_proc
- ext_proc filter configuration

Re-run this command and reload Envoy whenever applications are added or removed.

## Environment Variables

| Variable | Default | Description |
|----------|---------|-------------|
| `DB_DSN` | *(required)* | PostgreSQL connection string |
| `JWT_SECRET` | *(required)* | HMAC-SHA256 secret for signing JWTs |
| `REDIS_URL` | `redis://localhost:6379` | Redis connection URL |
| `REDIS_KEY_PREFIX` | `BOUNCR_TOKEN:` | Key prefix for session tokens in Redis |
| `GRPC_PORT` | `50051` | gRPC ext_proc listen port |
| `ADMIN_PORT` | `8081` | Admin HTTP server listen port |
| `LISTEN_PORT` | `3000` | Envoy listen port (used by `gen-envoy-config`) |
| `TOKEN_COOKIE_NAME` | `BOUNCR_TOKEN` | Cookie name for session tokens |
| `BACKEND_HEADER_NAME` | `x-bouncr-credential` | Header name for the JWT sent to backends |
| `REALM_REFRESH_INTERVAL` | `30s` | Interval for periodic realm cache refresh |

## Admin API

The admin HTTP server provides operational endpoints:

### `POST /_refresh`

Triggers an immediate realm cache refresh from the database.

```bash
curl -X POST http://localhost:8081/_refresh
# 204 No Content
```

### `GET /_clusters`

Returns the current cluster definitions as JSON (derived from DB applications).

```bash
curl http://localhost:8081/_clusters
```

```json
[
  {
    "cluster_name": "api_3005",
    "address": "api",
    "port": 3005,
    "virtual_path": "/bouncr/api",
    "pass_to": "http://api:3005/bouncr/api"
  }
]
```

### `GET /_healthcheck`

Returns `200 OK`.

## How It Works

### Token Extraction

Tokens are extracted in this order:
1. `Authorization: Bearer <token>` header
2. Cookie named `BOUNCR_TOKEN` (configurable)

### Realm Matching

Realms are loaded from the database with their parent application. Each realm has a URL pattern that is compiled into a regex: `^<virtual_path>($|/<realm_url>)`.

For example, an application with `virtual_path=/bouncr/api` and a realm with `url=.*` produces the pattern `^/bouncr/api($|/.*)`, matching all paths under `/bouncr/api`.

### Path Rewriting

When a realm matches, the request path is rewritten:

```
Request:     /bouncr/api/users
virtual_path: /bouncr/api
pass_to:      http://api:3005/bouncr/api

Rewritten:   /bouncr/api/users  (strip virtual_path, prepend backend path)
```

### JWT Generation

If a valid session token is found in Redis, a JWT is generated with:
- All fields from the session profile (sub, account, email, etc.)
- `permissions` array filtered to the matched realm
- HS256 signature

**Backend requirement**: `bouncr-api-server` must verify HS256 JWTs. Set the same `JWT_SECRET` in both bouncr-proxy and the API server.
