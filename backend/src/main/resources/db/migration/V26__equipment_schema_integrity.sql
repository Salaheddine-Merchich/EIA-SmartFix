-- V26: Schema integrity constraints and refined trigger_keywords for equipment-specific matching

ALTER TABLE equipment_schemas
    ADD CONSTRAINT uk_equipment_schemas_equipment_file UNIQUE (equipment_id, file_path);

ALTER TABLE equipment_schemas
    ADD CONSTRAINT uk_equipment_schemas_file_path UNIQUE (file_path);

-- VEICHI VAR-VEI-SI23
UPDATE equipment_schemas SET trigger_keywords = ARRAY['si23', 'dimension', 'veichi', 'var-vei']
WHERE id = 'c0010001-0001-0001-0001-000000000001';

UPDATE equipment_schemas SET trigger_keywords = ARRAY['si23', 'puissance', 'rs', 'st', 'uv', 'w', 'var-vei', 'veichi']
WHERE id = 'c0010002-0002-0002-0002-000000000002';

UPDATE equipment_schemas SET trigger_keywords = ARRAY['x1', 'ta-tc', 'veille', 'sommeil', 'marche a sec', 'f14', 'si23', 'var-vei', 'veichi']
WHERE id = 'c0010003-0003-0003-0003-000000000003';

UPDATE equipment_schemas SET trigger_keywords = ARRAY['si23', 'borne', 'terminal', 'veichi', 'var-vei']
WHERE id = 'c0010018-0018-0018-0018-000000000018';

-- SEN-EAU
UPDATE equipment_schemas SET trigger_keywords = ARRAY['sonde', 'eau', 'ta-tc', 'x1', 'marche a sec', 'sen-eau', 'manque eau']
WHERE id = 'c0010004-0004-0004-0004-000000000004';

-- Goodrive VAR-GD-100PV
UPDATE equipment_schemas SET trigger_keywords = ARRAY['goodrive', 'pv', 'installation', 'gd-100', 'var-gd']
WHERE id = 'c0010005-0005-0005-0005-000000000005';

UPDATE equipment_schemas SET trigger_keywords = ARRAY['pv', 'cablage', 'out1', 'alarme plein', 'goodrive', 'gd-100', 'var-gd']
WHERE id = 'c0010006-0006-0006-0006-000000000006';

UPDATE equipment_schemas SET trigger_keywords = ARRAY['borne', 'rs', 't', 'pv', 'terminal', 'goodrive', 'gd-100', 'var-gd']
WHERE id = 'c0010008-0008-0008-0008-000000000008';

UPDATE equipment_schemas SET trigger_keywords = ARRAY['out1', 'di', 'do', 'commande', 'pv', 'goodrive', 'gd-100', 'var-gd']
WHERE id = 'c0010020-0020-0020-0020-000000000020';

-- POM-PV
UPDATE equipment_schemas SET trigger_keywords = ARRAY['pompe', 'pv', 'demarrage', 'ne demarre plus', 'pom-pv', 'pompe solaire']
WHERE id = 'c0010007-0007-0007-0007-000000000007';

-- MOT-PV
UPDATE equipment_schemas SET trigger_keywords = ARRAY['moteur', 'uv', 'w', 'mot-pv', 'moteur pompe']
WHERE id = 'c0010009-0009-0009-0009-000000000009';

-- CAP-PV
UPDATE equipment_schemas SET trigger_keywords = ARRAY['panneau', 'cap-pv', 'pv', 'dc', 'panneaux']
WHERE id = 'c0010010-0010-0010-0010-000000000010';
