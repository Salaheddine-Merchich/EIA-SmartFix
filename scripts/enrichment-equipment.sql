-- Script d'enrichissement équipements pour le RAG EIA SmartFix
-- Ajoute 10 nouveaux équipements couvrant toutes les familles industrielles

-- Instrumentation (3 équipements)
INSERT INTO equipment (id, code, designation, famille, zone, constructeur, mise_en_service)
VALUES
    ('10101010-1010-1010-1010-101010101010', 'PTI-056', 'Capteur pression hydraulique circuit principal', 'Instrumentation', 'Zone A', 'Honeywell', '2019-05-15'),
    ('20202020-2020-2020-2020-202020202020', 'FIT-078', 'Débitmètre électromagnétique eau de refroidissement', 'Instrumentation', 'Zone B', 'Endress+Hauser', '2020-08-22'),
    ('30303030-3030-3030-3030-303030303030', 'AIT-134', 'Analyseur pH circuit traitement', 'Instrumentation', 'Zone C', 'Yokogawa', '2021-03-10');

-- Automatisme (3 équipements) 
INSERT INTO equipment (id, code, designation, famille, zone, constructeur, mise_en_service)
VALUES
    ('40404040-4040-4040-4040-404040404040', 'PLC-067', 'Automate Allen-Bradley ControlLogix ligne broyage', 'Automatisme', 'Zone A', 'Allen-Bradley', '2018-11-20'),
    ('50505050-5050-5050-5050-505050505050', 'DI-089', 'Module entrées/sorties Modbus TCP', 'Automatisme', 'Zone B', 'Schneider Electric', '2020-02-28'),
    ('60606060-6060-6060-6060-606060606060', 'UPS-012', 'Onduleur ligne critique 50kVA', 'Automatisme', 'Zone C', 'APC', '2019-12-05');

-- Mécanique (2 équipements)
INSERT INTO equipment (id, code, designation, famille, zone, constructeur, mise_en_service)
VALUES
    ('70707070-7070-7070-7070-707070707070', 'RED-045', 'Réducteur moteur convoyeur minerai', 'Mécanique', 'Zone D', 'SEW Eurodrive', '2018-04-18'),
    ('80808080-8080-8080-8080-808080808080', 'VEN-123', 'Ventilateur extraction atelier broyage', 'Mécanique', 'Zone A', 'Ziehl-Abegg', '2021-01-12');

-- Électricité (2 équipements)
INSERT INTO equipment (id, code, designation, famille, zone, constructeur, mise_en_service)  
VALUES
    ('90909090-9090-9090-9090-909090909090', 'CTR-234', 'Contacteur moteur pompe haute pression', 'Électricité', 'Zone B', 'Schneider Electric', '2020-06-15'),
    ('a0a0a0a0-a0a0-a0a0-a0a0-a0a0a0a0a0a0', 'TRF-567', 'Transformateur alimentation 400/230V', 'Électricité', 'Zone C', 'Legrand', '2019-09-30');