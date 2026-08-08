/** Merge class names — lightweight utility without extra dependencies. */
export function cn(...classes: Array<string | false | null | undefined>): string {
  return classes.filter(Boolean).join(' ');
}
