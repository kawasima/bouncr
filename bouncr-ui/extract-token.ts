import type { IncomingMessage } from 'node:http'

/**
 * Extracts the session token from an incoming HTTP request.
 * Checks the Authorization: Bearer header first, then the session cookie.
 *
 * Cookie name matching strips RFC 6265bis prefixes (__Host-, __Secure-) before
 * comparing with cookieName, because HostCookie serializes the name with the
 * prefix (e.g. "__Host-BOUNCR_TOKEN") but the configured name is "BOUNCR_TOKEN".
 *
 * Both authorization and cookie headers are normalized to a single string before
 * parsing — Node's http.IncomingMessage allows duplicate headers as string[],
 * so we join with ", " (the RFC 7230 canonical form) to avoid crashes.
 */
export function extractToken(req: Pick<IncomingMessage, 'headers'>, cookieName: string): string | null {
  const rawAuth = req.headers['authorization']
  // Authorization is singular by convention; take the first value if duplicated.
  const auth = Array.isArray(rawAuth) ? rawAuth[0] : rawAuth
  if (typeof auth === 'string' && auth.startsWith('Bearer ')) {
    return auth.slice('Bearer '.length).trim() || null
  }

  const rawCookie = req.headers['cookie']
  const cookieHeader = Array.isArray(rawCookie) ? rawCookie.join('; ') : rawCookie
  if (typeof cookieHeader === 'string') {
    for (const part of cookieHeader.split(';')) {
      const [rawName, ...rest] = part.trim().split('=')
      const name = rawName.replace(/^__Host-|^__Secure-/, '')
      if (name === cookieName) return rest.join('=')
    }
  }
  return null
}
