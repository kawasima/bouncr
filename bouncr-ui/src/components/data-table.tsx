import type { ReactNode } from 'react';
import { Button } from '@/components/ui/button';

export interface ColumnDef<T> {
  header: string;
  accessor: keyof T | ((row: T) => ReactNode);
}

interface DataTableProps<T> {
  columns: ColumnDef<T>[];
  data: T[];
  onRowClick?: (row: T) => void;
  hasMore?: boolean;
  onLoadMore?: () => void;
  isLoadingMore?: boolean;
}

export function DataTable<T>({
  columns,
  data,
  onRowClick,
  hasMore,
  onLoadMore,
  isLoadingMore,
}: DataTableProps<T>) {
  function getCellValue(row: T, col: ColumnDef<T>): ReactNode {
    if (typeof col.accessor === 'function') {
      return col.accessor(row);
    }
    const val = row[col.accessor];
    if (val === null || val === undefined) return '-';
    return String(val);
  }

  return (
    <div>
      <table className="w-full">
        <thead>
          <tr className="border-b border-gold-muted">
            {columns.map((col) => (
              <th
                key={col.header}
                className="px-4 py-3 text-left text-xs font-medium uppercase tracking-[0.15em] text-gold"
              >
                {col.header}
              </th>
            ))}
          </tr>
        </thead>
        <tbody>
          {data.length === 0 ? (
            <tr>
              <td colSpan={columns.length} className="py-12 text-center italic text-muted-foreground">
                No data found
              </td>
            </tr>
          ) : (
            data.map((row, i) => (
              <tr
                key={i}
                className={`border-b border-gold-muted/30 transition-colors ${onRowClick ? 'cursor-pointer hover:bg-gold/5' : ''}`}
                onClick={() => onRowClick?.(row)}
              >
                {columns.map((col) => (
                  <td key={col.header} className="px-4 py-3 text-sm">
                    {getCellValue(row, col)}
                  </td>
                ))}
              </tr>
            ))
          )}
        </tbody>
      </table>
      {hasMore && onLoadMore && (
        <div className="mt-6 flex justify-center">
          <Button
            variant="outline"
            onClick={onLoadMore}
            disabled={isLoadingMore}
            className="uppercase tracking-[0.15em] text-xs border-gold-muted text-gold hover:bg-gold/10"
          >
            {isLoadingMore ? 'Loading...' : 'Continue'}
          </Button>
        </div>
      )}
    </div>
  );
}
