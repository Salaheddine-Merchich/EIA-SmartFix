import { cn } from '../utils/cn';

export interface FilterOption {
  value: string;
  label: string;
}

export interface EnterpriseFiltersProps {
  filters: Array<{
    id: string;
    label: string;
    value: string;
    options: FilterOption[];
    onChange: (value: string) => void;
  }>;
  className?: string;
}

/** Horizontal filter bar for list pages. */
export function EnterpriseFilters({ filters, className }: EnterpriseFiltersProps) {
  return (
    <div className={cn('flex flex-wrap gap-3', className)}>
      {filters.map((filter) => (
        <div key={filter.id} className="min-w-[140px]">
          <label htmlFor={filter.id} className="mb-1 block text-xs font-medium text-slate-500 dark:text-slate-400">
            {filter.label}
          </label>
          <select
            id={filter.id}
            value={filter.value}
            onChange={(e) => filter.onChange(e.target.value)}
            className="w-full rounded-lg border border-slate-200 bg-white px-3 py-1.5 text-sm dark:border-slate-700 dark:bg-slate-900 dark:text-slate-100"
          >
            {filter.options.map((opt) => (
              <option key={opt.value} value={opt.value}>{opt.label}</option>
            ))}
          </select>
        </div>
      ))}
    </div>
  );
}
