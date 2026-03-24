import { useState, useEffect } from 'react';
import { useSearchParams, Link } from 'react-router-dom';
import { ApiError } from '@/api/client';
import * as api from '@/api/endpoints';
import type { Problem } from '@/api/types';
import { Button } from '@/components/ui/button';
import { ProblemAlert } from '@/components/problem-alert';
import { LoadingSpinner } from '@/components/loading-spinner';
import { ROUTES } from '@/routes/route-paths';

export function ResetPasswordPage() {
  const [searchParams] = useSearchParams();
  const code = searchParams.get('code');
  const [problem, setProblem] = useState<Problem | null>(
    code ? null : { status: 400, detail: 'Missing reset code' },
  );
  const [initialPassword, setInitialPassword] = useState<string | null>(null);
  const [loading, setLoading] = useState(!!code);

  useEffect(() => {
    if (!code) return;
    api.resetPassword({ code })
      .then((result) => {
        setInitialPassword(result?.password ?? null);
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
        <div className="mansion-card p-10">
          <div className="mb-8 text-center">
            <h1 className="mansion-heading text-2xl">A New Beginning</h1>
            <p className="mansion-subtitle mt-2 text-sm">Your credentials have been renewed</p>
          </div>

          <ProblemAlert problem={problem} />

          {initialPassword && (
            <>
              <div className="border border-gold-muted bg-gold/5 p-5 text-center">
                <p className="text-xs uppercase tracking-[0.15em] text-muted-foreground">Your New Password</p>
                <p className="mt-2 font-mono text-lg text-gold">{initialPassword}</p>
              </div>
              <p className="mt-4 text-center text-sm italic text-muted-foreground">
                Please preserve this password. You may change it after signing in.
              </p>
            </>
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
