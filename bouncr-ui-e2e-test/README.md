# bouncr-ui-e2e-test

Cucumber + Playwright E2E tests for the Bouncr UI. Tests run against a live Bouncr environment (UI + API server + proxy + Redis + PostgreSQL).

## Prerequisites

The full Bouncr stack must be running before executing tests.

### Option A: Docker Compose (recommended)

```bash
# From the project root — builds and starts all services
docker compose -f docker-compose.dev.yml up --build
```

This builds and starts everything (Redis, PostgreSQL, api-server, proxy, Envoy, UI) with development defaults. No manual build steps needed. The UI is available at `http://localhost:8080`.

### Option B: Local development servers

Use this if you want to run the api-server or UI on the host (e.g. for debugging with hot-reload).

```bash
# 1. Start infrastructure only
docker compose -f docker-compose.dev.yml up redis db

# 2. Start the API server on host (H2 in-memory, auto-reset on startup)
cd bouncr-api-server && mvn compile exec:java -Pdev

# 3. Start the proxy on host
cd bouncr-proxy
export DB_DSN="postgres://bouncr:bouncr@localhost:5432/bouncr?sslmode=disable"
export JWT_SECRET="abcdefghijklmnopqrstuvwxyzabcdef"
export INTERNAL_SIGNING_KEY="CHANGE-ME"
go run ./cmd/bouncr-proxy/

# 4. Start Envoy
envoy -c envoy/envoy.yaml

# 5. Start the UI on host
cd bouncr-ui && npm run dev
```

With local dev servers, use `E2E_BASE_URL=http://localhost:3000` (Envoy) when running tests.

## Install

```bash
npm install
npx playwright install chromium
```

## Run tests

```bash
# Headless (default) — UI at http://localhost:8080
npm test

# Headed (visible browser window)
HEADED=1 npm test

# Custom base URL (e.g. local Vite dev server through Envoy)
E2E_BASE_URL=http://localhost:3000 npm test

# Custom admin password
E2E_ADMIN_PASSWORD=mypassword npm test

# Run a specific feature file
npx cucumber-js features/sign-in.feature

# Run with a tag filter
npx cucumber-js --tags "@smoke"
```

## Environment variables

| Variable | Default | Description |
|---|---|---|
| `E2E_BASE_URL` | `http://localhost:8080` | Base URL of the Bouncr UI |
| `E2E_ADMIN_PASSWORD` | `admin` | Password for the initial `admin` user |
| `HEADED` | (unset) | Set to `1` to show the browser window |

## Test structure

```
features/           Gherkin .feature files (test scenarios)
  sign-in.feature
  user-management.feature
  admin-access-control.feature
  group-management.feature
  role-permission.feature
  password-reset.feature
  invitation.feature
  oidc-application.feature

steps/              Step definitions (TypeScript)
  common.steps.ts           General UI steps (navigate, click, fill)
  sign-in.steps.ts          Sign-in / sign-out / initial password change
  user-management.steps.ts  User CRUD
  group-management.steps.ts Group operations
  role-permission.steps.ts  Role and permission assignment
  password-reset.steps.ts   Password reset flow
  invitation.steps.ts       User invitation workflow
  admin-access-control.steps.ts  Permission-based UI access
  oidc-application.steps.ts OIDC application management

support/            Test infrastructure
  world.ts          Cucumber World (shared state per scenario)
  hooks.ts          BeforeAll / Before / After / AfterAll lifecycle
  auth.helper.ts    API helpers for sign-in, user/group/role setup
  config.ts         Test configuration and test user definitions
```

## How it works

1. **BeforeAll** (runs once): Signs in as `admin`, creates 4 test roles with specific permission sets, 4 test groups, 4 test users, and caches their session tokens.

2. **Before** (per scenario): Launches a fresh browser context and page.

3. **Steps** use Playwright to interact with the UI (navigation, form filling, assertions) and the API directly (for data setup/verification).

4. **After** (per scenario): Closes page and browser context.

5. **AfterAll** (runs once): Deletes all test users, groups, and roles created during setup.

## Reports

HTML reports are generated automatically at `reports/cucumber-report.html`.
