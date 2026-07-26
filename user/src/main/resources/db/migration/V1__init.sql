-- Baseline: khớp đúng trạng thái schema user_service đã tồn tại (tạo trước đây qua init-schemas.sql).
-- Không tự chạy trên DB đã có sẵn dữ liệu — xem application.yml (spring.flyway.baseline-*).
CREATE TABLE users (
  id BIGSERIAL PRIMARY KEY,
  email VARCHAR(255) UNIQUE NOT NULL,
  password_hash VARCHAR(255) NOT NULL,
  full_name VARCHAR(100),
  avatar_url TEXT,
  plan VARCHAR(20) DEFAULT 'FREE',   -- FREE | PRO (đồng bộ với payment_service.subscriptions)
  created_at TIMESTAMPTZ DEFAULT now()
);

CREATE TABLE dietary_preferences (
  id BIGSERIAL PRIMARY KEY,
  user_id BIGINT NOT NULL REFERENCES users(id),
  type VARCHAR(20) NOT NULL,   -- ALLERGY | DIET
  value VARCHAR(50) NOT NULL   -- 'hải sản', 'chay', ...
);

CREATE INDEX idx_dietary_preferences_user ON dietary_preferences (user_id);
