import { useCallback, useEffect, useMemo, useRef, useState } from 'react';

import { createPortal } from 'react-dom';

import { AnimatePresence, motion } from 'framer-motion';

import { EnterpriseButton, EnterpriseSearch } from '@/design-system';

import { useLive } from '../providers/LiveProvider';

import { LiveNotificationRow } from './LiveNotificationRow';

import type { LiveEventCategory } from '../types';

import { CATEGORY_LABELS } from '../utils/eventPresentation';



const PANEL_MAX_WIDTH = 380;

const VIEWPORT_MARGIN = 8;



const FILTERS: Array<{ id: LiveEventCategory | 'all'; label: string }> = [

  { id: 'all', label: 'Toutes' },

  { id: 'maintenance', label: CATEGORY_LABELS.maintenance },

  { id: 'alert', label: CATEGORY_LABELS.alert },

  { id: 'knowledge', label: CATEGORY_LABELS.knowledge },

  { id: 'ai', label: CATEGORY_LABELS.ai },

  { id: 'system', label: CATEGORY_LABELS.system },

];



function computePanelPosition(triggerRect: DOMRect) {

  const panelWidth = Math.min(PANEL_MAX_WIDTH, window.innerWidth - VIEWPORT_MARGIN * 2);

  const left = Math.max(VIEWPORT_MARGIN, triggerRect.right - panelWidth);

  const top = triggerRect.bottom + VIEWPORT_MARGIN;

  return { top, left, width: panelWidth };

}



export function LiveNotificationCenter() {

  const { unreadCount, visibleNotifications, markRead, markAllRead, dismiss, clearAll } = useLive();

  const [open, setOpen] = useState(false);

  const [filter, setFilter] = useState<LiveEventCategory | 'all'>('all');

  const [query, setQuery] = useState('');

  const [panelStyle, setPanelStyle] = useState<{ top: number; left: number; width: number } | null>(null);



  const triggerRef = useRef<HTMLButtonElement>(null);

  const panelRef = useRef<HTMLDivElement>(null);



  const updatePosition = useCallback(() => {

    const rect = triggerRef.current?.getBoundingClientRect();

    if (!rect) return;

    setPanelStyle(computePanelPosition(rect));

  }, []);



  const filtered = useMemo(() => {

    const q = query.trim().toLowerCase();

    return visibleNotifications.filter((n) => {

      if (filter !== 'all' && n.category !== filter) return false;

      if (!q) return true;

      return `${n.title} ${n.message}`.toLowerCase().includes(q);

    });

  }, [visibleNotifications, filter, query]);



  useEffect(() => {

    if (!open) return;

    updatePosition();

    window.addEventListener('resize', updatePosition);

    window.addEventListener('scroll', updatePosition, true);

    return () => {

      window.removeEventListener('resize', updatePosition);

      window.removeEventListener('scroll', updatePosition, true);

    };

  }, [open, updatePosition]);



  useEffect(() => {

    if (!open) return;



    const handleMouseDown = (event: MouseEvent) => {

      const target = event.target as Node;

      if (triggerRef.current?.contains(target) || panelRef.current?.contains(target)) return;

      setOpen(false);

    };



    const handleKeyDown = (event: KeyboardEvent) => {

      if (event.key === 'Escape') setOpen(false);

    };



    document.addEventListener('mousedown', handleMouseDown);

    document.addEventListener('keydown', handleKeyDown);

    return () => {

      document.removeEventListener('mousedown', handleMouseDown);

      document.removeEventListener('keydown', handleKeyDown);

    };

  }, [open]);



  const panel = (

    <AnimatePresence>

      {open && panelStyle && (

        <motion.div

          ref={panelRef}

          initial={{ opacity: 0, y: -4, scale: 0.98 }}

          animate={{ opacity: 1, y: 0, scale: 1 }}

          exit={{ opacity: 0, y: -4, scale: 0.98 }}

          transition={{ duration: 0.15 }}

          className="fixed z-[100] rounded-xl border border-slate-200 bg-white shadow-xl dark:border-slate-700 dark:bg-slate-900"

          style={{ top: panelStyle.top, left: panelStyle.left, width: panelStyle.width }}

          role="dialog"

          aria-label="Centre de notifications"

        >

          <div className="border-b border-slate-200 px-4 py-3 dark:border-slate-700">

            <div className="flex items-center justify-between">

              <h2 className="text-sm font-semibold text-slate-900 dark:text-slate-100">Notifications</h2>

              <div className="flex gap-1">

                <EnterpriseButton variant="ghost" size="sm" onClick={markAllRead}>Tout lire</EnterpriseButton>

                <EnterpriseButton variant="ghost" size="sm" onClick={clearAll}>Effacer</EnterpriseButton>

              </div>

            </div>

            <div className="mt-2">

              <EnterpriseSearch value={query} onSearch={setQuery} placeholder="Rechercher…" aria-label="Rechercher notifications" />

            </div>

            <div className="mt-2 flex flex-wrap gap-1" role="tablist" aria-label="Filtrer par catégorie">

              {FILTERS.map((f) => (

                <button

                  key={f.id}

                  type="button"

                  role="tab"

                  aria-selected={filter === f.id}

                  onClick={() => setFilter(f.id)}

                  className={`rounded-md px-2 py-0.5 text-[11px] font-medium ${

                    filter === f.id

                      ? 'bg-emerald-600 text-white'

                      : 'bg-slate-100 text-slate-600 dark:bg-slate-800 dark:text-slate-300'

                  }`}

                >

                  {f.label}

                </button>

              ))}

            </div>

          </div>

          <ul className="max-h-80 space-y-2 overflow-y-auto p-3">

            {filtered.length === 0 ? (

              <li className="py-8 text-center text-sm text-slate-500">Aucune notification</li>

            ) : (

              filtered.map((item) => (

                <LiveNotificationRow key={item.id} item={item} onMarkRead={markRead} onDismiss={dismiss} />

              ))

            )}

          </ul>

        </motion.div>

      )}

    </AnimatePresence>

  );



  return (

    <>

      <button

        ref={triggerRef}

        type="button"

        onClick={() => {
          if (open) {
            setOpen(false);
            return;
          }
          const rect = triggerRef.current?.getBoundingClientRect();
          if (rect) setPanelStyle(computePanelPosition(rect));
          setOpen(true);
        }}

        className="relative rounded-lg p-2 text-slate-400 transition-colors hover:bg-slate-900 hover:text-white focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-emerald-500"

        aria-label={`Notifications${unreadCount ? `, ${unreadCount} non lues` : ''}`}

        aria-expanded={open}

        aria-haspopup="dialog"

      >

        <svg className="h-5 w-5" fill="none" viewBox="0 0 24 24" strokeWidth={1.5} stroke="currentColor" aria-hidden>

          <path

            strokeLinecap="round"

            strokeLinejoin="round"

            d="M14.857 17.082h4.964M8.5 17.082h4.357m-4.357 0a3.375 3.375 0 01-3.375-3.375V9.75a5.25 5.25 0 0110.5 0v3.957a3.375 3.375 0 01-3.375 3.375z"

          />

        </svg>

        {unreadCount > 0 && (

          <motion.span

            initial={{ scale: 0.8 }}

            animate={{ scale: 1 }}

            className="absolute -right-0.5 -top-0.5 flex h-4 min-w-4 items-center justify-center rounded-full bg-emerald-500 px-1 text-[10px] font-bold text-white"

          >

            {unreadCount > 99 ? '99+' : unreadCount}

          </motion.span>

        )}

      </button>



      {createPortal(panel, document.body)}

    </>

  );

}

