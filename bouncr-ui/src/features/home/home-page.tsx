import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { useAuth } from '@/auth/auth-context';
import * as api from '@/api/endpoints';
import type { User, UserAction, OtpKey, Problem } from '@/api/types';
import { ApiError } from '@/api/client';
import { Button } from '@/components/ui/button';
import { LoadingSpinner } from '@/components/loading-spinner';
import { ProblemAlert } from '@/components/problem-alert';
import { AdminMenu } from './admin-menu';
import { OtpSection } from './otp-section';
import { PasskeySection } from './passkey-section';
import { ROUTES } from '@/routes/route-paths';

export function HomePage() {
  const { token, account } = useAuth();
  const [user, setUser] = useState<User | null>(null);
  const [otpKey, setOtpKey] = useState<OtpKey | null>(null);
  const [actions, setActions] = useState<UserAction[]>([]);
  const [loading, setLoading] = useState(true);
  const [problem, setProblem] = useState<Problem | null>(null);

  async function loadData() {
    if (!token || !account) return;
    try {
      const [userData, otpData, actionsData] = await Promise.all([
        api.getUser(account, token, '(permissions,groups,oidc_providers)'),
        api.getOtpKey(token).catch(() => ({ key: null }) as OtpKey),
        api.getActions({ actor: account, limit: 10 }, token).catch(() => [] as UserAction[]),
      ]);
      setUser(userData);
      setOtpKey(otpData);
      setActions(actionsData);
    } catch (err) {
      if (err instanceof ApiError) setProblem(err.problem);
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    loadData();
  }, [token, account]); // eslint-disable-line react-hooks/exhaustive-deps

  if (loading) return <LoadingSpinner />;

  if (!user) {
    return <ProblemAlert problem={problem ?? { status: 0, detail: 'Failed to load user data' }} />;
  }

  const permissions = user.permissions ?? [];

  return (
    <div className="grid gap-8 md:grid-cols-[260px_1fr]">
      <aside className="space-y-8">
        <div className="mansion-card p-6">
          <p className="font-serif-display text-xl text-gold">{user.account}</p>
          {user.name && <p className="mt-1 text-sm italic text-muted-foreground">{String(user.name)}</p>}
          {user.email && <p className="text-sm text-muted-foreground">{String(user.email)}</p>}

          <div className="mansion-divider my-4" />

          <div className="flex gap-3">
            <Button variant="outline" size="sm" asChild className="text-xs uppercase tracking-[0.1em] border-gold-muted text-gold hover:bg-gold/10">
              <Link to={ROUTES.CHANGE_PROFILE}>Edit Profile</Link>
            </Button>
            <Button variant="outline" size="sm" asChild className="text-xs uppercase tracking-[0.1em] border-gold-muted text-gold hover:bg-gold/10">
              <Link to={ROUTES.CHANGE_PASSWORD}>Password</Link>
            </Button>
          </div>
        </div>
        <AdminMenu permissions={permissions} />
      </aside>

      <div className="space-y-8">
        <ProblemAlert problem={problem} />

        <div className="mansion-card p-8">
          <h2 className="mansion-heading text-sm mb-6">Profile</h2>
          <dl className="grid grid-cols-[130px_1fr] gap-y-4 text-sm">
            <dt className="text-xs uppercase tracking-[0.15em] text-gold-muted">Account</dt>
            <dd>{user.account}</dd>
            <dt className="text-xs uppercase tracking-[0.15em] text-gold-muted">Name</dt>
            <dd>{user.name ? String(user.name) : '-'}</dd>
            <dt className="text-xs uppercase tracking-[0.15em] text-gold-muted">Email</dt>
            <dd>{user.email ? String(user.email) : '-'}</dd>
          </dl>
        </div>

        {user.groups && user.groups.length > 0 && (
          <div className="mansion-card p-8">
            <h2 className="mansion-heading text-sm mb-6">Groups</h2>
            <div className="flex flex-wrap gap-3">
              {user.groups.map((g) => (
                <span
                  key={g.id}
                  className="border border-gold-muted px-4 py-1.5 text-xs uppercase tracking-[0.15em] text-gold"
                >
                  {g.name}
                </span>
              ))}
            </div>
          </div>
        )}

        {actions.length > 0 && (
          <div className="mansion-card p-8">
            <h2 className="mansion-heading text-sm mb-6">Sign-in History</h2>
            <table className="w-full">
              <thead>
                <tr className="border-b border-gold/20">
                  <th className="px-3 py-2 text-left text-xs font-medium uppercase tracking-[0.15em] text-gold">Type</th>
                  <th className="px-3 py-2 text-left text-xs font-medium uppercase tracking-[0.15em] text-gold">IP</th>
                  <th className="px-3 py-2 text-left text-xs font-medium uppercase tracking-[0.15em] text-gold">Date</th>
                </tr>
              </thead>
              <tbody>
                {actions.map((a) => (
                  <tr key={a.id} className="border-b border-gold/10">
                    <td className="px-3 py-2 text-sm">{a.action_type}</td>
                    <td className="px-3 py-2 text-sm font-mono text-muted-foreground">{a.actor_ip}</td>
                    <td className="px-3 py-2 text-sm text-muted-foreground">{a.created_at}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}

        <OtpSection otpKey={otpKey?.key} account={user.account} onRefresh={loadData} />
        <PasskeySection onRefresh={loadData} />
      </div>
    </div>
  );
}
