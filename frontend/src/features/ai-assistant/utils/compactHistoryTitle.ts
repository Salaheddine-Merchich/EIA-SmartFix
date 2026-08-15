export const HISTORY_TITLE_MAX = 36;

export function compactHistoryTitle(raw: string, max = HISTORY_TITLE_MAX): string {
  const trimmed = raw.trim().replace(/\s+/g, ' ');
  if (!trimmed) return '';
  if (trimmed.length <= max) return trimmed;
  const slice = trimmed.slice(0, max);
  const lastSpace = slice.lastIndexOf(' ');
  return lastSpace > 16 ? slice.slice(0, lastSpace) : slice;
}
