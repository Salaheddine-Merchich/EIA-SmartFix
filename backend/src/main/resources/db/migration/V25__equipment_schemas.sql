-- V25: Technical equipment schemas (wiring, terminals, dimensions) linked to V22 equipment

CREATE TABLE equipment_schemas (
    id UUID PRIMARY KEY,
    equipment_id UUID NOT NULL REFERENCES equipment(id) ON DELETE CASCADE,
    label VARCHAR(200) NOT NULL,
    schema_type VARCHAR(50) NOT NULL,
    source_pdf VARCHAR(300),
    source_page INT,
    file_path VARCHAR(500) NOT NULL,
    mime_type VARCHAR(100) NOT NULL DEFAULT 'image/png',
    caption TEXT,
    trigger_keywords TEXT[] NOT NULL DEFAULT '{}',
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_equipment_schemas_equipment ON equipment_schemas (equipment_id);
CREATE INDEX idx_equipment_schemas_active ON equipment_schemas (active) WHERE active = TRUE;
CREATE INDEX idx_equipment_schemas_keywords ON equipment_schemas USING GIN (trigger_keywords);

-- Seed metadata; PNG files copied from classpath on startup (EquipmentSchemaSeedConfiguration)
INSERT INTO equipment_schemas (id, equipment_id, label, schema_type, source_pdf, source_page, file_path, caption, trigger_keywords)
VALUES
    ('c0010001-0001-0001-0001-000000000001', 'b0020001-0001-0001-0001-000000000001', 'Dimensions variateur SI23', 'dimension', 'Manuel-VEICHI-4-KW-SI23-D5-004G.pdf', 4, 'equipment/b0020001-0001-0001-0001-000000000001/veichi-p04-dimension.png', 'Dimensions mecaniques variateur VEICHI SI23', ARRAY['si23', 'dimension', 'veichi']),
    ('c0010002-0002-0002-0002-000000000002', 'b0020001-0001-0001-0001-000000000001', 'Cablage puissance SI23', 'wiring', 'Manuel-VEICHI-4-KW-SI23-D5-004G.pdf', 6, 'equipment/b0020001-0001-0001-0001-000000000001/veichi-p06-wiring-power.png', 'Cablage puissance variateur PV', ARRAY['si23', 'puissance', 'rs', 'st', 'uv', 'w']),
    ('c0010003-0003-0003-0003-000000000003', 'b0020001-0001-0001-0001-000000000001', 'Cablage commande X1 TA-TC', 'wiring', 'Manuel-VEICHI-4-KW-SI23-D5-004G.pdf', 7, 'equipment/b0020001-0001-0001-0001-000000000001/veichi-p07-wiring-control.png', 'Bornier commande X1, entrees TA-TC, veille et sommeil', ARRAY['x1', 'ta-tc', 'veille', 'sommeil', 'marche a sec', 'f14', 'si23']),
    ('c0010004-0004-0004-0004-000000000004', 'b0020006-0006-0006-0006-000000000006', 'Sonde manque d''eau TA-TC', 'wiring', 'Manuel-VEICHI-4-KW-SI23-D5-004G.pdf', 7, 'equipment/b0020006-0006-0006-0006-000000000006/veichi-p07-sonde-eau.png', 'Raccordement sonde et relais manque d''eau sur X1', ARRAY['sonde', 'eau', 'ta-tc', 'x1', 'marche a sec']),
    ('c0010005-0005-0005-0005-000000000005', 'b0020002-0002-0002-0002-000000000002', 'Installation Goodrive 100-PV', 'install', 'Manuel-GD-100-FR-1.pdf', 12, 'equipment/b0020002-0002-0002-0002-000000000002/goodrive-p12-install.png', 'Installation variateur Goodrive 100-PV', ARRAY['goodrive', 'pv', 'installation']),
    ('c0010006-0006-0006-0006-000000000006', 'b0020002-0002-0002-0002-000000000002', 'Schema systeme PV complet', 'wiring', 'Manuel-GD-100-FR-1.pdf', 13, 'equipment/b0020002-0002-0002-0002-000000000002/goodrive-p13-system-pv.png', 'Cablage systeme pompage solaire PV', ARRAY['pv', 'pompe', 'cablage', 'out1', 'alarme plein', 'goodrive']),
    ('c0010007-0007-0007-0007-000000000007', 'b0020004-0004-0004-0004-000000000004', 'Chaine pompe PV Goodrive', 'wiring', 'Manuel-GD-100-FR-1.pdf', 13, 'equipment/b0020004-0004-0004-0004-000000000004/goodrive-p13-pompe-pv.png', 'Raccordement pompe solaire sur variateur PV', ARRAY['pompe', 'pv', 'demarrage', 'ne demarre plus']),
    ('c0010008-0008-0008-0008-000000000008', 'b0020002-0002-0002-0002-000000000002', 'Bornes R/S/T et PV', 'terminal', 'Manuel-GD-100-FR-1.pdf', 14, 'equipment/b0020002-0002-0002-0002-000000000002/goodrive-p14-terminals.png', 'Bornes alimentation et entree PV', ARRAY['borne', 'rs', 't', 'pv', 'terminal']),
    ('c0010009-0009-0009-0009-000000000009', 'b0020003-0003-0003-0003-000000000003', 'Sorties U/V/W moteur PV', 'terminal', 'Manuel-GD-100-FR-1.pdf', 15, 'equipment/b0020003-0003-0003-0003-000000000003/goodrive-p15-motor-terminals.png', 'Bornes sortie moteur U V W', ARRAY['moteur', 'uv', 'w', 'pompe pv']),
    ('c0010010-0010-0010-0010-000000000010', 'b0020005-0005-0005-0005-000000000005', 'Raccordement panneaux CAP-PV', 'wiring', 'Manuel-GD-100-FR-1.pdf', 13, 'equipment/b0020005-0005-0005-0005-000000000005/goodrive-p13-cap-pv.png', 'Champ panneaux solaires vers variateur', ARRAY['panneau', 'cap-pv', 'pv', 'dc']),
    ('c0010011-0011-0011-0011-000000000011', 'b0010008-0008-0008-0008-000000000008', 'Cablage entrees Hitachi SJ200', 'wiring', 'manuel vv hitachi.pdf', 53, 'equipment/b0010008-0008-0008-0008-000000000008/hitachi-p53-wiring.png', 'Schema cablage entrees variateur SJ200', ARRAY['e21', 'hitachi', 'sj200', 'fw', 'rv']),
    ('c0010012-0012-0012-0012-000000000012', 'b0010008-0008-0008-0008-000000000008', 'Bornier controle SJ200', 'terminal', 'manuel vv hitachi.pdf', 60, 'equipment/b0010008-0008-0008-0008-000000000008/hitachi-p60-terminals.png', 'Bornier de commande convoyage', ARRAY['e21', 'borne', 'rs', 'convoyage']),
    ('c0010013-0013-0013-0013-000000000013', 'b0010007-0007-0007-0007-000000000007', 'Unite ZCU-12 ACS880-11', 'block', 'FR_ACS880-11_HW_H-1.pdf', 128, 'equipment/b0010007-0007-0007-0007-000000000007/acs880-p128-zcu12.png', 'Bloc controle ZCU-12 variateur ACS880-11', ARRAY['zcu', 'acs880', 'abb']),
    ('c0010014-0014-0014-0014-000000000014', 'b0010007-0007-0007-0007-000000000007', 'Cablage securite STO', 'wiring', 'FR_ACS880-11_HW_H-1.pdf', 218, 'equipment/b0010007-0007-0007-0007-000000000007/acs880-p218-sto.png', 'Raccordement entrees securite STO', ARRAY['sto', 'securite', 'acs880']),
    ('c0010015-0015-0015-0015-000000000015', 'b0010001-0001-0001-0001-000000000001', 'Controle filature ACS880', 'wiring', 'Data ocp.pdf', 30, 'equipment/b0010001-0001-0001-0001-000000000001/acs880-spin-p30-control.png', 'Schema controle filature variateur ACS880', ARRAY['filature', 'acs880', 'spinning']),
    ('c0010016-0016-0016-0016-000000000016', 'b0010005-0005-0005-0005-000000000005', 'Encodeur FEN filature', 'wiring', 'Data ocp.pdf', 30, 'equipment/b0010005-0005-0005-0005-000000000005/acs880-spin-p30-encoder.png', 'Raccordement module encodeur FEN', ARRAY['encodeur', 'fen', 'filature']),
    ('c0010017-0017-0017-0017-000000000017', 'b0010006-0006-0006-0006-000000000006', 'Frein mecanique filature', 'wiring', 'Data ocp.pdf', 108, 'equipment/b0010006-0006-0006-0006-000000000006/acs880-spin-p108-brake.png', 'Cablage frein mecanique filature', ARRAY['frein', 'filature', 'frein-mec']),
    ('c0010018-0018-0018-0018-000000000018', 'b0020001-0001-0001-0001-000000000001', 'Variateur SI23 vue bornes', 'terminal', 'Manuel-VEICHI-4-KW-SI23-D5-004G.pdf', 5, 'equipment/b0020001-0001-0001-0001-000000000001/veichi-p05-terminals.png', 'Vue bornes variateur SI23', ARRAY['si23', 'borne', 'terminal', 'veichi']),
    ('c0010019-0019-0019-0019-000000000019', 'b0010008-0008-0008-0008-000000000008', 'Sorties puissance SJ200', 'wiring', 'manuel vv hitachi.pdf', 63, 'equipment/b0010008-0008-0008-0008-000000000008/hitachi-p63-power.png', 'Cablage sorties puissance SJ200', ARRAY['sj200', 'puissance', 'convoyage', 'e21']),
    ('c0010020-0020-0020-0020-000000000020', 'b0020002-0002-0002-0002-000000000002', 'Commande numerique Goodrive', 'terminal', 'Manuel-GD-100-FR-1.pdf', 16, 'equipment/b0020002-0002-0002-0002-000000000002/goodrive-p16-digital-io.png', 'Entrees/sorties numeriques Goodrive PV', ARRAY['out1', 'di', 'do', 'commande', 'pv'])
ON CONFLICT (id) DO NOTHING;
