-- Chạy tự động bởi container postgres khi khởi tạo lần đầu (docker-entrypoint-initdb.d)
-- Nguồn spec: CLAUDE.md mục 3.1 — PostgreSQL gộp 4 schema, 1 instance

CREATE SCHEMA IF NOT EXISTS user_service;
CREATE SCHEMA IF NOT EXISTS inventory_service;
CREATE SCHEMA IF NOT EXISTS matching_service;
CREATE SCHEMA IF NOT EXISTS payment_service;
CREATE EXTENSION IF NOT EXISTS vector;   -- dùng trong matching_service

-- ==================== user_service ====================
CREATE TABLE user_service.users (
  id BIGSERIAL PRIMARY KEY,
  email VARCHAR(255) UNIQUE NOT NULL,
  password_hash VARCHAR(255) NOT NULL,
  full_name VARCHAR(100),
  avatar_url TEXT,
  plan VARCHAR(20) DEFAULT 'FREE',   -- FREE | PRO (đồng bộ với payment_service.subscriptions)
  created_at TIMESTAMP DEFAULT now()
);
CREATE TABLE user_service.dietary_preferences (
  id BIGSERIAL PRIMARY KEY,
  user_id BIGINT NOT NULL REFERENCES user_service.users(id),
  type VARCHAR(20) NOT NULL,   -- ALLERGY | DIET
  value VARCHAR(50) NOT NULL   -- 'hải sản', 'chay', ...
);

-- ==================== inventory_service ====================
CREATE TABLE inventory_service.user_ingredients (
  id BIGSERIAL PRIMARY KEY,
  user_id BIGINT NOT NULL,
  ingredient_name VARCHAR(100) NOT NULL,
  normalized_name VARCHAR(100) NOT NULL,   -- khóa so khớp thật với Matching/RAG
  quantity DECIMAL(10,2),
  unit VARCHAR(20),
  expiry_date DATE,
  source VARCHAR(20) DEFAULT 'MANUAL',     -- MANUAL | IMAGE_RECOGNITION
  status VARCHAR(20) DEFAULT 'FRESH',      -- FRESH | EXPIRING_SOON | EXPIRED | USED
  created_at TIMESTAMP DEFAULT now(),
  updated_at TIMESTAMP
);
CREATE INDEX idx_user_status ON inventory_service.user_ingredients (user_id, status);
CREATE INDEX idx_user_expiry ON inventory_service.user_ingredients (user_id, expiry_date);

-- ==================== matching_service ====================
-- embedding cho RAG — xem CLAUDE.md mục 6
CREATE TABLE matching_service.recipe_embeddings (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  recipe_id VARCHAR(64) NOT NULL,      -- _id bên MongoDB, soft-reference khác hệ DB
  content TEXT NOT NULL,
  metadata JSONB,
  embedding vector(768)
);
CREATE INDEX ON matching_service.recipe_embeddings USING hnsw (embedding vector_cosine_ops);

-- ==================== payment_service ====================
CREATE TABLE payment_service.plans (
  code VARCHAR(30) PRIMARY KEY,          -- PRO_MONTHLY, PRO_YEARLY
  price_vnd INT NOT NULL,
  duration_days INT NOT NULL,
  active BOOLEAN DEFAULT TRUE
);
CREATE TABLE payment_service.payment_transactions (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id BIGINT NOT NULL,
  plan_code VARCHAR(30) NOT NULL REFERENCES payment_service.plans(code),
  amount_vnd INT NOT NULL,
  provider VARCHAR(20) NOT NULL,          -- MOMO | VNPAY
  status VARCHAR(20) NOT NULL,            -- PENDING | SUCCESS | FAILED | EXPIRED
  provider_trans_id VARCHAR(64),
  created_at TIMESTAMP DEFAULT now(),
  updated_at TIMESTAMP DEFAULT now(),
  UNIQUE (provider, provider_trans_id)    -- chống ghi trùng IPN
);
CREATE TABLE payment_service.subscriptions (
  id BIGSERIAL PRIMARY KEY,
  user_id BIGINT NOT NULL,
  plan_code VARCHAR(30) NOT NULL REFERENCES payment_service.plans(code),
  start_at TIMESTAMP NOT NULL,
  end_at TIMESTAMP NOT NULL,
  status VARCHAR(20) NOT NULL,            -- ACTIVE | EXPIRED | CANCELLED
  source_transaction_id UUID NOT NULL REFERENCES payment_service.payment_transactions(id)
);
CREATE INDEX idx_user_active ON payment_service.subscriptions (user_id, status, end_at);
