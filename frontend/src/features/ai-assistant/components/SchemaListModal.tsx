import { useEffect, useState } from 'react';
import { EnterpriseButton, EnterpriseModal } from '@/design-system';
import { equipmentApi } from '@/shared/api';
import type { EquipmentSchemaRef } from '@/shared/types';
import { formatSchemaSource } from '../utils/formatSchemaSource';
import { schemaTypeLabel } from '../utils/schemaTypeLabel';
import { ChevronRightIcon, schemaIcon, schemaTypeTone } from './SchemaIcons';
import { SchemaLightboxModal } from './SchemaLightboxModal';
import { toSchemaRef } from '../utils/toSchemaRef';

interface SchemaListModalProps {
  equipmentId: string;
  equipmentCode: string;
  fallbackSchemas: EquipmentSchemaRef[];
  onClose: () => void;
}

export function SchemaListModal({
  equipmentId,
  equipmentCode,
  fallbackSchemas,
  onClose,
}: SchemaListModalProps) {
  const [activeSchema, setActiveSchema] = useState<EquipmentSchemaRef | null>(null);
  const [schemas, setSchemas] = useState<EquipmentSchemaRef[]>(fallbackSchemas);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    let cancelled = false;

    void equipmentApi
      .listSchemas(equipmentId)
      .then((items) => {
        if (cancelled) return;
        setSchemas(items.map(toSchemaRef));
      })
      .catch(() => {
        if (cancelled) return;
        setSchemas(fallbackSchemas);
      })
      .finally(() => {
        if (!cancelled) {
          setLoading(false);
        }
      });

    return () => {
      cancelled = true;
    };
  }, [equipmentId, fallbackSchemas]);

  return (
    <>
      <EnterpriseModal
        open
        onClose={onClose}
        title={`Schémas — ${equipmentCode}`}
        size="md"
        footer={<EnterpriseButton variant="secondary" onClick={onClose}>Fermer</EnterpriseButton>}
      >
        <p className="mb-4 text-sm text-slate-500 dark:text-slate-400">
          Extraits des manuels constructeur
        </p>
        {loading && (
          <p className="mb-4 text-sm text-slate-500 dark:text-slate-400">Chargement des schémas…</p>
        )}
        {!loading && schemas.length === 0 && (
          <p className="text-sm text-slate-500 dark:text-slate-400">
            Aucun schéma disponible pour cet équipement.
          </p>
        )}
        <ul className="space-y-2">
          {schemas.map((schema) => {
            const Icon = schemaIcon(schema.schemaType);
            const source = formatSchemaSource(schema.sourcePdf, schema.sourcePage);
            return (
              <li key={schema.schemaId}>
                <button
                  type="button"
                  onClick={() => setActiveSchema(schema)}
                  className="flex w-full items-center gap-3 rounded-lg border border-slate-200 bg-white px-3 py-3 text-left hover:border-sky-300 dark:border-slate-700 dark:bg-slate-900 dark:hover:border-sky-700"
                >
                  <span
                    className={`flex h-10 w-10 shrink-0 items-center justify-center rounded-lg ${schemaTypeTone(schema.schemaType)}`}
                  >
                    <Icon className="h-5 w-5" />
                  </span>
                  <span className="min-w-0 flex-1">
                    <span className="block text-sm font-medium text-slate-900 dark:text-slate-100">
                      {schema.label}
                    </span>
                    {schema.caption && (
                      <span className="mt-0.5 block truncate text-xs text-slate-600 dark:text-slate-400">
                        {schema.caption}
                      </span>
                    )}
                    <span className="mt-1.5 flex items-center gap-2">
                      <span
                        className={`inline-flex rounded-md px-1.5 py-0.5 text-[10px] font-semibold uppercase tracking-wide ${schemaTypeTone(schema.schemaType)}`}
                      >
                        {schemaTypeLabel(schema.schemaType)}
                      </span>
                      {source && (
                        <span className="min-w-0 truncate text-xs text-slate-500">{source}</span>
                      )}
                    </span>
                  </span>
                  <ChevronRightIcon className="h-4 w-4 shrink-0 text-slate-400" />
                </button>
              </li>
            );
          })}
        </ul>
      </EnterpriseModal>

      {activeSchema && (
        <SchemaLightboxModal schema={activeSchema} onClose={() => setActiveSchema(null)} />
      )}
    </>
  );
}
