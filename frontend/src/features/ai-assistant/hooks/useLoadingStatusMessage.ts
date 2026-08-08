import { useEffect, useState } from 'react';

const LOADING_MESSAGES = [
  'Analyse de votre demande…',
  "Recherche d'interventions et documents similaires…",
  'Génération de la réponse… (peut prendre 1-2 minutes)',
  'Finalisation de la réponse…',
] as const;

export function useLoadingStatusMessage(loading: boolean): string {
  const [loadingMessage, setLoadingMessage] = useState<string>(LOADING_MESSAGES[0]);

  useEffect(() => {
    if (!loading) {
      setLoadingMessage(LOADING_MESSAGES[0]);
      return;
    }

    setLoadingMessage(LOADING_MESSAGES[0]);
    const retrievalTimer = window.setTimeout(() => {
      setLoadingMessage(LOADING_MESSAGES[1]);
    }, 3_000);
    const llmTimer = window.setTimeout(() => {
      setLoadingMessage(LOADING_MESSAGES[2]);
    }, 10_000);
    const finalTimer = window.setTimeout(() => {
      setLoadingMessage(LOADING_MESSAGES[3]);
    }, 120_000);

    return () => {
      window.clearTimeout(retrievalTimer);
      window.clearTimeout(llmTimer);
      window.clearTimeout(finalTimer);
    };
  }, [loading]);

  return loadingMessage;
}
