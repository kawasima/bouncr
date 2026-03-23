import { useState, useEffect } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import * as api from '@/api/endpoints';
import { ApiError } from '@/api/client';
import { AdminCrudPage } from '@/features/admin/admin-crud-page';
import type { AdminCrudConfig } from '@/features/admin/use-admin-crud';
import type { ColumnDef } from '@/components/data-table';
import type { Role, Permission, Problem } from '@/api/types';
import { Button } from '@/components/ui/button';
import { ProblemAlert } from '@/components/problem-alert';
import { usePermissions } from '@/auth/permission-context';
import { RESOURCE_PERMISSIONS } from '@/auth/permissions';

const config: AdminCrudConfig<Role> = {
  fetchList: api.getRoles,
  fetchOne: api.getRole,
  create: (data) => api.createRole(data as { name: string; description: string }),
  update: (name, data) => api.updateRole(name, data as { name: string; description: string }),
  getIdentifier: (r) => r.name,
};

const columns: ColumnDef<Role>[] = [
  { header: 'Name', accessor: 'name' },
  { header: 'Description', accessor: 'description' },
];

const roleSchema = z.object({
  name: z.string().min(1, 'Name is required'),
  description: z.string().min(1, 'Description is required'),
});

type RoleFormData = z.infer<typeof roleSchema>;

function RoleEditForm({
  target,
  onSubmit,
  problem,
}: {
  target: Role | null;
  onSubmit: (data: Record<string, unknown>) => Promise<boolean>;
  problem: Problem | null;
}) {
  const [allPermissions, setAllPermissions] = useState<Permission[]>([]);
  const [selectedPerms, setSelectedPerms] = useState<Set<number>>(
    new Set(target?.permissions?.map((p) => p.id) ?? []),
  );
  const [permProblem, setPermProblem] = useState<Problem | null>(null);
  const [savingPerms, setSavingPerms] = useState(false);

  useEffect(() => {
    api.getPermissions({ limit: 1000 }).then(setAllPermissions).catch(() => {});
  }, []);

  const { register, handleSubmit, formState: { errors, isSubmitting } } = useForm<RoleFormData>({
    resolver: zodResolver(roleSchema),
    defaultValues: target ?? { name: '', description: '' },
  });

  const handleSavePermissions = async () => {
    if (!target) return;
    setSavingPerms(true);
    setPermProblem(null);
    try {
      const perms = allPermissions.filter((p) => selectedPerms.has(p.id));
      await api.updateRolePermissions(target.name, perms);
    } catch (err) {
      if (err instanceof ApiError) setPermProblem(err.problem);
    } finally {
      setSavingPerms(false);
    }
  };

  return (
    <div className="space-y-8">
      <form onSubmit={handleSubmit((d) => onSubmit(d))} className="max-w-md space-y-6">
        <ProblemAlert problem={problem} />
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
        <Button
          type="submit"
          disabled={isSubmitting}
          className="bg-gold text-primary-foreground uppercase tracking-[0.15em] text-xs font-semibold hover:bg-gold/90"
        >
          {isSubmitting ? 'Saving...' : 'Save'}
        </Button>
      </form>

      {target && allPermissions.length > 0 && (
        <div className="space-y-4">
          <div className="mansion-divider" />
          <h3 className="text-xs uppercase tracking-[0.15em] text-gold font-medium">Permissions</h3>
          <ProblemAlert problem={permProblem} />
          <div className="max-h-64 overflow-y-auto space-y-1 border border-gold/20 rounded-sm p-3">
            {allPermissions.map((p) => (
              <label key={p.id} className="flex items-center gap-2 cursor-pointer hover:bg-gold/5 px-2 py-1 rounded-sm">
                <input
                  type="checkbox"
                  checked={selectedPerms.has(p.id)}
                  onChange={(e) => {
                    const next = new Set(selectedPerms);
                    if (e.target.checked) next.add(p.id);
                    else next.delete(p.id);
                    setSelectedPerms(next);
                  }}
                  className="accent-gold"
                />
                <span className="text-sm">{p.name}</span>
              </label>
            ))}
          </div>
          <Button
            type="button"
            onClick={handleSavePermissions}
            disabled={savingPerms}
            className="bg-gold text-primary-foreground uppercase tracking-[0.15em] text-xs font-semibold hover:bg-gold/90"
          >
            {savingPerms ? 'Saving...' : 'Save Permissions'}
          </Button>
        </div>
      )}
    </div>
  );
}

export function RolesAdminPage() {
  const { hasPermission } = usePermissions();
  const canCreate = hasPermission(...RESOURCE_PERMISSIONS.role.create);

  return (
    <AdminCrudPage
      title="Role"
      config={config}
      columns={columns}
      canCreate={canCreate}
      renderEditForm={(props) => <RoleEditForm {...props} />}
    />
  );
}
