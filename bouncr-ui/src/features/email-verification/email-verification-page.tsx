import { useState, useEffect } from 'react';
import { useSearchParams, Link } from 'react-router-dom';
import { ApiError } from '@/api/client';
import * as api from '@/api/endpoints';
import type { Problem } from '@/api/types';
import { Button } from '@/components/ui/button';
import { ProblemAlert } from '@/components/problem-alert';
import { LoadingSpinner } from '@/components/loading-spinner';
import { ROUTES } from '@/routes/route-paths';

export function EmailVerificationPage() {
  const [searchParams] = useSearchParams();
  const code = searchParams.get('code');
  const [problem, setProblem] = useState<Problem | null>(null);
  const [verified, setVerified] = useState(false);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    if (!code) {
      setProblem({ status: 400, detail: 'Missing verification code' });
      setLoading(false);
      return;
    }
    api.verifyEmail(code)
      .then(() => {
        setVerified(true);
        setLoading(false);
      })
      .catch((err) => {
        if (err instanceof ApiError) setProblem(err.problem);
        else setProblem({ status: 0, detail: 'An unexpected error occurred' });
        setLoading(false);
      });
  }, [code]);

  if (loading) return <LoadingSpinner />;

  return (
    <div className="flex min-h-[70vh] items-center justify-center">
      <div className="w-full max-w-md">
        <div className="mansion-card p-10 text-center">
          <div className="mb-8">
            <h1 className="mansion-heading text-2xl">Verification</h1>
          </div>

          <ProblemAlert problem={problem} />

          {verified && (
            <div className="border border-gold-muted bg-gold/5 p-5">
              <p className="text-sm text-foreground">
                Your identity has been confirmed. Welcome to the residence.
              </p>
            </div>
          )}

          <div className="mansion-ornament mt-6">
            <span>✦</span>
          </div>

          <Button asChild className="mt-6 w-full bg-gold text-primary-foreground uppercase tracking-[0.2em] text-sm font-semibold hover:bg-gold/90">
            <Link to={ROUTES.SIGN_IN}>Proceed to Sign In</Link>
          </Button>
        </div>
      </div>
    </div>
  );
}
