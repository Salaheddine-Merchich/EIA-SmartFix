import { cn } from '../utils/cn';

export function EnterpriseAvatar({
  name,
  size = 'md',
  className,
}: {
  name: string;
  size?: 'sm' | 'md' | 'lg';
  className?: string;
}) {
  const initials = name
    .split(/\s+/)
    .slice(0, 2)
    .map((p) => p[0]?.toUpperCase())
    .join('') || '?';

  const sizeClass = {
    sm: 'h-8 w-8 text-xs',
    md: 'h-10 w-10 text-sm',
    lg: 'h-12 w-12 text-base',
  }[size];

  return (
    <div
      className={cn(
        'inline-flex items-center justify-center rounded-full bg-slate-800 font-semibold text-white dark:bg-slate-700',
        sizeClass,
        className,
      )}
      aria-label={name}
      title={name}
    >
      {initials}
    </div>
  );
}
