import { type KeyboardEvent, useState, useRef, useEffect, memo } from 'react';
import { EnterpriseButton } from '@/design-system';
import { ASSISTANT_LAYOUT } from '../constants/layout';

interface AiComposerProps {
  loading: boolean;
  onSend: (value: string) => void;
  onStop: () => void;
  initialValue?: string;
  onInitialValueConsumed?: () => void;
}

function AiComposerComponent({
  loading,
  onSend,
  onStop,
  initialValue = '',
  onInitialValueConsumed,
}: AiComposerProps) {
  const [value, setValue] = useState('');
  const textareaRef = useRef<HTMLTextAreaElement>(null);

  useEffect(() => {
    if (!initialValue) return;
    setValue(initialValue);
    onInitialValueConsumed?.();
  }, [initialValue, onInitialValueConsumed]);

  const canSend = value.trim().length > 0 && !loading;
  const maxChars = 1000;
  const charCount = value.length;
  const isNearLimit = charCount > maxChars * 0.8;

  useEffect(() => {
    const textarea = textareaRef.current;
    if (textarea) {
      textarea.style.height = 'auto';
      textarea.style.height = `${Math.min(textarea.scrollHeight, 120)}px`;
    }
  }, [value]);

  useEffect(() => {
    textareaRef.current?.focus();
  }, []);

  const submit = () => {
    if (!canSend) return;
    const next = value.trim();
    setValue('');
    onSend(next);
    textareaRef.current?.focus();
  };

  const onKeyDown = (event: KeyboardEvent<HTMLTextAreaElement>) => {
    if (loading) return;
    if (event.key === 'Enter' && !event.shiftKey) {
      event.preventDefault();
      submit();
    }
  };

  return (
    <div className={`border-t border-slate-200 bg-white dark:border-slate-800 dark:bg-slate-950 ${ASSISTANT_LAYOUT.pagePaddingX} py-4`}>
      <div className={`relative mx-auto w-full ${ASSISTANT_LAYOUT.threadMaxWidth}`}>
        <div className="rounded-xl border border-slate-200 bg-white p-3 shadow-sm dark:border-slate-700 dark:bg-slate-900">
          <div className="flex items-end gap-3">
            <div className="flex-1">
              <label htmlFor="ai-composer" className="sr-only">
                Décrivez votre panne
              </label>
              <textarea
                ref={textareaRef}
                id="ai-composer"
                rows={1}
                value={value}
                onChange={(e) => setValue(e.target.value.slice(0, maxChars))}
                onKeyDown={onKeyDown}
                placeholder={loading ? 'Génération…' : 'Décrivez votre panne ou symptômes observés…'}
                className="w-full resize-none border-0 bg-transparent px-1 py-2 text-sm text-slate-900 placeholder:text-slate-400 focus:outline-none focus-visible:ring-2 focus-visible:ring-emerald-500 focus-visible:ring-offset-2 dark:text-slate-100 dark:placeholder:text-slate-500 dark:focus-visible:ring-offset-slate-900"
                style={{ minHeight: '40px', maxHeight: '120px' }}
              />
            </div>

            {loading ? (
              <EnterpriseButton
                type="button"
                variant="secondary"
                size="sm"
                onClick={onStop}
                className="shrink-0 whitespace-nowrap"
              >
                Arrêter la génération
              </EnterpriseButton>
            ) : (
              <EnterpriseButton
                type="button"
                size="sm"
                onClick={submit}
                disabled={!canSend}
                aria-label="Envoyer le message"
                className="shrink-0"
              >
                Envoyer
              </EnterpriseButton>
            )}
          </div>
        </div>

        <div className="mt-2 flex items-center justify-between text-xs text-slate-500 dark:text-slate-400">
          <span>Entrée pour envoyer · Maj+Entrée pour nouvelle ligne</span>
          <span className={isNearLimit ? 'font-medium text-amber-600' : 'tabular-nums text-slate-400'}>
            {charCount}/{maxChars}
          </span>
        </div>
      </div>
    </div>
  );
}

export const AiComposer = memo(AiComposerComponent);
