import { useEffect, useState } from 'react';
import { EnterpriseButton, EnterpriseModal } from '@/design-system';
import { equipmentApi } from '@/shared/api';
import type { EquipmentSchemaRef } from '@/shared/types';
import { groupSchemasByEquipment } from '../utils/groupSchemasByEquipment';
import { ChevronRightIcon, SchemaSheetIcon } from './SchemaIcons';

interface SchemaEquipmentPickerModalProps {
  schemas: EquipmentSchemaRef[];
  onClose: () => void;
  onSelectEquipment: (equipmentId: string, equipmentCode: string) => void;
}

export function SchemaEquipmentPickerModal({
  schemas,
  onClose,
  onSelectEquipment,
}: SchemaEquipmentPickerModalProps) {
  const groups = groupSchemasByEquipment(schemas);
  const [schemaCounts, setSchemaCounts] = useState<Record<string, number>>({});

  useEffect(() => {
    let cancelled = false;

    const groupsNeedingCount = groups.filter(
      (group) => group.totalSchemasForEquipment == null,
    );

    if (groupsNeedingCount.length === 0) {
      return;
    }

    void Promise.all(
      groupsNeedingCount.map(async (group) => {
        try {
          const items = await equipmentApi.listSchemas(group.equipmentId);
          return [group.equipmentId, items.length] as const;
        } catch {
          return [group.equipmentId, group.schemas.length] as const;
        }
      }),
    ).then((entries) => {
      if (cancelled) return;
      setSchemaCounts(Object.fromEntries(entries));
    });

    return () => {
      cancelled = true;
    };
  }, [groups]);

  function schemaCountForGroup(group: (typeof groups)[number]): number {
    return group.totalSchemasForEquipment ?? schemaCounts[group.equipmentId] ?? group.schemas.length;
  }

  return (
    <EnterpriseModal
      open
      onClose={onClose}
      title="Schémas techniques"
      size="md"
      footer={<EnterpriseButton variant="secondary" onClick={onClose}>Fermer</EnterpriseButton>}
    >
      <p className="mb-4 text-sm text-slate-500 dark:text-slate-400">
        Schémas extraits des manuels constructeur pour l&apos;équipement concerné
      </p>
      <ul className="space-y-2">
        {groups.map((group) => {
          const count = schemaCountForGroup(group);
          return (
            <li key={group.equipmentId}>
              <button
                type="button"
                onClick={() => onSelectEquipment(group.equipmentId, group.equipmentCode)}
                className="flex w-full items-center justify-between gap-3 rounded-lg border border-slate-200 bg-white px-4 py-3 text-left hover:border-sky-300 hover:bg-sky-50/50 dark:border-slate-700 dark:bg-slate-900 dark:hover:border-sky-700"
              >
                <span className="flex h-10 w-10 shrink-0 items-center justify-center rounded-lg bg-sky-100 text-sky-700 dark:bg-sky-950 dark:text-sky-300">
                  <SchemaSheetIcon className="h-5 w-5" />
                </span>
                <span className="min-w-0 flex-1">
                  <span className="block font-medium text-slate-900 dark:text-slate-100">
                    {group.equipmentCode}
                  </span>
                  {group.equipmentDesignation && (
                    <span className="mt-0.5 block truncate text-xs text-slate-500 dark:text-slate-400">
                      {group.equipmentDesignation}
                    </span>
                  )}
                </span>
                <span className="shrink-0 text-xs text-slate-500">
                  {count} schéma{count > 1 ? 's' : ''}
                </span>
                <ChevronRightIcon className="h-4 w-4 shrink-0 text-slate-400" />
              </button>
            </li>
          );
        })}
      </ul>
    </EnterpriseModal>
  );
}
