import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import * as api from '@/api/endpoints';
import { AdminCrudPage } from '@/features/admin/admin-crud-page';
import type { AdminCrudConfig } from '@/features/admin/use-admin-crud';
import type { ColumnDef } from '@/components/data-table';
import type { OidcProvider, Problem } from '@/api/types';
import { Button } from '@/components/ui/button';
import { ProblemAlert } from '@/components/problem-alert';
import { usePermissions } from '@/auth/use-permissions';
import { RESOURCE_PERMISSIONS } from '@/auth/permissions';

const config: AdminCrudConfig<OidcProvider> = {
  fetchList: api.getOidcProviders,
  fetchOne: api.getOidcProvider,
  create: (data) => api.createOidcProvider(data as unknown as OidcProvider & { client_secret: string }),
  update: (name, data) => api.updateOidcProvider(name, data as unknown as OidcProvider & { client_secret: string }),
  getIdentifier: (p) => p.name,
};

const columns: ColumnDef<OidcProvider>[] = [
  { header: 'Name', accessor: 'name' },
  { header: 'Response Type', accessor: 'response_type' },
  { header: 'Scope', accessor: 'scope' },
];

const oidcProviderSchema = z.object({
  name: z.string().min(1, 'Name is required'),
  client_id: z.string().min(1, 'Client ID is required'),
  client_secret: z.string().min(1, 'Client Secret is required'),
  scope: z.string().min(1, 'Scope is required'),
  response_type: z.string().min(1, 'Response Type is required'),
  authorization_endpoint: z.string().url('Must be a valid URL'),
  token_endpoint: z.string().url('Must be a valid URL'),
  token_endpoint_auth_method: z.string().min(1, 'Auth Method is required'),
  redirect_uri: z.string().optional(),
  jwks_uri: z.string().optional(),
  issuer: z.string().optional(),
  pkce_enabled: z.boolean().optional(),
});

type OidcProviderFormData = z.infer<typeof oidcProviderSchema>;

function OidcProviderEditForm({
  target,
  onSubmit,
  problem,
  canUpdate = true,
}: {
  target: OidcProvider | null;
  onSubmit: (data: Record<string, unknown>) => Promise<boolean>;
  problem: Problem | null;
  canUpdate?: boolean;
}) {
  const isReadOnly = !!target && !canUpdate;
  const { register, handleSubmit, formState: { errors, isSubmitting } } = useForm<OidcProviderFormData>({
    resolver: zodResolver(oidcProviderSchema),
    defaultValues: target ?? {
      name: '', client_id: '', client_secret: '', scope: 'openid',
      response_type: 'code', authorization_endpoint: '', token_endpoint: '',
      token_endpoint_auth_method: 'client_secret_basic', redirect_uri: '', jwks_uri: '', issuer: '',
      pkce_enabled: false,
    },
  });

  const fields: { id: keyof OidcProviderFormData; label: string; type?: string; placeholder?: string; disabled?: boolean }[] = [
    { id: 'name', label: 'Name', disabled: !!target || isReadOnly },
    { id: 'client_id', label: 'Client ID' },
    { id: 'client_secret', label: 'Client Secret', type: 'password' },
    { id: 'scope', label: 'Scope', placeholder: 'openid email profile' },
    { id: 'response_type', label: 'Response Type', placeholder: 'code' },
    { id: 'authorization_endpoint', label: 'Authorization Endpoint' },
    { id: 'token_endpoint', label: 'Token Endpoint' },
    { id: 'token_endpoint_auth_method', label: 'Token Endpoint Auth Method', placeholder: 'client_secret_basic' },
    { id: 'redirect_uri', label: 'Redirect URI' },
    { id: 'jwks_uri', label: 'JWKS URI' },
    { id: 'issuer', label: 'Issuer' },
  ];

  return (
    <form onSubmit={handleSubmit((d) => onSubmit(d))} className="max-w-lg space-y-6">
      <ProblemAlert problem={problem} />
      {fields.map((f) => (
        <div key={f.id} className="space-y-2">
          <label htmlFor={f.id} className="text-xs uppercase tracking-[0.15em] text-muted-foreground">
            {f.label}
          </label>
          <input
            id={f.id}
            type={f.type ?? 'text'}
            {...register(f.id)}
            disabled={f.disabled || isReadOnly}
            placeholder={f.placeholder}
            className="mansion-input w-full py-2"
          />
          {errors[f.id] && <p className="text-sm text-destructive">{errors[f.id]?.message}</p>}
        </div>
      ))}
      <div className="flex items-center gap-3">
        <input type="checkbox" id="pkce_enabled" {...register('pkce_enabled')} disabled={isReadOnly} className="accent-gold" />
        <label htmlFor="pkce_enabled" className="text-xs uppercase tracking-[0.15em] text-muted-foreground">
          PKCE Enabled
        </label>
      </div>
      {(!target || canUpdate) && (
        <Button
          type="submit"
          disabled={isSubmitting}
          className="bg-gold text-primary-foreground uppercase tracking-[0.15em] text-xs font-semibold hover:bg-gold/90"
        >
          {isSubmitting ? 'Saving...' : 'Save'}
        </Button>
      )}
    </form>
  );
}

export function OidcProvidersAdminPage() {
  const { hasPermission } = usePermissions();
  const canCreate = hasPermission(...RESOURCE_PERMISSIONS.oidcProvider.create);
  const canUpdate = hasPermission(...RESOURCE_PERMISSIONS.oidcProvider.update);

  return (
    <AdminCrudPage
      title="OpenID Connect Provider"
      config={config}
      columns={columns}
      canCreate={canCreate}
      canUpdate={canUpdate}
      renderEditForm={(props) => <OidcProviderEditForm {...props} canUpdate={props.canUpdate} />}
    />
  );
}
