import { useState } from 'react';
import { useNavigate, useLocation } from 'react-router-dom';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { useAuth } from '@/auth/auth-context';
import { ApiError, PROBLEM_TYPES } from '@/api/client';
import * as api from '@/api/endpoints';
import { isWebAuthnSupported, getAssertion } from '@/lib/webauthn';
import type { Problem } from '@/api/types';
import { Button } from '@/components/ui/button';
import { ProblemAlert } from '@/components/problem-alert';
import { ROUTES } from '@/routes/route-paths';
import { Link } from 'react-router-dom';

const signInSchema = z.object({
  account: z.string().min(1, 'Account is required'),
  password: z.string().min(1, 'Password is required'),
  one_time_password: z.string().optional(),
});

type SignInForm = z.infer<typeof signInSchema>;

export function SignInPage() {
  const { login } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const [problem, setProblem] = useState<Problem | null>(null);
  const [otpRequired, setOtpRequired] = useState(false);
  const [passkeySubmitting, setPasskeySubmitting] = useState(false);

  const { register, handleSubmit, getValues, formState: { errors, isSubmitting } } = useForm<SignInForm>({
    resolver: zodResolver(signInSchema),
  });

  async function onPasskeySignIn() {
    setProblem(null);
    setPasskeySubmitting(true);
    try {
      const enteredAccount = getValues('account') || undefined;
      const options = await api.getWebAuthnSignInOptions(enteredAccount);
      const authJSON = await getAssertion(options);
      const session = await api.signInWithWebAuthn(authJSON);
      login(session.account, session.token);
      const from = (location.state as { from?: { pathname: string } })?.from?.pathname ?? ROUTES.HOME;
      navigate(from, { replace: true });
    } catch (err) {
      if (err instanceof ApiError) {
        setProblem(err.problem);
      } else if (err instanceof DOMException && err.name === 'NotAllowedError') {
        // User cancelled
      } else {
        setProblem({ status: 0, detail: 'Passkey sign-in failed.' });
      }
    } finally {
      setPasskeySubmitting(false);
    }
  }

  async function onSubmit(data: SignInForm) {
    setProblem(null);
    try {
      const session = await api.signIn({
        account: data.account,
        password: data.password,
        one_time_password: data.one_time_password || undefined,
      });
      login(data.account, session.token);
      const from = (location.state as { from?: { pathname: string } })?.from?.pathname ?? ROUTES.HOME;
      navigate(from, { replace: true });
    } catch (err) {
      if (err instanceof ApiError) {
        if (err.problem.type === PROBLEM_TYPES.ONE_TIME_PASSWORD_IS_NEEDED) {
          setOtpRequired(true);
          return;
        }
        if (err.problem.type === PROBLEM_TYPES.PASSWORD_MUST_BE_CHANGED) {
          navigate(ROUTES.CHANGE_PASSWORD, { state: { account: data.account } });
          return;
        }
        setProblem(err.problem);
      } else {
        setProblem({ status: 0, detail: 'An unexpected error occurred' });
      }
    }
  }

  return (
    <div className="flex min-h-[70vh] items-center justify-center">
      <div className="w-full max-w-md">
        <div className="mansion-card p-10">
          <div className="mb-8 text-center">
            <h1 className="mansion-heading text-2xl">Sign In</h1>
            <p className="mansion-subtitle mt-2 text-sm">Your Privileged Gateway</p>
          </div>

          <ProblemAlert problem={problem} />

          <form onSubmit={handleSubmit(onSubmit)} className="mt-6 space-y-6">
            <div className="space-y-2">
              <label htmlFor="account" className="text-xs uppercase tracking-[0.15em] text-muted-foreground">
                Account
              </label>
              <input id="account" {...register('account')} className="mansion-input w-full py-2" />
              {errors.account && <p className="text-sm text-destructive">{errors.account.message}</p>}
            </div>
            <div className="space-y-2">
              <label htmlFor="password" className="text-xs uppercase tracking-[0.15em] text-muted-foreground">
                Password
              </label>
              <input id="password" type="password" {...register('password')} className="mansion-input w-full py-2" />
              {errors.password && <p className="text-sm text-destructive">{errors.password.message}</p>}
            </div>
            {otpRequired && (
              <div className="space-y-2">
                <label htmlFor="otp" className="text-xs uppercase tracking-[0.15em] text-muted-foreground">
                  One-Time Password
                </label>
                <input id="otp" {...register('one_time_password')} placeholder="Enter OTP code" className="mansion-input w-full py-2" />
              </div>
            )}
            <Button
              type="submit"
              className="w-full bg-gold text-primary-foreground uppercase tracking-[0.2em] text-sm font-semibold hover:bg-gold/90 transition-colors"
              disabled={isSubmitting}
            >
              {isSubmitting ? 'Entering...' : 'Enter'}
            </Button>
          </form>

          {isWebAuthnSupported() && (
            <>
              <div className="mansion-divider my-6" />
              <Button
                type="button"
                variant="outline"
                className="w-full uppercase tracking-[0.15em] text-xs border-gold-muted text-gold hover:bg-gold/10"
                disabled={isSubmitting || passkeySubmitting}
                onClick={onPasskeySignIn}
              >
                {passkeySubmitting ? 'Authenticating...' : 'Sign in with Passkey'}
              </Button>
            </>
          )}

          <div className="mansion-ornament mt-8">
            <span>✦</span>
          </div>

          <div className="mt-4 text-center text-sm">
            <Link to={ROUTES.RESET_PASSWORD_CHALLENGE} className="text-muted-foreground transition-colors hover:text-gold">
              Forgot password?
            </Link>
          </div>
        </div>
      </div>
    </div>
  );
}
