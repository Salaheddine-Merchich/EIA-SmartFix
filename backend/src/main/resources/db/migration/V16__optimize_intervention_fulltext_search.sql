-- V16: Optimize full-text search on interventions with pre-computed search vector
-- This replaces dynamic to_tsvector() calls with a pre-computed column and GIN index

-- Add search_vector column to interventions table
ALTER TABLE interventions 
ADD COLUMN search_vector tsvector;

-- Create function to compute search vector from intervention data
CREATE OR REPLACE FUNCTION compute_intervention_search_vector(intervention_row interventions)
RETURNS tsvector AS $$
DECLARE
    equipment_code TEXT := '';
    equipment_designation TEXT := '';
    equipment_constructeur TEXT := '';
    failure_description TEXT := '';
    failure_code_defaut TEXT := '';
BEGIN
    -- Get equipment data
    SELECT e.code, e.designation, e.constructeur 
    INTO equipment_code, equipment_designation, equipment_constructeur
    FROM equipment e 
    JOIN failures f ON f.equipment_id = e.id 
    WHERE f.id = intervention_row.failure_id;
    
    -- Get failure data
    SELECT f.description_initiale, f.code_defaut
    INTO failure_description, failure_code_defaut
    FROM failures f
    WHERE f.id = intervention_row.failure_id;
    
    -- Compute and return the search vector
    RETURN to_tsvector('french',
        coalesce(intervention_row.symptomes, '') || ' ' ||
        coalesce(intervention_row.cause_racine, '') || ' ' ||
        coalesce(intervention_row.actions_correctives, '') || ' ' ||
        coalesce(intervention_row.analyse_technique, '') || ' ' ||
        coalesce(intervention_row.description, '') || ' ' ||
        coalesce(failure_description, '') || ' ' ||
        coalesce(failure_code_defaut, '') || ' ' ||
        coalesce(equipment_code, '') || ' ' ||
        coalesce(equipment_designation, '') || ' ' ||
        coalesce(equipment_constructeur, '')
    );
END;
$$ LANGUAGE plpgsql IMMUTABLE;

-- Populate the search_vector column for existing interventions
UPDATE interventions 
SET search_vector = compute_intervention_search_vector(interventions.*)
WHERE search_vector IS NULL;

-- Create GIN index on the search_vector column for fast full-text search
CREATE INDEX idx_interventions_search_vector_gin ON interventions USING gin(search_vector);

-- Create trigger function to automatically update search_vector when intervention changes
CREATE OR REPLACE FUNCTION update_intervention_search_vector()
RETURNS TRIGGER AS $$
BEGIN
    NEW.search_vector := compute_intervention_search_vector(NEW.*);
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Create trigger to auto-update search_vector on INSERT/UPDATE
DROP TRIGGER IF EXISTS trg_intervention_search_vector ON interventions;
CREATE TRIGGER trg_intervention_search_vector
    BEFORE INSERT OR UPDATE ON interventions
    FOR EACH ROW
    EXECUTE FUNCTION update_intervention_search_vector();

-- Create trigger function to update intervention search vectors when related data changes
CREATE OR REPLACE FUNCTION update_related_intervention_search_vectors()
RETURNS TRIGGER AS $$
BEGIN
    -- Update all interventions related to the modified equipment/failure
    IF TG_TABLE_NAME = 'equipment' THEN
        UPDATE interventions 
        SET search_vector = compute_intervention_search_vector(interventions.*)
        FROM failures f
        WHERE interventions.failure_id = f.id 
        AND f.equipment_id = COALESCE(NEW.id, OLD.id);
    ELSIF TG_TABLE_NAME = 'failures' THEN
        UPDATE interventions 
        SET search_vector = compute_intervention_search_vector(interventions.*)
        WHERE interventions.failure_id = COALESCE(NEW.id, OLD.id);
    END IF;
    
    RETURN COALESCE(NEW, OLD);
END;
$$ LANGUAGE plpgsql;

-- Create triggers to update interventions when equipment or failure data changes
DROP TRIGGER IF EXISTS trg_equipment_update_interventions ON equipment;
CREATE TRIGGER trg_equipment_update_interventions
    AFTER UPDATE OF code, designation, constructeur ON equipment
    FOR EACH ROW
    WHEN (OLD.code IS DISTINCT FROM NEW.code 
          OR OLD.designation IS DISTINCT FROM NEW.designation 
          OR OLD.constructeur IS DISTINCT FROM NEW.constructeur)
    EXECUTE FUNCTION update_related_intervention_search_vectors();

DROP TRIGGER IF EXISTS trg_failure_update_interventions ON failures;
CREATE TRIGGER trg_failure_update_interventions
    AFTER UPDATE OF description_initiale, code_defaut ON failures
    FOR EACH ROW
    WHEN (OLD.description_initiale IS DISTINCT FROM NEW.description_initiale 
          OR OLD.code_defaut IS DISTINCT FROM NEW.code_defaut)
    EXECUTE FUNCTION update_related_intervention_search_vectors();

-- Add constraint to ensure search_vector is never null for validated interventions
ALTER TABLE interventions 
ADD CONSTRAINT chk_search_vector_not_null 
CHECK (statut_validation != 'VALIDEE' OR search_vector IS NOT NULL);

-- Performance verification queries (commented, for manual testing):
-- EXPLAIN ANALYZE SELECT COUNT(*) FROM interventions WHERE search_vector @@ plainto_tsquery('french', 'moteur');
-- EXPLAIN ANALYZE SELECT i.* FROM interventions i WHERE i.search_vector @@ plainto_tsquery('french', 'hydraulique') AND i.statut_validation = 'VALIDEE';