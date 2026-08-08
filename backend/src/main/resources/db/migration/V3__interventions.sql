-- V3: Interventions and Documents
CREATE TABLE interventions (
    id                          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    failure_id                  UUID         NOT NULL REFERENCES failures (id) ON DELETE CASCADE,
    technicien_id               UUID         NOT NULL REFERENCES users (id) ON DELETE RESTRICT,
    description                 TEXT,
    symptomes                   TEXT,
    cause_racine                TEXT,
    analyse_technique           TEXT,
    actions_correctives         TEXT,
    pieces_remplacees           TEXT,
    duree_arret_minutes         INTEGER,
    temps_intervention_minutes  INTEGER,
    statut_validation           VARCHAR(50)  NOT NULL DEFAULT 'BROUILLON',
    validateur_id               UUID         REFERENCES users (id) ON DELETE SET NULL,
    date_validation             TIMESTAMP,
    commentaire_validation      TEXT,
    created_at                  TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at                  TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE TABLE intervention_documents (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    intervention_id UUID         NOT NULL REFERENCES interventions (id) ON DELETE CASCADE,
    nom_fichier     VARCHAR(255) NOT NULL,
    chemin_stockage VARCHAR(500) NOT NULL,
    type_mime       VARCHAR(100),
    taille_octets   BIGINT,
    created_at      TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_interventions_failure_id ON interventions (failure_id);
CREATE INDEX idx_interventions_statut ON interventions (statut_validation);
CREATE INDEX idx_interventions_technicien ON interventions (technicien_id);
CREATE INDEX idx_intervention_documents_intervention ON intervention_documents (intervention_id);

-- Full-text search index
CREATE INDEX idx_interventions_fts ON interventions
    USING GIN (to_tsvector('french',
        coalesce(symptomes, '') || ' ' ||
        coalesce(cause_racine, '') || ' ' ||
        coalesce(actions_correctives, '') || ' ' ||
        coalesce(analyse_technique, '') || ' ' ||
        coalesce(description, '')
    ));
