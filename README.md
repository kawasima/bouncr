# Bouncr

Bouncr is a reverse proxy with authentication and authorization for backend applications.

![bouncer](http://2.bp.blogspot.com/-kVVeXhsM8yU/VIhOpmLlnDI/AAAAAAAApfY/O5N9L72Byo4/s450/job_sp.png)

Bouncr has the following features:

- Authenticate
    - Various types of credentials
        - Password
        - OpenID Connect
    - Two factor authentication (using Google Authenticator etc.)
- Authorization (based on Group - Role - Permission)
- Sign in / Sign out
- Audit
    - Show security activities
- IdP
    - OpenID Connect provider
- Administration pages
    - Manage users
    - Manage groups
    - Manage applications and realms
    - Manage roles
    - Manage OpenID Connect applications

## Modules

| Module | Language | Description |
|--------|----------|-------------|
| `bouncr-proxy` | Go | Envoy ext_proc sidecar for authentication, authorization, and dynamic routing |
| `bouncr-api-server` | Java | REST API server (enkan/kotowari-restful) |
| `bouncr-components` | Java | Shared entities, migrations, and components |
| `bouncr-ui` | TypeScript | React SPA (Vite) |
| `envoy/` | YAML | Envoy proxy configuration |

## Quick Start

### Docker Compose

```bash
docker compose up
```

This starts Redis, PostgreSQL, bouncr-proxy (ext_proc), and Envoy. The proxy listens on port 3000.

### Local Development

```bash
# 1. Start dependencies
docker compose up redis db

# 2. Start the API server
cd bouncr-api-server
mvn compile exec:java -Pdev,slf4j-simple

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
# API server (Java)
cd bouncr-api-server
mvn package

# Proxy (Go)
cd bouncr-proxy
go build -o bouncr-proxy ./cmd/bouncr-proxy/

# UI
cd bouncr-ui
npm run build
```

## Migration Upgrade Note

From this change onward, historical Java migrations `V1` through `V28` are consolidated into a single baseline migration `B28__BouncrV0_3_0`.

For an existing database that already has `V1..V28` rows in `flyway_schema_history`, run a one-time Flyway repair before starting the upgraded application, so removed local migration classes are marked as deleted in history.

```bash
# Example (adjust URL/user/password for your environment)
flyway \
  -url=jdbc:postgresql://<host>:5432/<db> \
  -user=<user> \
  -password=<password> \
  repair
```

Fresh databases are created from `B28__BouncrV0_3_0`.

## License

Copyright © 2017-2025 kawasima

Distributed under the Eclipse Public License, the same as Clojure.
