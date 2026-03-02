import * as api from '@/api/endpoints';
import { AdminCrudPage } from '@/features/admin/admin-crud-page';
import { NameDescriptionForm } from '@/features/admin/name-description-form';
import type { AdminCrudConfig } from '@/features/admin/use-admin-crud';
import type { ColumnDef } from '@/components/data-table';
import type { Permission } from '@/api/types';

const config: AdminCrudConfig<Permission> = {
  fetchList: api.getPermissions,
  fetchOne: api.getPermission,
  create: (data, token) => api.createPermission(data as { name: string; description: string }, token),
  update: (name, data, token) => api.updatePermission(name, data as { name: string; description: string }, token),
  getIdentifier: (p) => p.name,
};

const columns: ColumnDef<Permission>[] = [
  { header: 'Name', accessor: 'name' },
  { header: 'Description', accessor: 'description' },
];

export function PermissionsAdminPage() {
  return (
    <AdminCrudPage
      title="Permission"
      config={config}
      columns={columns}
      renderEditForm={(props) => <NameDescriptionForm {...props} />}
    />
  );
}
