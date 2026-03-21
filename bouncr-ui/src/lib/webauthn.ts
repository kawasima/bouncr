export function base64urlToBuffer(base64url: string): ArrayBuffer {
  const base64 = base64url.replace(/-/g, '+').replace(/_/g, '/');
  const pad = base64.length % 4 === 0 ? '' : '='.repeat(4 - (base64.length % 4));
  const binary = atob(base64 + pad);
  const bytes = new Uint8Array(binary.length);
  for (let i = 0; i < binary.length; i++) {
    bytes[i] = binary.charCodeAt(i);
  }
  return bytes.buffer;
}

export function bufferToBase64url(buffer: ArrayBuffer): string {
  const bytes = new Uint8Array(buffer);
  let binary = '';
  for (const byte of bytes) {
    binary += String.fromCharCode(byte);
  }
  return btoa(binary).replace(/\+/g, '-').replace(/\//g, '_').replace(/=/g, '');
}

export interface RegistrationOptions {
  challenge: string;
  rp: { id: string; name: string };
  user: { id: string; name: string; displayName: string };
  pubKeyCredParams: Array<{ type: string; alg: number }>;
  excludeCredentials?: Array<{ type: string; id: string }>;
  authenticatorSelection?: Record<string, string>;
  attestation?: string;
}

export interface AuthenticationOptions {
  challenge: string;
  rpId: string;
  allowCredentials?: Array<{ type: string; id: string; transports?: string[] }>;
  userVerification?: string;
}

export async function createCredential(options: RegistrationOptions): Promise<string> {
  const publicKey: PublicKeyCredentialCreationOptions = {
    challenge: base64urlToBuffer(options.challenge),
    rp: options.rp,
    user: {
      id: base64urlToBuffer(options.user.id),
      name: options.user.name,
      displayName: options.user.displayName,
    },
    pubKeyCredParams: options.pubKeyCredParams.map((p) => ({
      type: p.type as PublicKeyCredentialType,
      alg: p.alg,
    })),
    excludeCredentials: options.excludeCredentials?.map((c) => ({
      type: c.type as PublicKeyCredentialType,
      id: base64urlToBuffer(c.id),
    })),
    authenticatorSelection: options.authenticatorSelection as AuthenticatorSelectionCriteria | undefined,
    attestation: (options.attestation ?? 'none') as AttestationConveyancePreference,
  };

  const credential = (await navigator.credentials.create({ publicKey })) as PublicKeyCredential;
  const response = credential.response as AuthenticatorAttestationResponse;

  const registrationResponseJSON = JSON.stringify({
    id: credential.id,
    rawId: bufferToBase64url(credential.rawId),
    type: credential.type,
    response: {
      attestationObject: bufferToBase64url(response.attestationObject),
      clientDataJSON: bufferToBase64url(response.clientDataJSON),
      transports: response.getTransports?.() ?? [],
    },
  });

  return registrationResponseJSON;
}

export async function getAssertion(options: AuthenticationOptions): Promise<string> {
  const publicKey: PublicKeyCredentialRequestOptions = {
    challenge: base64urlToBuffer(options.challenge),
    rpId: options.rpId,
    allowCredentials: options.allowCredentials?.map((c) => ({
      type: c.type as PublicKeyCredentialType,
      id: base64urlToBuffer(c.id),
      transports: c.transports as AuthenticatorTransport[] | undefined,
    })),
    userVerification: (options.userVerification ?? 'preferred') as UserVerificationRequirement,
  };

  const credential = (await navigator.credentials.get({ publicKey })) as PublicKeyCredential;
  const response = credential.response as AuthenticatorAssertionResponse;

  const authenticationResponseJSON = JSON.stringify({
    id: credential.id,
    rawId: bufferToBase64url(credential.rawId),
    type: credential.type,
    response: {
      authenticatorData: bufferToBase64url(response.authenticatorData),
      clientDataJSON: bufferToBase64url(response.clientDataJSON),
      signature: bufferToBase64url(response.signature),
      userHandle: response.userHandle ? bufferToBase64url(response.userHandle) : null,
    },
  });

  return authenticationResponseJSON;
}

export function isWebAuthnSupported(): boolean {
  return typeof window !== 'undefined' && !!window.PublicKeyCredential;
}
