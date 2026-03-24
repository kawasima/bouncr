import { useState } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import * as api from '@/api/endpoints';
import { ApiError } from '@/api/client';
import { AdminCrudPage } from '@/features/admin/admin-crud-page';
import type { AdminCrudConfig } from '@/features/admin/use-admin-crud';
import type { ColumnDef } from '@/components/data-table';
import type { Permission, Problem } from '@/api/types';
import { Button } from '@/components/ui/button';
import { ProblemAlert } from '@/components/problem-alert';
import { usePermissions } from '@/auth/use-permissions';
import { RESOURCE_PERMISSIONS } from '@/auth/permissions';
import { Trash2 } from 'lucide-react';

const config: AdminCrudConfig<Permission> = {
  fetchList: api.getPermissions,
  fetchOne: api.getPermission,
  create: (data) => api.createPermission(data as { name: string; description: string }),
  update: (name, data) => api.updatePermission(name, data as { name: string; description: string }),
  getIdentifier: (p) => p.name,
};

const columns: ColumnDef<Permission>[] = [
  { header: 'Name', accessor: 'name' },
  { header: 'Description', accessor: 'description' },
];

const schema = z.object({
  name: z.string().min(1, 'Name is required'),
  description: z.string().min(1, 'Description is required'),
});

type FormData = z.infer<typeof schema>;

function PermissionEditForm({
  target,
  onSubmit,
  problem,
  onDeleted,
  canUpdate = true,
}: {
  target: Permission | null;
  onSubmit: (data: Record<string, unknown>) => Promise<boolean>;
  problem: Problem | null;
  onDeleted?: () => void;
  canUpdate?: boolean;
}) {
  const isReadOnly = !!target && !canUpdate;
  const [deleting, setDeleting] = useState(false);
  const [confirmDelete, setConfirmDelete] = useState(false);
  const [deleteProblem, setDeleteProblem] = useState<Problem | null>(null);
  const { register, handleSubmit, formState: { errors, isSubmitting } } = useForm<FormData>({
    resolver: zodResolver(schema),
    defaultValues: target ?? { name: '', description: '' },
  });

  return (
    <form onSubmit={handleSubmit((d) => { if (!isReadOnly) onSubmit(d); })} className="max-w-md space-y-6">
      <ProblemAlert problem={problem} />
      <ProblemAlert problem={deleteProblem} />
      <div className="space-y-2">
        <label htmlFor="name" className="text-xs uppercase tracking-[0.15em] text-muted-foreground">Name</label>
        <input id="name" {...register('name')} disabled={!!target || isReadOnly} className="mansion-input w-full py-2" />
        {errors.name && <p className="text-sm text-destructive">{errors.name.message}</p>}
      </div>
      <div className="space-y-2">
        <label htmlFor="description" className="text-xs uppercase tracking-[0.15em] text-muted-foreground">Description</label>
        <input id="description" {...register('description')} disabled={isReadOnly} className="mansion-input w-full py-2" />
        {errors.description && <p className="text-sm text-destructive">{errors.description.message}</p>}
      </div>
      <div className="flex items-center gap-4">
        {!isReadOnly && (
          <Button
            type="submit"
            disabled={isSubmitting}
            className="bg-gold text-primary-foreground uppercase tracking-[0.15em] text-xs font-semibold hover:bg-gold/90"
          >
            {isSubmitting ? 'Saving...' : 'Save'}
          </Button>
        )}
        {target && onDeleted && (
          <>
            {confirmDelete ? (
              <div className="flex items-center gap-2">
                <span className="text-xs text-destructive">Delete this permission?</span>
                <Button
                  type="button"
                  onClick={async () => {
                    setDeleting(true);
                    try {
                      await api.deletePermission(target.name);
                      onDeleted();
                    } catch (err) {
                      if (err instanceof ApiError) setDeleteProblem(err.problem);
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
                <Button type="button" onClick={() => setConfirmDelete(false)} variant="outline" className="text-xs">
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
  );
}

export function PermissionsAdminPage() {
  const { hasPermission } = usePermissions();
  const canCreate = hasPermission(...RESOURCE_PERMISSIONS.permission.create);
  const canUpdate = hasPermission(...RESOURCE_PERMISSIONS.permission.update);
  const canDelete = hasPermission(...RESOURCE_PERMISSIONS.permission.delete);

  return (
    <AdminCrudPage
      title="Permission"
      config={config}
      columns={columns}
      canCreate={canCreate}
      canUpdate={canUpdate}
      renderEditForm={(props) => (
        <PermissionEditForm {...props} onDeleted={canDelete ? props.onDeleted : undefined} canUpdate={props.canUpdate} />
      )}
    />
  );
}
