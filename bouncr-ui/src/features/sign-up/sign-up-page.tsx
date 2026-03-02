import { useState } from 'react';
import { useSearchParams, Link } from 'react-router-dom';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { ApiError } from '@/api/client';
import * as api from '@/api/endpoints';
import type { Problem } from '@/api/types';
import { Button } from '@/components/ui/button';
import { ProblemAlert } from '@/components/problem-alert';
import { ROUTES } from '@/routes/route-paths';

const signUpSchema = z.object({
  account: z.string().min(1, 'Account is required'),
  name: z.string().min(1, 'Name is required'),
  email: z.string().email('Valid email is required'),
});

type SignUpForm = z.infer<typeof signUpSchema>;

export function SignUpPage() {
  const [searchParams] = useSearchParams();
  const code = searchParams.get('code');
  const [problem, setProblem] = useState<Problem | null>(null);
  const [initialPassword, setInitialPassword] = useState<string | null>(null);
  const [enablePassword, setEnablePassword] = useState(true);
  const [signedUpWithoutPassword, setSignedUpWithoutPassword] = useState(false);

  const { register, handleSubmit, formState: { errors, isSubmitting } } = useForm<SignUpForm>({
    resolver: zodResolver(signUpSchema),
  });

  async function onSubmit(data: SignUpForm) {
    setProblem(null);
    try {
      if (enablePassword) {
        const result = await api.signUp({
          account: data.account,
          name: data.name,
          email: data.email,
          code: code ?? undefined,
          enable_password_credential: true,
        });
        setInitialPassword(result.password);
      } else {
        await api.signUp({
          account: data.account,
          name: data.name,
          email: data.email,
          code: code ?? undefined,
          enable_password_credential: false,
        });
        setSignedUpWithoutPassword(true);
      }
    } catch (err) {
      if (err instanceof ApiError) {
        setProblem(err.problem);
      } else {
        setProblem({ status: 0, detail: 'An unexpected error occurred' });
      }
    }
  }

  if (signedUpWithoutPassword) {
    return (
      <div className="flex min-h-[70vh] items-center justify-center">
        <div className="w-full max-w-md">
          <div className="mansion-card p-10">
            <div className="mb-8 text-center">
              <h1 className="mansion-heading text-2xl">Welcome</h1>
              <p className="mansion-subtitle mt-2 text-sm">Your residence has been established</p>
            </div>
            <p className="text-center text-sm text-muted-foreground">
              Your account has been created without a password credential.
              Sign in using an OIDC provider.
            </p>
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

  if (initialPassword) {
    return (
      <div className="flex min-h-[70vh] items-center justify-center">
        <div className="w-full max-w-md">
          <div className="mansion-card p-10">
            <div className="mb-8 text-center">
              <h1 className="mansion-heading text-2xl">Welcome</h1>
              <p className="mansion-subtitle mt-2 text-sm">Your residence has been established</p>
            </div>

            <div className="border border-gold-muted bg-gold/5 p-5 text-center">
              <p className="text-xs uppercase tracking-[0.15em] text-muted-foreground">Your Initial Password</p>
              <p className="mt-2 font-mono text-lg text-gold">{initialPassword}</p>
            </div>

            <p className="mt-4 text-center text-sm italic text-muted-foreground">
              Please preserve this password. You may change it after signing in.
            </p>

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

  return (
    <div className="flex min-h-[70vh] items-center justify-center">
      <div className="w-full max-w-md">
        <div className="mansion-card p-10">
          <div className="mb-8 text-center">
            <h1 className="mansion-heading text-2xl">Begin Your Residence</h1>
            <p className="mansion-subtitle mt-2 text-sm">Create your distinguished presence</p>
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
              <label htmlFor="name" className="text-xs uppercase tracking-[0.15em] text-muted-foreground">
                Name
              </label>
              <input id="name" {...register('name')} className="mansion-input w-full py-2" />
              {errors.name && <p className="text-sm text-destructive">{errors.name.message}</p>}
            </div>
            <div className="space-y-2">
              <label htmlFor="email" className="text-xs uppercase tracking-[0.15em] text-muted-foreground">
                Email
              </label>
              <input id="email" type="email" {...register('email')} className="mansion-input w-full py-2" />
              {errors.email && <p className="text-sm text-destructive">{errors.email.message}</p>}
            </div>
            <label className="flex items-center gap-2 cursor-pointer">
              <input
                type="checkbox"
                checked={!enablePassword}
                onChange={(e) => setEnablePassword(!e.target.checked)}
                className="accent-gold"
              />
              <span className="text-xs text-muted-foreground">
                Disable password (use OIDC provider only)
              </span>
            </label>
            <Button
              type="submit"
              className="w-full bg-gold text-primary-foreground uppercase tracking-[0.2em] text-sm font-semibold hover:bg-gold/90"
              disabled={isSubmitting}
            >
              {isSubmitting ? 'Establishing...' : 'Establish'}
            </Button>
          </form>

          <div className="mansion-ornament mt-8">
            <span>✦</span>
          </div>

          <div className="mt-4 text-center text-sm">
            <Link to={ROUTES.SIGN_IN} className="text-muted-foreground transition-colors hover:text-gold">
              Already a resident? Sign in
            </Link>
          </div>
        </div>
      </div>
    </div>
  );
}
