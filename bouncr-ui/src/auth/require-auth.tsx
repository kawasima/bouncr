import { Navigate, Outlet, useLocation } from 'react-router-dom';
import { useAuth } from './use-auth';

export function RequireAuth() {
  const { isAuthenticated } = useAuth();
  const location = useLocation();

  if (!isAuthenticated) {
    return <Navigate to="/sign_in" state={{ from: location }} replace />;
  }

  return <Outlet />;
}
