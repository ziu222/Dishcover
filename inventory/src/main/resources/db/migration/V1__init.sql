-- Baseline: khớp đúng trạng thái schema inventory_service đã tồn tại (tạo trước đây qua init-schemas.sql).
CREATE TABLE user_ingredients (
  id BIGSERIAL PRIMARY KEY,
  user_id BIGINT NOT NULL,
  ingredient_name VARCHAR(100) NOT NULL,
  normalized_name VARCHAR(100) NOT NULL,   -- khóa so khớp thật với Matching/RAG
  quantity DECIMAL(10,2),
  unit VARCHAR(20),
  expiry_date DATE,
  source VARCHAR(20) DEFAULT 'MANUAL',     -- MANUAL | IMAGE_RECOGNITION
  status VARCHAR(20) DEFAULT 'FRESH',      -- FRESH | EXPIRING_SOON | EXPIRED | USED
  created_at TIMESTAMPTZ DEFAULT now(),
  updated_at TIMESTAMPTZ
);

CREATE INDEX idx_user_status ON user_ingredients (user_id, status);
CREATE INDEX idx_user_expiry ON user_ingredients (user_id, expiry_date);
