export function formatSchemaSource(sourcePdf?: string, sourcePage?: number): string | null {
  if (sourcePdf && sourcePage) {
    return `${sourcePdf} · p.${sourcePage}`;
  }
  if (sourcePdf) {
    return sourcePdf;
  }
  return null;
}
