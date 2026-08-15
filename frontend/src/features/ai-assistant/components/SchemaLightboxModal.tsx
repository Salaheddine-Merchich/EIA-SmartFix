import { useEffect, useState } from 'react';
import { EnterpriseButton, EnterpriseModal } from '@/design-system';
import { equipmentApi } from '@/shared/api';
import type { EquipmentSchemaRef } from '@/shared/types';
import { formatSchemaSource } from '../utils/formatSchemaSource';
import { schemaTypeLabel } from '../utils/schemaTypeLabel';

interface SchemaLightboxModalProps {
  schema: EquipmentSchemaRef;
  onClose: () => void;
}

export function SchemaLightboxModal({ schema, onClose }: SchemaLightboxModalProps) {
  const [imageUrl, setImageUrl] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let objectUrl: string | null = null;
    let cancelled = false;

    void equipmentApi
      .downloadSchemaBlob(schema.equipmentId, schema.schemaId)
      .then((blob) => {
        if (cancelled) return;
        objectUrl = URL.createObjectURL(blob);
        setImageUrl(objectUrl);
      })
      .catch(() => {
        if (!cancelled) setError('Impossible de charger le schéma.');
      });

    return () => {
      cancelled = true;
      if (objectUrl) URL.revokeObjectURL(objectUrl);
    };
  }, [schema.equipmentId, schema.schemaId]);

  const sourceLabel = formatSchemaSource(schema.sourcePdf, schema.sourcePage);
  const equipmentLine = schema.equipmentDesignation
    ? `${schema.equipmentCode} · ${schema.equipmentDesignation}`
    : schema.equipmentCode;

  return (
    <EnterpriseModal
      open
      onClose={onClose}
      title={schema.label}
      size="xl"
      footer={<EnterpriseButton variant="secondary" onClick={onClose}>Fermer</EnterpriseButton>}
    >
      <div className="space-y-3">
        <div>
          <div className="flex flex-wrap items-center gap-2">
            <p className="text-sm font-medium text-slate-800 dark:text-slate-200">{equipmentLine}</p>
            <span className="inline-flex rounded-md bg-emerald-50 px-2 py-0.5 text-[11px] font-semibold uppercase tracking-wide text-emerald-800 dark:bg-emerald-950/50 dark:text-emerald-300">
              {schemaTypeLabel(schema.schemaType)}
            </span>
          </div>
          {schema.caption && (
            <p className="mt-1.5 text-sm text-slate-500 dark:text-slate-400">{schema.caption}</p>
          )}
        </div>

        {error && <p className="text-sm text-red-600 dark:text-red-400">{error}</p>}

        <div className="overflow-hidden rounded-lg border border-slate-200 bg-slate-100 dark:border-slate-700 dark:bg-slate-800">
          {!error && !imageUrl && (
            <p className="py-16 text-center text-sm text-slate-500">Chargement du schéma…</p>
          )}
          {imageUrl && (
            <div className="flex items-center justify-center p-4">
              <img
                src={imageUrl}
                alt={schema.label}
                className="mx-auto max-h-[60vh] w-auto max-w-full object-contain"
              />
            </div>
          )}
          {sourceLabel && (
            <p className="border-t border-slate-200 px-3 py-2 text-xs text-slate-500 dark:border-slate-700 dark:text-slate-400">
              Source : {sourceLabel}
            </p>
          )}
        </div>
      </div>
    </EnterpriseModal>
  );
}
