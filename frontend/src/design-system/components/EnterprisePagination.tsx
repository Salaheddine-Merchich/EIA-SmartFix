import { cn } from '../utils/cn';
import { EnterpriseButton } from './EnterpriseButton';

export interface EnterprisePaginationProps {
  page: number;
  totalPages: number;
  onPageChange: (page: number) => void;
  className?: string;
}

export function EnterprisePagination({ page, totalPages, onPageChange, className }: EnterprisePaginationProps) {
  if (totalPages <= 1) return null;

  const canPrev = page > 0;
  const canNext = page < totalPages - 1;

  return (
    <nav
      className={cn('flex items-center justify-between gap-3', className)}
      aria-label="Pagination"
    >
      <p className="text-sm text-slate-600 dark:text-slate-400">
        Page <span className="font-medium text-slate-900 dark:text-slate-100">{page + 1}</span> sur{' '}
        <span className="font-medium text-slate-900 dark:text-slate-100">{totalPages}</span>
      </p>
      <div className="flex items-center gap-2">
        <EnterpriseButton
          variant="secondary"
          size="sm"
          disabled={!canPrev}
          onClick={() => onPageChange(page - 1)}
        >
          Précédent
        </EnterpriseButton>
        <EnterpriseButton
          variant="secondary"
          size="sm"
          disabled={!canNext}
          onClick={() => onPageChange(page + 1)}
        >
          Suivant
        </EnterpriseButton>
      </div>
    </nav>
  );
}
