import { AnimatePresence, motion } from 'framer-motion';
import { useEffect } from 'react';
import { slideInRight } from '../animations';
import { cn } from '../utils/cn';
import { EnterpriseButton } from './EnterpriseButton';

export interface EnterpriseDrawerProps {
  open: boolean;
  onClose: () => void;
  title: string;
  children: React.ReactNode;
  side?: 'right' | 'left';
}

/** Side drawer panel for filters and detail views. */
export function EnterpriseDrawer({ open, onClose, title, children, side = 'right' }: EnterpriseDrawerProps) {
  useEffect(() => {
    if (!open) return undefined;
    document.body.style.overflow = 'hidden';
    return () => { document.body.style.overflow = ''; };
  }, [open]);

  return (
    <AnimatePresence>
      {open && (
        <div className="fixed inset-0 z-50" role="dialog" aria-modal="true">
          <button
            type="button"
            className="absolute inset-0 bg-slate-900/40 dark:bg-black/60"
            onClick={onClose}
            aria-label="Fermer"
          />
          <motion.aside
            variants={slideInRight}
            initial="initial"
            animate="animate"
            exit="exit"
            className={cn(
              'absolute top-0 flex h-full w-full max-w-md flex-col border-slate-200 bg-white shadow-xl dark:border-slate-700 dark:bg-slate-900',
              side === 'right' ? 'right-0 border-l' : 'left-0 border-r',
            )}
          >
            <div className="flex shrink-0 items-center justify-between border-b border-slate-100 px-5 py-4 dark:border-slate-800">
              <h2 className="text-base font-semibold text-slate-900 dark:text-slate-100">{title}</h2>
              <EnterpriseButton variant="ghost" size="sm" onClick={onClose} aria-label="Fermer le panneau">
                <svg className="h-4 w-4" fill="none" viewBox="0 0 24 24" strokeWidth={1.5} stroke="currentColor" aria-hidden="true">
                  <path strokeLinecap="round" strokeLinejoin="round" d="M6 18L18 6M6 6l12 12" />
                </svg>
              </EnterpriseButton>
            </div>
            <div className="min-h-0 flex-1 overflow-y-auto p-5 pb-6">{children}</div>
          </motion.aside>
        </div>
      )}
    </AnimatePresence>
  );
}
