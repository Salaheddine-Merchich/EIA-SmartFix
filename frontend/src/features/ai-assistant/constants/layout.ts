/** Shared layout tokens for the AI assistant UI. */
export const ASSISTANT_LAYOUT = {
  threadMaxWidth: 'max-w-3xl',
  pagePaddingX: 'px-4 sm:px-6',
  pagePaddingY: 'py-4',
  threadGap: 'gap-4',
  sidePanelWidth: 'lg:w-[320px] xl:w-[360px]',
  historyPanelWidth: 'lg:w-[272px]',
  badgeText: 'text-xs',
} as const;

export const EXAMPLE_QUERIES = [
  {
    text: 'Defaut E21 variateur convoyeur Hitachi SJ200',
    category: 'Code défaut',
  },
  {
    text: 'Pompe PV ne démarre plus station solaire',
    category: 'Station PV',
  },
  {
    text: 'Code OUt1 affiché sur variateur Goodrive 100-PV',
    category: 'Code défaut',
  },
  {
    text: 'Code 2310 surintensité sortie variateur ACS880 filature',
    category: 'Filature',
  },
  {
    text: 'Sonde manque d\'eau TA-TC pompe solaire VEICHI',
    category: 'Instrumentation',
  },
  {
    text: 'Code E01 surintensité variateur Hitachi SJ200',
    category: 'Convoyage',
  },
] as const;
