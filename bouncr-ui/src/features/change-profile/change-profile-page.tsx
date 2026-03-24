import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { useAuth } from '@/auth/use-auth';
import { ApiError } from '@/api/client';
import * as api from '@/api/endpoints';
import type { Problem } from '@/api/types';
import { Button } from '@/components/ui/button';
import { ProblemAlert } from '@/components/problem-alert';
import { LoadingSpinner } from '@/components/loading-spinner';
import { ROUTES } from '@/routes/route-paths';

const schema = z.object({
  name: z.string().min(1, 'Name is required'),
  email: z.string().email('Valid email is required'),
});

type FormData = z.infer<typeof schema>;

export function ChangeProfilePage() {
  const { account } = useAuth();
  const navigate = useNavigate();
  const [problem, setProblem] = useState<Problem | null>(null);
  const [loading, setLoading] = useState(true);

  const { register, handleSubmit, reset, formState: { errors, isSubmitting } } = useForm<FormData>({
    resolver: zodResolver(schema),
  });

  useEffect(() => {
    if (!account) return;
    api.getUser(account).then((user) => {
      if (!user) return;
      reset({ name: String(user.name ?? ''), email: String(user.email ?? '') });
      setLoading(false);
    }).catch((err) => {
      if (err instanceof ApiError) setProblem(err.problem);
      setLoading(false);
    });
  }, [account, reset]);

  async function onSubmit(data: FormData) {
    if (!account) return;
    setProblem(null);
    try {
      await api.updateUser(account, data);
      navigate(ROUTES.HOME, { replace: true });
    } catch (err) {
      if (err instanceof ApiError) setProblem(err.problem);
      else setProblem({ status: 0, detail: 'An unexpected error occurred' });
    }
  }

  if (loading) return <LoadingSpinner />;

  return (
    <div className="flex min-h-[60vh] items-center justify-center">
      <div className="w-full max-w-md">
        <div className="mansion-card p-10">
          <div className="mb-8 text-center">
            <h1 className="mansion-heading text-2xl">Refine Your Presence</h1>
            <p className="mansion-subtitle mt-2 text-sm">Update your personal details</p>
          </div>

          <ProblemAlert problem={problem} />

          <form onSubmit={handleSubmit(onSubmit)} className="mt-6 space-y-6">
            <div className="space-y-2">
              <label className="text-xs uppercase tracking-[0.15em] text-muted-foreground">Account</label>
              <input value={account ?? ''} disabled className="mansion-input w-full py-2 opacity-50" />
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
            <Button
              type="submit"
              className="w-full bg-gold text-primary-foreground uppercase tracking-[0.2em] text-sm font-semibold hover:bg-gold/90"
              disabled={isSubmitting}
            >
              {isSubmitting ? 'Updating...' : 'Save Changes'}
            </Button>
          </form>
        </div>
      </div>
    </div>
  );
}
