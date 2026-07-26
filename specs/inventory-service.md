# Spec: Inventory Service — "Tủ lạnh ảo"

> Tài liệu bàn giao độc lập — người thực hiện (kể cả AI khác) không có lịch sử hội thoại,
> chỉ có file này + repo. Đọc hết trước khi code. Không tự suy diễn ngoài phạm vi mô tả.

**Phase:** Checklist mục 5 (User → **Inventory** → Recipe), phần 2/3
**Trạng thái phụ thuộc:** User Service đã xong (auth JWT, mẫu pattern để copy) — xem mục "Tài liệu tham khảo".
**Không thuộc phạm vi phase này:** Image Recognition Service (mục 7, phase riêng), Matching Service (mục 6).

---

## 1. Bối cảnh dự án (tóm tắt tối thiểu)

Web gợi ý công thức nấu ăn từ nguyên liệu còn dư trong tủ lạnh. Kiến trúc microservices Spring Boot,
mỗi service 1 schema Postgres riêng (trừ Recipe dùng MongoDB riêng). Inventory Service quản lý
**tủ lạnh ảo** của người dùng: danh sách nguyên liệu họ đang có, số lượng, hạn dùng.

`normalized_name` là khóa so khớp DUY NHẤT giữa Inventory ↔ Recipe ↔ Matching ↔ RAG — sai ở đây thì
toàn bộ gợi ý công thức phía sau sai theo. Không được tự nghĩ ra cách chuẩn hóa khác; dùng đúng
`common` module đã có.

## 2. Hạ tầng đã có sẵn — dùng lại, không viết mới

| Đã có | Ở đâu | Cách dùng |
|---|---|---|
| Bảng DB | `docker-setup/init-schemas.sql` → `inventory_service.user_ingredients` | Entity map đúng cột có sẵn, KHÔNG đổi tên cột/thêm migration riêng |
| Chuẩn hóa tên tiếng Việt | `common/.../text/VietnameseTextNormalizer.normalize(String)` | Static method, đã có test |
| Từ điển nguyên liệu | `common/.../ingredient/IngredientCatalog` — 194 mục | `resolve(rawName)` → normalized_name canonical; `lookup(rawName)` → `Optional<IngredientEntry>` (có category/shelfLifeDays/allergenGroup) |
| Hạn dùng mặc định | `common/.../ingredient/DefaultShelfLifeTable.forCategory(category)` | Fallback khi không xác định được `shelfLifeDays` cụ thể |
| Pattern JWT + layering | Module `user/` toàn bộ | **Copy cấu trúc, không copy nguyên xi code** — xem mục 5 |

**Module `inventory` đã scaffold rỗng** (`inventory/pom.xml`, `Application.java`, `application.yml` với port 8082,
datasource trỏ schema `inventory_service`) — chỉ thêm code, không tạo lại module.

## 3. Nhiệm vụ

CRUD nguyên liệu trong tủ lạnh ảo của **người dùng đang đăng nhập** (`userId` luôn lấy từ JWT, không
bao giờ nhận từ client — xem User Service `UserController` làm mẫu).

### 3.1. Entity — map đúng bảng có sẵn

```sql
-- đã tồn tại, xem đầy đủ tại docker-setup/init-schemas.sql
inventory_service.user_ingredients (
  id, user_id, ingredient_name, normalized_name, quantity, unit,
  expiry_date, source ('MANUAL'|'IMAGE_RECOGNITION'), status ('FRESH'|'EXPIRING_SOON'|'EXPIRED'|'USED'),
  created_at, updated_at
)
-- index có sẵn: (user_id, status), (user_id, expiry_date)
```

### 3.2. Endpoint bắt buộc

| Method | Path | Việc | Ghi chú |
|---|---|---|---|
| POST | `/inventory/items` | Thêm 1 nguyên liệu | `source=MANUAL` |
| GET | `/inventory/items` | List nguyên liệu của user | filter query param `status` optional |
| GET | `/inventory/items/{id}` | Chi tiết 1 dòng | 404 nếu không thuộc user hiện tại (không lộ 403 — kẻ dò ID không phân biệt được "không tồn tại" vs "không phải của bạn") |
| PATCH | `/inventory/items/{id}` | Sửa `quantity`/`unit`/`expiry_date`, hoặc đánh dấu `status=USED` | Không cho sửa `normalized_name` trực tiếp — sửa tên = xóa thêm lại |
| DELETE | `/inventory/items/{id}` | Xóa hẳn 1 dòng | |
| POST | `/inventory/items/batch` | Thêm nhiều dòng 1 lượt | `source=IMAGE_RECOGNITION`. **Endpoint này được gọi TỪ CLIENT sau khi người dùng đã xác nhận** kết quả nhận diện ảnh — Inventory Service KHÔNG gọi/được gọi trực tiếp bởi Image Service (CLAUDE.md mục 7, nguyên tắc kiến trúc đã chốt, không thảo luận lại) |

### 3.3. Business rule — ĐỌC KỸ, đây là phần dễ làm sai

**a) Chuẩn hóa lúc ghi, không tin client:**
Request chỉ gửi `ingredientName` (tên thô người dùng gõ hoặc Vision trả về). Server tự:
1. `normalizedName = IngredientCatalog.resolve(ingredientName)`
2. Nếu `IngredientCatalog.lookup(ingredientName)` có kết quả và request không gửi `expiryDate` →
   tự set `expiry_date = today + entry.shelfLifeDays()`; nếu không có trong catalog → dùng
   `DefaultShelfLifeTable.forCategory(null)` (fallback 7 ngày).

**b) KHÔNG thêm UNIQUE constraint (user_id, normalized_name) — quyết định đã chốt, không tự ý đổi:**
Một user có thể có **nhiều dòng cùng `normalized_name` với `expiry_date` khác nhau** — đây là 2 lô
hàng thật khác nhau (VD: trứng mua tuần trước hạn 10 ngày + trứng mua hôm nay hạn 21 ngày), không
phải dữ liệu trùng lặp lỗi. Vì vậy:
- `POST /inventory/items`: nếu đã có dòng cùng `user_id + normalized_name + expiry_date` (cùng lô) →
  cộng dồn `quantity` (update), không tạo dòng mới. Khác `expiry_date` → luôn tạo dòng mới.
- **Hệ quả cho Matching Service (phase sau, không phải việc của bạn nhưng phải biết để không "sửa" nhầm):**
  khi tính tập nguyên liệu user để so khớp Jaccard, Matching phải `SELECT DISTINCT normalized_name`
  (gộp theo tên, không đếm số dòng) — việc dedupe thuộc về Matching, không phải ràng buộc DB ở đây.

**c) `status` là derived field khi trả response, KHÔNG cần cron job:**
Tính lúc map Entity → DTO (không ghi ngược xuống DB mỗi lần đọc):
```
nếu status hiện tại == 'USED' → giữ nguyên 'USED' (user đã chủ động đánh dấu, không được ghi đè)
ngược lại nếu expiry_date < today → 'EXPIRED'
ngược lại nếu expiry_date <= today + 3 ngày → 'EXPIRING_SOON'
ngược lại → 'FRESH'
nếu expiry_date null → giữ nguyên status đang lưu trong DB
```
Ghi `status` xuống DB khi tạo mới (giá trị tính theo rule trên tại thời điểm tạo) và khi PATCH đổi
`expiry_date`/đánh dấu `USED`. Không cần scheduled job cho MVP.

## 4. DTO (record Java, KHÔNG expose entity — CLAUDE.md mục 9)

```java
record AddItemRequest(@NotBlank String ingredientName, BigDecimal quantity, String unit,
                       LocalDate expiryDate /* optional */) {}

record UpdateItemRequest(BigDecimal quantity, String unit, LocalDate expiryDate, String status) {}

record BatchAddRequest(@NotEmpty @Valid List<AddItemRequest> items) {}

record InventoryItemResponse(Long id, String ingredientName, String normalizedName,
                              BigDecimal quantity, String unit, LocalDate expiryDate,
                              String source, String status /* derived */) {}
```

## 5. Kiến trúc bắt buộc theo (copy pattern từ `user/`, KHÔNG copy-paste nguyên file)

- Package: `controller / service / repository / dto / exception / config / entity / security`
- **JWT**: service này CHƯA có Gateway lo xác thực tập trung (mục 8 chưa làm) → phải tự verify JWT
  giống hệt cách User Service làm (`JwtService`, `JwtAuthFilter`, `AuthenticatedUser`, `SecurityConfig`).
  **Khuyến nghị làm trước khi code business logic:** chuyển 3 class này từ `user/security/` sang
  `common/` (đổi package `com.dishcover.common.security`) để Inventory + 5 service còn lại dùng
  chung, không copy-paste 6 lần. Nếu làm việc này, nhớ cập nhật lại `user/` để import từ `common`
  và chạy lại toàn bộ test `user` để chắc không vỡ.
- Exception tập trung: `@RestControllerAdvice` trả `{code, message, traceId}` giống `GlobalExceptionHandler`
  của User Service.
- Swagger: copy `OpenApiConfiguration` của User Service, đổi title/description cho đúng service.
- `ddl-auto: validate` — entity phải khớp cột có sẵn, không để Hibernate tự tạo bảng.

## 6. Definition of Done

- [ ] Build sạch: `./mvnw -pl inventory,common test`
- [ ] Test tích hợp kiểu `AuthFlowIntegrationTest` (H2 profile `test`): thêm/sửa/xóa/list, batch add,
      404 khi truy cập item của user khác, validation lỗi trả `422`
- [ ] Guard test: normalize + shelf-life default áp dụng đúng khi thêm nguyên liệu lạ ngoài catalog
- [ ] Verify chạy thật với Postgres thật (không chỉ H2) — ít nhất 1 vòng thêm/sửa/xóa qua Swagger UI
- [ ] Swagger UI `/swagger-ui.html` liệt kê đủ 6 endpoint, Authorize dán JWT hoạt động
- [ ] Mỗi step commit riêng theo Conventional Commits, KHÔNG `Co-Authored-By` (quy tắc repo này)

## 7. KHÔNG được làm (ngoài phạm vi, đừng tự mở rộng)

- Không viết endpoint cho Image Recognition — đó là service khác, phase khác.
- Không viết logic Matching/Jaccard ở đây.
- Không thêm UNIQUE constraint DB (xem mục 3.3.b) — đã cân nhắc và loại bỏ.
- Không đổi tên cột trong `init-schemas.sql` để "cho đẹp" — sửa entity cho khớp DB, không phải ngược lại.

## 8. Tài liệu tham khảo

- `CLAUDE.md` mục 3.1 (schema đầy đủ), mục 7 (luồng Image Recognition — vì sao batch endpoint không
  nhận trực tiếp từ Image Service), mục 9 (SOLID/conventions), mục 13 (git workflow — commit từng step).
- `user/` toàn bộ module — pattern mẫu duy nhất được tham chiếu.
- `common/src/main/java/com/dishcover/common/ingredient/` — API thật của Catalog/ShelfLifeTable.
