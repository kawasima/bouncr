import { useEffect, useState } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { useAuth } from '@/auth/auth-context';
import { ROUTES } from '@/routes/route-paths';
import { LoadingSpinner } from '@/components/loading-spinner';

export function OidcCallbackPage() {
  const [searchParams] = useSearchParams();
  const { login } = useAuth();
  const navigate = useNavigate();
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    const account = searchParams.get('account');
    const code = searchParams.get('code');

    if (account) {
      login(account);
      navigate(ROUTES.HOME, { replace: true });
    } else if (code) {
      navigate(`${ROUTES.SIGN_UP}?code=${encodeURIComponent(code)}`, { replace: true });
    } else {
      setError('Invalid OIDC callback. Missing token or code.');
    }
  }, [searchParams, login, navigate]);

  if (error) {
    return (
      <div className="flex min-h-[70vh] items-center justify-center">
        <div className="w-full max-w-md">
          <div className="mansion-card p-10 text-center">
            <h1 className="mansion-heading text-xl">Authentication Error</h1>
            <div className="mt-6 border border-destructive/40 bg-destructive/5 p-4">
              <p className="text-sm text-destructive">{error}</p>
            </div>
          </div>
        </div>
      </div>
    );
  }

  return <LoadingSpinner />;
}
