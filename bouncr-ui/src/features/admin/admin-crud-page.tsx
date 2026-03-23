import { useEffect, type ReactNode } from 'react';
import type { AdminCrudConfig } from './use-admin-crud';
import { useAdminCrud } from './use-admin-crud';
import type { ColumnDef } from '@/components/data-table';
import { DataTable } from '@/components/data-table';
import { SearchInput } from '@/components/search-input';
import { ProblemAlert } from '@/components/problem-alert';
import { LoadingSpinner } from '@/components/loading-spinner';
import { Button } from '@/components/ui/button';
import { ArrowLeft, Plus } from 'lucide-react';
import type { Problem } from '@/api/types';

interface AdminCrudPageProps<T> {
  title: string;
  config: AdminCrudConfig<T>;
  columns: ColumnDef<T>[];
  canCreate?: boolean;
  canUpdate?: boolean;
  renderEditForm: (props: {
    target: T | null;
    onSubmit: (data: Record<string, unknown>) => Promise<boolean>;
    problem: Problem | null;
    onDeleted?: () => void;
    canUpdate?: boolean;
  }) => ReactNode;
}

export function AdminCrudPage<T>({ title, config, columns, canCreate = true, canUpdate = true, renderEditForm }: AdminCrudPageProps<T>) {
  const crud = useAdminCrud(config);

  useEffect(() => {
    crud.loadList('', 0, false);
  }, []); // eslint-disable-line react-hooks/exhaustive-deps

  if (crud.mode === 'edit') {
    return (
      <div className="space-y-6">
        <div className="flex items-center gap-4">
          <button
            onClick={crud.exitEdit}
            className="text-xs uppercase tracking-[0.15em] text-muted-foreground transition-colors hover:text-gold"
          >
            <ArrowLeft className="mr-1 inline h-4 w-4" />
            Back
          </button>
          <h2 className="mansion-heading text-sm">
            {crud.editTarget ? `Edit ${title}` : `New ${title}`}
          </h2>
        </div>
        {renderEditForm({
          target: crud.editTarget,
          onSubmit: async (data) => {
            if (crud.editTarget && !canUpdate) return false;
            if (!crud.editTarget && !canCreate) return false;
            return crud.save(data);
          },
          problem: crud.problem,
          onDeleted: () => { crud.exitEdit(); crud.loadList('', 0, false); },
          canUpdate,
        })}
      </div>
    );
  }

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <h2 className="mansion-heading text-lg">{title}</h2>
        {canCreate && (
          <Button
            size="sm"
            onClick={() => crud.enterEdit(null)}
            className="bg-gold text-primary-foreground uppercase tracking-[0.15em] text-xs font-semibold hover:bg-gold/90"
          >
            <Plus className="mr-1 h-4 w-4" />
            New
          </Button>
        )}
      </div>
      <SearchInput onSearch={crud.handleSearch} placeholder={`Search ${title.toLowerCase()}...`} />
      <ProblemAlert problem={crud.problem} />
      {crud.loading ? (
        <LoadingSpinner />
      ) : (
        <DataTable
          columns={columns}
          data={crud.items}
          onRowClick={(item) => crud.enterEdit(item)}
          hasMore={crud.hasMore}
          onLoadMore={crud.handleLoadMore}
          isLoadingMore={crud.loadingMore}
        />
      )}
    </div>
  );
}
