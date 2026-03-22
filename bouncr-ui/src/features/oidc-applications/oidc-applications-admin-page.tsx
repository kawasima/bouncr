import { useState, useEffect } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import * as api from '@/api/endpoints';
import { useAuth } from '@/auth/auth-context';
import { AdminCrudPage } from '@/features/admin/admin-crud-page';
import type { AdminCrudConfig } from '@/features/admin/use-admin-crud';
import type { ColumnDef } from '@/components/data-table';
import type { OidcApplication, OidcApplicationCreateRequest, Permission, Problem } from '@/api/types';
import { Button } from '@/components/ui/button';
import { ProblemAlert } from '@/components/problem-alert';

const config: AdminCrudConfig<OidcApplication> = {
  fetchList: api.getOidcApplications,
  fetchOne: api.getOidcApplication,
  create: (data, token) => api.createOidcApplication(data as unknown as OidcApplicationCreateRequest, token),
  update: (name, data, token) => api.updateOidcApplication(name, data as unknown as OidcApplicationCreateRequest, token),
  getIdentifier: (a) => a.name,
};

const columns: ColumnDef<OidcApplication>[] = [
  { header: 'Name', accessor: 'name' },
  { header: 'Home URL', accessor: 'home_url' },
  { header: 'Description', accessor: 'description' },
];

const oidcAppSchema = z.object({
  name: z.string().min(1, 'Name is required'),
  home_url: z.string().url('Must be a valid URL'),
  callback_url: z.string().url('Must be a valid URL'),
  description: z.string().min(1, 'Description is required'),
  backchannel_logout_uri: z.union([z.string().url('Must be a valid URL'), z.literal('')]).optional(),
  frontchannel_logout_uri: z.union([z.string().url('Must be a valid URL'), z.literal('')]).optional(),
  permissions: z.array(z.string()).optional(),
});

type OidcAppFormData = z.infer<typeof oidcAppSchema>;

function OidcAppEditForm({
  target,
  onSubmit,
  problem,
}: {
  target: OidcApplication | null;
  onSubmit: (data: Record<string, unknown>) => Promise<boolean>;
  problem: Problem | null;
}) {
  const { token } = useAuth();
  const [allPermissions, setAllPermissions] = useState<Permission[]>([]);
  const [selectedPerms, setSelectedPerms] = useState<Set<string>>(
    new Set(target?.permissions?.map((p) => p.name) ?? []),
  );

  useEffect(() => {
    if (!token) return;
    api.getPermissions({ limit: 1000 }, token).then(setAllPermissions).catch(() => {});
  }, [token]);

  const { register, handleSubmit, formState: { errors, isSubmitting } } = useForm<OidcAppFormData>({
    resolver: zodResolver(oidcAppSchema),
    defaultValues: target
      ? {
        name: target.name,
        home_url: target.home_url,
        callback_url: target.callback_url,
        description: target.description,
        backchannel_logout_uri: target.backchannel_logout_uri ?? '',
        frontchannel_logout_uri: target.frontchannel_logout_uri ?? '',
      }
      : {
        name: '',
        home_url: '',
        callback_url: '',
        description: '',
        backchannel_logout_uri: '',
        frontchannel_logout_uri: '',
      },
  });

  const doSubmit = (d: OidcAppFormData) => {
    const payload: Record<string, unknown> = {
      ...d,
      backchannel_logout_uri: d.backchannel_logout_uri?.trim() ?? '',
      frontchannel_logout_uri: d.frontchannel_logout_uri?.trim() ?? '',
      permissions: Array.from(selectedPerms),
    };
    return onSubmit(payload);
  };

  return (
    <form onSubmit={handleSubmit(doSubmit)} className="max-w-lg space-y-6">
      <ProblemAlert problem={problem} />

      {target && (
        <div className="mansion-card p-4 space-y-2">
          <p className="text-xs uppercase tracking-[0.15em] text-gold">Credentials</p>
          <div className="space-y-1 text-sm">
            <p><span className="text-muted-foreground">Client ID:</span> <code className="font-mono">{target.client_id}</code></p>
            <p><span className="text-muted-foreground">Client Secret:</span> <code className="font-mono">{target.client_secret ?? '••••••'}</code></p>
          </div>
        </div>
      )}

      <div className="space-y-2">
        <label htmlFor="name" className="text-xs uppercase tracking-[0.15em] text-muted-foreground">Name</label>
        <input id="name" {...register('name')} disabled={!!target} className="mansion-input w-full py-2" />
        {errors.name && <p className="text-sm text-destructive">{errors.name.message}</p>}
      </div>
      <div className="space-y-2">
        <label htmlFor="description" className="text-xs uppercase tracking-[0.15em] text-muted-foreground">Description</label>
        <input id="description" {...register('description')} className="mansion-input w-full py-2" />
        {errors.description && <p className="text-sm text-destructive">{errors.description.message}</p>}
      </div>
      <div className="space-y-2">
        <label htmlFor="home_url" className="text-xs uppercase tracking-[0.15em] text-muted-foreground">Homepage URL</label>
        <input id="home_url" {...register('home_url')} className="mansion-input w-full py-2" />
        {errors.home_url && <p className="text-sm text-destructive">{errors.home_url.message}</p>}
      </div>
      <div className="space-y-2">
        <label htmlFor="callback_url" className="text-xs uppercase tracking-[0.15em] text-muted-foreground">Callback URL</label>
        <input id="callback_url" {...register('callback_url')} className="mansion-input w-full py-2" />
        {errors.callback_url && <p className="text-sm text-destructive">{errors.callback_url.message}</p>}
      </div>
      <div className="space-y-2">
        <label htmlFor="backchannel_logout_uri" className="text-xs uppercase tracking-[0.15em] text-muted-foreground">Back-channel Logout URI</label>
        <input id="backchannel_logout_uri" {...register('backchannel_logout_uri')} className="mansion-input w-full py-2" />
        {errors.backchannel_logout_uri && <p className="text-sm text-destructive">{errors.backchannel_logout_uri.message}</p>}
      </div>
      <div className="space-y-2">
        <label htmlFor="frontchannel_logout_uri" className="text-xs uppercase tracking-[0.15em] text-muted-foreground">Front-channel Logout URI</label>
        <input id="frontchannel_logout_uri" {...register('frontchannel_logout_uri')} className="mansion-input w-full py-2" />
        {errors.frontchannel_logout_uri && <p className="text-sm text-destructive">{errors.frontchannel_logout_uri.message}</p>}
      </div>

      {allPermissions.length > 0 && (
        <div className="space-y-2">
          <label className="text-xs uppercase tracking-[0.15em] text-muted-foreground">Scopes (Permissions)</label>
          <div className="max-h-48 overflow-y-auto space-y-1 border border-gold/20 rounded-sm p-3">
            {allPermissions.map((p) => (
              <label key={p.name} className="flex items-center gap-2 cursor-pointer hover:bg-gold/5 px-2 py-1 rounded-sm">
                <input
                  type="checkbox"
                  checked={selectedPerms.has(p.name)}
                  onChange={(e) => {
                    const next = new Set(selectedPerms);
                    if (e.target.checked) next.add(p.name);
                    else next.delete(p.name);
                    setSelectedPerms(next);
                  }}
                  className="accent-gold"
                />
                <span className="text-sm">{p.name}</span>
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
        {isSubmitting ? 'Saving...' : 'Save'}
      </Button>
    </form>
  );
}

export function OidcApplicationsAdminPage() {
  return (
    <AdminCrudPage
      title="OpenID Connect Application"
      config={config}
      columns={columns}
      renderEditForm={(props) => <OidcAppEditForm {...props} />}
    />
  );
}
