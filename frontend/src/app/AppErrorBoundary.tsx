import { Component, type ErrorInfo, type ReactNode } from 'react';
import { EnterpriseButton, EnterpriseErrorState } from '@/design-system';

interface Props {
  children: ReactNode;
}

interface State {
  hasError: boolean;
  message?: string;
}

export class AppErrorBoundary extends Component<Props, State> {
  state: State = { hasError: false };

  static getDerivedStateFromError(error: Error): State {
    return { hasError: true, message: error.message };
  }

  componentDidCatch(error: Error, info: ErrorInfo) {
    console.error('AppErrorBoundary:', error, info);
  }

  private handleReload = () => {
    window.location.reload();
  };

  render() {
    if (this.state.hasError) {
      return (
        <div className="flex min-h-screen items-center justify-center bg-slate-50 p-6 dark:bg-slate-950">
          <div className="w-full max-w-md space-y-4">
            <EnterpriseErrorState
              title="Erreur inattendue"
              message={
                this.state.message ||
                "L'application a rencontré une erreur. Rechargez la page pour continuer."
              }
              onRetry={this.handleReload}
            />
            <div className="text-center">
              <EnterpriseButton variant="primary" onClick={this.handleReload}>
                Recharger l'application
              </EnterpriseButton>
            </div>
          </div>
        </div>
      );
    }

    return this.props.children;
  }
}
