import { useState, useEffect, useCallback } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import * as api from '@/api/endpoints';
import { ApiError } from '@/api/client';
import type { Group, Problem } from '@/api/types';
import { Button } from '@/components/ui/button';
import { ProblemAlert } from '@/components/problem-alert';

const invitationSchema = z.object({
  email: z.string().email('Valid email is required'),
});

type InvitationFormData = z.infer<typeof invitationSchema>;

export function InvitationsAdminPage() {
  const [problem, setProblem] = useState<Problem | null>(null);
  const [allGroups, setAllGroups] = useState<Group[]>([]);
  const [selectedGroups, setSelectedGroups] = useState<Set<number>>(new Set());
  const [createdCode, setCreatedCode] = useState<string | null>(null);

  const loadGroups = useCallback(async () => {
    try {
      const groups = await api.getGroups({ limit: 1000 });
      setAllGroups(groups ?? []);
    } catch { /* ignore */ }
  }, []);

  // eslint-disable-next-line react-hooks/set-state-in-effect
  useEffect(() => { loadGroups(); }, [loadGroups]);

  const { register, handleSubmit, reset, formState: { errors, isSubmitting } } = useForm<InvitationFormData>({
    resolver: zodResolver(invitationSchema),
    defaultValues: { email: '' },
  });

  const onSubmit = async (data: InvitationFormData) => {
    setProblem(null);
    try {
      const groupIds = Array.from(selectedGroups).map((id) => ({ id }));
      const result = await api.createInvitation({ email: data.email, groups: groupIds });
      setCreatedCode(result?.code ?? null);
    } catch (err) {
      if (err instanceof ApiError) setProblem(err.problem);
      else setProblem({ status: 0, detail: 'An unexpected error occurred' });
    }
  };

  const handleReset = () => {
    setCreatedCode(null);
    setProblem(null);
    setSelectedGroups(new Set());
    reset({ email: '' });
  };

  return (
    <div className="space-y-6">
      <h2 className="mansion-heading text-lg">New Invitation</h2>
      <ProblemAlert problem={problem} />
      {createdCode ? (
        <div className="space-y-6">
          <div className="mansion-card p-6 space-y-4">
            <p className="text-xs uppercase tracking-[0.15em] text-gold">Invitation Created</p>
            <p className="text-sm">Invitation code:</p>
            <code className="block font-mono text-sm bg-card p-3 border border-gold/20 rounded-sm break-all">{createdCode}</code>
          </div>
          <Button
            type="button"
            onClick={handleReset}
            className="bg-gold text-primary-foreground uppercase tracking-[0.15em] text-xs font-semibold hover:bg-gold/90"
          >
            Create Another
          </Button>
        </div>
      ) : (
        <form onSubmit={handleSubmit(onSubmit)} className="max-w-md space-y-6">
          <div className="space-y-2">
            <label htmlFor="email" className="text-xs uppercase tracking-[0.15em] text-muted-foreground">
              Email
            </label>
            <input id="email" type="email" {...register('email')} className="mansion-input w-full py-2" />
            {errors.email && <p className="text-sm text-destructive">{errors.email.message}</p>}
          </div>
          {allGroups.length > 0 && (
            <div className="space-y-2">
              <label className="text-xs uppercase tracking-[0.15em] text-muted-foreground">Groups</label>
              <div className="max-h-48 overflow-y-auto space-y-1 border border-gold/20 rounded-sm p-3">
                {allGroups.map((g) => (
                  <label key={g.id} className="flex items-center gap-2 cursor-pointer hover:bg-gold/5 px-2 py-1 rounded-sm">
                    <input
                      type="checkbox"
                      checked={selectedGroups.has(g.id)}
                      onChange={(e) => {
                        const next = new Set(selectedGroups);
                        if (e.target.checked) next.add(g.id);
                        else next.delete(g.id);
                        setSelectedGroups(next);
                      }}
                      className="accent-gold"
                    />
                    <span className="text-sm">{g.name}</span>
                  </label>
                ))}
              </div>
            </div>
          )}
          <Button
            type="submit"
            disabled={isSubmitting}
            className="bg-gold text-primary-foreground uppercase tracking-[0.15em] text-xs font-semibold hover:bg-gold/90"
          >
            {isSubmitting ? 'Creating...' : 'Create'}
          </Button>
        </form>
      )}
    </div>
  );
}
