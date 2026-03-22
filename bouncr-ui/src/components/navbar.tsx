import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '@/auth/auth-context';
import { Button } from '@/components/ui/button';
import { ROUTES } from '@/routes/route-paths';
import * as api from '@/api/endpoints';

const FRONTCHANNEL_PER_IFRAME_TIMEOUT_MS = 1500;
const FRONTCHANNEL_OVERALL_TIMEOUT_MS = 5000;
const FRONTCHANNEL_MAX_URLS = 20;
const FRONTCHANNEL_CONCURRENCY = 4;

function toSafeFrontchannelUrl(rawUrl: string): string | null {
  try {
    const parsed = new URL(rawUrl);
    if (parsed.protocol !== 'http:' && parsed.protocol !== 'https:') {
      return null;
    }
    return parsed.toString();
  } catch {
    return null;
  }
}

function loadFrontchannelUrl(url: string): Promise<void> {
  return new Promise<void>((resolve) => {
    const iframe = document.createElement('iframe');
    iframe.style.position = 'absolute';
    iframe.style.width = '0';
    iframe.style.height = '0';
    iframe.style.border = '0';
    iframe.style.opacity = '0';
    iframe.referrerPolicy = 'no-referrer';
    iframe.sandbox.add('allow-same-origin');
    iframe.sandbox.add('allow-scripts');
    iframe.setAttribute('aria-hidden', 'true');

    let finished = false;
    const done = () => {
      if (finished) return;
      finished = true;
      clearTimeout(timer);
      iframe.remove();
      resolve();
    };

    const timer = window.setTimeout(done, FRONTCHANNEL_PER_IFRAME_TIMEOUT_MS);
    iframe.onload = done;
    iframe.onerror = done;
    iframe.src = url;
    document.body.appendChild(iframe);
  });
}

async function runFrontchannelLogout(urls: string[]): Promise<void> {
  const safeUrls = urls
    .map(toSafeFrontchannelUrl)
    .filter((url): url is string => url !== null)
    .slice(0, FRONTCHANNEL_MAX_URLS);

  if (safeUrls.length === 0) {
    return;
  }

  const queue = [...safeUrls];
  const runWorker = async () => {
    while (queue.length > 0) {
      const next = queue.shift();
      if (!next) break;
      await loadFrontchannelUrl(next);
    }
  };

  const workerCount = Math.min(FRONTCHANNEL_CONCURRENCY, safeUrls.length);
  const workers = Array.from({ length: workerCount }, () => runWorker());
  let timeoutId: number | undefined;
  const timeoutPromise = new Promise<void>((resolve) => {
    timeoutId = window.setTimeout(resolve, FRONTCHANNEL_OVERALL_TIMEOUT_MS);
  });
  const workersPromise = Promise.all(workers).finally(() => {
    if (timeoutId !== undefined) {
      window.clearTimeout(timeoutId);
    }
  });
  await Promise.race([workersPromise, timeoutPromise]);
}

export function Navbar() {
  const { isAuthenticated, account, token, logout } = useAuth();
  const navigate = useNavigate();

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
