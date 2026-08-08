import { EnterpriseButton } from './EnterpriseButton';
import { cn } from '../utils/cn';

export interface EnterprisePaginationProps {
  page: number;
  totalPages: number;
  onPageChange: (page: number) => void;
  className?: string;
}

/** Simple pagination controls. */
export function EnterprisePagination({ page, totalPages, onPageChange, className }: EnterprisePaginationProps) {
  if (totalPages <= 1) return null;

  return (
    <nav className={cn('flex items-center justify-center gap-2', className)} aria-label="Pagination">
      <EnterpriseButton
        variant="secondary"
        size="sm"
        disabled={page <= 0}
        onClick={() => onPageChange(page - 1)}
      >
        Précédent
      </EnterpriseButton>
      <span className="text-sm text-slate-600 dark:text-slate-400">
        Page {page + 1} / {totalPages}
      </span>
      <EnterpriseButton
        variant="secondary"
        size="sm"
        disabled={page >= totalPages - 1}
        onClick={() => onPageChange(page + 1)}
      >
        Suivant
      </EnterpriseButton>
    </nav>
  );
}
