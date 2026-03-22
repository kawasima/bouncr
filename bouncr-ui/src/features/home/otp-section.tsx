import { useState } from 'react';
import * as api from '@/api/endpoints';
import { Button } from '@/components/ui/button';
import { ApiError } from '@/api/client';
import type { Problem } from '@/api/types';
import { ProblemAlert } from '@/components/problem-alert';

interface OtpSectionProps {
  otpKey: string | null | undefined;
  account: string;
  onRefresh: () => void;
}

export function OtpSection({ otpKey, account, onRefresh }: OtpSectionProps) {
  const [problem, setProblem] = useState<Problem | null>(null);
  const [loading, setLoading] = useState(false);

  async function handleGenerate() {
    setLoading(true);
    setProblem(null);
    try {
      await api.createOtpKey();
      onRefresh();
    } catch (err) {
      if (err instanceof ApiError) setProblem(err.problem);
    } finally {
      setLoading(false);
    }
  }

  async function handleDelete() {
    setLoading(true);
    setProblem(null);
    try {
      await api.deleteOtpKey();
      onRefresh();
    } catch (err) {
      if (err instanceof ApiError) setProblem(err.problem);
    } finally {
      setLoading(false);
    }
  }

  const otpUri = otpKey
    ? `otpauth://totp/Bouncr:${encodeURIComponent(account)}?secret=${otpKey}&issuer=Bouncr`
    : null;

  return (
    <div className="mansion-card p-8">
      <h2 className="mansion-heading text-sm mb-6">Two-Factor Authentication</h2>

      <ProblemAlert problem={problem} />

      {otpKey ? (
        <div className="space-y-6">
          <p className="text-sm italic text-muted-foreground">
            Your second factor is active. Scan the code below with your authenticator.
          </p>
          <div className="flex justify-center">
            <div className="border border-gold-muted p-3 bg-white">
              <img
                src={`https://api.qrserver.com/v1/create-qr-code/?size=200x200&data=${encodeURIComponent(otpUri!)}`}
                alt="OTP QR Code"
                className="h-44 w-44"
              />
            </div>
          </div>
          <Button
            variant="outline"
            onClick={handleDelete}
            disabled={loading}
            className="w-full text-xs uppercase tracking-[0.15em] border-destructive/40 text-destructive hover:bg-destructive/10"
          >
            {loading ? 'Removing...' : 'Remove OTP Key'}
          </Button>
        </div>
      ) : (
        <div className="space-y-4">
          <p className="text-sm italic text-muted-foreground">
            Elevate your security. Enable two-factor authentication to safeguard your residence.
          </p>
          <Button
            onClick={handleGenerate}
            disabled={loading}
            className="w-full bg-gold text-primary-foreground uppercase tracking-[0.2em] text-xs font-semibold hover:bg-gold/90"
          >
            {loading ? 'Generating...' : 'Enable OTP'}
          </Button>
        </div>
      )}
    </div>
  );
}
