import { useState, useEffect, useCallback } from 'react';
import * as api from '@/api/endpoints';
import { ApiError } from '@/api/client';
import { AdminCrudPage } from '@/features/admin/admin-crud-page';
import { NameDescriptionForm } from '@/features/admin/name-description-form';
import type { AdminCrudConfig } from '@/features/admin/use-admin-crud';
import type { ColumnDef } from '@/components/data-table';
import type { Group, User, Problem } from '@/api/types';
import { Button } from '@/components/ui/button';
import { ProblemAlert } from '@/components/problem-alert';
import { LoadingSpinner } from '@/components/loading-spinner';
import { X } from 'lucide-react';

const config: AdminCrudConfig<Group> = {
  fetchList: api.getGroups,
  fetchOne: api.getGroup,
  create: (data) => api.createGroup(data as { name: string; description: string }),
  update: (name, data) => api.updateGroup(name, data as { name: string; description: string }),
  getIdentifier: (g) => g.name,
};

const columns: ColumnDef<Group>[] = [
  { header: 'Name', accessor: 'name' },
  { header: 'Description', accessor: 'description' },
];

function GroupUsersSection({ groupName }: { groupName: string }) {
  const [users, setUsers] = useState<User[]>([]);
  const [loading, setLoading] = useState(true);
  const [problem, setProblem] = useState<Problem | null>(null);
  const [searchQuery, setSearchQuery] = useState('');
  const [searchResults, setSearchResults] = useState<User[]>([]);
  const [searching, setSearching] = useState(false);

  const loadUsers = useCallback(async () => {
    setLoading(true);
    try {
      const data = await api.getGroupUsers(groupName);
      setUsers(data);
    } catch (err) {
      if (err instanceof ApiError) setProblem(err.problem);
    } finally {
      setLoading(false);
    }
  }, [groupName]);

  useEffect(() => { loadUsers(); }, [loadUsers]);

  const handleSearch = async () => {
    if (!searchQuery.trim()) return;
    setSearching(true);
    try {
      const results = await api.getUsers({ q: searchQuery, limit: 20 });
      const memberAccounts = new Set(users.map((u) => u.account));
      setSearchResults(results.filter((u) => !memberAccounts.has(u.account)));
    } catch { /* ignore */ } finally {
      setSearching(false);
    }
  };

  const handleAdd = async (account: string) => {
    setProblem(null);
    try {
      await api.addGroupUsers(groupName, [account]);
      setSearchResults((prev) => prev.filter((u) => u.account !== account));
      loadUsers();
    } catch (err) {
      if (err instanceof ApiError) setProblem(err.problem);
    }
  };

  const handleRemove = async (account: string) => {
    setProblem(null);
    try {
      await api.removeGroupUsers(groupName, [account]);
      loadUsers();
    } catch (err) {
      if (err instanceof ApiError) setProblem(err.problem);
    }
  };

  if (loading) return <LoadingSpinner />;

  return (
    <div className="space-y-4 mt-8">
      <div className="mansion-divider" />
      <h3 className="text-xs uppercase tracking-[0.15em] text-gold font-medium">Users in Group</h3>
      <ProblemAlert problem={problem} />

      {/* Add user search */}
      <div className="flex gap-2">
        <input
          type="text"
          value={searchQuery}
          onChange={(e) => setSearchQuery(e.target.value)}
          onKeyDown={(e) => e.key === 'Enter' && (e.preventDefault(), handleSearch())}
          placeholder="Search users to add..."
          className="mansion-input flex-1 py-2"
        />
        <Button
          type="button"
          onClick={handleSearch}
          disabled={searching}
          className="bg-gold text-primary-foreground uppercase tracking-[0.15em] text-xs font-semibold hover:bg-gold/90"
        >
          Search
        </Button>
      </div>

      {searchResults.length > 0 && (
        <div className="border border-gold/20 rounded-sm p-3 space-y-1">
          <p className="text-xs text-muted-foreground mb-2">Click to add:</p>
          {searchResults.map((u) => (
            <button
              key={u.account}
              onClick={() => handleAdd(u.account)}
              className="block w-full text-left px-3 py-2 text-sm hover:bg-gold/10 rounded-sm transition-colors"
            >
              <span className="text-gold">{u.account}</span>
              {u.name && <span className="text-muted-foreground ml-2">{String(u.name)}</span>}
            </button>
          ))}
        </div>
      )}

      {/* Current members */}
      {users.length === 0 ? (
        <p className="text-sm text-muted-foreground">No users</p>
      ) : (
        <table className="w-full">
          <thead>
            <tr className="border-b border-gold-muted">
              <th className="px-3 py-2 text-left text-xs font-medium uppercase tracking-[0.15em] text-gold">Account</th>
              <th className="px-3 py-2 text-left text-xs font-medium uppercase tracking-[0.15em] text-gold">Name</th>
              <th className="px-3 py-2 w-10"></th>
            </tr>
          </thead>
          <tbody>
            {users.map((u) => (
              <tr key={u.account} className="border-b border-gold/10">
                <td className="px-3 py-2 text-sm">{u.account}</td>
                <td className="px-3 py-2 text-sm text-muted-foreground">{u.name ? String(u.name) : '-'}</td>
                <td className="px-3 py-2">
                  <button
                    onClick={() => handleRemove(u.account)}
                    className="text-muted-foreground hover:text-destructive transition-colors"
                    title="Remove from group"
                  >
                    <X className="h-4 w-4" />
                  </button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </div>
  );
}

function GroupEditFormWithUsers(props: {
  target: Group | null;
  onSubmit: (data: Record<string, unknown>) => Promise<boolean>;
  problem: Problem | null;
}) {
  return (
    <div>
      <NameDescriptionForm {...props} />
      {props.target && <GroupUsersSection groupName={props.target.name} />}
    </div>
  );
}

export function GroupsAdminPage() {
  return (
    <AdminCrudPage
      title="Group"
      config={config}
      columns={columns}
      renderEditForm={(props) => <GroupEditFormWithUsers {...props} />}
    />
  );
}
