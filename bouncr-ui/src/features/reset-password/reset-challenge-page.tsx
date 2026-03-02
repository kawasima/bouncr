import { useState } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { ApiError } from '@/api/client';
import * as api from '@/api/endpoints';
import type { Problem } from '@/api/types';
import { Button } from '@/components/ui/button';
import { ProblemAlert } from '@/components/problem-alert';
import { Link } from 'react-router-dom';
import { ROUTES } from '@/routes/route-paths';

const schema = z.object({
  account: z.string().min(1, 'Account is required'),
});

type FormData = z.infer<typeof schema>;

export function ResetChallengePage() {
  const [problem, setProblem] = useState<Problem | null>(null);
  const [completed, setCompleted] = useState(false);

  const { register, handleSubmit, formState: { errors, isSubmitting } } = useForm<FormData>({
    resolver: zodResolver(schema),
  });

  async function onSubmit(data: FormData) {
    setProblem(null);
    try {
      await api.requestPasswordReset({ account: data.account });
      setCompleted(true);
    } catch (err) {
      if (err instanceof ApiError) setProblem(err.problem);
      else setProblem({ status: 0, detail: 'An unexpected error occurred' });
    }
  }

  return (
    <div className="flex min-h-[70vh] items-center justify-center">
      <div className="w-full max-w-md">
        <div className="mansion-card p-10">
          <div className="mb-8 text-center">
            <h1 className="mansion-heading text-2xl">Reclaim Your Access</h1>
            <p className="mansion-subtitle mt-2 text-sm">We will send you a reset code</p>
          </div>

          {completed ? (
            <div className="border border-gold-muted bg-gold/5 p-5 text-center">
              <p className="text-sm text-foreground">
                A password reset code has been dispatched. Please consult your correspondence.
              </p>
            </div>
          ) : (
            <>
              <ProblemAlert problem={problem} />
              <form onSubmit={handleSubmit(onSubmit)} className="mt-6 space-y-6">
                <div className="space-y-2">
                  <label htmlFor="account" className="text-xs uppercase tracking-[0.15em] text-muted-foreground">
                    Account
                  </label>
                  <input id="account" {...register('account')} className="mansion-input w-full py-2" />
                  {errors.account && <p className="text-sm text-destructive">{errors.account.message}</p>}
                </div>
                <Button
                  type="submit"
                  className="w-full bg-gold text-primary-foreground uppercase tracking-[0.2em] text-sm font-semibold hover:bg-gold/90"
                  disabled={isSubmitting}
                >
                  {isSubmitting ? 'Dispatching...' : 'Send Reset Code'}
                </Button>
              </form>
            </>
          )}

          <div className="mansion-ornament mt-8">
            <span>✦</span>
          </div>

          <div className="mt-4 text-center text-sm">
            <Link to={ROUTES.SIGN_IN} className="text-muted-foreground transition-colors hover:text-gold">
              Return to Sign In
            </Link>
          </div>
        </div>
      </div>
    </div>
  );
}
