import { createContext, useCallback, useContext, useMemo, useState, type ReactNode } from 'react';
import { EnterpriseButton } from './EnterpriseButton';
import { EnterpriseModal } from './EnterpriseModal';

interface ConfirmOptions {
  title: string;
  message: string;
  confirmLabel?: string;
  cancelLabel?: string;
  variant?: 'danger' | 'primary';
}

interface ConfirmContextValue {
  confirm: (options: ConfirmOptions) => Promise<boolean>;
}

const ConfirmContext = createContext<ConfirmContextValue | null>(null);

/** Promise-based confirm dialog provider. */
export function EnterpriseConfirmProvider({ children }: { children: ReactNode }) {
  const [state, setState] = useState<(ConfirmOptions & { resolve: (v: boolean) => void }) | null>(null);

  const confirm = useCallback((options: ConfirmOptions) => {
    return new Promise<boolean>((resolve) => {
      setState({ ...options, resolve });
    });
  }, []);

  const close = (result: boolean) => {
    state?.resolve(result);
    setState(null);
  };

  const value = useMemo(() => ({ confirm }), [confirm]);

  return (
    <ConfirmContext.Provider value={value}>
      {children}
      <EnterpriseModal
        open={!!state}
        onClose={() => close(false)}
        title={state?.title ?? ''}
        footer={
          <>
            <EnterpriseButton variant="secondary" onClick={() => close(false)}>
              {state?.cancelLabel ?? 'Annuler'}
            </EnterpriseButton>
            <EnterpriseButton
              variant={state?.variant === 'danger' ? 'danger' : 'primary'}
              onClick={() => close(true)}
            >
              {state?.confirmLabel ?? 'Confirmer'}
            </EnterpriseButton>
          </>
        }
      >
        <p className="text-sm text-slate-600 dark:text-slate-400">{state?.message}</p>
      </EnterpriseModal>
    </ConfirmContext.Provider>
  );
}

export function useEnterpriseConfirm() {
  const ctx = useContext(ConfirmContext);
  if (!ctx) throw new Error('useEnterpriseConfirm must be used within EnterpriseConfirmProvider');
  return ctx;
}
