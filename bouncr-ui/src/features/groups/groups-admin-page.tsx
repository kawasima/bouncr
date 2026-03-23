import { useState, useEffect, useCallback } from 'react';
import * as api from '@/api/endpoints';
import { ApiError } from '@/api/client';
import { AdminCrudPage } from '@/features/admin/admin-crud-page';
import { NameDescriptionForm } from '@/features/admin/name-description-form';
import type { AdminCrudConfig } from '@/features/admin/use-admin-crud';
import type { ColumnDef } from '@/components/data-table';
import type { Group, Role, Realm, User, Problem } from '@/api/types';
import { Button } from '@/components/ui/button';
import { ProblemAlert } from '@/components/problem-alert';
import { LoadingSpinner } from '@/components/loading-spinner';
import { X, Trash2 } from 'lucide-react';

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

function GroupAssignmentsSection({ group }: { group: Group }) {
  const [realms, setRealms] = useState<Realm[]>([]);
  const [roles, setRoles] = useState<Role[]>([]);
  const [assignments, setAssignments] = useState<{ realm: { id: number; name: string }; role: { id: number; name: string } }[]>([]);
  const [loading, setLoading] = useState(true);
  const [selectedRealm, setSelectedRealm] = useState('');
  const [selectedRoles, setSelectedRoles] = useState<Set<number>>(new Set());
  const [submitting, setSubmitting] = useState(false);
  const [problem, setProblem] = useState<Problem | null>(null);

  const loadAssignments = useCallback(async () => {
    setLoading(true);
    try {
      const [realmList, roleList, assignmentList] = await Promise.all([
        api.getRealms({ limit: 1000 }),
        api.getRoles({ limit: 1000 }),
        api.getGroupAssignments(group.name),
      ]);
      setRealms(realmList);
      setRoles(roleList);
      setAssignments(assignmentList.map((a) => ({
        realm: { id: a.realm.id, name: a.realm.name },
        role: { id: a.role.id, name: a.role.name },
      })));
    } catch (err) {
      if (err instanceof ApiError) setProblem(err.problem);
    } finally {
      setLoading(false);
    }
  }, [group.name]);

  useEffect(() => { loadAssignments(); }, [loadAssignments]);

  const handleAdd = async () => {
    if (!selectedRealm || selectedRoles.size === 0) return;
    setSubmitting(true);
    setProblem(null);
    try {
      const realm = realms.find((r) => r.name === selectedRealm);
      if (!realm) return;
      const reqs = Array.from(selectedRoles).map((roleId) => {
        const role = roles.find((r) => r.id === roleId)!;
        return {
          group: { id: group.id, name: group.name },
          role: { id: role.id, name: role.name },
          realm: { id: realm.id, name: realm.name },
        };
      });
      await api.createAssignments(reqs);
      setSelectedRealm('');
      setSelectedRoles(new Set());
      loadAssignments();
    } catch (err) {
      if (err instanceof ApiError) setProblem(err.problem);
    } finally {
      setSubmitting(false);
    }
  };

  const handleRemove = async (row: AssignmentRow) => {
    setProblem(null);
    try {
      await api.deleteAssignments([{
        group: { id: group.id, name: group.name },
        role: row.role,
        realm: row.realm,
      }]);
      loadAssignments();
    } catch (err) {
      if (err instanceof ApiError) setProblem(err.problem);
    }
  };

  if (loading) return <LoadingSpinner />;

  return (
    <div className="space-y-4 mt-8">
      <div className="mansion-divider" />
      <h3 className="text-xs uppercase tracking-[0.15em] text-gold font-medium">Role Assignments</h3>
      <ProblemAlert problem={problem} />

      {/* Existing assignments */}
      {assignments.length > 0 && (
        <table className="w-full mb-4">
          <thead>
            <tr className="border-b border-gold-muted">
              <th className="px-3 py-2 text-left text-xs font-medium uppercase tracking-[0.15em] text-gold">Realm</th>
              <th className="px-3 py-2 text-left text-xs font-medium uppercase tracking-[0.15em] text-gold">Role</th>
              <th className="px-3 py-2 w-10"></th>
            </tr>
          </thead>
          <tbody>
            {assignments.map((a) => (
              <tr key={`${a.realm.id}-${a.role.id}`} className="border-b border-gold/10">
                <td className="px-3 py-2 text-sm">{a.realm.name}</td>
                <td className="px-3 py-2 text-sm">{a.role.name}</td>
                <td className="px-3 py-2">
                  <button
                    onClick={() => handleRemove(a)}
                    className="text-muted-foreground hover:text-destructive transition-colors"
                    title="Remove assignment"
                  >
                    <X className="h-4 w-4" />
                  </button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      )}

      {/* Add new assignment */}
      <div className="space-y-4">
        <div className="space-y-2">
          <label className="text-xs uppercase tracking-[0.15em] text-muted-foreground">Realm</label>
          <select
            value={selectedRealm}
            onChange={(e) => setSelectedRealm(e.target.value)}
            className="mansion-input w-full py-2 bg-transparent"
          >
            <option value="">Select a realm...</option>
            {realms.map((r) => (
              <option key={r.id} value={r.name}>{r.name}</option>
            ))}
          </select>
        </div>
        {selectedRealm && (() => {
          const selectedRealmObj = realms.find((r) => r.name === selectedRealm);
          const assignedRoleIds = new Set(
            assignments.filter((a) => a.realm.id === selectedRealmObj?.id).map((a) => a.role.id)
          );
          const available = roles.filter((r) => !assignedRoleIds.has(r.id));
          return available.length > 0 ? (
            <div className="space-y-2">
              <label className="text-xs uppercase tracking-[0.15em] text-muted-foreground">Roles</label>
              <div className="max-h-48 overflow-y-auto space-y-1 border border-gold/20 rounded-sm p-3">
                {available.map((r) => (
                  <label key={r.id} className="flex items-center gap-2 cursor-pointer hover:bg-gold/5 px-2 py-1 rounded-sm">
                    <input
                      type="checkbox"
                      checked={selectedRoles.has(r.id)}
                      onChange={(e) => {
                        const next = new Set(selectedRoles);
                        if (e.target.checked) next.add(r.id);
                        else next.delete(r.id);
                        setSelectedRoles(next);
                      }}
                      className="accent-gold"
                    />
                    <span className="text-sm">{r.name}</span>
                  </label>
                ))}
              </div>
            </div>
          ) : (
            <p className="text-sm text-muted-foreground">All roles are already assigned for this realm.</p>
          );
        })()}
        <Button
          type="button"
          onClick={handleAdd}
          disabled={submitting || !selectedRealm || selectedRoles.size === 0}
          className="bg-gold text-primary-foreground uppercase tracking-[0.15em] text-xs font-semibold hover:bg-gold/90"
        >
          {submitting ? 'Adding...' : 'Add Assignment'}
        </Button>
      </div>
    </div>
  );
}

function GroupEditFormWithUsers(props: {
  target: Group | null;
  onSubmit: (data: Record<string, unknown>) => Promise<boolean>;
  problem: Problem | null;
  onDeleted?: () => void;
}) {
  const [deleting, setDeleting] = useState(false);
  const [confirmDelete, setConfirmDelete] = useState(false);
  const [deleteProblem, setDeleteProblem] = useState<Problem | null>(null);

  const handleDelete = async () => {
    if (!props.target) return;
    setDeleting(true);
    try {
      await api.deleteGroup(props.target.name);
      props.onDeleted?.();
    } catch (err) {
      if (err instanceof ApiError) setDeleteProblem(err.problem);
    } finally {
      setDeleting(false);
      setConfirmDelete(false);
    }
  };

  const isWriteProtected = props.target?.writeProtected === true;

  return (
    <div>
      <NameDescriptionForm {...props} />
      <ProblemAlert problem={deleteProblem} />

      {props.target && !isWriteProtected && (
        <div className="mt-6">
          {confirmDelete ? (
            <div className="flex items-center gap-2">
              <span className="text-xs text-destructive">Delete this group?</span>
              <Button
                type="button"
                onClick={handleDelete}
                disabled={deleting}
                variant="outline"
                className="text-xs text-destructive border-destructive hover:bg-destructive/10"
              >
                {deleting ? 'Deleting...' : 'Confirm'}
              </Button>
              <Button
                type="button"
                onClick={() => setConfirmDelete(false)}
                variant="outline"
                className="text-xs"
              >
                Cancel
              </Button>
            </div>
          ) : (
            <Button
              type="button"
              onClick={() => setConfirmDelete(true)}
              variant="outline"
              className="text-xs text-destructive border-destructive/50 hover:bg-destructive/10"
            >
              <Trash2 className="mr-1 h-3 w-3" />
              Delete Group
            </Button>
          )}
        </div>
      )}

      {props.target && <GroupUsersSection groupName={props.target.name} />}
      {props.target && <GroupAssignmentsSection group={props.target} />}
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
