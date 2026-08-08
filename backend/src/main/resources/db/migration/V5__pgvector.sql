-- V5: pgvector for RAG (Phase 2)
CREATE EXTENSION IF NOT EXISTS vector;

CREATE TABLE intervention_embeddings (
    intervention_id UUID PRIMARY KEY REFERENCES interventions (id) ON DELETE CASCADE,
    embedding       vector(768) NOT NULL,
    contenu_indexe  TEXT        NOT NULL,
    indexe_le       TIMESTAMP   NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_intervention_embeddings_hnsw
    ON intervention_embeddings
    USING hnsw (embedding vector_cosine_ops);
