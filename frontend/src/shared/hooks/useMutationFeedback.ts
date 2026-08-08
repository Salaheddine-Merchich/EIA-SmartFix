import { useCallback, useState } from 'react';
import axios from 'axios';
import { useEnterpriseToast } from '@/design-system';

interface MutationOptions {
  successMessage?: string;
  errorMessage?: string;
}

interface ApiErrorBody {
  message?: string;
  details?: Record<string, string>;
}

const FIELD_LABELS: Record<string, string> = {
  email: 'Email',
  password: 'Mot de passe',
  nomPrenom: 'Nom prénom',
  role: 'Rôle',
};

function formatApiError(error: unknown, fallback: string): string {
  if (!axios.isAxiosError(error)) return fallback;

  const data = error.response?.data as ApiErrorBody | undefined;
  if (!data) return fallback;

  if (data.details && Object.keys(data.details).length > 0) {
    return Object.entries(data.details)
      .map(([field, msg]) => `${FIELD_LABELS[field] ?? field}: ${msg}`)
      .join(' · ');
  }

  return data.message ?? fallback;
}

export function useMutationFeedback() {
  const { toast } = useEnterpriseToast();
  const [loading, setLoading] = useState(false);

  const execute = useCallback(
    async <T,>(fn: () => Promise<T>, options: MutationOptions = {}): Promise<T | null> => {
      setLoading(true);
      try {
        const result = await fn();
        if (options.successMessage) {
          toast(options.successMessage, 'success');
        }
        return result;
      } catch (error) {
        const message = formatApiError(
          error,
          options.errorMessage ?? 'Une erreur est survenue.',
        );
        toast(message, 'error');
        return null;
      } finally {
        setLoading(false);
      }
    },
    [toast],
  );

  return { loading, execute };
}
