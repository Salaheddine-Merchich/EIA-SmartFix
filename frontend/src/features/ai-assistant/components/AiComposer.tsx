import { type KeyboardEvent, useState, useRef, useEffect, memo } from 'react';
import { EnterpriseButton } from '@/design-system';
import { ASSISTANT_LAYOUT } from '../constants/layout';
import { isValidAssistQuery } from '../utils/isValidAssistQuery';

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

  const trimmedValue = value.trim();
  const canSend = isValidAssistQuery(trimmedValue) && !loading;
  const showMinLengthHint = trimmedValue.length > 0 && !isValidAssistQuery(trimmedValue);
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
    <div className={`border-t border-slate-200 bg-slate-50 dark:border-slate-800 dark:bg-slate-950 ${ASSISTANT_LAYOUT.pagePaddingX} py-3`}>
      <div className={`mx-auto w-full ${ASSISTANT_LAYOUT.threadMaxWidth}`}>
        <div className="rounded-xl border border-slate-200 bg-white px-3 py-2.5 transition-colors focus-within:border-emerald-500/50 dark:border-slate-700 dark:bg-slate-900 dark:focus-within:border-emerald-500/50">
          <div className="flex items-end gap-2.5">
            <div className="min-w-0 flex-1">
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
                className="w-full resize-none border-0 bg-transparent px-0.5 py-1.5 text-sm leading-relaxed text-slate-900 outline-none placeholder:text-slate-400 dark:text-slate-100 dark:placeholder:text-slate-500"
                style={{ minHeight: '40px', maxHeight: '120px' }}
              />
            </div>

            {loading ? (
              <EnterpriseButton
                type="button"
                variant="secondary"
                size="sm"
                onClick={onStop}
                className="mb-0.5 h-9 shrink-0 whitespace-nowrap"
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
                className="mb-0.5 h-9 shrink-0"
              >
                Envoyer
              </EnterpriseButton>
            )}
          </div>

          <div className="mt-1.5 flex items-center justify-between gap-3 text-[11px] text-slate-500 dark:text-slate-400">
            <span>
              {showMinLengthHint
                ? 'Décrivez un symptôme, un équipement ou un code défaut du parc OCP.'
                : 'Entrée pour envoyer · Maj+Entrée pour nouvelle ligne'}
            </span>
            <span className={`tabular-nums ${isNearLimit || showMinLengthHint ? 'font-medium text-amber-600' : 'text-slate-400'}`}>
              {charCount}/{maxChars}
            </span>
          </div>
        </div>
      </div>
    </div>
  );
}

export const AiComposer = memo(AiComposerComponent);
