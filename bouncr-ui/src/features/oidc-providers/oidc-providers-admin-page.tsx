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
import { usePermissions } from '@/auth/permission-context';
import { RESOURCE_PERMISSIONS } from '@/auth/permissions';

const config: AdminCrudConfig<OidcProvider> = {
  fetchList: api.getOidcProviders,
  fetchOne: api.getOidcProvider,
  create: (data) => api.createOidcProvider(data as unknown as OidcProvider & { clientSecret: string }),
  update: (name, data) => api.updateOidcProvider(name, data as unknown as OidcProvider & { clientSecret: string }),
  getIdentifier: (p) => p.name,
};

const columns: ColumnDef<OidcProvider>[] = [
  { header: 'Name', accessor: 'name' },
  { header: 'Response Type', accessor: 'responseType' },
  { header: 'Scope', accessor: 'scope' },
];

const oidcProviderSchema = z.object({
  name: z.string().min(1, 'Name is required'),
  clientId: z.string().min(1, 'Client ID is required'),
  clientSecret: z.string().min(1, 'Client Secret is required'),
  scope: z.string().min(1, 'Scope is required'),
  responseType: z.string().min(1, 'Response Type is required'),
  authorizationEndpoint: z.string().url('Must be a valid URL'),
  tokenEndpoint: z.string().url('Must be a valid URL'),
  tokenEndpointAuthMethod: z.string().min(1, 'Auth Method is required'),
  redirectUri: z.string().optional(),
  jwksUri: z.string().optional(),
  issuer: z.string().optional(),
  pkceEnabled: z.boolean().optional(),
});

type OidcProviderFormData = z.infer<typeof oidcProviderSchema>;

function OidcProviderEditForm({
  target,
  onSubmit,
  problem,
}: {
  target: OidcProvider | null;
  onSubmit: (data: Record<string, unknown>) => Promise<boolean>;
  problem: Problem | null;
}) {
  const { register, handleSubmit, formState: { errors, isSubmitting } } = useForm<OidcProviderFormData>({
    resolver: zodResolver(oidcProviderSchema),
    defaultValues: target ?? {
      name: '', clientId: '', clientSecret: '', scope: 'openid',
      responseType: 'code', authorizationEndpoint: '', tokenEndpoint: '',
      tokenEndpointAuthMethod: 'client_secret_basic', redirectUri: '', jwksUri: '', issuer: '',
      pkceEnabled: false,
    },
  });

  const fields: { id: keyof OidcProviderFormData; label: string; type?: string; placeholder?: string; disabled?: boolean }[] = [
    { id: 'name', label: 'Name', disabled: !!target },
    { id: 'clientId', label: 'Client ID' },
    { id: 'clientSecret', label: 'Client Secret', type: 'password' },
    { id: 'scope', label: 'Scope', placeholder: 'openid email profile' },
    { id: 'responseType', label: 'Response Type', placeholder: 'code' },
    { id: 'authorizationEndpoint', label: 'Authorization Endpoint' },
    { id: 'tokenEndpoint', label: 'Token Endpoint' },
    { id: 'tokenEndpointAuthMethod', label: 'Token Endpoint Auth Method', placeholder: 'client_secret_basic' },
    { id: 'redirectUri', label: 'Redirect URI' },
    { id: 'jwksUri', label: 'JWKS URI' },
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
            disabled={f.disabled}
            placeholder={f.placeholder}
            className="mansion-input w-full py-2"
          />
          {errors[f.id] && <p className="text-sm text-destructive">{errors[f.id]?.message}</p>}
        </div>
      ))}
      <div className="flex items-center gap-3">
        <input type="checkbox" id="pkceEnabled" {...register('pkceEnabled')} className="accent-gold" />
        <label htmlFor="pkceEnabled" className="text-xs uppercase tracking-[0.15em] text-muted-foreground">
          PKCE Enabled
        </label>
      </div>
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

export function OidcProvidersAdminPage() {
  const { hasPermission } = usePermissions();
  const canCreate = hasPermission(...RESOURCE_PERMISSIONS.oidcProvider.create);

  return (
    <AdminCrudPage
      title="OpenID Connect Provider"
      config={config}
      columns={columns}
      canCreate={canCreate}
      renderEditForm={(props) => <OidcProviderEditForm {...props} />}
    />
  );
}
