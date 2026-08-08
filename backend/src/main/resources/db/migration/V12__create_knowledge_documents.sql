-- V12: Create knowledge_documents table for external technical documentation
-- This allows enriching RAG knowledge without adding fictional failures to production data

CREATE TABLE knowledge_documents (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    title VARCHAR(500) NOT NULL,
    content TEXT NOT NULL,
    document_type VARCHAR(100) NOT NULL CHECK (document_type IN ('manual', 'procedure', 'guide', 'faq', 'standard', 'troubleshooting')),
    equipment_family VARCHAR(100), -- NULL for general documents, specific family otherwise
    source VARCHAR(200) NOT NULL, -- e.g., 'Manuel Siemens S7', 'Guide OCP', 'Norme ISO 14224'
    language VARCHAR(10) DEFAULT 'fr',
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW(),
    active BOOLEAN DEFAULT TRUE,
    
    -- Add indexes for efficient RAG queries
    CONSTRAINT knowledge_documents_title_source_unique UNIQUE (title, source)
);

-- Create indexes for efficient search
CREATE INDEX idx_knowledge_documents_equipment_family ON knowledge_documents(equipment_family);
CREATE INDEX idx_knowledge_documents_document_type ON knowledge_documents(document_type);
CREATE INDEX idx_knowledge_documents_active ON knowledge_documents(active);
CREATE INDEX idx_knowledge_documents_created_at ON knowledge_documents(created_at);

-- Create full-text search index for content
CREATE INDEX idx_knowledge_documents_content_fts ON knowledge_documents USING gin(to_tsvector('french', content));
CREATE INDEX idx_knowledge_documents_title_fts ON knowledge_documents USING gin(to_tsvector('french', title));

-- Table for storing embeddings of knowledge documents (similar to intervention_embeddings)
CREATE TABLE knowledge_document_embeddings (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    document_id UUID NOT NULL REFERENCES knowledge_documents(id) ON DELETE CASCADE,
    content_chunk TEXT NOT NULL, -- Chunked content for better embeddings
    embedding vector(768), -- Same dimension as intervention embeddings (nomic-embed-text)
    chunk_index INTEGER NOT NULL, -- Order of chunks within document
    created_at TIMESTAMP DEFAULT NOW(),
    
    CONSTRAINT knowledge_document_embeddings_unique UNIQUE (document_id, chunk_index)
);

-- Index for vector similarity search
CREATE INDEX idx_knowledge_document_embeddings_vector ON knowledge_document_embeddings 
USING ivfflat (embedding vector_cosine_ops) WITH (lists = 100);

-- Index for efficient chunk retrieval
CREATE INDEX idx_knowledge_document_embeddings_document_id ON knowledge_document_embeddings(document_id);