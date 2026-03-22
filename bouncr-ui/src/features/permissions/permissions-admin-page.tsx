import * as api from '@/api/endpoints';
import { AdminCrudPage } from '@/features/admin/admin-crud-page';
import { NameDescriptionForm } from '@/features/admin/name-description-form';
import type { AdminCrudConfig } from '@/features/admin/use-admin-crud';
import type { ColumnDef } from '@/components/data-table';
import type { Permission } from '@/api/types';

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
