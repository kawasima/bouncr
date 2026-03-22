import type { Plugin } from 'vite'
import http from 'node:http'
import { URL } from 'node:url'
import jwt from 'jsonwebtoken'

const DEV_PERMISSIONS = [
  'any_user:read', 'any_user:create', 'any_user:update', 'any_user:delete',
  'any_group:read', 'any_group:create', 'any_group:update', 'any_group:delete',
  'any_role:read', 'any_role:create', 'any_role:update', 'any_role:delete',
  'any_permission:read', 'any_permission:create', 'any_permission:update', 'any_permission:delete',
  'any_application:read', 'any_application:create', 'any_application:update', 'any_application:delete',
  'any_realm:read', 'any_realm:create', 'any_realm:update', 'any_realm:delete',
  'user:read', 'user:create', 'user:update', 'user:delete',
  'group:read', 'group:create', 'group:update', 'group:delete',
  'role:read', 'role:create', 'role:update', 'role:delete',
  'permission:read', 'permission:create', 'permission:update', 'permission:delete',
  'application:read', 'application:create', 'application:update', 'application:delete',
  'realm:read', 'realm:create', 'realm:update', 'realm:delete',
  'oidc_provider:read', 'oidc_provider:create', 'oidc_provider:update', 'oidc_provider:delete',
  'oidc_application:read', 'oidc_application:create', 'oidc_application:update', 'oidc_application:delete',
  'assignments:read', 'assignments:create', 'assignments:delete',
  'invitation:create',
  'my:read', 'my:update', 'my:delete',
]

/**
 * Vite plugin that replaces Envoy + bouncr-proxy for local development.
 *
 * - Proxies all /bouncr requests to bouncr-api-server
 * - Intercepts sign-in response to capture token → account mapping
 * - For authenticated requests, generates a dev JWT with full permissions
 * - No Redis, no Docker required
 */
export default function bouncrAuth(): Plugin {
  // token → { account, userId } captured from sign-in responses
  const tokenStore = new Map<string, { account: string; userId: string }>()
  let jwtSecret: string
  let apiTarget: URL

  return {
    name: 'bouncr-auth',

    configureServer(server) {
      jwtSecret =
        process.env.VITE_JWT_SECRET || 'abcdefghijklmnopqrstuvwxyzabcdef'
      apiTarget = new URL(
        process.env.VITE_API_SERVER || 'http://localhost:3005',
      )

      server.middlewares.use('/bouncr', async (req, res) => {
        try {
          await proxyRequest(req, res)
        } catch (err) {
          console.error('[bouncr-auth] proxy error:', err)
          if (!res.headersSent) {
            res.writeHead(502, { 'Content-Type': 'application/json' })
            res.end(JSON.stringify({ status: 502, detail: 'Bad Gateway' }))
          }
        }
      })
    },
  }

  async function proxyRequest(
    req: http.IncomingMessage,
    res: http.ServerResponse,
  ) {
    const token = extractToken(req)
    const path = '/bouncr' + (req.url || '')

    const outHeaders: Record<string, string | string[] | undefined> = {
      ...req.headers,
    }

    // Inject JWT for authenticated requests
    if (token) {
      const user = tokenStore.get(token)
      if (user) {
        const credential = jwt.sign(
          {
            iss: 'bouncr',
            uid: user.userId,
            sub: user.account,
            permissions: DEV_PERMISSIONS,
          },
          jwtSecret,
          { algorithm: 'HS256' },
        )
        outHeaders['x-bouncr-credential'] = credential
        console.log(`[bouncr-auth] ${req.method} ${path} → JWT for ${user.account}`)
      } else {
        console.warn(`[bouncr-auth] ${req.method} ${path} → unknown token (not cached)`)
      }
      delete outHeaders['authorization']
    }

    delete outHeaders['host']

    // Capture sign-in/sign-up responses to populate tokenStore,
    // and forward the Set-Cookie header so the browser receives the HttpOnly cookie.
    const isSignIn =
      req.method === 'POST' &&
      (path === '/bouncr/api/sign_in' || path === '/bouncr/api/sign_up')

    if (isSignIn) {
      await proxyAndCapture(req, res, path, outHeaders)
    } else {
      await proxyPassthrough(req, res, path, outHeaders)
    }
  }

  /** Proxy sign-in/sign-up, buffer response to capture token. */
  function proxyAndCapture(
    req: http.IncomingMessage,
    res: http.ServerResponse,
    path: string,
    headers: Record<string, string | string[] | undefined>,
  ): Promise<void> {
    return new Promise((resolve) => {
      const proxyReq = http.request(
        {
          hostname: apiTarget.hostname,
          port: apiTarget.port,
          path,
          method: req.method,
          headers,
        },
        (proxyRes) => {
          const chunks: Buffer[] = []
          proxyRes.on('data', (chunk: Buffer) => chunks.push(chunk))
          proxyRes.on('end', () => {
            const body = Buffer.concat(chunks)
            res.writeHead(proxyRes.statusCode ?? 502, proxyRes.headers)
            res.end(body)

            if (
              proxyRes.statusCode === 200 ||
              proxyRes.statusCode === 201
            ) {
              try {
                const data = JSON.parse(body.toString())
                // Sign-in returns { token, user_id, ... } or similar
                // Sign-up may return { id, account, ... }
                const token: string | undefined =
                  data.token ?? data.session?.token
                const account: string | undefined =
                  data.user?.account ?? data.account ?? data.sub
                const userId: string | undefined =
                  String(data.user?.id ?? data.user_id ?? data.id ?? '0')

                if (token) {
                  tokenStore.set(token, {
                    account: account ?? 'dev',
                    userId: userId,
                  })
                  console.log(
                    `[bouncr-auth] Cached token ${token.substring(0, 8)}... for ${account}`,
                  )
                }
              } catch {
                // Not JSON — ignore
              }
            }
            resolve()
          })
          proxyRes.on('error', () => resolve())
        },
      )
      proxyReq.on('error', () => {
        if (!res.headersSent) {
          res.writeHead(502, { 'Content-Type': 'application/json' })
          res.end(JSON.stringify({ status: 502, detail: 'Cannot reach api-server' }))
        }
        resolve()
      })
      req.pipe(proxyReq, { end: true })
    })
  }

  /** Simple passthrough proxy. */
  function proxyPassthrough(
    req: http.IncomingMessage,
    res: http.ServerResponse,
    path: string,
    headers: Record<string, string | string[] | undefined>,
  ): Promise<void> {
    return new Promise((resolve) => {
      const proxyReq = http.request(
        {
          hostname: apiTarget.hostname,
          port: apiTarget.port,
          path,
          method: req.method,
          headers,
        },
        (proxyRes) => {
          res.writeHead(proxyRes.statusCode ?? 502, proxyRes.headers)
          proxyRes.pipe(res, { end: true })
          proxyRes.on('end', resolve)
        },
      )
      proxyReq.on('error', (err) => {
        console.error('[bouncr-auth] upstream error:', err.message)
        if (!res.headersSent) {
          res.writeHead(502, { 'Content-Type': 'application/json' })
          res.end(
            JSON.stringify({
              status: 502,
              detail: 'Cannot reach api-server at ' + apiTarget.href,
            }),
          )
        }
        resolve()
      })
      req.pipe(proxyReq, { end: true })
    })
  }
}

/**
 * Extracts the session token from the request.
 * Checks the Authorization: Bearer header first, then the BOUNCR_TOKEN cookie.
 */
function extractToken(req: http.IncomingMessage): string | null {
  const auth = req.headers['authorization']
  if (auth?.startsWith('Bearer ')) return auth.slice(7)

  const cookieHeader = req.headers['cookie']
  if (cookieHeader) {
    for (const part of cookieHeader.split(';')) {
      const [name, ...rest] = part.trim().split('=')
      if (name === 'BOUNCR_TOKEN') return rest.join('=')
    }
  }
  return null
}
