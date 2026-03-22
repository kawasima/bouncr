import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '@/auth/auth-context';
import { Button } from '@/components/ui/button';
import { ROUTES } from '@/routes/route-paths';
import * as api from '@/api/endpoints';

export function Navbar() {
  const { isAuthenticated, account, token, logout } = useAuth();
  const navigate = useNavigate();

  function runFrontchannelLogout(urls: string[]) {
    const timeoutMs = 1500;
    const loadOne = (url: string) => new Promise<void>((resolve) => {
      const iframe = document.createElement('iframe');
      iframe.style.position = 'absolute';
      iframe.style.width = '0';
      iframe.style.height = '0';
      iframe.style.border = '0';
      iframe.style.opacity = '0';
      iframe.setAttribute('aria-hidden', 'true');

      let finished = false;
      const done = () => {
        if (finished) return;
        finished = true;
        clearTimeout(timer);
        iframe.remove();
        resolve();
      };

      const timer = window.setTimeout(done, timeoutMs);
      iframe.onload = done;
      iframe.onerror = done;
      iframe.src = url;
      document.body.appendChild(iframe);
    });

    return urls.reduce((p, url) => p.then(() => loadOne(url)), Promise.resolve());
  }

  async function handleSignOut() {
    let frontchannelUrls: string[] = [];
    if (token) {
      try {
        const response = await api.signOut(token, token);
        frontchannelUrls = response?.frontchannel_logout_urls ?? [];
      } catch {
        // ignore server error, still logout locally
      }
    }
    if (frontchannelUrls.length > 0) {
      await runFrontchannelLogout(frontchannelUrls);
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
