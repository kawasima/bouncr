import { describe, it, expect } from 'vitest'
import { extractToken } from './extract-token'
import type { IncomingMessage } from 'node:http'

function req(headers: Record<string, string | string[]>): Pick<IncomingMessage, 'headers'> {
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

  it('handles authorization as string array (multiple header instances)', () => {
    // Authorization is singular by convention — use the first value only
    expect(extractToken(req({ authorization: ['Bearer first', 'Bearer second'] }), 'BOUNCR_TOKEN'))
      .toBe('first')
  })

  it('handles cookie as string array (multiple header instances)', () => {
    expect(extractToken(req({ cookie: ['__Host-BOUNCR_TOKEN=val1', 'other=val2'] }), 'BOUNCR_TOKEN'))
      .toBe('val1')
  })
})
