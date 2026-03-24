import { useState } from 'react';
import { useNavigate, useLocation } from 'react-router-dom';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { useAuth } from '@/auth/auth-context';
import { ApiError } from '@/api/client';
import * as api from '@/api/endpoints';
import type { Problem } from '@/api/types';
import { Button } from '@/components/ui/button';
import { ProblemAlert } from '@/components/problem-alert';
import { ROUTES } from '@/routes/route-paths';

const schema = z
  .object({
    old_password: z.string().min(1, 'Current password is required'),
    new_password: z.string().min(1, 'New password is required'),
    confirm_password: z.string().min(1, 'Confirm password is required'),
  })
  .refine((d) => d.new_password === d.confirm_password, {
    message: "Passwords don't match",
    path: ['confirm_password'],
  });

type FormData = z.infer<typeof schema>;

export function ChangePasswordPage() {
  const { account } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const [problem, setProblem] = useState<Problem | null>(null);

  const locationState = (location.state as { account?: string; old_password?: string } | null);
  const overrideAccount = locationState?.account ?? account;
  const overrideOldPassword = locationState?.old_password ?? '';

  const { register, handleSubmit, formState: { errors, isSubmitting } } = useForm<FormData>({
    resolver: zodResolver(schema),
    defaultValues: {
      old_password: overrideOldPassword,
      new_password: '',
      confirm_password: '',
    },
  });

  async function onSubmit(data: FormData) {
    if (!overrideAccount) return;
    setProblem(null);
    try {
      await api.updatePassword(
        { account: overrideAccount, old_password: data.old_password, new_password: data.new_password },
      );
      navigate(ROUTES.SIGN_IN, { replace: true });
    } catch (err) {
      if (err instanceof ApiError) setProblem(err.problem);
      else setProblem({ status: 0, detail: 'An unexpected error occurred' });
    }
  }

  return (
    <div className="flex min-h-[60vh] items-center justify-center">
      <div className="w-full max-w-md">
        <div className="mansion-card p-10">
          <div className="mb-8 text-center">
            <h1 className="mansion-heading text-2xl">Secure Your Legacy</h1>
            <p className="mansion-subtitle mt-2 text-sm">Update your credentials</p>
          </div>

          <ProblemAlert problem={problem} />

          <form onSubmit={handleSubmit(onSubmit)} className="mt-6 space-y-6">
            <div className="space-y-2">
              <label className="text-xs uppercase tracking-[0.15em] text-muted-foreground">Account</label>
              <input value={overrideAccount ?? ''} disabled className="mansion-input w-full py-2 opacity-50" />
            </div>
            <div className="space-y-2">
              <label htmlFor="old_password" className="text-xs uppercase tracking-[0.15em] text-muted-foreground">
                Current Password
              </label>
              <input id="old_password" type="password" {...register('old_password')} className="mansion-input w-full py-2" />
              {errors.old_password && <p className="text-sm text-destructive">{errors.old_password.message}</p>}
            </div>
            <div className="space-y-2">
              <label htmlFor="new_password" className="text-xs uppercase tracking-[0.15em] text-muted-foreground">
                New Password
              </label>
              <input id="new_password" type="password" {...register('new_password')} className="mansion-input w-full py-2" />
              {errors.new_password && <p className="text-sm text-destructive">{errors.new_password.message}</p>}
            </div>
            <div className="space-y-2">
              <label htmlFor="confirm_password" className="text-xs uppercase tracking-[0.15em] text-muted-foreground">
                Confirm Password
              </label>
              <input id="confirm_password" type="password" {...register('confirm_password')} className="mansion-input w-full py-2" />
              {errors.confirm_password && <p className="text-sm text-destructive">{errors.confirm_password.message}</p>}
            </div>
            <Button
              type="submit"
              className="w-full bg-gold text-primary-foreground uppercase tracking-[0.2em] text-sm font-semibold hover:bg-gold/90"
              disabled={isSubmitting}
            >
              {isSubmitting ? 'Securing...' : 'Confirm'}
            </Button>
          </form>
        </div>
      </div>
    </div>
  );
}
