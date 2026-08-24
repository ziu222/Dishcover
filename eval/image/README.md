# Bộ đánh giá Image Recognition — 30 ảnh (CLAUDE.md mục 7/11)

Đo accuracy THẬT của luồng nhận diện nguyên liệu (`POST /recognize`), không ước lượng.

## Cấu trúc

- `labels.json` — ground truth 30 ảnh, chia 4 nhóm theo CLAUDE.md mục 7:
  - `single` (10) — rõ 1 nguyên liệu
  - `multi` (10) — nhiều nguyên liệu
  - `confusable` (5) — khó/dễ nhầm (hành lá vs hẹ, ngò vs cần...)
  - `empty` (5) — KHÔNG có nguyên liệu (bàn trống, bao bì) → `expected: []`
- `run-eval.mjs` — runner: mint JWT → POST từng ảnh → so `normalizedName` model trả với ground truth
  → tính Precision/Recall/F1 (micro) + exact-match + theo nhóm + latency.
- `images/` — chỗ đặt 30 ảnh (gitignored — không commit ảnh vì bản quyền + dung lượng).

## Chuẩn bị 30 ảnh

Đặt ảnh vào `eval/image/images/` đúng tên trong `labels.json` (`single_01.jpg`, `multi_01.jpg`,
`empty_01.jpg`, ...). Rồi điền `expected` = danh sách `normalized_name` (khóa catalog: bỏ dấu +
đ→d, vd "trứng gà"→`trung ga`, "cà chua"→`ca chua`). Tra khóa chuẩn trong
`common/src/main/resources/ingredient-catalog.json`. Ảnh nhóm `empty` để `expected: []`.

Runner tự BỎ QUA slot còn placeholder `<...>` — điền được ảnh nào đo ảnh đó, không cần đủ 30 mới chạy.

## Chạy

```bash
# 1) Bật Image Service (terminal riêng)
JWT_SECRET=<>=32-ký-tự OPENAI_API_KEY=<key-thật> ./mvnw -pl image spring-boot:run

# 2) Chạy đo (JWT_SECRET GIỐNG service)
JWT_SECRET=<>=32-ký-tự node eval/image/run-eval.mjs
```

## Đọc kết quả

- **Precision** = trong các nguyên liệu model báo, bao nhiêu % đúng (thấp = hay "bịa" thừa).
- **Recall** = trong các nguyên liệu ảnh thật có, model tìm được bao nhiêu % (thấp = bỏ sót).
- **Exact-match** = % ảnh model khớp TRỌN tập nguyên liệu (nghiêm ngặt nhất).
- Nhóm `empty`: FP > 0 nghĩa là model "nhận" ra nguyên liệu ở ảnh trống → đo xu hướng bịa.

Chép bảng tổng hợp + theo nhóm vào `specs/image-service.md` mục 5 làm số liệu báo cáo.
