-- Purge FullRag enrichment data (10 equipment, 18 failures, 18 interventions).
-- Preserves seed API failures (March) and Flyway demo equipment (POM-008, etc.).
-- intervention_embeddings cascade on intervention delete.

DELETE FROM intervention_documents
WHERE intervention_id IN (
    SELECT i.id FROM interventions i
    JOIN failures f ON f.id = i.failure_id
    JOIN equipment e ON e.id = f.equipment_id
    WHERE e.code IN (
      'PTI-056', 'FIT-078', 'AIT-134', 'PLC-067', 'DI-089', 'UPS-012',
      'RED-045', 'VEN-123', 'CTR-234', 'TRF-567'
    )
);

DELETE FROM interventions
WHERE failure_id IN (
    SELECT f.id FROM failures f
    JOIN equipment e ON e.id = f.equipment_id
    WHERE e.code IN (
      'PTI-056', 'FIT-078', 'AIT-134', 'PLC-067', 'DI-089', 'UPS-012',
      'RED-045', 'VEN-123', 'CTR-234', 'TRF-567'
    )
);

DELETE FROM failures
WHERE equipment_id IN (
    SELECT id FROM equipment
    WHERE code IN (
      'PTI-056', 'FIT-078', 'AIT-134', 'PLC-067', 'DI-089', 'UPS-012',
      'RED-045', 'VEN-123', 'CTR-234', 'TRF-567'
    )
);

DELETE FROM equipment
WHERE code IN (
  'PTI-056', 'FIT-078', 'AIT-134', 'PLC-067', 'DI-089', 'UPS-012',
  'RED-045', 'VEN-123', 'CTR-234', 'TRF-567'
);
