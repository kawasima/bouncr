import { Link } from 'react-router-dom';
import { ROUTES } from '@/routes/route-paths';

const menuItems = [
  { label: 'Users', path: ROUTES.USERS },
  { label: 'Groups', path: ROUTES.GROUPS },
  { label: 'Applications', path: ROUTES.APPLICATIONS },
  { label: 'Roles', path: ROUTES.ROLES },
  { label: 'Permissions', path: ROUTES.PERMISSIONS },
  { label: 'OIDC Applications', path: ROUTES.OIDC_APPLICATIONS },
  { label: 'OIDC Providers', path: ROUTES.OIDC_PROVIDERS },
  { label: 'Invitations', path: ROUTES.INVITATIONS },
  { label: 'Audit', path: ROUTES.AUDIT },
] as const;

interface AdminMenuProps {
  permissions: string[];
}

export function AdminMenu({ permissions }: AdminMenuProps) {
  const hasAdminAccess = permissions.some((p) => p.startsWith('any_user:'));

  if (!hasAdminAccess) return null;

  return (
    <div className="space-y-3">
      <h3 className="mansion-heading text-xs">Administration</h3>
      <div className="mansion-divider" />
      <nav className="space-y-1">
        {menuItems.map((item) => (
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
