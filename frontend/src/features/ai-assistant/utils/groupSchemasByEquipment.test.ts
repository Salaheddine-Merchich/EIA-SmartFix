import { describe, expect, it } from 'vitest';
import { groupSchemasByEquipment } from './groupSchemasByEquipment';
import type { EquipmentSchemaRef } from '@/shared/types';

describe('groupSchemasByEquipment', () => {
  it('groups schemas by equipment and sorts by code', () => {
    const schemas: EquipmentSchemaRef[] = [
      {
        schemaId: '1',
        equipmentId: 'b',
        equipmentCode: 'VAR-GD-100PV',
        label: 'PV system',
        schemaType: 'wiring',
        downloadUrl: '/download/1',
      },
      {
        schemaId: '2',
        equipmentId: 'a',
        equipmentCode: 'VAR-VEI-SI23',
        label: 'X1 wiring',
        schemaType: 'wiring',
        downloadUrl: '/download/2',
      },
      {
        schemaId: '3',
        equipmentId: 'a',
        equipmentCode: 'VAR-VEI-SI23',
        label: 'Terminals',
        schemaType: 'terminal',
        downloadUrl: '/download/3',
      },
    ];

    const groups = groupSchemasByEquipment(schemas);

    expect(groups).toHaveLength(2);
    expect(groups[0].equipmentCode).toBe('VAR-GD-100PV');
    expect(groups[1].equipmentCode).toBe('VAR-VEI-SI23');
    expect(groups[1].schemas).toHaveLength(2);
  });

  it('ignores duplicate schema ids when grouping', () => {
    const schemas: EquipmentSchemaRef[] = [
      {
        schemaId: 'dup',
        equipmentId: 'a',
        equipmentCode: 'VAR-VEI-SI23',
        label: 'First',
        schemaType: 'wiring',
        downloadUrl: '/download/1',
      },
      {
        schemaId: 'dup',
        equipmentId: 'a',
        equipmentCode: 'VAR-VEI-SI23',
        label: 'Duplicate',
        schemaType: 'wiring',
        downloadUrl: '/download/2',
      },
    ];

    const groups = groupSchemasByEquipment(schemas);

    expect(groups).toHaveLength(1);
    expect(groups[0].schemas).toHaveLength(1);
  });
});
