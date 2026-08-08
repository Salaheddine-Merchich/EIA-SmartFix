-- V8: Trace du déclarant (auteur) distinct du responsable EIA assigné

ALTER TABLE failures ADD COLUMN declarant_id UUID REFERENCES users (id) ON DELETE SET NULL;

UPDATE failures
SET declarant_id = responsable_id
WHERE declarant_id IS NULL AND responsable_id IS NOT NULL;

UPDATE failures
SET declarant_id = '11111111-1111-1111-1111-111111111111'
WHERE declarant_id IS NULL;

CREATE INDEX idx_failures_declarant_id ON failures (declarant_id);
