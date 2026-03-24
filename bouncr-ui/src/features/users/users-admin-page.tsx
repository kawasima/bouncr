import { useState, useEffect } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import * as api from '@/api/endpoints';
import { usePermissions } from '@/auth/permission-context';
import { RESOURCE_PERMISSIONS } from '@/auth/permissions';
import { ApiError } from '@/api/client';
import { AdminCrudPage } from '@/features/admin/admin-crud-page';
import type { AdminCrudConfig } from '@/features/admin/use-admin-crud';
import type { ColumnDef } from '@/components/data-table';
import type { User, Problem } from '@/api/types';
import { Button } from '@/components/ui/button';
import { ProblemAlert } from '@/components/problem-alert';
import { Lock, Trash2 } from 'lucide-react';
import { gravatarUrl } from '@/lib/gravatar';

function useGravatar(email: string | undefined, size: number = 160) {
  const placeholder = `https://www.gravatar.com/avatar/?s=${size}&d=identicon`;
  const [state, setState] = useState({ url: placeholder, loaded: false });
  useEffect(() => {
    let cancelled = false;
    // eslint-disable-next-line react-hooks/set-state-in-effect -- reset loaded state on email change
    setState((prev) => ({ ...prev, loaded: false }));
    gravatarUrl(email, size).then((u) => {
      if (cancelled) return;
      const img = document.createElement('img');
      img.onload = () => { if (!cancelled) setState({ url: u, loaded: true }); };
      img.onerror = () => { if (!cancelled) setState({ url: u, loaded: true }); };
      img.src = u;
    });
    return () => { cancelled = true; };
  }, [email, size]);
  return state;
}

const config: AdminCrudConfig<User> = {
  fetchList: api.getUsers,
  fetchOne: (account) => api.getUser(account, 'groups'),
  create: api.createUser,
  update: api.updateUser,
  getIdentifier: (u) => u.account,
};

const columns: ColumnDef<User>[] = [
  { header: 'Account', accessor: 'account' },
  { header: 'Name', accessor: (u) => u.name ? String(u.name) : '-' },
  { header: 'Email', accessor: (u) => u.email ? String(u.email) : '-' },
];

const userSchema = z.object({
  account: z.string().min(1, 'Account is required'),
  name: z.string().min(1, 'Name is required'),
  email: z.string().email('Valid email is required'),
});

type UserFormData = z.infer<typeof userSchema>;

function PasswordCredentialSection({ account, onCreated }: { account: string; onCreated: () => void }) {
  const [open, setOpen] = useState(false);
  const [password, setPassword] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<Problem | null>(null);

  const handleCreate = async () => {
    if (!password) return;
    setSubmitting(true);
    setError(null);
    try {
      await api.createPasswordCredential({ account, password, initial: true });
      onCreated();
    } catch (err) {
      if (err instanceof ApiError) setError(err.problem);
    } finally {
      setSubmitting(false);
    }
  };

  if (!open) {
    return (
      <button
        type="button"
        onClick={() => setOpen(true)}
        className="flex items-center gap-2 text-xs uppercase tracking-[0.15em] text-gold hover:text-gold/80 transition-colors"
      >
        <Lock className="h-3 w-3" />
        Add password credential
      </button>
    );
  }

  return (
    <div className="space-y-4 border border-gold/20 rounded-sm p-4">
      <p className="text-xs uppercase tracking-[0.15em] text-muted-foreground">
        <Lock className="inline h-3 w-3 mr-1" />
        New password credential
      </p>
      <ProblemAlert problem={error} />
      <div className="space-y-2">
        <label htmlFor="new-password" className="text-xs uppercase tracking-[0.15em] text-muted-foreground">
          Password
        </label>
        <input
          id="new-password"
          type="password"
          value={password}
          onChange={(e) => setPassword(e.target.value)}
          className="mansion-input w-full py-2"
        />
      </div>
      <div className="flex gap-3">
        <Button
          type="button"
          onClick={handleCreate}
          disabled={submitting || !password}
          className="bg-gold text-primary-foreground uppercase tracking-[0.15em] text-xs font-semibold hover:bg-gold/90"
        >
          {submitting ? 'Creating...' : 'Create'}
        </Button>
        <Button
          type="button"
          onClick={() => { setOpen(false); setPassword(''); setError(null); }}
          variant="outline"
          className="uppercase tracking-[0.15em] text-xs"
        >
          Cancel
        </Button>
      </div>
    </div>
  );
}

function UserEditForm({
  target,
  onSubmit,
  problem,
  onDeleted,
  canUpdate = true,
}: {
  target: User | null;
  onSubmit: (data: Record<string, unknown>) => Promise<boolean>;
  problem: Problem | null;
  onDeleted?: () => void;
  canUpdate?: boolean;
}) {
  const isCreate = !target;
  const isReadOnly = !isCreate && !canUpdate;
  const [enablePassword, setEnablePassword] = useState(false);
  const [password, setPassword] = useState('');
  const [credProblem, setCredProblem] = useState<Problem | null>(null);
  const [credCreated, setCredCreated] = useState(false);
  const [deleting, setDeleting] = useState(false);
  const [confirmDelete, setConfirmDelete] = useState(false);
  const { url: avatarUrl, loaded: avatarLoaded } = useGravatar(target?.email ? String(target.email) : undefined);

  const { register, handleSubmit, formState: { errors, isSubmitting } } = useForm<UserFormData>({
    resolver: zodResolver(userSchema),
    defaultValues: target
      ? { account: target.account, name: String(target.name ?? ''), email: String(target.email ?? '') }
      : { account: '', name: '', email: '' },
  });

  const handleFormSubmit = async (d: UserFormData) => {
    if (isReadOnly) return false;
    const ok = await onSubmit(d);
    if (ok && isCreate && enablePassword && password) {
      try {
        await api.createPasswordCredential({ account: d.account, password, initial: true });
        setCredCreated(true);
      } catch (err) {
        if (err instanceof ApiError) setCredProblem(err.problem);
      }
    }
    return ok;
  };

  const handleDelete = async () => {
    if (!target) return;
    setDeleting(true);
    try {
      await api.deleteUser(target.account);
      onDeleted?.();
    } catch (err) {
      if (err instanceof ApiError) setCredProblem(err.problem);
    } finally {
      setDeleting(false);
      setConfirmDelete(false);
    }
  };

  return (
    <div className="space-y-8">
      {target && (
        <div className="flex gap-6 items-start">
          <div className="w-20 h-20 rounded-sm border border-gold/20 overflow-hidden bg-muted">
            <img
              src={avatarUrl}
              alt=""
              className={`w-full h-full object-cover transition-opacity duration-200 ${avatarLoaded ? 'opacity-100' : 'opacity-0'}`}
            />
          </div>
          <div className="space-y-2">
            <p className="font-[var(--font-serif-display)] text-lg text-gold">{target.account}</p>
            {target.name && <p className="text-sm">{String(target.name)}</p>}
            {target.email && <p className="text-sm text-muted-foreground">{String(target.email)}</p>}
            {target.groups && target.groups.length > 0 && (
              <div className="flex flex-wrap gap-2 mt-2">
                {target.groups.map((g) => (
                  <span key={g.id} className="text-xs border border-gold/30 rounded-sm px-2 py-0.5 text-gold">
                    {g.name}
                  </span>
                ))}
              </div>
            )}
          </div>
        </div>
      )}

      <form onSubmit={handleSubmit(handleFormSubmit)} className="max-w-md space-y-6">
        <ProblemAlert problem={problem} />
        <ProblemAlert problem={credProblem} />
        {credCreated && (
          <div className="text-sm text-gold border border-gold/30 rounded-sm p-3">
            Password credential created successfully.
          </div>
        )}
        <div className="space-y-2">
          <label htmlFor="account" className="text-xs uppercase tracking-[0.15em] text-muted-foreground">
            Account
          </label>
          <input id="account" {...register('account')} disabled={!!target || isReadOnly} className="mansion-input w-full py-2" />
          {errors.account && <p className="text-sm text-destructive">{errors.account.message}</p>}
        </div>
        <div className="space-y-2">
          <label htmlFor="name" className="text-xs uppercase tracking-[0.15em] text-muted-foreground">
            Name
          </label>
          <input id="name" {...register('name')} disabled={isReadOnly} className="mansion-input w-full py-2" />
          {errors.name && <p className="text-sm text-destructive">{errors.name.message}</p>}
        </div>
        <div className="space-y-2">
          <label htmlFor="email" className="text-xs uppercase tracking-[0.15em] text-muted-foreground">
            Email
          </label>
          <input id="email" type="email" {...register('email')} disabled={isReadOnly} className="mansion-input w-full py-2" />
          {errors.email && <p className="text-sm text-destructive">{errors.email.message}</p>}
        </div>

        {isCreate && (
          <div className="space-y-4 border border-gold/20 rounded-sm p-4">
            <label className="flex items-center gap-2 cursor-pointer">
              <input
                type="checkbox"
                checked={enablePassword}
                onChange={(e) => setEnablePassword(e.target.checked)}
                className="accent-gold"
              />
              <span className="text-xs uppercase tracking-[0.15em] text-muted-foreground">
                <Lock className="inline h-3 w-3 mr-1" />
                Create password credential
              </span>
            </label>
            {enablePassword && (
              <div className="space-y-2">
                <label htmlFor="password" className="text-xs uppercase tracking-[0.15em] text-muted-foreground">
                  Password
                </label>
                <input
                  id="password"
                  type="password"
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                  className="mansion-input w-full py-2"
                />
              </div>
            )}
          </div>
        )}

        {!isCreate && !credCreated && canUpdate && (
          <PasswordCredentialSection account={target.account} onCreated={() => setCredCreated(true)} />
        )}

        <div className="flex items-center gap-4">
          {(isCreate || canUpdate) && (
          <Button
            type="submit"
            disabled={isSubmitting}
            className="bg-gold text-primary-foreground uppercase tracking-[0.15em] text-xs font-semibold hover:bg-gold/90"
          >
            {isSubmitting ? 'Saving...' : 'Save'}
          </Button>
          )}
          {!isCreate && onDeleted && (
            <>
              {confirmDelete ? (
                <div className="flex items-center gap-2">
                  <span className="text-xs text-destructive">Delete this user?</span>
                  <Button
                    type="button"
                    onClick={handleDelete}
                    disabled={deleting}
                    variant="outline"
                    className="text-xs text-destructive border-destructive hover:bg-destructive/10"
                  >
                    {deleting ? 'Deleting...' : 'Confirm'}
                  </Button>
                  <Button
                    type="button"
                    onClick={() => setConfirmDelete(false)}
                    variant="outline"
                    className="text-xs"
                  >
                    Cancel
                  </Button>
                </div>
              ) : (
                <Button
                  type="button"
                  onClick={() => setConfirmDelete(true)}
                  variant="outline"
                  className="text-xs text-destructive border-destructive/50 hover:bg-destructive/10"
                >
                  <Trash2 className="mr-1 h-3 w-3" />
                  Delete
                </Button>
              )}
            </>
          )}
        </div>
      </form>
    </div>
  );
}

export function UsersAdminPage() {
  const { hasPermission } = usePermissions();
  const canCreate = hasPermission(...RESOURCE_PERMISSIONS.user.create);
  const canUpdate = hasPermission(...RESOURCE_PERMISSIONS.user.update);
  const canDelete = hasPermission(...RESOURCE_PERMISSIONS.user.delete);

  return (
    <AdminCrudPage
      title="User"
      config={config}
      columns={columns}
      canCreate={canCreate}
      canUpdate={canUpdate}
      renderEditForm={(props) => (
        <UserEditForm {...props} onDeleted={canDelete ? props.onDeleted : undefined} canUpdate={props.canUpdate} />
      )}
    />
  );
}
