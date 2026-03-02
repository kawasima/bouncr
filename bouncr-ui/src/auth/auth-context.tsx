import { createContext, useContext, useReducer, useEffect, useCallback, type ReactNode } from 'react';
import { STORAGE_KEY } from '@/lib/constants';

interface GuestState {
  status: 'guest';
}

interface LoggedInState {
  status: 'logged_in';
  account: string;
  token: string;
}

type AuthState = GuestState | LoggedInState;

type AuthAction =
  | { type: 'LOGIN'; account: string; token: string }
  | { type: 'LOGOUT' };

interface AuthContextValue {
  state: AuthState;
  login: (account: string, token: string) => void;
  logout: () => void;
  token: string | null;
  account: string | null;
  isAuthenticated: boolean;
}

const AuthContext = createContext<AuthContextValue | null>(null);

function authReducer(_state: AuthState, action: AuthAction): AuthState {
  switch (action.type) {
    case 'LOGIN':
      return { status: 'logged_in', account: action.account, token: action.token };
    case 'LOGOUT':
      return { status: 'guest' };
  }
}

function loadInitialState(): AuthState {
  try {
    const stored = sessionStorage.getItem(STORAGE_KEY);
    if (stored) {
      const parsed = JSON.parse(stored) as { account: string; token: string };
      if (parsed.account && parsed.token) {
        return { status: 'logged_in', account: parsed.account, token: parsed.token };
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
      sessionStorage.setItem(STORAGE_KEY, JSON.stringify({ account: state.account, token: state.token }));
    } else {
      sessionStorage.removeItem(STORAGE_KEY);
    }
  }, [state]);

  // Cross-tab sync
  useEffect(() => {
    function handleStorage(e: StorageEvent) {
      if (e.key !== STORAGE_KEY) return;
      if (e.newValue) {
        try {
          const parsed = JSON.parse(e.newValue) as { account: string; token: string };
          dispatch({ type: 'LOGIN', account: parsed.account, token: parsed.token });
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

  const login = useCallback((account: string, token: string) => {
    dispatch({ type: 'LOGIN', account, token });
  }, []);

  const logout = useCallback(() => {
    dispatch({ type: 'LOGOUT' });
  }, []);

  const value: AuthContextValue = {
    state,
    login,
    logout,
    token: state.status === 'logged_in' ? state.token : null,
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
