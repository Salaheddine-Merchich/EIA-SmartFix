-- V11: Remove fictional equipment, failures and interventions added by V10
-- This restores the database to contain only real OCP equipment and failures

-- First, remove fictional interventions (they reference failures, so must be removed first)
DELETE FROM interventions WHERE id IN (
    'a8888888-8888-8888-8888-888888888888',  -- Réparation vérin hydraulique
    'a9999999-9999-9999-9999-999999999999',  -- Rétablissement Profinet
    'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaab',  -- Retension courroie
    'abbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb'   -- Remplacement contacteur
);

-- Remove corresponding intervention embeddings if they exist
DELETE FROM intervention_embeddings WHERE intervention_id IN (
    'a8888888-8888-8888-8888-888888888888',
    'a9999999-9999-9999-9999-999999999999',
    'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaab',
    'abbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb'
);

-- Second, remove fictional failures (they reference equipment, so must be removed before equipment)
DELETE FROM failures WHERE id IN (
    'f7777777-7777-7777-7777-777777777777',  -- Vérin hydraulique fuite
    'f8888888-8888-8888-8888-888888888888',  -- Communication Profinet perdue
    'f9999999-9999-9999-9999-999999999999',  -- Courroie convoyeur glisse
    'fabbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb'   -- Contacteur moteur ne ferme pas
);

-- Finally, remove fictional equipment added by V10
DELETE FROM equipment WHERE id IN (
    'e1010101-1010-1010-1010-101010101010',  -- HYDR-015 Vérin hydraulique
    'e2020202-2020-2020-2020-202020202020',  -- PLC-021 Automate Siemens S7-1500
    'e3030303-3030-3030-3030-303030303030',  -- CVY-003 Convoyeur principal
    'e4040404-4040-4040-4040-404040404040'   -- ELC-007 Contacteur moteur 75kW
);

-- Verify the cleanup - this should now show only legitimate equipment/failures from V4, V7, V9
-- Expected remaining equipment: MOT-001, VAR-012, CAP-045, POM-008 (4 total)
-- Expected remaining failures: should be only those created by real OCP users or legitimate demo data