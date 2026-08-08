-- V14: Remove all failures and interventions while preserving demo equipment and users
-- This allows OCP teams to add their own real failures while keeping the infrastructure

-- First, remove all intervention embeddings (they reference interventions)
DELETE FROM intervention_embeddings;

-- Second, remove all interventions (they reference failures)
DELETE FROM interventions;

-- Finally, remove all failures to start fresh
DELETE FROM failures;

-- Verification queries (commented, can be run manually to check cleanup):
-- SELECT COUNT(*) FROM failures;        -- Should return 0
-- SELECT COUNT(*) FROM interventions;   -- Should return 0  
-- SELECT COUNT(*) FROM intervention_embeddings; -- Should return 0
-- SELECT COUNT(*) FROM equipment;       -- Should return 4 (MOT-001, VAR-012, CAP-045, POM-008)
-- SELECT COUNT(*) FROM users;           -- Should return 3 (Admin, Responsable, Technicien)
-- SELECT COUNT(*) FROM knowledge_documents; -- Should return 4 (technical documentation)