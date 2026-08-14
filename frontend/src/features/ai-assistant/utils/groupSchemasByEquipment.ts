import type { EquipmentSchemaRef } from '@/shared/types';

export interface EquipmentSchemaGroup {
  equipmentId: string;
  equipmentCode: string;
  equipmentDesignation?: string;
  totalSchemasForEquipment?: number;
  schemas: EquipmentSchemaRef[];
}

export function groupSchemasByEquipment(schemas: EquipmentSchemaRef[]): EquipmentSchemaGroup[] {
  const byEquipment = new Map<string, EquipmentSchemaGroup>();
  const seenSchemaIds = new Set<string>();

  for (const schema of schemas) {
    if (seenSchemaIds.has(schema.schemaId)) {
      continue;
    }
    seenSchemaIds.add(schema.schemaId);

    const existing = byEquipment.get(schema.equipmentId);
    if (existing) {
      existing.schemas.push(schema);
      if (schema.equipmentDesignation && !existing.equipmentDesignation) {
        existing.equipmentDesignation = schema.equipmentDesignation;
      }
      if (schema.totalSchemasForEquipment && !existing.totalSchemasForEquipment) {
        existing.totalSchemasForEquipment = schema.totalSchemasForEquipment;
      }
    } else {
      byEquipment.set(schema.equipmentId, {
        equipmentId: schema.equipmentId,
        equipmentCode: schema.equipmentCode,
        equipmentDesignation: schema.equipmentDesignation,
        totalSchemasForEquipment: schema.totalSchemasForEquipment,
        schemas: [schema],
      });
    }
  }

  return Array.from(byEquipment.values()).sort((a, b) =>
    a.equipmentCode.localeCompare(b.equipmentCode, 'fr'),
  );
}
