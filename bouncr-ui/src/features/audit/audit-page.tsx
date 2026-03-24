import { useState, useEffect, useCallback } from 'react';
import { ApiError } from '@/api/client';
import * as api from '@/api/endpoints';
import type { UserAction, Problem } from '@/api/types';
import { DataTable, type ColumnDef } from '@/components/data-table';
import { SearchInput } from '@/components/search-input';
import { ProblemAlert } from '@/components/problem-alert';
import { LoadingSpinner } from '@/components/loading-spinner';
import { PAGE_SIZE } from '@/lib/constants';
import { format, parseISO } from 'date-fns';

const columns: ColumnDef<UserAction>[] = [
  {
    header: 'Time',
    accessor: (a) => {
      try {
        return format(parseISO(a.created_at), 'yyyy-MM-dd HH:mm:ss');
      } catch {
        return a.created_at;
      }
    },
  },
  { header: 'Actor', accessor: 'actor' },
  { header: 'Action', accessor: 'action_type' },
  { header: 'IP', accessor: 'actor_ip' },
  { header: 'Details', accessor: (a) => a.options ?? '-' },
];

export function AuditPage() {
  const [actions, setActions] = useState<UserAction[]>([]);
  const [loading, setLoading] = useState(true);
  const [loadingMore, setLoadingMore] = useState(false);
  const [problem, setProblem] = useState<Problem | null>(null);
  const [search, setSearch] = useState('');
  const [offset, setOffset] = useState(0);
  const [hasMore, setHasMore] = useState(false);

  const loadActions = useCallback(
    async (actor: string, newOffset: number, append: boolean) => {
      if (append) setLoadingMore(true);
      else setLoading(true);
      setProblem(null);
      try {
        const params = {
          limit: PAGE_SIZE,
          offset: newOffset,
          actor: actor || '*',
        };
        const result = await api.getActions(params) ?? [];
        if (append) {
          setActions((prev) => [...prev, ...result]);
        } else {
          setActions(result);
        }
        setOffset(newOffset + result.length);
        setHasMore(result.length >= PAGE_SIZE);
      } catch (err) {
        if (err instanceof ApiError) setProblem(err.problem);
      } finally {
        setLoading(false);
        setLoadingMore(false);
      }
    },
    [],
  );

  useEffect(() => {
    loadActions('', 0, false);
  }, [loadActions]);

  const handleSearch = useCallback(
    (keyword: string) => {
      setSearch(keyword);
      setOffset(0);
      loadActions(keyword, 0, false);
    },
    [loadActions],
  );

  return (
    <div className="space-y-6">
      <h2 className="mansion-heading text-lg">Audit Log</h2>
      <SearchInput onSearch={handleSearch} placeholder="Search by actor..." />
      <ProblemAlert problem={problem} />
      {loading ? (
        <LoadingSpinner />
      ) : (
        <DataTable
          columns={columns}
          data={actions}
          hasMore={hasMore}
          onLoadMore={() => loadActions(search, offset, true)}
          isLoadingMore={loadingMore}
        />
      )}
    </div>
  );
}
