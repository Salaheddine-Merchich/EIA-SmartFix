/** Shared layout tokens for the AI assistant premium UI. */
export const ASSISTANT_LAYOUT = {
  threadMaxWidth: 'max-w-3xl',
  pagePaddingX: 'px-4 sm:px-6',
  pagePaddingY: 'py-4',
  threadGap: 'gap-4',
  sidePanelWidth: 'lg:w-[320px] xl:w-[360px]',
  badgeText: 'text-xs',
} as const;

export const EXAMPLE_QUERIES = [
  {
    icon: '⚡',
    text: 'Code défaut E001 sur variateur ABB ACS880 au démarrage',
    category: 'Code défaut',
  },
  {
    icon: '🔥',
    text: 'Surchauffe moteur convoyeur, température carter > 85°C',
    category: 'Symptôme',
  },
  {
    icon: '📡',
    text: 'Capteur niveau silo 3 affiche 100% malgré vidange',
    category: 'Instrumentation',
  },
] as const;
