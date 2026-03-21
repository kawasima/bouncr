import { useState, useEffect } from 'react';
import { useAuth } from '@/auth/auth-context';
import * as api from '@/api/endpoints';
import { ApiError } from '@/api/client';
import type { Problem, WebAuthnCredentialInfo } from '@/api/types';
import { ProblemAlert } from '@/components/problem-alert';
import { Button } from '@/components/ui/button';
import { isWebAuthnSupported, createCredential } from '@/lib/webauthn';

export function PasskeySection({ onRefresh }: { onRefresh: () => void }) {
  const { token } = useAuth();
  const [credentials, setCredentials] = useState<WebAuthnCredentialInfo[]>([]);
  const [problem, setProblem] = useState<Problem | null>(null);
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    if (!token) return;
    api.getWebAuthnCredentials(token).then(setCredentials).catch((err) => {
      if (err instanceof ApiError) setProblem(err.problem);
    });
  }, [token]);

  async function handleRegister() {
    if (!token) return;
    setLoading(true);
    setProblem(null);
    try {
      const options = await api.getWebAuthnRegisterOptions(token);
      const registrationResponseJSON = await createCredential(options);
      const name = prompt('Name this passkey (optional):') || null;
      await api.registerWebAuthn(registrationResponseJSON, name, token);
      const updated = await api.getWebAuthnCredentials(token);
      setCredentials(updated);
      onRefresh();
    } catch (err) {
      if (err instanceof ApiError) {
        setProblem(err.problem);
      } else if (err instanceof DOMException && err.name === 'NotAllowedError') {
        setProblem({ status: 0, detail: 'Passkey registration was cancelled.' });
      } else {
        setProblem({ status: 0, detail: 'Failed to register passkey.' });
      }
    } finally {
      setLoading(false);
    }
  }

  async function handleDelete(id: number) {
    if (!token) return;
    setLoading(true);
    setProblem(null);
    try {
      await api.deleteWebAuthnCredential(id, token);
      setCredentials((prev) => prev.filter((c) => c.id !== id));
    } catch (err) {
      if (err instanceof ApiError) setProblem(err.problem);
    } finally {
      setLoading(false);
    }
  }

  if (!isWebAuthnSupported()) return null;

  return (
    <div className="mansion-card p-8">
      <h2 className="mansion-heading text-sm mb-6">Passkeys</h2>

      <ProblemAlert problem={problem} />

      {credentials.length > 0 && (
        <div className="space-y-3 mb-6">
          {credentials.map((c) => (
            <div key={c.id} className="flex items-center justify-between border border-gold/20 px-4 py-3">
              <div>
                <p className="text-sm">{c.credential_name || 'Unnamed passkey'}</p>
                {c.transports && (
                  <p className="text-xs text-muted-foreground">{c.transports}</p>
                )}
              </div>
              <Button
                variant="outline"
                size="sm"
                onClick={() => handleDelete(c.id)}
                disabled={loading}
                className="text-xs uppercase tracking-[0.1em] border-destructive/40 text-destructive hover:bg-destructive/10"
              >
                Remove
              </Button>
            </div>
          ))}
        </div>
      )}

      <Button
        onClick={handleRegister}
        disabled={loading}
        className="w-full bg-gold text-primary-foreground uppercase tracking-[0.2em] text-xs font-semibold hover:bg-gold/90"
      >
        {loading ? 'Registering...' : 'Register New Passkey'}
      </Button>
    </div>
  );
}
