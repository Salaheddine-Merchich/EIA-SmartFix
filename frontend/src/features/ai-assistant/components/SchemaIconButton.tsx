import { useState } from 'react';
import type { EquipmentSchemaRef } from '@/shared/types';
import { SchemaEquipmentPickerModal } from './SchemaEquipmentPickerModal';
import { SchemaListModal } from './SchemaListModal';
import { SchemaSheetIcon } from './SchemaIcons';

interface SchemaIconButtonProps {
  schemas: EquipmentSchemaRef[];
}

export function SchemaIconButton({ schemas }: SchemaIconButtonProps) {
  const [pickerOpen, setPickerOpen] = useState(false);
  const [selectedEquipment, setSelectedEquipment] = useState<{
    equipmentId: string;
    equipmentCode: string;
  } | null>(null);

  if (!schemas.length) return null;

  const equipmentSchemas = selectedEquipment
    ? schemas.filter((schema) => schema.equipmentId === selectedEquipment.equipmentId)
    : [];

  return (
    <>
      <button
        type="button"
        onClick={() => setPickerOpen(true)}
        className="inline-flex items-center gap-1.5 rounded-lg border border-sky-200 bg-sky-50 px-2.5 py-1 text-xs font-medium text-sky-700 hover:border-sky-400 hover:bg-sky-100 focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-sky-600 dark:border-sky-800 dark:bg-sky-950/40 dark:text-sky-300 dark:hover:border-sky-600 dark:hover:bg-sky-950/60"
        title="Voir les schémas techniques"
        aria-label="Voir les schémas techniques"
      >
        <SchemaSheetIcon className="h-3.5 w-3.5 shrink-0" />
        Schémas
      </button>

      {pickerOpen && (
        <SchemaEquipmentPickerModal
          schemas={schemas}
          onClose={() => setPickerOpen(false)}
          onSelectEquipment={(equipmentId, equipmentCode) => {
            setPickerOpen(false);
            setSelectedEquipment({ equipmentId, equipmentCode });
          }}
        />
      )}

      {selectedEquipment && (
        <SchemaListModal
          equipmentId={selectedEquipment.equipmentId}
          equipmentCode={selectedEquipment.equipmentCode}
          fallbackSchemas={equipmentSchemas}
          onClose={() => setSelectedEquipment(null)}
        />
      )}
    </>
  );
}
