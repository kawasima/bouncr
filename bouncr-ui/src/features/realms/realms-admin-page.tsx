import { useState, useEffect, useCallback } from 'react';
import { useSearchParams } from 'react-router-dom';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import * as api from '@/api/endpoints';
import { ApiError } from '@/api/client';
import type { Application, Realm, Group, Role, Problem } from '@/api/types';
import { Button } from '@/components/ui/button';
import { ProblemAlert } from '@/components/problem-alert';
import { LoadingSpinner } from '@/components/loading-spinner';
import { ArrowLeft, Plus, X } from 'lucide-react';

const realmSchema = z.object({
  name: z.string().min(1, 'Name is required'),
  description: z.string().min(1, 'Description is required'),
  url: z.string().min(1, 'URL is required'),
});

type RealmFormData = z.infer<typeof realmSchema>;

function AssignmentSection({ realm, appName }: { realm: Realm; appName: string }) {
  const [groups, setGroups] = useState<Group[]>([]);
  const [roles, setRoles] = useState<Role[]>([]);
  const [assignments, setAssignments] = useState<{ group: { id: number; name: string }; role: { id: number; name: string } }[]>([]);
  const [selectedGroup, setSelectedGroup] = useState('');
  const [selectedRoles, setSelectedRoles] = useState<Set<number>>(new Set());
  const [submitting, setSubmitting] = useState(false);
  const [problem, setProblem] = useState<Problem | null>(null);

  const loadData = useCallback(async () => {
    try {
      const [g, r, a] = await Promise.all([
        api.getGroups({ limit: 1000 }),
        api.getRoles({ limit: 1000 }),
        api.getRealmAssignments(appName, realm.name),
      ]);
      setGroups(g);
      setRoles(r);
      setAssignments(a.map((x) => ({ group: x.group, role: x.role })));
    } catch (err) {
      if (err instanceof ApiError) setProblem(err.problem);
    }
  }, [appName, realm.name]);

  useEffect(() => { loadData(); }, [loadData]);

  const handleAdd = async () => {
    if (!selectedGroup || selectedRoles.size === 0) return;
    setSubmitting(true);
    setProblem(null);
    try {
      const group = groups.find((g) => g.name === selectedGroup);
      if (!group) return;
      const reqs = Array.from(selectedRoles).map((roleId) => {
        const role = roles.find((r) => r.id === roleId)!;
        return {
          group: { id: group.id, name: group.name },
          role: { id: role.id, name: role.name },
          realm: { id: realm.id, name: realm.name },
        };
      });
      await api.createAssignments(reqs);
      setSelectedGroup('');
      setSelectedRoles(new Set());
      loadData();
    } catch (err) {
      if (err instanceof ApiError) setProblem(err.problem);
    } finally {
      setSubmitting(false);
    }
  };

  const handleRemove = async (row: { group: { id: number; name: string }; role: { id: number; name: string } }) => {
    setProblem(null);
    try {
      await api.deleteAssignments([{
        group: row.group,
        role: row.role,
        realm: { id: realm.id, name: realm.name },
      }]);
      loadData();
    } catch (err) {
      if (err instanceof ApiError) setProblem(err.problem);
    }
  };

  return (
    <div className="space-y-4 border-t border-gold/20 pt-6 mt-6">
      <h3 className="text-xs uppercase tracking-[0.15em] text-gold">Assignments</h3>
      <ProblemAlert problem={problem} />

      {/* Existing assignments */}
      {assignments.length > 0 && (
        <table className="w-full mb-4">
          <thead>
            <tr className="border-b border-gold-muted">
              <th className="px-3 py-2 text-left text-xs font-medium uppercase tracking-[0.15em] text-gold">Group</th>
              <th className="px-3 py-2 text-left text-xs font-medium uppercase tracking-[0.15em] text-gold">Role</th>
              <th className="px-3 py-2 w-10"></th>
            </tr>
          </thead>
          <tbody>
            {assignments.map((a) => (
              <tr key={`${a.group.id}-${a.role.id}`} className="border-b border-gold/10">
                <td className="px-3 py-2 text-sm">{a.group.name}</td>
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
          <label className="text-xs uppercase tracking-[0.15em] text-muted-foreground">Group</label>
          <select
            value={selectedGroup}
            onChange={(e) => setSelectedGroup(e.target.value)}
            className="mansion-input w-full py-2 bg-transparent"
          >
            <option value="">Select a group...</option>
            {groups.map((g) => (
              <option key={g.id} value={g.name}>{g.name}</option>
            ))}
          </select>
        </div>
        {selectedGroup && (() => {
          const assignedRoleIds = new Set(
            assignments.filter((a) => a.group.name === selectedGroup).map((a) => a.role.id)
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
            <p className="text-sm text-muted-foreground">All roles are already assigned for this group.</p>
          );
        })()}
        <Button
          type="button"
          onClick={handleAdd}
          disabled={submitting || !selectedGroup || selectedRoles.size === 0}
          className="bg-gold text-primary-foreground uppercase tracking-[0.15em] text-xs font-semibold hover:bg-gold/90"
        >
          {submitting ? 'Adding...' : 'Add Assignment'}
        </Button>
      </div>
    </div>
  );
}

function RealmEditForm({
  target,
  onSubmit,
  problem,
  appVirtualPath,
  appName,
}: {
  target: Realm | null;
  onSubmit: (data: RealmFormData) => Promise<boolean>;
  problem: Problem | null;
  appVirtualPath: string;
  appName: string;
}) {
  const { register, handleSubmit, formState: { errors, isSubmitting } } = useForm<RealmFormData>({
    resolver: zodResolver(realmSchema),
    defaultValues: target ?? { name: '', description: '', url: '' },
  });

  return (
    <div className="space-y-0">
      <form onSubmit={handleSubmit((d) => onSubmit(d))} className="max-w-md space-y-6">
        <ProblemAlert problem={problem} />
        <div className="space-y-2">
          <label htmlFor="name" className="text-xs uppercase tracking-[0.15em] text-muted-foreground">
            Name
          </label>
          <input id="name" {...register('name')} className="mansion-input w-full py-2" />
          {errors.name && <p className="text-sm text-destructive">{errors.name.message}</p>}
        </div>
        <div className="space-y-2">
          <label htmlFor="description" className="text-xs uppercase tracking-[0.15em] text-muted-foreground">
            Description
          </label>
          <input id="description" {...register('description')} className="mansion-input w-full py-2" />
          {errors.description && <p className="text-sm text-destructive">{errors.description.message}</p>}
        </div>
        <div className="space-y-2">
          <label htmlFor="url" className="text-xs uppercase tracking-[0.15em] text-muted-foreground">
            URL Pattern
          </label>
          <div className="flex items-center gap-2">
            <span className="text-xs text-muted-foreground">{appVirtualPath}/</span>
            <input id="url" {...register('url')} className="mansion-input flex-1 py-2" disabled={!!target} />
          </div>
          {errors.url && <p className="text-sm text-destructive">{errors.url.message}</p>}
        </div>
        <Button
          type="submit"
          disabled={isSubmitting}
          className="bg-gold text-primary-foreground uppercase tracking-[0.15em] text-xs font-semibold hover:bg-gold/90"
        >
          {isSubmitting ? 'Saving...' : 'Save'}
        </Button>
      </form>
      {target && <AssignmentSection realm={target} appName={appName} />}
    </div>
  );
}

export function RealmsAdminPage() {
  const [searchParams] = useSearchParams();
  const appName = searchParams.get('app') ?? '';
  const [app, setApp] = useState<Application | null>(null);
  const [realms, setRealms] = useState<Realm[]>([]);
  const [loading, setLoading] = useState(true);
  const [problem, setProblem] = useState<Problem | null>(null);
  const [mode, setMode] = useState<'list' | 'edit'>('list');
  const [editTarget, setEditTarget] = useState<Realm | null>(null);

  const loadData = useCallback(async () => {
    if (!appName) return;
    setLoading(true);
    try {
      const [appData, realmData] = await Promise.all([
        api.getApplication(appName),
        api.getRealms(appName),
      ]);
      setApp(appData);
      setRealms(realmData);
    } catch (err) {
      if (err instanceof ApiError) setProblem(err.problem);
    } finally {
      setLoading(false);
    }
  }, [appName]);

  useEffect(() => { loadData(); }, [loadData]);

  const handleSave = async (data: RealmFormData) => {
    if (!appName) return false;
    setProblem(null);
    try {
      if (editTarget) {
        await api.updateRealm(appName, editTarget.name, { name: data.name, description: data.description });
      } else {
        await api.createRealm(appName, data);
      }
      setMode('list');
      setEditTarget(null);
      loadData();
      return true;
    } catch (err) {
      if (err instanceof ApiError) setProblem(err.problem);
      return false;
    }
  };

  if (loading) return <LoadingSpinner />;

  if (mode === 'edit') {
    return (
      <div className="space-y-6">
        <div className="flex items-center gap-4">
          <button
            onClick={() => { setMode('list'); setEditTarget(null); setProblem(null); }}
            className="text-xs uppercase tracking-[0.15em] text-muted-foreground transition-colors hover:text-gold"
          >
            <ArrowLeft className="mr-1 inline h-4 w-4" />
            Back
          </button>
          <h2 className="mansion-heading text-sm">
            {editTarget ? `Edit Realm` : `New Realm`}
          </h2>
        </div>
        <RealmEditForm
          target={editTarget}
          onSubmit={handleSave}
          problem={problem}
          appVirtualPath={app?.virtual_path ?? ''}
          appName={appName}
        />
      </div>
    );
  }

  return (
    <div className="space-y-6">
      <div className="flex items-center gap-4">
        <a
          href="/admin/applications"
          className="text-xs uppercase tracking-[0.15em] text-muted-foreground transition-colors hover:text-gold"
        >
          <ArrowLeft className="mr-1 inline h-4 w-4" />
          Applications
        </a>
        <h2 className="mansion-heading text-sm">Application: {appName}</h2>
      </div>
      <div className="flex items-center justify-between">
        <h3 className="mansion-heading text-lg">Realms</h3>
        <Button
          size="sm"
          onClick={() => { setMode('edit'); setEditTarget(null); setProblem(null); }}
          className="bg-gold text-primary-foreground uppercase tracking-[0.15em] text-xs font-semibold hover:bg-gold/90"
        >
          <Plus className="mr-1 h-4 w-4" />
          New
        </Button>
      </div>
      <ProblemAlert problem={problem} />
      {realms.length === 0 ? (
        <p className="text-sm text-muted-foreground">No realms</p>
      ) : (
        <table className="w-full">
          <thead>
            <tr className="border-b border-gold-muted">
              <th className="px-3 py-3 text-left text-xs font-medium uppercase tracking-[0.15em] text-gold">Name</th>
              <th className="px-3 py-3 text-left text-xs font-medium uppercase tracking-[0.15em] text-gold">Description</th>
              <th className="px-3 py-3 text-left text-xs font-medium uppercase tracking-[0.15em] text-gold">URL</th>
            </tr>
          </thead>
          <tbody>
            {realms.map((realm) => (
              <tr
                key={realm.id}
                className="border-b border-gold/10 transition-colors hover:bg-gold/5 cursor-pointer"
                onClick={() => { setMode('edit'); setEditTarget(realm); setProblem(null); }}
              >
                <td className="px-3 py-3 text-sm">{realm.name}</td>
                <td className="px-3 py-3 text-sm text-muted-foreground">{realm.description}</td>
                <td className="px-3 py-3 text-sm font-mono text-muted-foreground">{realm.url}</td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </div>
  );
}
