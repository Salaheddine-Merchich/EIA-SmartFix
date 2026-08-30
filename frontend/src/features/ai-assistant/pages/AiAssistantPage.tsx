import { useCallback, useMemo, useState, useEffect } from 'react';
import { useLocation } from 'react-router-dom';
import { cn, EnterpriseDrawer, useEnterpriseConfirm } from '@/design-system';
import { AiAssistantHeader } from '../components/AiAssistantHeader';
import { AiComposer } from '../components/AiComposer';
import { ConversationThread } from '../components/ConversationThread';
import { SuggestionCards } from '../components/SuggestionCards';
import { ConversationHistorySidebar } from '../components/ConversationHistorySidebar';
import { ConversationStateBanner } from '../components/ConversationStateBanner';
import { AiDiagnosticTracePanel } from '../explainability';
import { useAiConversation } from '../hooks/useAiConversation';
import { useGenerationNavigationGuard } from '../hooks/useGenerationNavigationGuard';
import { useHistorySidebarOpen } from '../hooks/useHistorySidebarOpen';
import { ASSISTANT_LAYOUT } from '../constants/layout';
import type { AiDiagnosticTrace } from '@/shared/types';

interface AiAssistantLocationState {
  failureId?: string;
  equipmentId?: string;
  prefilledDescription?: string;
}

function isMobileViewport() {
  return typeof window !== 'undefined' && window.matchMedia('(max-width: 1023px)').matches;
}

export default function AiAssistantPage() {
  const location = useLocation();
  const { confirm } = useEnterpriseConfirm();
  const { open: historyOpen, toggleSidebar, closeSidebar } = useHistorySidebarOpen();
  const {
    messages,
    loading,
    loadingMessage,
    status,
    similarInterventions,
    assistContext,
    sendMessage,
    cancelGeneration,
    clearConversation,
    history,
    historyLoading,
    conversationId,
    openConversation,
    deleteConversation,
    clearAllHistory,
    setAssistContext,
    setComposerPrefill,
    composerPrefill,
    clearComposerPrefill,
  } = useAiConversation();
  const [traceOpen, setTraceOpen] = useState(false);
  const [activeTrace, setActiveTrace] = useState<AiDiagnosticTrace | null>(null);

  useGenerationNavigationGuard(loading);

  useEffect(() => {
    const state = location.state as AiAssistantLocationState | null;
    if (!state?.failureId && !state?.equipmentId && !state?.prefilledDescription) return;
    setAssistContext({
      failureId: state.failureId,
      equipmentId: state.equipmentId,
    });
    if (state.prefilledDescription) {
      setComposerPrefill(state.prefilledDescription);
    }
    window.history.replaceState({}, document.title);
  }, [location.state, setAssistContext, setComposerPrefill]);

  const lastMessageCancelled = useMemo(() => {
    const last = messages[messages.length - 1];
    return last?.role === 'assistant' && last.error?.kind === 'cancelled';
  }, [messages]);

  const showSimilarPanel = messages.length > 0 || loading;

  const openTrace = (trace: AiDiagnosticTrace) => {
    setActiveTrace(trace);
    setTraceOpen(true);
  };

  const handleSelectConversation = useCallback(
    (id: string) => {
      void openConversation(id);
      if (isMobileViewport()) {
        closeSidebar();
      }
    },
    [openConversation, closeSidebar],
  );

  const handleDeleteConversation = useCallback(
    (id: string) => {
      void (async () => {
        const ok = await confirm({
          title: 'Supprimer cette conversation',
          message:
            'Cette conversation sera définitivement retirée de votre historique. Cette action est irréversible.',
          confirmLabel: 'Supprimer',
          cancelLabel: 'Annuler',
          variant: 'danger',
        });
        if (ok) {
          await deleteConversation(id);
        }
      })();
    },
    [confirm, deleteConversation],
  );

  const handleDeleteAllConversations = useCallback(() => {
    void (async () => {
      const ok = await confirm({
        title: 'Vider l’historique',
        message:
          'Toutes vos conversations avec l’assistant seront définitivement supprimées. Cette action est irréversible.',
        confirmLabel: 'Tout supprimer',
        cancelLabel: 'Annuler',
        variant: 'danger',
      });
      if (ok) {
        await clearAllHistory();
      }
    })();
  }, [confirm, clearAllHistory]);

  const historySidebarProps = {
    items: history,
    activeId: conversationId,
    loading: historyLoading,
    onSelect: handleSelectConversation,
    onNew: clearConversation,
    onDelete: handleDeleteConversation,
    onDeleteAll: handleDeleteAllConversations,
  };

  return (
    <div className="flex h-full min-h-0 flex-col bg-white dark:bg-slate-950">
      <AiAssistantHeader
        status={status}
        historyOpen={historyOpen}
        onToggleHistory={toggleSidebar}
        contextHint={
          assistContext.failureId || assistContext.equipmentId
            ? 'Contexte panne actif — les suggestions seront ciblées sur cet équipement.'
            : undefined
        }
      />

      <div className="flex min-h-0 flex-1 overflow-hidden lg:flex-row">
        <div
          className={cn(
            'hidden shrink-0 overflow-hidden transition-[width] duration-200 ease-out lg:block lg:h-full',
            historyOpen ? 'w-[272px]' : 'w-0',
          )}
          aria-hidden={!historyOpen}
        >
          <ConversationHistorySidebar
            {...historySidebarProps}
            onClose={closeSidebar}
            className="h-full"
          />
        </div>

        <section className="flex min-h-0 flex-1 flex-col lg:min-w-0">
          <div className="min-h-0 flex-1">
            <ConversationThread
              messages={messages}
              loading={loading}
              loadingMessage={loadingMessage}
              onViewAnalysis={openTrace}
              onExampleSelect={sendMessage}
            />
          </div>

          <div className={`${ASSISTANT_LAYOUT.pagePaddingX} pb-2`}>
            <ConversationStateBanner
              loadingMessage={loadingMessage}
              loading={loading}
              status={status}
              lastMessageCancelled={lastMessageCancelled}
            />
          </div>

          <AiComposer
            loading={loading}
            onSend={sendMessage}
            onStop={cancelGeneration}
            initialValue={composerPrefill}
            onInitialValueConsumed={clearComposerPrefill}
          />
        </section>

        {showSimilarPanel && (
          <div className={`min-h-[240px] w-full ${ASSISTANT_LAYOUT.sidePanelWidth} shrink-0`}>
            <SuggestionCards items={similarInterventions} loading={loading} />
          </div>
        )}
      </div>

      <div className="lg:hidden">
        <EnterpriseDrawer
          open={historyOpen}
          onClose={closeSidebar}
          title="Historique"
          side="left"
        >
          <ConversationHistorySidebar
            {...historySidebarProps}
            showHeader={false}
            className="-m-5 h-[calc(100%+2.5rem)] w-[calc(100%+2.5rem)] border-0"
          />
        </EnterpriseDrawer>
      </div>

      <EnterpriseDrawer
        open={traceOpen}
        onClose={() => setTraceOpen(false)}
        title="Analyse du diagnostic IA"
      >
        {activeTrace && <AiDiagnosticTracePanel trace={activeTrace} />}
      </EnterpriseDrawer>
    </div>
  );
}
