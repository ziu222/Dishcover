-- Baseline: khớp đúng trạng thái schema payment_service đã tồn tại (tạo trước đây qua init-schemas.sql).
CREATE TABLE plans (
  code VARCHAR(30) PRIMARY KEY,          -- PRO_MONTHLY, PRO_YEARLY
  price_vnd INT NOT NULL,
  duration_days INT NOT NULL,
  active BOOLEAN DEFAULT TRUE
);

CREATE TABLE payment_transactions (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id BIGINT NOT NULL,
  plan_code VARCHAR(30) NOT NULL REFERENCES plans(code),
  amount_vnd INT NOT NULL,
  provider VARCHAR(20) NOT NULL,          -- MOMO | VNPAY
  status VARCHAR(20) NOT NULL,            -- PENDING | SUCCESS | FAILED | EXPIRED
  provider_trans_id VARCHAR(64),
  created_at TIMESTAMPTZ DEFAULT now(),
  updated_at TIMESTAMPTZ DEFAULT now(),
  UNIQUE (provider, provider_trans_id)    -- chống ghi trùng IPN
);

CREATE TABLE subscriptions (
  id BIGSERIAL PRIMARY KEY,
  user_id BIGINT NOT NULL,
  plan_code VARCHAR(30) NOT NULL REFERENCES plans(code),
  start_at TIMESTAMPTZ NOT NULL,
  end_at TIMESTAMPTZ NOT NULL,
  status VARCHAR(20) NOT NULL,            -- ACTIVE | EXPIRED | CANCELLED
  source_transaction_id UUID NOT NULL REFERENCES payment_transactions(id)
);

CREATE INDEX idx_user_active ON subscriptions (user_id, status, end_at);
