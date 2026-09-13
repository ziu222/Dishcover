-- Role thật cho phân quyền admin (docs/specs/admin-recipe-authorization.md). Mặc định USER cho toàn
-- bộ user hiện có; gán ADMIN đầu tiên làm thủ công qua SQL, không tự động hoá.
ALTER TABLE user_service.users ADD COLUMN role VARCHAR(20) NOT NULL DEFAULT 'USER';
