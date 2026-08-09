import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useReducer,
  useState,
  type ReactNode,
} from 'react';
import { AnimatePresence, motion } from 'framer-motion';
import { useAuth } from '@/features/auth/context/AuthContext';
import { AUTH_TOKEN_REFRESHED_EVENT } from '@/shared/api/client';
import { connectLiveStream } from '../services/liveStreamService';
import { useLiveStatus } from '../hooks/useLiveStatus';
import { isCriticalLiveEvent } from '../utils/eventPresentation';
import type {
  LiveConnectionState,
  LiveEvent,
  LiveEventCategory,
  LiveNotification,
  LiveStatus,
} from '../types';

const MAX_NOTIFICATIONS = 100;

type NotificationAction =
  | { type: 'ADD'; event: LiveEvent }
  | { type: 'MARK_READ'; id: string }
  | { type: 'MARK_ALL_READ' }
  | { type: 'DISMISS'; id: string }
  | { type: 'CLEAR_ALL' };

function notificationReducer(state: LiveNotification[], action: NotificationAction): LiveNotification[] {
  switch (action.type) {
    case 'ADD': {
      const next: LiveNotification = { ...action.event, read: false, dismissed: false };
      return [next, ...state.filter((n) => n.id !== next.id)].slice(0, MAX_NOTIFICATIONS);
    }
    case 'MARK_READ':
      return state.map((n) => (n.id === action.id ? { ...n, read: true } : n));
    case 'MARK_ALL_READ':
      return state.map((n) => ({ ...n, read: true }));
    case 'DISMISS':
      return state.map((n) => (n.id === action.id ? { ...n, dismissed: true } : n));
    case 'CLEAR_ALL':
      return [];
    default:
      return state;
  }
}

interface LiveToast {
  id: string;
  title: string;
  message: string;
  variant: 'info' | 'warning' | 'critical';
}

interface LiveContextValue {
  connectionState: LiveConnectionState;
  notifications: LiveNotification[];
  visibleNotifications: LiveNotification[];
  unreadCount: number;
  status?: LiveStatus;
  statusLoading: boolean;
  markRead: (id: string) => void;
  markAllRead: () => void;
  dismiss: (id: string) => void;
  clearAll: () => void;
  recentEvents: LiveEvent[];
}

const LiveContext = createContext<LiveContextValue | null>(null);

const toastStyles = {
  info: 'border-slate-200 bg-white text-slate-800 dark:border-slate-700 dark:bg-slate-900 dark:text-slate-100',
  warning: 'border-amber-200 bg-amber-50 text-amber-900 dark:border-amber-900 dark:bg-amber-950 dark:text-amber-100',
  critical: 'border-red-200 bg-red-50 text-red-900 dark:border-red-900 dark:bg-red-950 dark:text-red-100',
};

function toastVariant(type: LiveEvent['type']): LiveToast['variant'] {
  if (type === 'CRITICAL_ALERT' || type === 'AI_UNAVAILABLE') return 'critical';
  if (type === 'FAILURE_CREATED' || type === 'INTERVENTION_SUBMITTED') return 'warning';
  return 'info';
}

export function LiveProvider({ children }: { children: ReactNode }) {
  const { isAuthenticated } = useAuth();
  const [streamToken, setStreamToken] = useState<string | null>(() => localStorage.getItem('accessToken'));
  const [connectionState, setConnectionState] = useState<LiveConnectionState>('disconnected');
  const [notifications, dispatch] = useReducer(notificationReducer, []);
  const [recentEvents, setRecentEvents] = useState<LiveEvent[]>([]);
  const [toasts, setToasts] = useState<LiveToast[]>([]);
  const { data: status, isLoading: statusLoading } = useLiveStatus(isAuthenticated);

  const pushToast = useCallback((event: LiveEvent) => {
    const id = `live-toast-${event.id}`;
    const variant = toastVariant(event.type);
    setToasts((prev) => [...prev, { id, title: event.title, message: event.message, variant }]);
    setTimeout(() => setToasts((prev) => prev.filter((t) => t.id !== id)), 4500);
  }, []);

  const handleEvent = useCallback(
    (event: LiveEvent) => {
      if (event.type === 'STATUS_UPDATE') return;
      dispatch({ type: 'ADD', event });
      setRecentEvents((prev) => [event, ...prev.filter((e) => e.id !== event.id)].slice(0, 50));
      if (isCriticalLiveEvent(event.type)) pushToast(event);
    },
    [pushToast],
  );

  useEffect(() => {
    const syncToken = () => setStreamToken(localStorage.getItem('accessToken'));
    syncToken();
    window.addEventListener(AUTH_TOKEN_REFRESHED_EVENT, syncToken);
    return () => window.removeEventListener(AUTH_TOKEN_REFRESHED_EVENT, syncToken);
  }, []);

  useEffect(() => {
    if (!isAuthenticated || !streamToken) {
      setConnectionState('disconnected');
      return;
    }

    let cancelled = false;
    let attempt = 0;
    let reconnectTimer: ReturnType<typeof setTimeout> | undefined;
    let abortStream: (() => void) | undefined;

    const clearReconnect = () => {
      if (reconnectTimer !== undefined) {
        clearTimeout(reconnectTimer);
        reconnectTimer = undefined;
      }
    };

    const scheduleReconnect = () => {
      if (cancelled) return;
      clearReconnect();
      const delayMs = Math.min(1000 * 2 ** attempt, 15_000);
      attempt += 1;
      setConnectionState('connecting');
      reconnectTimer = setTimeout(connect, delayMs);
    };

    const connect = () => {
      if (cancelled) return;
      clearReconnect();
      setConnectionState('connecting');
      abortStream = connectLiveStream(streamToken, {
        onEvent: handleEvent,
        onConnected: () => {
          if (cancelled) return;
          attempt = 0;
          setConnectionState('connected');
        },
        onDisconnected: () => {
          if (cancelled) return;
          setConnectionState('disconnected');
          scheduleReconnect();
        },
        onError: () => {
          if (cancelled) return;
          setConnectionState('error');
          scheduleReconnect();
        },
      });
    };

    connect();

    return () => {
      cancelled = true;
      clearReconnect();
      abortStream?.();
    };
  }, [isAuthenticated, streamToken, handleEvent]);

  const visibleNotifications = useMemo(
    () => notifications.filter((n) => !n.dismissed),
    [notifications],
  );
  const unreadCount = useMemo(
    () => visibleNotifications.filter((n) => !n.read).length,
    [visibleNotifications],
  );

  const value = useMemo<LiveContextValue>(
    () => ({
      connectionState,
      notifications,
      visibleNotifications,
      unreadCount,
      status,
      statusLoading,
      markRead: (id) => dispatch({ type: 'MARK_READ', id }),
      markAllRead: () => dispatch({ type: 'MARK_ALL_READ' }),
      dismiss: (id) => dispatch({ type: 'DISMISS', id }),
      clearAll: () => dispatch({ type: 'CLEAR_ALL' }),
      recentEvents,
    }),
    [connectionState, notifications, visibleNotifications, unreadCount, status, statusLoading, recentEvents],
  );

  return (
    <LiveContext.Provider value={value}>
      {children}
      <div className="pointer-events-none fixed bottom-4 right-4 z-[110] flex max-w-sm flex-col gap-2" aria-live="assertive">
        <AnimatePresence>
          {toasts.map((toast) => (
            <motion.div
              key={toast.id}
              initial={{ opacity: 0, y: 10, scale: 0.98 }}
              animate={{ opacity: 1, y: 0, scale: 1 }}
              exit={{ opacity: 0, y: 8, scale: 0.98 }}
              transition={{ duration: 0.2 }}
              className={`pointer-events-auto rounded-xl border px-4 py-3 shadow-lg backdrop-blur-sm ${toastStyles[toast.variant]}`}
              role="alert"
            >
              <p className="text-sm font-semibold">{toast.title}</p>
              <p className="mt-0.5 text-xs opacity-90">{toast.message}</p>
            </motion.div>
          ))}
        </AnimatePresence>
      </div>
    </LiveContext.Provider>
  );
}

export function useLive() {
  const ctx = useContext(LiveContext);
  if (!ctx) throw new Error('useLive must be used within LiveProvider');
  return ctx;
}

export function useLiveNotifications(category?: LiveEventCategory | 'all') {
  const { visibleNotifications } = useLive();
  return useMemo(() => {
    if (!category || category === 'all') return visibleNotifications;
    return visibleNotifications.filter((n) => n.category === category);
  }, [visibleNotifications, category]);
}
