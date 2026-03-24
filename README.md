# Bouncr

[![License](https://img.shields.io/badge/License-EPL%201.0-blue.svg)](https://opensource.org/licenses/EPL-1.0)
[![Java](https://img.shields.io/badge/Java-25-orange.svg)](https://openjdk.org/)
[![Go](https://img.shields.io/badge/Go-1.25-00ADD8.svg)](https://go.dev/)
[![enkan](https://img.shields.io/badge/enkan-0.14.0-green.svg)](https://github.com/kawasima/enkan)

An authentication gateway and OIDC Identity Provider for backend applications, powered by Envoy ext_proc.

## Why Bouncr

- **Stateless backend applications by design** — authentication state is handled at the proxy/gateway layer, and backend apps receive request-scoped identity via `x-bouncr-credential`
- **Easier testing and operations** — apps can be tested without session stores/sticky sessions, and auth behavior can be reproduced by header-based credentials in integration tests
- **Pragmatic self-hosted auth core** — combines OIDC/OAuth2 identity endpoints with a clear Group → Role → Permission → Realm model for service-to-service and backend authorization

![bouncer](http://2.bp.blogspot.com/-kVVeXhsM8yU/VIhOpmLlnDI/AAAAAAAApfY/O5N9L72Byo4/s450/job_sp.png)

## Features

### Authentication Gateway

- **Transparent auth proxy** — Envoy ext_proc injects `x-bouncr-credential` JWT into every request; backend apps need zero auth code
- **Multiple credential types** — Password, OpenID Connect (RP), TOTP two-factor authentication
- **BFF session management** — Short-lived access cache (15 min) + long-lived refresh marker (7 days) with transparent refresh via bouncr-proxy
- **Fine-grained authorization** — Group → Role → Permission model with per-Realm scope
- **Cross-application session reuse (current Phase 3 behavior)** — one Bouncr session token is reused across applications; proxy filters `permissionsByRealm` into realm-specific `permissions` before forwarding

### OIDC Identity Provider

Bouncr acts as a full OIDC Identity Provider ([comparable to Kanidm](https://kanidm.github.io/kanidm/master/integrations/oauth2.html)):

| Endpoint | Path | RFC |
|---|---|---|
| Authorization | `GET /oauth2/authorize` | RFC 6749 §4.1 |
| Token | `POST /oauth2/token` | RFC 6749 §4.1.3 |
| UserInfo | `GET /oauth2/userinfo` | OIDC Core §5.3 |
| JWKS | `GET /oauth2/openid/:client_id/certs` | RFC 7517 |
| Discovery | `GET /oauth2/openid/:client_id/.well-known/openid-configuration` | OIDC Discovery 1.0 |
| Token Introspection | `POST /oauth2/token/introspect` | RFC 7662 |
| Token Revocation | `POST /oauth2/token/revoke` | RFC 7009 |

**Supported grant types:** `authorization_code` (with PKCE S256), `refresh_token` (rotation), `client_credentials`

**Security:**
- Client secrets hashed with PBKDF2 (shown once on creation)
- Private keys encrypted at rest with AES-256-GCM
- RS256 JWT signing with per-client RSA key pairs
- Constant-time comparisons for secrets and PKCE verification

**Logout propagation (Issue #80):**
- Back-channel logout on sign-out: Bouncr sends OIDC Logout Token (`iss`, `aud`, `iat`, `jti`, `events`, `sub`) to each registered RP `backchannel_logout_uri` (best-effort)
- Front-channel logout on sign-out: API returns `frontchannel_logout_urls` for UI-side hidden iframe dispatch (bounded parallel fan-out with overall timeout in UI)

### Administration

- User, Group, Role, Permission management via REST API
- Application and Realm configuration for dynamic routing
- OidcApplication registration (client_id, client_secret, RSA keys)
- Audit trail of user actions

## Architecture

```
Browser / RP
  └─ Envoy (port 3000)
       └─ ext_proc filter → bouncr-proxy (Go, gRPC)
            ├─ Redis: token lookup + session refresh
            ├─ RealmCache: path → application routing
            └─ JWT injection (x-bouncr-credential)
                 └─ Backend cluster (via cluster_header routing)
                      └─ bouncr-api-server (Java, enkan/kotowari-restful)
```

> **Backend contract:** Requests that match a realm but carry no valid session token are still routed to the backend — without the `x-bouncr-credential` header. Backends **must** verify the presence and validity of this header before granting access to protected resources.

## Modules

| Module | Language | Description |
|---|---|---|
| `bouncr-api-server` | Java | REST API + OIDC IdP (enkan/kotowari-restful) |
| `bouncr-proxy` | Go | Envoy ext_proc sidecar for auth + routing |
| `bouncr-components` | Java | Domain types, migrations, shared components |
| `bouncr-ui` | TypeScript | React SPA (Vite) |
| `bouncr-e2e-test` | Java | E2E integration tests (Playwright) |
| `envoy/` | YAML | Envoy proxy configuration |

## Quick Start

### Docker Compose

```bash
docker compose up
```

Starts Redis, PostgreSQL, bouncr-proxy (ext_proc), and Envoy. The proxy listens on port 3000.

### Local Development

```bash
# 1. Start dependencies
docker compose up redis db

# 2. Start the API server (H2 in-memory, MemoryStore)
cd bouncr-api-server
mvn compile exec:java -Pdev

# 3. Start the proxy
cd bouncr-proxy
export DB_DSN="postgres://bouncr:bouncr@localhost:5432/bouncr?sslmode=disable"
export JWT_SECRET="your-secret-key"
go run ./cmd/bouncr-proxy/

# 4. Start Envoy
envoy -c envoy/envoy.yaml

# 5. Start the UI
cd bouncr-ui
npm run dev
```

## Build

```bash
# API server (Java 25)
mvn package -pl bouncr-api-server -am

# Proxy (Go 1.25)
cd bouncr-proxy && go build -o bouncr-proxy ./cmd/bouncr-proxy/

# UI
cd bouncr-ui && npm run build
```

## Testing

```bash
# Unit tests
mvn test -pl bouncr-api-server -am

# E2E tests (starts full API server in-process with Playwright)
mvn test -pl bouncr-e2e-test -am

# Go tests
cd bouncr-proxy && go test ./...
```

## Configuration

### Environment Variables

| Variable | Default | Description |
|---|---|---|
| `JDBC_URL` | (required for production) | PostgreSQL connection URL |
| `REDIS_URL` | (required for production) | Redis connection URL |
| `JWT_SECRET` | `abcdefghijklmnopqrstuvwxyzabcdef` | HS256 shared secret for proxy↔API JWT |
| `PORT` | `3005` | API server HTTP port |
| `OIDC_KEY_ENCRYPTION_KEY` | (optional) | 32-byte Base64 AES-256 key for private key encryption at rest |
| `DB_DSN` | (required for proxy) | PostgreSQL DSN for bouncr-proxy |
| `API_SERVER_URL` | `http://localhost:3005` | API server URL for proxy token refresh |

## Production Deployment

### Prerequisites

- PostgreSQL 16+
- Redis 7+
- Envoy 1.31+ (or compatible version with ext_proc support)

### Important Configuration

| Variable | Component | Default | Description |
|---|---|---|---|
| `JDBC_URL` | api-server | (required) | PostgreSQL JDBC connection URL |
| `JDBC_USER` | api-server | (required) | Database user |
| `JDBC_PASSWORD` | api-server | (required) | Database password |
| `REDIS_URL` | api-server, proxy | (required) | Redis connection URL |
| `JWT_SECRET` | api-server, proxy | (required) | HS256 shared secret — **must be identical** between api-server and proxy |
| `INTERNAL_SIGNING_KEY` | api-server, proxy | (required) | HMAC key for internal API calls — **must be identical** |
| `PORT` | api-server | `3005` | API server HTTP listen port |
| `CORS_ORIGINS` | api-server | (none) | Allowed CORS origins (restrict in production) |
| `ADMIN_PASSWORD` | api-server | (random) | Initial admin user password (only used on first startup) |
| `OIDC_KEY_ENCRYPTION_KEY` | api-server | (optional) | 32-byte Base64 AES-256 key for private key encryption at rest |
| `DB_DSN` | proxy | (required) | PostgreSQL DSN for bouncr-proxy |
| `GRPC_PORT` | proxy | `50051` | gRPC listen port for ext_proc |
| `ADMIN_PORT` | proxy | `8081` | Admin HTTP port (`/_healthcheck`, `/_refresh`, `/_clusters`) |
| `TOKEN_COOKIE_NAME` | proxy | `BOUNCR_TOKEN` | Cookie name for session token |
| `BACKEND_HEADER_NAME` | proxy | `x-bouncr-credential` | Header name for JWT passed to backends |
| `JWT_EXPIRATION` | proxy | `300` | JWT expiration in seconds |
| `REALM_REFRESH_INTERVAL` | proxy | `30s` | How often the proxy refreshes realm/application cache from DB |
| `API_SERVER_URL` | proxy | `http://localhost:3005` | API server URL for token refresh |

### Critical Notes

1. **`CLEAR_SCHEMA` must be `false` (default) in production.** The `dev` profile sets `CLEAR_SCHEMA=true`, which **drops and recreates the entire database on every startup**. Never use `-Pdev` in production.

2. **`JWT_SECRET` and `INTERNAL_SIGNING_KEY` must match** between api-server and proxy. Use strong random values (32+ characters).

3. **TLS termination**: Configure TLS at the Envoy layer or a load balancer in front of Envoy. Bouncr services communicate over plaintext internally.

4. **Backend must verify `x-bouncr-credential`**: Requests that match a realm but have no valid token are still routed to the backend — without the credential header. Backends **must** check for the presence and validity of this header before granting access. Do not assume that a routed request is authenticated.

5. **Envoy configuration**: Use `bouncr-proxy gen-envoy-config` to generate Envoy cluster configuration from the database:
   ```bash
   export DB_DSN="postgres://..."
   bouncr-proxy gen-envoy-config > envoy.yaml
   ```

6. **Docker image build order**:
   ```bash
   # 1. Build api-server image (requires Maven + JDK 25)
   cd bouncr-api-server && mvn compile jib:dockerBuild

   # 2. Build proxy and UI images
   docker build -t bouncr/bouncr-proxy bouncr-proxy
   docker build -t bouncr/bouncr-ui bouncr-ui

   # 3. Start all services
   docker compose up
   ```

## Migration Upgrade Note

From v0.3.0 onward, historical Java migrations `V1` through `V28` are consolidated into a single baseline migration `B28__BouncrV0_3_0`.

For an existing database, run a one-time Flyway repair:

```bash
flyway -url=jdbc:postgresql://<host>:5432/<db> -user=<user> -password=<password> repair
```

Fresh databases are created from `B28__BouncrV0_3_0`.

## License

Copyright © 2017-2026 kawasima

Distributed under the [Eclipse Public License 1.0](https://opensource.org/licenses/EPL-1.0).
