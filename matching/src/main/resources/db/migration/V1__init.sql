-- Baseline: khớp đúng trạng thái schema matching_service đã tồn tại (tạo trước đây qua init-schemas.sql).
-- embedding cho RAG — xem CLAUDE.md mục 6
CREATE TABLE recipe_embeddings (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  recipe_id VARCHAR(64) NOT NULL,      -- _id bên MongoDB, soft-reference khác hệ DB
  content TEXT NOT NULL,
  metadata JSONB,
  embedding vector(768)
);

CREATE INDEX ON recipe_embeddings USING hnsw (embedding vector_cosine_ops);
CREATE INDEX idx_recipe_embeddings_recipe_id ON recipe_embeddings (recipe_id);
