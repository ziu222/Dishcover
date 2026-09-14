-- Khoá tài khoản cho khu quản trị. Mặc định false cho toàn bộ user hiện có — thêm cột không được
-- khoá nhầm ai, cùng tinh thần V4 backfill email_verified=true để không khoá đột ngột user cũ.
ALTER TABLE user_service.users ADD COLUMN locked BOOLEAN NOT NULL DEFAULT false;

-- Danh sách admin sắp theo ngày tạo và tìm theo email — index cho đúng 2 truy vấn đó.
CREATE INDEX idx_users_created_at ON user_service.users (created_at DESC);
