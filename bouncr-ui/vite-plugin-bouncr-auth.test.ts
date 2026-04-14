import { describe, it, expect } from 'vitest'
import type { IncomingMessage } from 'node:http'

// extractToken is a module-level function — import via dynamic re-export shim.
// We test the logic by reproducing the function inline here, keeping it in sync
// with the implementation. This avoids a circular dependency on the Vite plugin
// module (which imports vite internals not available in the test environment).
//
// If extractToken is ever moved to a separate utility module, import it directly.
function extractToken(req: Pick<IncomingMessage, 'headers'>, cookieName: string): string | null {
  const auth = req.headers['authorization']
  if (typeof auth === 'string' && auth.startsWith('Bearer ')) return auth.slice(7)

  const cookieHeader = req.headers['cookie']
  if (typeof cookieHeader === 'string') {
    for (const part of cookieHeader.split(';')) {
      const [rawName, ...rest] = part.trim().split('=')
      const name = rawName.replace(/^__Host-|^__Secure-/, '')
      if (name === cookieName) return rest.join('=')
    }
  }
  return null
}

function req(headers: Record<string, string>): Pick<IncomingMessage, 'headers'> {
  return { headers }
}

describe('extractToken', () => {
  it('extracts token from Authorization Bearer header', () => {
    expect(extractToken(req({ authorization: 'Bearer my-token' }), 'BOUNCR_TOKEN'))
      .toBe('my-token')
  })

  it('extracts plain cookie by name', () => {
    expect(extractToken(req({ cookie: 'BOUNCR_TOKEN=plain-value' }), 'BOUNCR_TOKEN'))
      .toBe('plain-value')
  })

  it('extracts __Host- prefixed cookie', () => {
    expect(extractToken(req({ cookie: '__Host-BOUNCR_TOKEN=host-value' }), 'BOUNCR_TOKEN'))
      .toBe('host-value')
  })

  it('extracts __Secure- prefixed cookie', () => {
    expect(extractToken(req({ cookie: '__Secure-BOUNCR_TOKEN=secure-value' }), 'BOUNCR_TOKEN'))
      .toBe('secure-value')
  })

  it('returns null when cookie name does not match', () => {
    expect(extractToken(req({ cookie: 'OTHER=xyz' }), 'BOUNCR_TOKEN'))
      .toBeNull()
  })

  it('handles cookie value containing = (e.g. base64)', () => {
    expect(extractToken(req({ cookie: '__Host-BOUNCR_TOKEN=abc=def==' }), 'BOUNCR_TOKEN'))
      .toBe('abc=def==')
  })

  it('returns null when no headers present', () => {
    expect(extractToken(req({}), 'BOUNCR_TOKEN')).toBeNull()
  })

  it('Authorization takes precedence over cookie', () => {
    expect(extractToken(
      req({ authorization: 'Bearer auth-token', cookie: '__Host-BOUNCR_TOKEN=cookie-token' }),
      'BOUNCR_TOKEN'
    )).toBe('auth-token')
  })
})
