import { useState } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { Link } from 'react-router-dom';
import * as api from '@/api/endpoints';
import { ApiError } from '@/api/client';
import { AdminCrudPage } from '@/features/admin/admin-crud-page';
import type { AdminCrudConfig } from '@/features/admin/use-admin-crud';
import type { ColumnDef } from '@/components/data-table';
import type { Application, Problem } from '@/api/types';
import { Button } from '@/components/ui/button';
import { ProblemAlert } from '@/components/problem-alert';
import { usePermissions } from '@/auth/use-permissions';
import { RESOURCE_PERMISSIONS } from '@/auth/permissions';
import { Trash2 } from 'lucide-react';

const config: AdminCrudConfig<Application> = {
  fetchList: api.getApplications,
  fetchOne: api.getApplication,
  create: (data) => api.createApplication(data as unknown as Application),
  update: (name, data) => api.updateApplication(name, data),
  getIdentifier: (a) => a.name,
};

const columns: ColumnDef<Application>[] = [
  { header: 'Name', accessor: 'name' },
  { header: 'Virtual Path', accessor: 'virtual_path' },
  { header: 'Pass To', accessor: 'pass_to' },
  {
    header: '',
    accessor: (app) => (
      <Link
        to={`/admin/realms?app=${encodeURIComponent(app.name)}`}
        className="text-xs uppercase tracking-[0.15em] text-gold hover:text-gold/80"
        onClick={(e) => e.stopPropagation()}
      >
        Realms
      </Link>
    ),
  },
];

const appSchema = z.object({
  name: z.string().min(1, 'Name is required'),
  description: z.string().min(1, 'Description is required'),
  pass_to: z.string().min(1, 'Pass To is required'),
  virtual_path: z.string().min(1, 'Virtual Path is required'),
  top_page: z.string().min(1, 'Top Page is required'),
});

type AppFormData = z.infer<typeof appSchema>;

function AppEditForm({
  target,
  onSubmit,
  problem,
  onDeleted,
  canUpdate = true,
}: {
  target: Application | null;
  onSubmit: (data: Record<string, unknown>) => Promise<boolean>;
  problem: Problem | null;
  onDeleted?: () => void;
  canUpdate?: boolean;
}) {
  const isReadOnly = !!target && !canUpdate;
  const [deleting, setDeleting] = useState(false);
  const [confirmDelete, setConfirmDelete] = useState(false);
  const [deleteProblem, setDeleteProblem] = useState<Problem | null>(null);
  const { register, handleSubmit, formState: { errors, isSubmitting } } = useForm<AppFormData>({
    resolver: zodResolver(appSchema),
    defaultValues: target ?? { name: '', description: '', pass_to: '', virtual_path: '', top_page: '' },
  });

  return (
    <form onSubmit={handleSubmit((d) => onSubmit(d))} className="max-w-md space-y-6">
      <ProblemAlert problem={problem} />
      <div className="space-y-2">
        <label htmlFor="name" className="text-xs uppercase tracking-[0.15em] text-muted-foreground">
          Name
        </label>
        <input id="name" {...register('name')} disabled={!!target} className="mansion-input w-full py-2" />
        {errors.name && <p className="text-sm text-destructive">{errors.name.message}</p>}
      </div>
      <div className="space-y-2">
        <label htmlFor="description" className="text-xs uppercase tracking-[0.15em] text-muted-foreground">
          Description
        </label>
        <input id="description" {...register('description')} disabled={isReadOnly} className="mansion-input w-full py-2" />
        {errors.description && <p className="text-sm text-destructive">{errors.description.message}</p>}
      </div>
      <div className="space-y-2">
        <label htmlFor="pass_to" className="text-xs uppercase tracking-[0.15em] text-muted-foreground">
          Pass To
        </label>
        <input id="pass_to" {...register('pass_to')} disabled={isReadOnly} placeholder="http://localhost:8080" className="mansion-input w-full py-2" />
        {errors.pass_to && <p className="text-sm text-destructive">{errors.pass_to.message}</p>}
      </div>
      <div className="space-y-2">
        <label htmlFor="virtual_path" className="text-xs uppercase tracking-[0.15em] text-muted-foreground">
          Virtual Path
        </label>
        <input id="virtual_path" {...register('virtual_path')} disabled={isReadOnly} placeholder="/app" className="mansion-input w-full py-2" />
        {errors.virtual_path && <p className="text-sm text-destructive">{errors.virtual_path.message}</p>}
      </div>
      <div className="space-y-2">
        <label htmlFor="top_page" className="text-xs uppercase tracking-[0.15em] text-muted-foreground">
          Top Page
        </label>
        <input id="top_page" {...register('top_page')} disabled={isReadOnly} placeholder="/app/" className="mansion-input w-full py-2" />
        {errors.top_page && <p className="text-sm text-destructive">{errors.top_page.message}</p>}
      </div>
      <ProblemAlert problem={deleteProblem} />
      <div className="flex items-center gap-4">
        {(!target || canUpdate) && (
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
                <span className="text-xs text-destructive">Delete this application?</span>
                <Button
                  type="button"
                  onClick={async () => {
                    setDeleting(true);
                    try {
                      await api.deleteApplication(target.name);
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

export function ApplicationsAdminPage() {
  const { hasPermission } = usePermissions();
  const canCreate = hasPermission(...RESOURCE_PERMISSIONS.application.create);
  const canUpdate = hasPermission(...RESOURCE_PERMISSIONS.application.update);
  const canDelete = hasPermission(...RESOURCE_PERMISSIONS.application.delete);

  return (
    <AdminCrudPage
      title="Application"
      config={config}
      columns={columns}
      canCreate={canCreate}
      canUpdate={canUpdate}
      renderEditForm={(props) => (
        <AppEditForm {...props} onDeleted={canDelete ? props.onDeleted : undefined} canUpdate={props.canUpdate} />
      )}
    />
  );
}
