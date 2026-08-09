-- V3 expression GIN superseded by V16 search_vector + idx_interventions_search_vector_gin.
-- App queries use i.search_vector only; this index is unused write overhead.
DROP INDEX IF EXISTS idx_interventions_fts;
