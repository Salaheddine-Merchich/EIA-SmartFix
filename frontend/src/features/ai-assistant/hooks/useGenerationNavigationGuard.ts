import { useEffect, useRef } from 'react';
import { useBlocker } from 'react-router-dom';

import { useEnterpriseConfirm } from '@/design-system';

export function useGenerationNavigationGuard(loading: boolean) {
  const { confirm } = useEnterpriseConfirm();
  const confirmInFlightRef = useRef(false);

  const blocker = useBlocker(
    ({ currentLocation, nextLocation }) =>
      loading && currentLocation.pathname !== nextLocation.pathname,
  );

  useEffect(() => {
    if (blocker.state !== 'blocked' || confirmInFlightRef.current) return;

    confirmInFlightRef.current = true;
    void (async () => {
      const shouldLeave = await confirm({
        title: 'Génération en cours',
        message: 'Une réponse est en cours de génération. Voulez-vous vraiment quitter cette page ?',
        confirmLabel: 'Quitter',
        cancelLabel: 'Rester',
        variant: 'danger',
      });
      if (shouldLeave) {
        blocker.proceed?.();
      } else {
        blocker.reset?.();
      }
      confirmInFlightRef.current = false;
    })();
  }, [blocker, confirm]);

  useEffect(() => {
    if (!loading) return;

    const onBeforeUnload = (event: BeforeUnloadEvent) => {
      event.preventDefault();
      event.returnValue = '';
    };

    window.addEventListener('beforeunload', onBeforeUnload);
    return () => window.removeEventListener('beforeunload', onBeforeUnload);
  }, [loading]);
}
