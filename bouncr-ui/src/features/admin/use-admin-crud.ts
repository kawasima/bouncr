import { useState, useCallback } from 'react';
import { ApiError } from '@/api/client';
import type { Problem, PaginationParams } from '@/api/types';
import { PAGE_SIZE } from '@/lib/constants';

export interface AdminCrudConfig<T> {
  fetchList: (params: PaginationParams & { q?: string }) => Promise<T[]>;
  fetchOne: (name: string) => Promise<T>;
  create: (data: Record<string, unknown>) => Promise<T>;
  update: (name: string, data: Record<string, unknown>) => Promise<T>;
  getIdentifier: (item: T) => string;
}

type Mode = 'list' | 'edit';

export function useAdminCrud<T>(config: AdminCrudConfig<T>) {
  const [items, setItems] = useState<T[]>([]);
  const [mode, setMode] = useState<Mode>('list');
  const [editTarget, setEditTarget] = useState<T | null>(null);
  const [loading, setLoading] = useState(false);
  const [loadingMore, setLoadingMore] = useState(false);
  const [problem, setProblem] = useState<Problem | null>(null);
  const [search, setSearch] = useState('');
  const [offset, setOffset] = useState(0);
  const [hasMore, setHasMore] = useState(false);

  const loadList = useCallback(
    async (keyword: string, newOffset: number, append: boolean) => {
      if (append) setLoadingMore(true);
      else setLoading(true);
      setProblem(null);
      try {
        const params: PaginationParams & { q?: string } = {
          limit: PAGE_SIZE,
          offset: newOffset,
        };
        if (keyword) params.q = keyword;
        const result = await config.fetchList(params);
        if (append) {
          setItems((prev) => [...prev, ...result]);
        } else {
          setItems(result);
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
    [config],
  );

  const handleSearch = useCallback(
    (keyword: string) => {
      setSearch(keyword);
      setOffset(0);
      loadList(keyword, 0, false);
    },
    [loadList],
  );

  const handleLoadMore = useCallback(() => {
    loadList(search, offset, true);
  }, [loadList, search, offset]);

  const enterEdit = useCallback(
    async (item: T | null) => {
      if (item) {
        try {
          const detail = await config.fetchOne(config.getIdentifier(item));
          setEditTarget(detail);
        } catch (err) {
          if (err instanceof ApiError) setProblem(err.problem);
          return;
        }
      } else {
        setEditTarget(null);
      }
      setMode('edit');
      setProblem(null);
    },
    [config],
  );

  const exitEdit = useCallback(() => {
    setMode('list');
    setEditTarget(null);
    setProblem(null);
  }, []);

  const save = useCallback(
    async (data: Record<string, unknown>) => {
      setProblem(null);
      try {
        if (editTarget) {
          await config.update(config.getIdentifier(editTarget), data);
        } else {
          await config.create(data);
        }
        exitEdit();
        loadList(search, 0, false);
        return true;
      } catch (err) {
        if (err instanceof ApiError) setProblem(err.problem);
        return false;
      }
    },
    [editTarget, config, exitEdit, loadList, search],
  );

  return {
    items,
    mode,
    editTarget,
    loading,
    loadingMore,
    problem,
    search,
    hasMore,
    loadList,
    handleSearch,
    handleLoadMore,
    enterEdit,
    exitEdit,
    save,
    setProblem,
  };
}
