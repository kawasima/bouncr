import { useState, useEffect, useRef } from 'react';
import * as api from '@/api/endpoints';
import { ApiError } from '@/api/client';
import type { Problem, WebAuthnCredentialInfo } from '@/api/types';
import { ProblemAlert } from '@/components/problem-alert';
import { Button } from '@/components/ui/button';
import { isWebAuthnSupported, createCredential } from '@/lib/webauthn';

export function PasskeySection({ onRefresh }: { onRefresh: () => void }) {
  const [credentials, setCredentials] = useState<WebAuthnCredentialInfo[]>([]);
  const [problem, setProblem] = useState<Problem | null>(null);
  const [loading, setLoading] = useState(false);
  const [namingPasskey, setNamingPasskey] = useState(false);
  const [passkeyName, setPasskeyName] = useState('');
  const pendingResponseRef = useRef<string | null>(null);
  const nameInputRef = useRef<HTMLInputElement>(null);

  useEffect(() => {
    api.getWebAuthnCredentials()
      .then((r) => setCredentials(r ?? []))
      .catch(() => setCredentials([]));
  }, []);

  useEffect(() => {
    if (namingPasskey && nameInputRef.current) {
      nameInputRef.current.focus();
    }
  }, [namingPasskey]);

  async function handleRegister() {
    setLoading(true);
    setProblem(null);
    try {
      const options = await api.getWebAuthnRegisterOptions();
      if (!options) throw new Error('Failed to get registration options from server');
      const registrationResponseJSON = await createCredential(options);
      pendingResponseRef.current = registrationResponseJSON;
      setPasskeyName('');
      setNamingPasskey(true);
      setLoading(false);
    } catch (err) {
      if (err instanceof ApiError) {
        setProblem(err.problem);
      } else if (err instanceof DOMException && err.name === 'NotAllowedError') {
        setProblem({ status: 0, detail: 'Passkey registration was cancelled.' });
      } else {
        setProblem({ status: 0, detail: 'Failed to register passkey.' });
      }
      setLoading(false);
    }
  }

  async function handleConfirmName() {
    if (!pendingResponseRef.current) return;
    setLoading(true);
    try {
      await api.registerWebAuthn(pendingResponseRef.current, passkeyName.trim() || null);
      const updated = await api.getWebAuthnCredentials();
      setCredentials(updated ?? []);
      onRefresh();
    } catch (err) {
      if (err instanceof ApiError) setProblem(err.problem);
      else setProblem({ status: 0, detail: 'Failed to register passkey.' });
    } finally {
      pendingResponseRef.current = null;
      setNamingPasskey(false);
      setLoading(false);
    }
  }

  async function handleDelete(id: number) {
    setLoading(true);
    setProblem(null);
    try {
      await api.deleteWebAuthnCredential(id);
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

      {namingPasskey ? (
        <div className="space-y-3 border border-gold/20 rounded-sm p-4">
          <label htmlFor="passkey-name" className="text-xs uppercase tracking-[0.15em] text-muted-foreground">
            Name this passkey (optional)
          </label>
          <input
            ref={nameInputRef}
            id="passkey-name"
            value={passkeyName}
            onChange={(e) => setPasskeyName(e.target.value)}
            onKeyDown={(e) => { if (e.key === 'Enter') { e.preventDefault(); handleConfirmName(); } }}
            placeholder="e.g. MacBook Touch ID"
            className="mansion-input w-full py-2"
          />
          <div className="flex gap-3">
            <Button
              onClick={handleConfirmName}
              disabled={loading}
              className="bg-gold text-primary-foreground uppercase tracking-[0.15em] text-xs font-semibold hover:bg-gold/90"
            >
              {loading ? 'Saving...' : 'Save'}
            </Button>
            <Button
              onClick={() => { setNamingPasskey(false); pendingResponseRef.current = null; }}
              variant="outline"
              className="uppercase tracking-[0.15em] text-xs"
            >
              Cancel
            </Button>
          </div>
        </div>
      ) : (
        <Button
          onClick={handleRegister}
          disabled={loading}
          className="w-full bg-gold text-primary-foreground uppercase tracking-[0.2em] text-xs font-semibold hover:bg-gold/90"
        >
          {loading ? 'Registering...' : 'Register New Passkey'}
        </Button>
      )}
    </div>
  );
}
