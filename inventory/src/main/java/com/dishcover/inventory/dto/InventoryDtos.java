package com.dishcover.inventory.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** Gom nhóm toàn bộ DTO request/response của Inventory Service (class chỉ chứa các record tĩnh, không khởi tạo được). */
public final class InventoryDtos {

    private InventoryDtos() {
    }

    /**
     * ingredientName là tên thô (người dùng gõ hoặc Vision trả về) — server tự chuẩn hóa, không tin client gửi normalizedName.
     *
     * @param ingredientName tên nguyên liệu thô do client gửi lên
     * @param expiryDate hạn sử dụng; nếu để trống, service tự suy ra từ bảng hạn dùng mặc định
     */
    public record AddItemRequest(
            // @Size khớp độ dài cột DB (VARCHAR 100/20) — chặn tại biên trả 422 thay vì để giá trị
            // quá cỡ lọt xuống DB gây DataIntegrityViolation (từng bị che thành 401).
            @NotBlank @Size(max = 100) String ingredientName,
            // DECIMAL(10,2): tối đa 8 chữ số phần nguyên + 2 phần thập phân.
            @DecimalMin(value = "0", inclusive = false) @Digits(integer = 8, fraction = 2) BigDecimal quantity,
            @Size(max = 20) String unit,
            LocalDate expiryDate
    ) {
    }

    /**
     * Request cập nhật một phần (partial update) một dòng nguyên liệu đã tồn tại — mọi field
     * đều tùy chọn, field null nghĩa là giữ nguyên giá trị cũ, chỉ field khác null mới được ghi.
     *
     * @param status trạng thái mới muốn ghi đè trực tiếp (chỉ nhận FRESH/EXPIRING_SOON/EXPIRED/USED)
     */
    public record UpdateItemRequest(
            @DecimalMin(value = "0", inclusive = false) @Digits(integer = 8, fraction = 2) BigDecimal quantity,
            @Size(max = 20) String unit,
            LocalDate expiryDate,
            @Pattern(regexp = "FRESH|EXPIRING_SOON|EXPIRED|USED", message = "status không hợp lệ") String status
    ) {
    }

    /**
     * Request thêm nhiều nguyên liệu cùng lúc (dùng sau bước xác nhận nhận diện ảnh).
     *
     * @param items danh sách nguyên liệu cần thêm, không được rỗng
     */
    public record BatchAddRequest(
            @NotEmpty @Valid List<AddItemRequest> items
    ) {
    }

    /**
     * DTO trả về cho client cho một dòng nguyên liệu trong tủ lạnh ảo. {@code status} ở đây là
     * giá trị đã được tính lại (derived) theo hạn dùng tại thời điểm trả response, không phải
     * đọc thẳng từ cột lưu trữ trong DB.
     *
     * @param normalizedName tên đã chuẩn hóa dùng làm khóa so khớp nội bộ
     * @param source nguồn nhập liệu ({@code MANUAL} hoặc {@code IMAGE_RECOGNITION})
     * @param status trạng thái hiển thị đã được derive (FRESH/EXPIRING_SOON/EXPIRED/USED)
     */
    public record InventoryItemResponse(
            Long id,
            String ingredientName,
            String normalizedName,
            BigDecimal quantity,
            String unit,
            LocalDate expiryDate,
            String source,
            String status
    ) {
    }
}
