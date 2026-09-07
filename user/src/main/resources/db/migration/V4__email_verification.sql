ALTER TABLE user_service.users ADD COLUMN email_verified BOOLEAN NOT NULL DEFAULT false;
-- Backfill user đã tồn tại TRƯỚC migration này thành đã xác thực — họ đã chứng minh email dùng
-- được (đăng ký/đăng nhập thành công nhiều lần từ trước), không có gì để buộc xác thực lại. Tránh
-- khoá cứng toàn bộ tài khoản cũ ngay khi deploy tính năng này, cùng tinh thần "dọn dữ liệu cũ
-- trước khi thêm ràng buộc mới" đã áp dụng ở V3__dietary_preferences_unique.sql.
UPDATE user_service.users SET email_verified = true;
