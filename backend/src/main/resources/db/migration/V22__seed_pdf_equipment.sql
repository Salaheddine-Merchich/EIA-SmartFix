-- V22: Equipment from manufacturer PDF manuals (ABB, Hitachi, VEICHI, INVT Goodrive)

INSERT INTO equipment (id, code, designation, famille, zone, constructeur, mise_en_service)
VALUES
    ('b0010001-0001-0001-0001-000000000001', 'VAR-ACS-SPIN', 'Variateur ACS880 filature +N5500', 'Variateur', 'Zone Filature', 'ABB', '2019-04-15'),
    ('b0010002-0002-0002-0002-000000000002', 'VAR-ACS-TRV', 'Variateur ACS880 traverse', 'Variateur', 'Zone Filature', 'ABB', '2019-04-15'),
    ('b0010003-0003-0003-0003-000000000003', 'MOT-FIL', 'Moteur filature', 'Moteur', 'Zone Filature', 'ABB', '2019-04-15'),
    ('b0010004-0004-0004-0004-000000000004', 'MOT-TRV', 'Moteur traverse', 'Moteur', 'Zone Filature', 'ABB', '2019-04-15'),
    ('b0010005-0005-0005-0005-000000000005', 'ENC-FEN', 'Module encodeur FEN-xx', 'Instrumentation', 'Zone Filature', 'ABB', '2019-04-15'),
    ('b0010006-0006-0006-0006-000000000006', 'FREIN-MEC', 'Frein mecanique filature/traverse', 'Mecanique', 'Zone Filature', NULL, '2019-04-15'),
    ('b0010007-0007-0007-0007-000000000007', 'VAR-ABB-11', 'Variateur ACS880-11', 'Variateur', 'Zone Process', 'ABB', '2020-06-01'),
    ('b0010008-0008-0008-0008-000000000008', 'VAR-HIT-SJ200', 'Variateur de frequence Hitachi SJ200', 'Variateur', 'Zone Convoyage', 'Hitachi', '2018-09-10'),
    ('b0020001-0001-0001-0001-000000000001', 'VAR-VEI-SI23', 'Variateur VEICHI SI23-D5-004G pompage solaire 4 kW', 'Variateur', 'Station PV', 'VEICHI', '2022-03-20'),
    ('b0020002-0002-0002-0002-000000000002', 'VAR-GD-100PV', 'Variateur Goodrive 100-PV pompage solaire', 'Variateur', 'Station PV', 'INVT', '2022-05-12'),
    ('b0020003-0003-0003-0003-000000000003', 'MOT-PV', 'Moteur pompe asynchrone photovoltaique', 'Moteur', 'Station PV', NULL, '2022-03-20'),
    ('b0020004-0004-0004-0004-000000000004', 'POM-PV', 'Pompe solaire', 'Pompe', 'Station PV', NULL, '2022-03-20'),
    ('b0020005-0005-0005-0005-000000000005', 'CAP-PV', 'Champ panneaux solaires', 'Alimentation', 'Station PV', NULL, '2022-03-20'),
    ('b0020006-0006-0006-0006-000000000006', 'SEN-EAU', 'Relais et sonde manque d''eau', 'Capteur', 'Station PV', NULL, '2022-03-20')
ON CONFLICT (code) DO NOTHING;
