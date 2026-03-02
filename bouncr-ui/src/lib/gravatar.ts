export async function gravatarUrl(email: string | undefined, size: number = 80): Promise<string> {
  if (!email) {
    return `https://www.gravatar.com/avatar/?s=${size}&d=identicon`;
  }
  const normalized = email.trim().toLowerCase();
  const encoder = new TextEncoder();
  const data = encoder.encode(normalized);
  const hashBuffer = await crypto.subtle.digest('SHA-256', data);
  const hashArray = Array.from(new Uint8Array(hashBuffer));
  const hashHex = hashArray.map((b) => b.toString(16).padStart(2, '0')).join('');
  return `https://www.gravatar.com/avatar/${hashHex}?s=${size}&d=identicon`;
}
