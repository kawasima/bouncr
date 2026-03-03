import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '@/auth/auth-context';
import { Button } from '@/components/ui/button';
import { ROUTES } from '@/routes/route-paths';
import * as api from '@/api/endpoints';

export function Navbar() {
  const { isAuthenticated, account, token, logout } = useAuth();
  const navigate = useNavigate();

  async function handleSignOut() {
    if (token) {
      try {
        await api.signOut(token, token);
      } catch {
        // ignore server error, still logout locally
      }
    }
    logout();
    navigate(ROUTES.SIGN_IN);
  }

  return (
    <header className="border-b border-gold-muted bg-background/90 backdrop-blur-sm shadow-[0_1px_8px_oklch(0.62_0.1_75_/_8%)]">
      <div className="container mx-auto flex h-16 items-center justify-between px-6">
        <Link
          to={ROUTES.HOME}
          className="mansion-heading text-2xl tracking-[0.3em]"
        >
          Bouncr
        </Link>
        <nav className="flex items-center gap-6">
          {isAuthenticated ? (
            <>
              <span className="text-sm italic text-muted-foreground">{account}</span>
              <Button
                variant="outline"
                size="sm"
                onClick={handleSignOut}
                className="uppercase tracking-[0.15em] text-xs border-gold-muted text-gold hover:bg-gold/10 hover:text-gold"
              >
                Sign Out
              </Button>
            </>
          ) : (
            <Button variant="outline" size="sm" asChild className="uppercase tracking-[0.15em] text-xs border-gold-muted text-gold hover:bg-gold/10 hover:text-gold">
              <Link to={ROUTES.SIGN_IN}>Sign In</Link>
            </Button>
          )}
        </nav>
      </div>
    </header>
  );
}
