import type { EquipmentSchema, EquipmentSchemaRef } from '@/shared/types';

export function toSchemaRef(schema: EquipmentSchema): EquipmentSchemaRef {
  return {
    schemaId: schema.id,
    equipmentId: schema.equipmentId,
    equipmentCode: schema.equipmentCode,
    label: schema.label,
    schemaType: schema.schemaType,
    sourcePdf: schema.sourcePdf,
    sourcePage: schema.sourcePage,
    caption: schema.caption,
    downloadUrl: `/api/v1/equipment/${schema.equipmentId}/schemas/${schema.id}/download`,
  };
}
