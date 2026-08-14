import type { RetrievedSchema } from '../types';
import { ChevronRightIcon, schemaIcon, schemaTypeTone } from '@/features/ai-assistant/components/SchemaIcons';
import { formatSchemaSource } from '@/features/ai-assistant/utils/formatSchemaSource';
import { schemaTypeLabel } from '@/features/ai-assistant/utils/schemaTypeLabel';

interface SchemaEvidenceCardProps {
  schema: RetrievedSchema;
  index: number;
  onOpen?: (schema: RetrievedSchema) => void;
}

export function SchemaEvidenceCard({ schema, index, onOpen }: SchemaEvidenceCardProps) {
  const Icon = schemaIcon(schema.schemaType);
  const source = formatSchemaSource(schema.sourcePdf, schema.sourcePage);

  return (
    <button
      type="button"
      onClick={() => onOpen?.(schema)}
      className="flex w-full items-center gap-3 rounded-lg border border-slate-200 bg-white p-3 text-left hover:border-sky-300 dark:border-slate-700 dark:bg-slate-900 dark:hover:border-sky-700"
    >
      <span
        className={`flex h-10 w-10 shrink-0 items-center justify-center rounded-lg ${schemaTypeTone(schema.schemaType)}`}
      >
        <Icon className="h-5 w-5" />
      </span>
      <div className="min-w-0 flex-1">
        <p className="text-xs font-medium uppercase tracking-wide text-slate-500">Schéma #{index + 1}</p>
        <p className="mt-0.5 text-sm font-medium text-slate-900 dark:text-slate-100">{schema.label}</p>
        {schema.caption && (
          <p className="mt-0.5 truncate text-xs text-slate-600 dark:text-slate-400">{schema.caption}</p>
        )}
        <div className="mt-1.5 flex items-center gap-2">
          <span
            className={`inline-flex rounded-md px-1.5 py-0.5 text-[10px] font-semibold uppercase tracking-wide ${schemaTypeTone(schema.schemaType)}`}
          >
            {schemaTypeLabel(schema.schemaType)}
          </span>
          <span className="min-w-0 truncate text-xs text-slate-500">
            {schema.equipmentCode}
            {schema.equipmentDesignation ? ` · ${schema.equipmentDesignation}` : ''}
            {source ? ` · ${source}` : ''}
          </span>
        </div>
      </div>
      <ChevronRightIcon className="h-4 w-4 shrink-0 text-slate-400" />
    </button>
  );
}
