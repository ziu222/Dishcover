# Docker — hạ tầng dev (Postgres + Mongo)

> Bước hiện tại chỉ dựng 2 database. Service Spring Boot (Gateway, User, Inventory...) sẽ được thêm vào `docker-compose.yml` này khi scaffold monorepo (checklist mục 1 trong `CLAUDE.md`) — lúc đó Gateway mới gắn vào `public-net`, các service khác gắn `private-net` không expose port.

## Chạy

```bash
cd docker-setup
cp .env.example .env    # sửa password trước khi dùng thật
docker compose up -d
```

## Kiểm tra

```bash
docker compose ps                                   # cả 2 container đều "healthy"
docker exec -it larder-postgres psql -U larder_app -d larder -c "\dn"   # phải thấy 4 schema
docker exec -it larder-mongo mongosh -u larder_app -p changeme --eval "db.adminCommand('ping')"
```

Kết nối từ tool ngoài máy (DBeaver, MongoDB Compass...): `localhost:5432` (Postgres) / `localhost:27017` (Mongo) — 2 port này expose ra host để tiện dev, khác với nguyên tắc Private Network áp dụng cho các service ứng dụng (Gateway/User/...).

## Dừng / xoá sạch

```bash
docker compose down          # dừng, giữ volume (giữ data)
docker compose down -v       # dừng + xoá luôn data (khi cần seed lại từ đầu)
```

## File trong thư mục này

| File | Vai trò |
|---|---|
| `docker-compose.yml` | Định nghĩa container postgres + mongo, network `public-net`/`private-net` |
| `init-schemas.sql` | Tự chạy khi Postgres khởi tạo lần đầu — tạo 4 schema + bảng (đồng bộ CLAUDE.md mục 3.1) |
| `.env.example` | Mẫu biến môi trường — copy thành `.env`, không commit `.env` thật |
| `Dockerfile.template` | Mẫu multi-stage build cho từng service Spring Boot, copy khi scaffold monorepo |
