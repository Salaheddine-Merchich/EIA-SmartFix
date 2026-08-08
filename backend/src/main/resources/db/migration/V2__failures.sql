-- V2: Failures
CREATE TABLE failures (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    equipment_id        UUID         NOT NULL REFERENCES equipment (id) ON DELETE RESTRICT,
    date_heure          TIMESTAMP    NOT NULL,
    criticite           VARCHAR(50)  NOT NULL,
    zone_service        VARCHAR(150),
    responsable_id      UUID         REFERENCES users (id) ON DELETE SET NULL,
    statut              VARCHAR(50)  NOT NULL DEFAULT 'OUVERTE',
    description_initiale TEXT,
    code_defaut         VARCHAR(100),
    created_at          TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_failures_equipment_id ON failures (equipment_id);
CREATE INDEX idx_failures_statut ON failures (statut);
CREATE INDEX idx_failures_date_heure ON failures (date_heure DESC);
CREATE INDEX idx_failures_code_defaut ON failures (code_defaut);
