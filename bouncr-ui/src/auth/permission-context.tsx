import { createContext, useReducer, useMemo, useEffect, useCallback, type ReactNode } from 'react';
import { useAuth } from './use-auth';
import * as api from '@/api/endpoints';

export interface PermissionContextValue {
  permissions: string[];
  loading: boolean;
  error: boolean;
  hasPermission: (...names: string[]) => boolean;
}

// eslint-disable-next-line react-refresh/only-export-components
export const PermissionContext = createContext<PermissionContextValue | null>(null);

type PermState = { permissions: string[]; loading: boolean; error: boolean };
type PermAction =
  | { type: 'FETCH_START' }
  | { type: 'FETCH_SUCCESS'; permissions: string[] }
  | { type: 'FETCH_ERROR' };

function permReducer(_state: PermState, action: PermAction): PermState {
  switch (action.type) {
    case 'FETCH_START': return { permissions: [], loading: true, error: false };
    case 'FETCH_SUCCESS': return { permissions: action.permissions, loading: false, error: false };
    case 'FETCH_ERROR': return { permissions: [], loading: false, error: true };
  }
}

export function PermissionProvider({ children }: { children: ReactNode }) {
  const { account, isAuthenticated } = useAuth();
  const [{ permissions, loading, error }, dispatch] = useReducer(permReducer, {
    permissions: [],
    loading: true,
    error: false,
  });

  useEffect(() => {
    if (!isAuthenticated || !account) {
      return;
    }
    let cancelled = false;
    dispatch({ type: 'FETCH_START' });
    api.getUser(account, '(permissions)')
      .then((user) => {
        if (cancelled) return;
        dispatch({ type: 'FETCH_SUCCESS', permissions: user?.permissions ?? [] });
      })
      .catch(() => {
        if (cancelled) return;
        dispatch({ type: 'FETCH_ERROR' });
      });

    return () => { cancelled = true; };
  }, [account, isAuthenticated]);

  const permissionSet = useMemo(() => new Set(permissions), [permissions]);

  const hasPermission = useCallback(
    (...names: string[]) => names.some((n) => permissionSet.has(n)),
    [permissionSet],
  );

  const value = useMemo(() => {
    if (!isAuthenticated) {
      return { permissions: [], loading: false, error: false, hasPermission: () => false };
    }
    return { permissions, loading, error, hasPermission };
  }, [isAuthenticated, permissions, loading, error, hasPermission]);

  return (
    <PermissionContext.Provider value={value}>
      {children}
    </PermissionContext.Provider>
  );
}
