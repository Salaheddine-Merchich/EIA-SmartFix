-- Phase 3: indexes for list search (ILIKE/trigram) and history filters.
-- interventions.search_vector already exists from V16; this migration does not recreate it.

CREATE EXTENSION IF NOT EXISTS pg_trgm;

CREATE INDEX IF NOT EXISTS idx_failures_code_defaut_trgm
    ON failures USING gin (code_defaut gin_trgm_ops);

CREATE INDEX IF NOT EXISTS idx_failures_description_trgm
    ON failures USING gin (description_initiale gin_trgm_ops);

CREATE INDEX IF NOT EXISTS idx_equipment_code_trgm
    ON equipment USING gin (code gin_trgm_ops);

CREATE INDEX IF NOT EXISTS idx_equipment_designation_trgm
    ON equipment USING gin (designation gin_trgm_ops);

CREATE INDEX IF NOT EXISTS idx_equipment_constructeur_trgm
    ON equipment USING gin (constructeur gin_trgm_ops);

CREATE INDEX IF NOT EXISTS idx_failures_equipment_date
    ON failures (equipment_id, date_heure DESC);

CREATE INDEX IF NOT EXISTS idx_interventions_failure_created
    ON interventions (failure_id, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_interventions_statut_created
    ON interventions (statut_validation, created_at DESC)
    WHERE statut_validation = 'VALIDEE';
