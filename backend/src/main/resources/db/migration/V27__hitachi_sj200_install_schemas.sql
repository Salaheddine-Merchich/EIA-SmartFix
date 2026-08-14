-- V27: Hitachi SJ200 — real installation manual pages (sections 2-13, 2-20, 2-23)

UPDATE equipment_schemas
SET label = 'Dimensions variateur SJ200',
    schema_type = 'dimension',
    source_page = 13,
    file_path = 'equipment/b0010008-0008-0008-0008-000000000008/hitachi-p2-13-dimension.png',
    caption = 'Cotes mecaniques variateur SJ200 (SJ200-002NFE/NFU)',
    trigger_keywords = ARRAY['sj200', 'hitachi', 'dimension', 'installation', 'e21', 'var-hit', 'convoyage']
WHERE id = 'c0010011-0011-0011-0011-000000000011';

UPDATE equipment_schemas
SET label = 'Bornes et cablage entree SJ200',
    schema_type = 'terminal',
    source_page = 20,
    file_path = 'equipment/b0010008-0008-0008-0008-000000000008/hitachi-p2-20-terminals-input.png',
    caption = 'Dimensions bornes, couples de serrage et cablage entree R/L1 S/L2 T/L3',
    trigger_keywords = ARRAY['sj200', 'hitachi', 'borne', 'terminal', 'entree', 'r/l1', 'couple serrage', 'e21', 'convoyage']
WHERE id = 'c0010012-0012-0012-0012-000000000012';

UPDATE equipment_schemas
SET label = 'Cablage sortie moteur SJ200',
    schema_type = 'wiring',
    source_page = 23,
    file_path = 'equipment/b0010008-0008-0008-0008-000000000008/hitachi-p2-23-motor-output.png',
    caption = 'Raccordement sortie variateur vers moteur U/T1 V/T2 W/T3',
    trigger_keywords = ARRAY['sj200', 'hitachi', 'moteur', 'sortie', 'u/t1', 'v/t2', 'w/t3', 'e21', 'convoyage', 'puissance']
WHERE id = 'c0010019-0019-0019-0019-000000000019';
