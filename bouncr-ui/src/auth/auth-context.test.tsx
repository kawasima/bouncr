import { describe, it, expect, vi, beforeEach } from 'vitest';
import { renderHook, act } from '@testing-library/react';
import { AuthProvider } from './auth-context';
import { useAuth } from './use-auth';

// Mock the api endpoints module so periodic session check doesn't make real calls
vi.mock('@/api/endpoints', () => ({
  getUser: vi.fn().mockResolvedValue({}),
}));

// Mock the ApiError import used by auth-context
vi.mock('@/api/client', () => ({
  ApiError: class ApiError extends Error {
    status: number;
    problem: unknown;
    constructor(problem: unknown, status: number) {
      super('mock');
      this.status = status;
      this.problem = problem;
    }
  },
}));

const STORAGE_KEY = 'bouncr_session';

// Provide a working localStorage mock for jsdom
const store = new Map<string, string>();
const localStorageMock = {
  getItem: (key: string) => store.get(key) ?? null,
  setItem: (key: string, value: string) => store.set(key, value),
  removeItem: (key: string) => store.delete(key),
  clear: () => store.clear(),
  get length() { return store.size; },
  key: (index: number) => Array.from(store.keys())[index] ?? null,
};
Object.defineProperty(globalThis, 'localStorage', { value: localStorageMock, writable: true });

beforeEach(() => {
  store.clear();
});

function renderAuthHook() {
  return renderHook(() => useAuth(), {
    wrapper: ({ children }: { children: React.ReactNode }) => (
      <AuthProvider>{children}</AuthProvider>
    ),
  });
}

describe('AuthProvider', () => {
  it('initial state is guest', () => {
    const { result } = renderAuthHook();

    expect(result.current.state.status).toBe('guest');
    expect(result.current.isAuthenticated).toBe(false);
    expect(result.current.account).toBeNull();
  });

  it('login sets state to logged_in with account', () => {
    const { result } = renderAuthHook();

    act(() => {
      result.current.login('testuser');
    });

    expect(result.current.state.status).toBe('logged_in');
    expect(result.current.isAuthenticated).toBe(true);
    expect(result.current.account).toBe('testuser');
  });

  it('logout returns to guest', () => {
    const { result } = renderAuthHook();

    act(() => {
      result.current.login('testuser');
    });
    expect(result.current.state.status).toBe('logged_in');

    act(() => {
      result.current.logout();
    });
    expect(result.current.state.status).toBe('guest');
    expect(result.current.isAuthenticated).toBe(false);
    expect(result.current.account).toBeNull();
  });

  it('persists logged_in state to localStorage', () => {
    const { result } = renderAuthHook();

    act(() => {
      result.current.login('testuser');
    });

    const stored = localStorage.getItem(STORAGE_KEY);
    expect(stored).not.toBeNull();
    expect(JSON.parse(stored!)).toEqual({ account: 'testuser' });
  });

  it('removes localStorage entry on logout', () => {
    const { result } = renderAuthHook();

    act(() => {
      result.current.login('testuser');
    });
    expect(localStorage.getItem(STORAGE_KEY)).not.toBeNull();

    act(() => {
      result.current.logout();
    });
    expect(localStorage.getItem(STORAGE_KEY)).toBeNull();
  });

  it('restores state from localStorage on mount', () => {
    localStorage.setItem(STORAGE_KEY, JSON.stringify({ account: 'restored_user' }));

    const { result } = renderAuthHook();

    expect(result.current.state.status).toBe('logged_in');
    expect(result.current.account).toBe('restored_user');
  });

  it('starts as guest when localStorage has invalid data', () => {
    localStorage.setItem(STORAGE_KEY, 'not-valid-json');

    const { result } = renderAuthHook();

    expect(result.current.state.status).toBe('guest');
  });
});

describe('useAuth', () => {
  it('throws when used outside AuthProvider', () => {
    expect(() => {
      renderHook(() => useAuth());
    }).toThrow('useAuth must be used within AuthProvider');
  });
});
