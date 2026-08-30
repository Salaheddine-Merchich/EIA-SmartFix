import { useCallback, useEffect, useState } from 'react';

const STORAGE_KEY = 'eia-ai-history-sidebar-open';

function readStoredOpen(): boolean | null {
  try {
    const raw = localStorage.getItem(STORAGE_KEY);
    if (raw === 'true') return true;
    if (raw === 'false') return false;
    return null;
  } catch {
    return null;
  }
}

function defaultOpenForViewport(): boolean {
  if (typeof window === 'undefined') return true;
  return window.matchMedia('(min-width: 1024px)').matches;
}

function persistOpen(value: boolean) {
  try {
    localStorage.setItem(STORAGE_KEY, String(value));
  } catch {
    // ignore quota / private mode
  }
}

export function useHistorySidebarOpen() {
  const [open, setOpen] = useState(() => readStoredOpen() ?? defaultOpenForViewport());

  useEffect(() => {
    persistOpen(open);
  }, [open]);

  const openSidebar = useCallback(() => setOpen(true), []);
  const closeSidebar = useCallback(() => setOpen(false), []);
  const toggleSidebar = useCallback(() => setOpen((prev) => !prev), []);

  return { open, openSidebar, closeSidebar, toggleSidebar };
}
