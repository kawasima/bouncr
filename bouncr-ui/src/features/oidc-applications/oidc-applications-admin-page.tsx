import { useState, useEffect } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import * as api from '@/api/endpoints';
import { AdminCrudPage } from '@/features/admin/admin-crud-page';
import type { AdminCrudConfig } from '@/features/admin/use-admin-crud';
import type { ColumnDef } from '@/components/data-table';
import type { OidcApplication, OidcApplicationCreateRequest, OidcApplicationUpdateRequest, Permission, Role, Problem } from '@/api/types';
import { ApiError } from '@/api/client';
import { Button } from '@/components/ui/button';
import { ProblemAlert } from '@/components/problem-alert';
import { usePermissions } from '@/auth/use-permissions';
import { RESOURCE_PERMISSIONS } from '@/auth/permissions';

const GRANT_TYPES = [
  { value: 'authorization_code', label: 'Authorization Code' },
  { value: 'client_credentials', label: 'Client Credentials' },
  { value: 'refresh_token', label: 'Refresh Token' },
] as const;

const config: AdminCrudConfig<OidcApplication> = {
  fetchList: api.getOidcApplications,
  fetchOne: api.getOidcApplication,
  create: (data) => api.createOidcApplication(data as unknown as OidcApplicationCreateRequest),
  update: (name, data) => api.updateOidcApplication(name, data as unknown as OidcApplicationUpdateRequest),
  getIdentifier: (a) => a.name,
};

const columns: ColumnDef<OidcApplication>[] = [
  { header: 'Name', accessor: 'name' },
  { header: 'Description', accessor: 'description' },
];

const httpUrl = z.string().url('Must be a valid URL').refine(
  (val) => val.startsWith('http://') || val.startsWith('https://'),
  { message: 'Must be an http or https URL' },
);

const oidcAppSchema = z.object({
  name: z.string().min(1, 'Name is required'),
  grant_types: z.array(z.string()).min(1, 'At least one grant type is required'),
  description: z.union([z.string(), z.literal('')]).optional(),
  home_uri: z.union([httpUrl, z.literal('')]).optional(),
  callback_uri: z.union([httpUrl, z.literal('')]).optional(),
  backchannel_logout_uri: z.union([httpUrl, z.literal('')]).optional(),
  frontchannel_logout_uri: z.union([httpUrl, z.literal('')]).optional(),
  permissions: z.array(z.string()).optional(),
}).superRefine((data, ctx) => {
  if (data.grant_types.includes('authorization_code') && (!data.callback_uri || data.callback_uri === '')) {
    ctx.addIssue({
      code: z.ZodIssueCode.custom,
      message: 'Callback URL is required when Authorization Code grant is enabled',
      path: ['callback_uri'],
    });
  }
});

type OidcAppFormData = z.infer<typeof oidcAppSchema>;

function OidcAppEditForm({
  target,
  onSubmit,
  problem,
  onDeleted,
  canUpdate = true,
}: {
  target: OidcApplication | null;
  onSubmit: (data: Record<string, unknown>) => Promise<boolean>;
  problem: Problem | null;
  onDeleted?: () => void;
  canUpdate?: boolean;
}) {
  const isCreate = !target;
  const isReadOnly = !!target && !canUpdate;
  const [createdCredentials, setCreatedCredentials] = useState<{ clientId: string; clientSecret: string } | null>(null);
  const [regeneratedSecret, setRegeneratedSecret] = useState<string | null>(null);
  const [createProblem, setCreateProblem] = useState<Problem | null>(null);
  const [deleting, setDeleting] = useState(false);
  const [confirmDelete, setConfirmDelete] = useState(false);
  const [allPermissions, setAllPermissions] = useState<Permission[]>([]);
  const [allRoles, setAllRoles] = useState<Role[]>([]);
  const [selectedPerms, setSelectedPerms] = useState<Set<string>>(
    new Set(target?.permissions?.map((p) => p.name) ?? []),
  );

  useEffect(() => {
    Promise.all([
      api.getPermissions({ limit: 1000 }),
      api.getRoles({ limit: 1000 }),
    ]).then(([perms, roles]) => {
      setAllPermissions(perms ?? []);
      setAllRoles(roles ?? []);
    }).catch(() => {});
  }, []);

  const handleAddRolePermissions = async (roleName: string) => {
    try {
      const perms = await api.getRolePermissions(roleName);
      setSelectedPerms((prev) => {
        const next = new Set(prev);
        (perms ?? []).forEach((p) => next.add(p.name));
        return next;
      });
    } catch { /* ignore */ }
  };

  const { register, handleSubmit, watch, setValue, formState: { errors, isSubmitting } } = useForm<OidcAppFormData>({
    resolver: zodResolver(oidcAppSchema),
    defaultValues: target
      ? {
        name: target.name,
        grant_types: target.grant_types ?? ['authorization_code', 'refresh_token'],
        description: target.description ?? '',
        home_uri: target.home_uri ?? '',
        callback_uri: target.callback_uri ?? '',
        backchannel_logout_uri: target.backchannel_logout_uri ?? '',
        frontchannel_logout_uri: target.frontchannel_logout_uri ?? '',
      }
      : {
        name: '',
        grant_types: ['authorization_code', 'refresh_token'],
        description: '',
        home_uri: '',
        callback_uri: '',
        backchannel_logout_uri: '',
        frontchannel_logout_uri: '',
      },
  });

  const watchedGrants = watch('grant_types') ?? [];
  const hasAuthCode = watchedGrants.includes('authorization_code');

  const buildPayload = (d: OidcAppFormData): OidcApplicationCreateRequest => {
    const payload: OidcApplicationCreateRequest = {
      name: d.name,
      grant_types: d.grant_types,
      permissions: Array.from(selectedPerms),
    };
    if (d.description?.trim()) payload.description = d.description.trim();
    if (d.grant_types.includes('authorization_code')) {
      if (d.callback_uri?.trim()) payload.callback_uri = d.callback_uri.trim();
      if (d.home_uri?.trim()) payload.home_uri = d.home_uri.trim();
      payload.backchannel_logout_uri = d.backchannel_logout_uri?.trim() ?? '';
      payload.frontchannel_logout_uri = d.frontchannel_logout_uri?.trim() ?? '';
    }
    return payload;
  };

  const doSubmit = async (d: OidcAppFormData) => {
    if (isCreate) {
      setCreateProblem(null);
      try {
        const result = await api.createOidcApplication(buildPayload(d));
        if (!result) throw new Error('No response from server when creating OIDC application');
        setCreatedCredentials({
          clientId: result.client_id ?? '',
          clientSecret: result.client_secret ?? '',
        });
        return true;
      } catch (err) {
        if (err instanceof ApiError) setCreateProblem(err.problem);
        return false;
      }
    }
    return onSubmit(buildPayload(d) as unknown as Record<string, unknown>);
  };

  if (createdCredentials) {
    return (
      <div className="max-w-lg space-y-6">
        <div className="mansion-card p-6 space-y-4 border-gold">
          <h3 className="text-xs uppercase tracking-[0.15em] text-gold font-medium">Application Created</h3>
          <p className="text-sm text-muted-foreground">
            Copy these credentials now. The client secret will not be shown again.
          </p>
          <div className="space-y-3">
            <div>
              <p className="text-xs uppercase tracking-[0.15em] text-muted-foreground mb-1">Client ID</p>
              <code className="block font-mono text-sm bg-black/20 p-2 rounded-sm select-all">{createdCredentials.clientId}</code>
            </div>
            <div>
              <p className="text-xs uppercase tracking-[0.15em] text-muted-foreground mb-1">Client Secret</p>
              <code className="block font-mono text-sm bg-black/20 p-2 rounded-sm select-all text-gold">{createdCredentials.clientSecret}</code>
            </div>
          </div>
        </div>
        <Button
          onClick={() => onDeleted?.()}
          className="bg-gold text-primary-foreground uppercase tracking-[0.15em] text-xs font-semibold hover:bg-gold/90"
        >
          Done
        </Button>
      </div>
    );
  }

  return (
    <form onSubmit={handleSubmit(doSubmit)} className="max-w-lg space-y-6">
      <ProblemAlert problem={problem} />
      <ProblemAlert problem={createProblem} />

      {target && (
        <div className="mansion-card p-4 space-y-3">
          <p className="text-xs uppercase tracking-[0.15em] text-gold">Credentials</p>
          <div className="space-y-1 text-sm">
            <p><span className="text-muted-foreground">Client ID:</span> <code className="font-mono select-all">{target.client_id}</code></p>
          </div>
          {regeneratedSecret ? (
            <div className="space-y-2">
              <p className="text-xs text-muted-foreground">New secret generated. Copy it now — it will not be shown again.</p>
              <code className="block font-mono text-sm bg-black/20 p-2 rounded-sm select-all text-gold">{regeneratedSecret}</code>
            </div>
          ) : canUpdate ? (
            <Button
              type="button"
              variant="outline"
              className="text-xs uppercase tracking-[0.1em]"
              onClick={async () => {
                try {
                  const result = await api.regenerateOidcApplicationSecret(target.name);
                  setRegeneratedSecret(result?.client_secret ?? null);
                } catch (err) {
                  if (err instanceof ApiError) setCreateProblem(err.problem);
                }
              }}
            >
              Regenerate Secret
            </Button>
          ) : null}
        </div>
      )}

      <div className="space-y-2">
        <label htmlFor="name" className="text-xs uppercase tracking-[0.15em] text-muted-foreground">Name</label>
        <input id="name" {...register('name')} disabled={!!target} className="mansion-input w-full py-2" />
        {errors.name && <p className="text-sm text-destructive">{errors.name.message}</p>}
      </div>

      <div className="space-y-2">
        <label className="text-xs uppercase tracking-[0.15em] text-muted-foreground">Grant Types</label>
        <div className="space-y-1 border border-gold/20 rounded-sm p-3">
          {GRANT_TYPES.map((gt) => (
            <label key={gt.value} className="flex items-center gap-2 cursor-pointer hover:bg-gold/5 px-2 py-1 rounded-sm">
              <input
                type="checkbox"
                checked={watchedGrants.includes(gt.value)}
                onChange={(e) => {
                  const current = watchedGrants;
                  const next = e.target.checked
                    ? [...current, gt.value]
                    : current.filter((v) => v !== gt.value);
                  setValue('grant_types', next, { shouldValidate: true });
                }}
                disabled={isReadOnly}
                className="accent-gold"
              />
              <span className="text-sm">{gt.label}</span>
            </label>
          ))}
        </div>
        {errors.grant_types && (
          <p className="text-sm text-destructive">{errors.grant_types.message}</p>
        )}
      </div>

      <div className="space-y-2">
        <label htmlFor="description" className="text-xs uppercase tracking-[0.15em] text-muted-foreground">Description</label>
        <input id="description" {...register('description')} disabled={isReadOnly} className="mansion-input w-full py-2" />
        {errors.description && <p className="text-sm text-destructive">{errors.description.message}</p>}
      </div>

      {hasAuthCode && (
        <>
          <div className="space-y-2">
            <label htmlFor="callback_uri" className="text-xs uppercase tracking-[0.15em] text-muted-foreground">
              Callback URL <span className="text-destructive">*</span>
            </label>
            <input id="callback_uri" {...register('callback_uri')} disabled={isReadOnly} className="mansion-input w-full py-2" />
            {errors.callback_uri && <p className="text-sm text-destructive">{errors.callback_uri.message}</p>}
          </div>
          <div className="space-y-2">
            <label htmlFor="home_uri" className="text-xs uppercase tracking-[0.15em] text-muted-foreground">Homepage URL</label>
            <input id="home_uri" {...register('home_uri')} disabled={isReadOnly} className="mansion-input w-full py-2" />
            {errors.home_uri && <p className="text-sm text-destructive">{errors.home_uri.message}</p>}
          </div>
          <div className="space-y-2">
            <label htmlFor="backchannel_logout_uri" className="text-xs uppercase tracking-[0.15em] text-muted-foreground">Back-channel Logout URI</label>
            <input id="backchannel_logout_uri" {...register('backchannel_logout_uri')} disabled={isReadOnly} className="mansion-input w-full py-2" />
            {errors.backchannel_logout_uri && <p className="text-sm text-destructive">{errors.backchannel_logout_uri.message}</p>}
          </div>
          <div className="space-y-2">
            <label htmlFor="frontchannel_logout_uri" className="text-xs uppercase tracking-[0.15em] text-muted-foreground">Front-channel Logout URI</label>
            <input id="frontchannel_logout_uri" {...register('frontchannel_logout_uri')} disabled={isReadOnly} className="mansion-input w-full py-2" />
            {errors.frontchannel_logout_uri && <p className="text-sm text-destructive">{errors.frontchannel_logout_uri.message}</p>}
          </div>
        </>
      )}

      {allPermissions.length > 0 && (
        <div className="space-y-3">
          <label className="text-xs uppercase tracking-[0.15em] text-muted-foreground">Scopes (Permissions)</label>

          {allRoles.length > 0 && (
            <div className="flex gap-2 items-center">
              <span className="text-xs text-muted-foreground">Add from role:</span>
              <select
                onChange={(e) => {
                  if (e.target.value) {
                    handleAddRolePermissions(e.target.value);
                    e.target.value = '';
                  }
                }}
                defaultValue=""
                disabled={isReadOnly}
                className="mansion-input py-1 text-sm bg-transparent"
              >
                <option value="">Select a role...</option>
                {allRoles.map((r) => (
                  <option key={r.id} value={r.name}>{r.name}</option>
                ))}
              </select>
            </div>
          )}

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
                  disabled={isReadOnly}
                  className="accent-gold"
                />
                <span className="text-sm">{p.name}</span>
              </label>
            ))}
          </div>
        </div>
      )}

      <div className="flex items-center gap-4">
        {(!target || canUpdate) && (
          <Button
            type="submit"
            disabled={isSubmitting || watchedGrants.length === 0}
            className="bg-gold text-primary-foreground uppercase tracking-[0.15em] text-xs font-semibold hover:bg-gold/90"
          >
            {isSubmitting ? 'Saving...' : 'Save'}
          </Button>
        )}
        {!isCreate && onDeleted && (
          <>
            {confirmDelete ? (
              <div className="flex items-center gap-2">
                <span className="text-xs text-destructive">Delete this application?</span>
                <Button
                  type="button"
                  onClick={async () => {
                    if (!target) return;
                    setDeleting(true);
                    try {
                      await api.deleteOidcApplication(target.name);
                      onDeleted?.();
                    } catch (err) {
                      if (err instanceof ApiError) setCreateProblem(err.problem);
                    } finally {
                      setDeleting(false);
                      setConfirmDelete(false);
                    }
                  }}
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
                Delete
              </Button>
            )}
          </>
        )}
      </div>
    </form>
  );
}

export function OidcApplicationsAdminPage() {
  const { hasPermission } = usePermissions();
  const canCreate = hasPermission(...RESOURCE_PERMISSIONS.oidcApplication.create);
  const canDelete = hasPermission(...RESOURCE_PERMISSIONS.oidcApplication.delete);
  const canUpdate = hasPermission(...RESOURCE_PERMISSIONS.oidcApplication.update);

  return (
    <AdminCrudPage
      title="OpenID Connect Application"
      config={config}
      columns={columns}
      canCreate={canCreate}
      canUpdate={canUpdate}
      renderEditForm={(props) => (
        <OidcAppEditForm
          {...props}
          onDeleted={canDelete ? props.onDeleted : undefined}
          canUpdate={canUpdate}
        />
      )}
    />
  );
}
