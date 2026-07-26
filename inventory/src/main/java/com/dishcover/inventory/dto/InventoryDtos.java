package com.dishcover.inventory.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public final class InventoryDtos {

    private InventoryDtos() {
    }

    /** ingredientName là tên thô (người dùng gõ hoặc Vision trả về) — server tự chuẩn hóa, không tin client gửi normalizedName. */
    public record AddItemRequest(
            @NotBlank String ingredientName,
            @DecimalMin(value = "0", inclusive = false) BigDecimal quantity,
            String unit,
            LocalDate expiryDate
    ) {
    }

    public record UpdateItemRequest(
            @DecimalMin(value = "0", inclusive = false) BigDecimal quantity,
            String unit,
            LocalDate expiryDate,
            @Pattern(regexp = "FRESH|EXPIRING_SOON|EXPIRED|USED", message = "status không hợp lệ") String status
    ) {
    }

    public record BatchAddRequest(
            @NotEmpty @Valid List<AddItemRequest> items
    ) {
    }

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
