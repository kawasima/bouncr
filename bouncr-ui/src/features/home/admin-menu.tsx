import { Link } from 'react-router-dom';
import { ROUTES } from '@/routes/route-paths';
import { usePermissions } from '@/auth/permission-context';
import { RESOURCE_PERMISSIONS } from '@/auth/permissions';

const menuItems = [
  { label: 'Users', path: ROUTES.USERS, permissions: RESOURCE_PERMISSIONS.user.read },
  { label: 'Groups', path: ROUTES.GROUPS, permissions: RESOURCE_PERMISSIONS.group.read },
  { label: 'Applications', path: ROUTES.APPLICATIONS, permissions: RESOURCE_PERMISSIONS.application.read },
  { label: 'Roles', path: ROUTES.ROLES, permissions: RESOURCE_PERMISSIONS.role.read },
  { label: 'Permissions', path: ROUTES.PERMISSIONS, permissions: RESOURCE_PERMISSIONS.permission.read },
  { label: 'OIDC Applications', path: ROUTES.OIDC_APPLICATIONS, permissions: RESOURCE_PERMISSIONS.oidcApplication.read },
  { label: 'OIDC Providers', path: ROUTES.OIDC_PROVIDERS, permissions: RESOURCE_PERMISSIONS.oidcProvider.read },
  { label: 'Invitations', path: ROUTES.INVITATIONS, permissions: RESOURCE_PERMISSIONS.invitation.create },
  { label: 'Audit', path: ROUTES.AUDIT, permissions: RESOURCE_PERMISSIONS.user.read },
] as const;

export function AdminMenu() {
  const { hasPermission } = usePermissions();

  const visibleItems = menuItems.filter((item) => hasPermission(...item.permissions));
  if (visibleItems.length === 0) return null;

  return (
    <div className="space-y-3">
      <h3 className="mansion-heading text-xs">Administration</h3>
      <div className="mansion-divider" />
      <nav className="space-y-1">
        {visibleItems.map((item) => (
          <Link
            key={item.path}
            to={item.path}
            className="block px-3 py-2 text-xs uppercase tracking-[0.15em] text-muted-foreground transition-colors duration-300 hover:text-gold"
          >
            {item.label}
          </Link>
        ))}
      </nav>
    </div>
  );
}
