const SCHEMA_TYPE_LABELS: Record<string, string> = {
  wiring: 'Câblage',
  terminal: 'Bornes',
  dimension: 'Dimensions',
  install: 'Installation',
  block: 'Bloc fonctionnel',
};

export function schemaTypeLabel(schemaType: string): string {
  return SCHEMA_TYPE_LABELS[schemaType] ?? schemaType;
}
