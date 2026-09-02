-- Mục tiêu calo/macro/ngày, 1-1 với user (unique user_id — upsert khi ghi lại).
-- Không lưu goal_type — chỉ lưu con số cuối cùng người dùng đã xác nhận (kể cả khi bắt đầu từ
-- preset), tránh phải tính lại nếu sau này đổi hằng số preset ở tầng frontend.
CREATE TABLE calorie_goals (
  id BIGSERIAL PRIMARY KEY,
  user_id BIGINT NOT NULL UNIQUE REFERENCES users(id),
  calorie_target INTEGER NOT NULL,
  protein_target INTEGER NOT NULL,
  carb_target INTEGER NOT NULL,
  fat_target INTEGER NOT NULL,
  updated_at TIMESTAMPTZ DEFAULT now()
);
