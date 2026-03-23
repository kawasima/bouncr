import { Link } from 'react-router-dom';
import { ROUTES } from '@/routes/route-paths';
import { usePermissions } from '@/auth/permission-context';

const menuItems = [
  { label: 'Users', path: ROUTES.USERS, permissions: ['any_user:read', 'user:read'] },
  { label: 'Groups', path: ROUTES.GROUPS, permissions: ['any_group:read', 'group:read'] },
  { label: 'Applications', path: ROUTES.APPLICATIONS, permissions: ['any_application:read', 'application:read'] },
  { label: 'Roles', path: ROUTES.ROLES, permissions: ['any_role:read', 'role:read'] },
  { label: 'Permissions', path: ROUTES.PERMISSIONS, permissions: ['any_permission:read', 'permission:read'] },
  { label: 'OIDC Applications', path: ROUTES.OIDC_APPLICATIONS, permissions: ['oidc_application:read'] },
  { label: 'OIDC Providers', path: ROUTES.OIDC_PROVIDERS, permissions: ['oidc_provider:read'] },
  { label: 'Invitations', path: ROUTES.INVITATIONS, permissions: ['invitation:create'] },
  { label: 'Audit', path: ROUTES.AUDIT, permissions: ['any_user:read', 'user:read'] },
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
