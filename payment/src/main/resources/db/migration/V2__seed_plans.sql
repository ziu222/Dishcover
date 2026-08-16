-- Bảng giá 2 gói PRO (CLAUDE.md mục 8). Seed bằng migration chứ không bằng code khởi động:
-- đây là dữ liệu cấu hình có khóa ngoại từ payment_transactions/subscriptions trỏ tới, phải tồn
-- tại trước khi service nhận đơn đầu tiên.
-- ON CONFLICT DO NOTHING để chạy lại trên DB đã có dữ liệu cũng không lỗi.
INSERT INTO plans (code, price_vnd, duration_days, active) VALUES
  ('PRO_MONTHLY',  49000,  30, TRUE),
  ('PRO_YEARLY',  399000, 365, TRUE)
ON CONFLICT (code) DO NOTHING;
