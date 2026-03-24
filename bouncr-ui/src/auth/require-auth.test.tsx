import { describe, it, expect, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { RequireAuth } from './require-auth';

// Mock useAuth to control authentication state
const mockUseAuth = vi.fn();
vi.mock('./auth-context', () => ({
  useAuth: () => mockUseAuth(),
}));

function renderWithRouter(initialPath: string) {
  return render(
    <MemoryRouter initialEntries={[initialPath]}>
      <Routes>
        <Route element={<RequireAuth />}>
          <Route path="/dashboard" element={<div>Dashboard Content</div>} />
        </Route>
        <Route path="/sign_in" element={<div>Sign In Page</div>} />
      </Routes>
    </MemoryRouter>
  );
}

describe('RequireAuth', () => {
  it('renders child route when authenticated', () => {
    mockUseAuth.mockReturnValue({ isAuthenticated: true });

    renderWithRouter('/dashboard');

    expect(screen.getByText('Dashboard Content')).toBeInTheDocument();
  });

  it('redirects to /sign_in when not authenticated', () => {
    mockUseAuth.mockReturnValue({ isAuthenticated: false });

    renderWithRouter('/dashboard');

    expect(screen.getByText('Sign In Page')).toBeInTheDocument();
    expect(screen.queryByText('Dashboard Content')).not.toBeInTheDocument();
  });
});
