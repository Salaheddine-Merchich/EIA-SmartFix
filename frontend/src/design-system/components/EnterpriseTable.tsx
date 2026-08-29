import { cn } from '../utils/cn';
import { EnterpriseEmptyState } from './EnterpriseEmptyState';

type ColumnAlign = 'left' | 'center' | 'right';

export interface Column<T> {
  key: string;
  header: string;
  render: (row: T) => React.ReactNode;
  /** @deprecated Prefer headerClassName / cellClassName */
  className?: string;
  headerClassName?: string;
  cellClassName?: string;
  align?: ColumnAlign;
  /** CSS length for <col> (e.g. '8rem', '280px', '18%') */
  width?: string;
  nowrap?: boolean;
}

export interface EnterpriseTableProps<T> {
  columns: Column<T>[];
  data: T[];
  keyExtractor: (row: T) => string;
  emptyMessage?: string;
  className?: string;
}

const alignClasses: Record<ColumnAlign, string> = {
  left: 'text-left',
  center: 'text-center',
  right: 'text-right',
};

/** Data table with responsive overflow and dark mode. */
export function EnterpriseTable<T>({
  columns,
  data,
  keyExtractor,
  emptyMessage = 'Aucune donnée',
  className,
}: EnterpriseTableProps<T>) {
  if (data.length === 0) {
    return (
      <EnterpriseEmptyState
        title={emptyMessage}
        description="Aucun élément à afficher pour le moment."
      />
    );
  }

  return (
    <div className={cn('overflow-x-auto', className)}>
      <table className="w-full min-w-[900px] table-fixed text-sm">
        <colgroup>
          {columns.map((col) => (
            <col key={col.key} style={col.width ? { width: col.width } : undefined} />
          ))}
        </colgroup>
        <thead>
          <tr className="border-b border-slate-200 bg-slate-100/90 dark:border-slate-800 dark:bg-slate-900/80">
            {columns.map((col) => (
              <th
                key={col.key}
                scope="col"
                className={cn(
                  'sticky top-0 z-10 bg-slate-100/95 px-4 py-3 text-xs font-semibold uppercase tracking-wide text-slate-500 dark:bg-slate-900/95 dark:text-slate-400',
                  alignClasses[col.align ?? 'left'],
                  col.className,
                  col.headerClassName,
                )}
              >
                {col.header}
              </th>
            ))}
          </tr>
        </thead>
        <tbody>
          {data.map((row, rowIndex) => (
            <tr
              key={keyExtractor(row)}
              className={cn(
                'border-b border-slate-100 transition-colors hover:bg-slate-50/60 dark:border-slate-800 dark:hover:bg-slate-800/40',
                rowIndex % 2 === 1 && 'bg-slate-50/50 dark:bg-slate-800/20',
              )}
            >
              {columns.map((col) => (
                <td
                  key={col.key}
                  className={cn(
                    'px-4 py-3 text-slate-700 dark:text-slate-300',
                    alignClasses[col.align ?? 'left'],
                    col.nowrap && 'whitespace-nowrap',
                    col.className,
                    col.cellClassName,
                  )}
                >
                  {col.render(row)}
                </td>
              ))}
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}
