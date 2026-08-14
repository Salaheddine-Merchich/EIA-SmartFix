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
      size="lg"
      footer={<EnterpriseButton variant="secondary" onClick={onClose}>Fermer</EnterpriseButton>}
    >
      <div className="space-y-3">
        <p className="text-sm text-slate-600 dark:text-slate-400">
          {equipmentLine} · {schemaTypeLabel(schema.schemaType)}
        </p>
        {schema.caption && (
          <p className="text-sm text-slate-600 dark:text-slate-400">{schema.caption}</p>
        )}
        {error && <p className="text-sm text-red-600">{error}</p>}
        {!error && !imageUrl && (
          <p className="py-12 text-center text-sm text-slate-500">Chargement du schéma…</p>
        )}
        {imageUrl && (
          <img
            src={imageUrl}
            alt={schema.label}
            className="mx-auto max-h-[70vh] w-full rounded-lg border border-slate-200 object-contain dark:border-slate-700"
          />
        )}
        {sourceLabel && (
          <p className="text-xs text-slate-500 dark:text-slate-400">Source : {sourceLabel}</p>
        )}
      </div>
    </EnterpriseModal>
  );
}
