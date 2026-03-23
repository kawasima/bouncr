import { createContext, useContext, useState, useMemo, useEffect, useCallback, type ReactNode } from 'react';
import { useAuth } from './auth-context';
import * as api from '@/api/endpoints';

interface PermissionContextValue {
  permissions: string[];
  loading: boolean;
  error: boolean;
  hasPermission: (...names: string[]) => boolean;
}

const PermissionContext = createContext<PermissionContextValue | null>(null);

export function PermissionProvider({ children }: { children: ReactNode }) {
  const { account, isAuthenticated } = useAuth();
  const [permissions, setPermissions] = useState<string[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(false);

  useEffect(() => {
    if (!isAuthenticated || !account) {
      setPermissions([]);
      setLoading(false);
      setError(false);
      return;
    }
    setLoading(true);
    setError(false);
    api.getUser(account, '(permissions)')
      .then((user) => setPermissions(user.permissions ?? []))
      .catch(() => {
        setPermissions([]);
        setError(true);
      })
      .finally(() => setLoading(false));
  }, [account, isAuthenticated]);

  const permissionSet = useMemo(() => new Set(permissions), [permissions]);

  const hasPermission = useCallback(
    (...names: string[]) => names.some((n) => permissionSet.has(n)),
    [permissionSet],
  );

  return (
    <PermissionContext.Provider value={{ permissions, loading, error, hasPermission }}>
      {children}
    </PermissionContext.Provider>
  );
}

export function usePermissions(): PermissionContextValue {
  const ctx = useContext(PermissionContext);
  if (!ctx) throw new Error('usePermissions must be used within PermissionProvider');
  return ctx;
}
