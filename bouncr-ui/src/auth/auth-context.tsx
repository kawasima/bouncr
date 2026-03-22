import { createContext, useContext, useReducer, useEffect, useCallback, type ReactNode } from 'react';
import { STORAGE_KEY } from '@/lib/constants';

interface GuestState {
  status: 'guest';
}

interface LoggedInState {
  status: 'logged_in';
  account: string;
}

type AuthState = GuestState | LoggedInState;

type AuthAction =
  | { type: 'LOGIN'; account: string }
  | { type: 'LOGOUT' };

interface AuthContextValue {
  state: AuthState;
  login: (account: string) => void;
  logout: () => void;
  account: string | null;
  isAuthenticated: boolean;
}

const AuthContext = createContext<AuthContextValue | null>(null);

function authReducer(_state: AuthState, action: AuthAction): AuthState {
  switch (action.type) {
    case 'LOGIN':
      return { status: 'logged_in', account: action.account };
    case 'LOGOUT':
      return { status: 'guest' };
  }
}

function loadInitialState(): AuthState {
  try {
    const stored = sessionStorage.getItem(STORAGE_KEY);
    if (stored) {
      const parsed = JSON.parse(stored) as { account: string };
      if (parsed.account) {
        return { status: 'logged_in', account: parsed.account };
      }
    }
  } catch {
    // ignore
  }
  return { status: 'guest' };
}

export function AuthProvider({ children }: { children: ReactNode }) {
  const [state, dispatch] = useReducer(authReducer, undefined, loadInitialState);

  useEffect(() => {
    if (state.status === 'logged_in') {
      sessionStorage.setItem(STORAGE_KEY, JSON.stringify({ account: state.account }));
    } else {
      sessionStorage.removeItem(STORAGE_KEY);
    }
  }, [state]);

  // Cross-tab sync: propagate login/logout across tabs via sessionStorage events
  useEffect(() => {
    function handleStorage(e: StorageEvent) {
      if (e.key !== STORAGE_KEY) return;
      if (e.newValue) {
        try {
          const parsed = JSON.parse(e.newValue) as { account: string };
          if (parsed.account) {
            dispatch({ type: 'LOGIN', account: parsed.account });
          }
        } catch {
          // ignore
        }
      } else {
        dispatch({ type: 'LOGOUT' });
      }
    }
    window.addEventListener('storage', handleStorage);
    return () => window.removeEventListener('storage', handleStorage);
  }, []);

  const login = useCallback((account: string) => {
    dispatch({ type: 'LOGIN', account });
  }, []);

  const logout = useCallback(() => {
    dispatch({ type: 'LOGOUT' });
  }, []);

  const value: AuthContextValue = {
    state,
    login,
    logout,
    account: state.status === 'logged_in' ? state.account : null,
    isAuthenticated: state.status === 'logged_in',
  };

  return <AuthContext value={value}>{children}</AuthContext>;
}

export function useAuth(): AuthContextValue {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error('useAuth must be used within AuthProvider');
  return ctx;
}
