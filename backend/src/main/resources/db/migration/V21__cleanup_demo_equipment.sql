-- V21: Remove demo equipment and operational data before PDF manufacturer seed (V22-V24)
-- Preserves users (V4) and knowledge_documents (V13/V17)

DELETE FROM intervention_embeddings;
DELETE FROM knowledge_document_embeddings;
DELETE FROM interventions;
DELETE FROM failures;

-- Remove all legacy/demo equipment (V4/V7 seeds + manual enrichment scripts)
DELETE FROM equipment;

-- Expected after this migration (before V22):
-- SELECT COUNT(*) FROM equipment;              -- 0
-- SELECT COUNT(*) FROM failures;               -- 0
-- SELECT COUNT(*) FROM interventions;        -- 0
-- SELECT COUNT(*) FROM intervention_embeddings; -- 0
