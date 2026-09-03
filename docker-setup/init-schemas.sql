-- Chạy tự động bởi container postgres khi khởi tạo lần đầu (docker-entrypoint-initdb.d).
-- CHỈ tạo schema + extension — đây là hạ tầng dùng chung, không thuộc quyền sở hữu của 1 service nào.
-- Bảng/index của từng schema do CHÍNH service đó tự quản lý qua Flyway (db/migration/V1__init.sql
-- trong module tương ứng — user/inventory/matching), chạy lúc service khởi động lần đầu.
-- Nguồn spec: CLAUDE.md mục 3.1 — PostgreSQL gộp 4 schema, 1 instance

CREATE SCHEMA IF NOT EXISTS user_service;
CREATE SCHEMA IF NOT EXISTS inventory_service;
CREATE SCHEMA IF NOT EXISTS matching_service;
CREATE SCHEMA IF NOT EXISTS notification_service;
CREATE EXTENSION IF NOT EXISTS vector;   -- dùng trong matching_service
