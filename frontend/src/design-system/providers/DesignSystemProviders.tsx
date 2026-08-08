import type { ReactNode } from 'react';
import { ThemeProvider } from '../theme/ThemeProvider';
import { EnterpriseConfirmProvider } from '../components/EnterpriseConfirmDialog';
import { EnterpriseToastProvider } from '../components/EnterpriseToast';

/** Wraps theme, toast, and confirm providers. */
export function DesignSystemProviders({ children }: { children: ReactNode }) {
  return (
    <ThemeProvider>
      <EnterpriseToastProvider>
        <EnterpriseConfirmProvider>
          {children}
        </EnterpriseConfirmProvider>
      </EnterpriseToastProvider>
    </ThemeProvider>
  );
}
