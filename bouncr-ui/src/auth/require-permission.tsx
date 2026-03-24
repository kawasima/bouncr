import { Outlet } from 'react-router-dom';
import { usePermissions } from './use-permissions';
import { LoadingSpinner } from '@/components/loading-spinner';

interface RequirePermissionProps {
  permissions: string[];
}

export function RequirePermission({ permissions }: RequirePermissionProps) {
  const { hasPermission, loading, error } = usePermissions();

  if (loading) return <LoadingSpinner />;

  if (error) {
    return (
      <div className="flex items-center justify-center min-h-[40vh]">
        <div className="mansion-card p-8 text-center space-y-3">
          <p className="text-sm text-muted-foreground">Failed to load permissions. Please try again later.</p>
        </div>
      </div>
    );
  }

  if (!hasPermission(...permissions)) {
    return (
      <div className="flex items-center justify-center min-h-[40vh]">
        <div className="mansion-card p-8 text-center space-y-3">
          <p className="font-serif-display text-2xl text-gold">403</p>
          <p className="text-sm text-muted-foreground">You do not have permission to access this page.</p>
        </div>
      </div>
    );
  }

  return <Outlet />;
}
