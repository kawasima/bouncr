import { Link, useLocation } from 'react-router-dom';
import { ROUTES } from '@/routes/route-paths';
import { cn } from '@/lib/utils';

const adminLinks = [
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

export function AdminSidebar() {
  const location = useLocation();

  return (
    <nav className="space-y-1">
      {adminLinks.map((link) => (
        <Link
          key={link.path}
          to={link.path}
          className={cn(
            'block px-4 py-2.5 text-xs font-medium uppercase tracking-[0.2em] transition-all duration-300',
            location.pathname === link.path
              ? 'border-l-2 border-gold text-gold'
              : 'border-l-2 border-transparent text-muted-foreground hover:text-gold/80',
          )}
        >
          {link.label}
        </Link>
      ))}
    </nav>
  );
}
