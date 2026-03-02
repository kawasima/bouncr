import { Link } from 'react-router-dom';
import { Button } from '@/components/ui/button';
import { ROUTES } from '@/routes/route-paths';

export function NotFoundPage() {
  return (
    <div className="flex min-h-[70vh] flex-col items-center justify-center">
      <h1 className="font-serif-display text-8xl font-bold text-gold">404</h1>
      <div className="mansion-ornament my-6 w-48">
        <span>✦</span>
      </div>
      <p className="italic text-muted-foreground">This path leads nowhere</p>
      <Button asChild className="mt-8 bg-gold text-primary-foreground uppercase tracking-[0.2em] text-xs font-semibold hover:bg-gold/90">
        <Link to={ROUTES.HOME}>Return Home</Link>
      </Button>
    </div>
  );
}
