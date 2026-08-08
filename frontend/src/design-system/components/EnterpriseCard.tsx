import { motion } from 'framer-motion';
import { cn } from '../utils/cn';
import { cardHover } from '../animations';

export interface EnterpriseCardProps {
  children: React.ReactNode;
  className?: string;
  padding?: 'none' | 'sm' | 'md' | 'lg';
  hover?: boolean;
}

const paddingMap = {
  none: '',
  sm: 'p-4',
  md: 'p-5',
  lg: 'p-6',
};

/** Surface container — border, subtle shadow, dark-mode support. */
export function EnterpriseCard({
  children,
  className,
  padding = 'md',
  hover = false,
}: EnterpriseCardProps) {
  const Component = hover ? motion.div : 'div';
  const motionProps = hover ? cardHover : {};

  return (
    <Component
      className={cn(
        'rounded-xl border border-slate-200/80 bg-white shadow-sm',
        'dark:border-slate-800 dark:bg-slate-900',
        paddingMap[padding],
        className,
      )}
      {...motionProps}
    >
      {children}
    </Component>
  );
}
