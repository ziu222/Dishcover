-- Bug thật phát hiện lúc live-verify: (user_id, type, value) không có ràng buộc duy nhất nào,
-- addPreference() insert thẳng không check tồn tại -> user bấm 2 lần tạo 2 dòng trùng hệt nhau.
-- Dọn dữ liệu trùng hiện có trước (giữ lại id nhỏ nhất mỗi nhóm) rồi mới thêm UNIQUE, tránh
-- migration fail nếu production đã có bản ghi trùng từ trước khi sửa.
DELETE FROM dietary_preferences a
USING dietary_preferences b
WHERE a.id > b.id
  AND a.user_id = b.user_id
  AND a.type = b.type
  AND a.value = b.value;

CREATE UNIQUE INDEX ux_dietary_preferences_user_type_value
  ON dietary_preferences (user_id, type, value);
