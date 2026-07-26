package com.dishcover.inventory.service;

import com.dishcover.common.ingredient.DefaultShelfLifeTable;
import com.dishcover.common.ingredient.IngredientCatalog;
import com.dishcover.common.ingredient.IngredientEntry;
import com.dishcover.inventory.dto.InventoryDtos.AddItemRequest;
import com.dishcover.inventory.dto.InventoryDtos.InventoryItemResponse;
import com.dishcover.inventory.dto.InventoryDtos.UpdateItemRequest;
import com.dishcover.inventory.entity.UserIngredient;
import com.dishcover.inventory.exception.ApiExceptions.ResourceNotFoundException;
import com.dishcover.inventory.repository.UserIngredientRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Business rule đầy đủ ở specs/inventory-service.md mục 3.3:
 * (a) chuẩn hóa tên + fallback hạn dùng lúc ghi, (b) upsert theo lô hàng (cùng user+normalized_name
 * +expiry_date thì cộng dồn số lượng thay vì tạo dòng mới — KHÔNG phải bug nếu thấy nhiều dòng cùng
 * normalized_name khác expiry_date, đó là 2 lô thật), (c) status derived khi map response, không cron job.
 */
@Service
public class InventoryService {

    private static final int NEAR_EXPIRY_DAYS = 3;

    private final UserIngredientRepository repo;
    private final IngredientCatalog catalog;

    public InventoryService(UserIngredientRepository repo, IngredientCatalog catalog) {
        this.repo = repo;
        this.catalog = catalog;
    }

    @Transactional(readOnly = true)
    public List<InventoryItemResponse> list(Long userId, String statusFilter) {
        List<UserIngredient> items = statusFilter == null
                ? repo.findByUserId(userId)
                : repo.findByUserIdAndStatus(userId, statusFilter);
        return items.stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public InventoryItemResponse getOne(Long userId, Long id) {
        return toResponse(requireOwned(userId, id));
    }

    @Transactional
    public InventoryItemResponse addItem(Long userId, AddItemRequest req) {
        return toResponse(upsert(userId, req, "MANUAL"));
    }

    @Transactional
    public List<InventoryItemResponse> addBatch(Long userId, List<AddItemRequest> items) {
        return items.stream()
                .map(req -> upsert(userId, req, "IMAGE_RECOGNITION"))
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public InventoryItemResponse update(Long userId, Long id, UpdateItemRequest req) {
        UserIngredient item = requireOwned(userId, id);
        if (req.quantity() != null) {
            item.setQuantity(req.quantity());
        }
        if (req.unit() != null) {
            item.setUnit(req.unit());
        }
        if (req.expiryDate() != null) {
            item.setExpiryDate(req.expiryDate());
            // Đổi hạn dùng thì tính lại status ngay (trừ khi cùng request đã tự đánh dấu USED bên dưới)
            item.setStatus(deriveStatus(item.getStatus(), req.expiryDate()));
        }
        if (req.status() != null) {
            item.setStatus(req.status());
        }
        return toResponse(item);
    }

    @Transactional
    public void delete(Long userId, Long id) {
        repo.deleteByIdAndUserId(id, userId);
    }

    // ---- helpers ----

    private UserIngredient requireOwned(Long userId, Long id) {
        return repo.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy nguyên liệu id=" + id));
    }

    private UserIngredient upsert(Long userId, AddItemRequest req, String source) {
        String normalizedName = catalog.resolve(req.ingredientName());
        LocalDate expiryDate = req.expiryDate() != null ? req.expiryDate() : defaultExpiryDate(req.ingredientName());

        Optional<UserIngredient> existing =
                repo.findByUserIdAndNormalizedNameAndExpiryDate(userId, normalizedName, expiryDate);
        if (existing.isPresent()) {
            UserIngredient item = existing.get();
            item.setQuantity(sumQuantity(item.getQuantity(), req.quantity()));
            if (req.unit() != null) {
                item.setUnit(req.unit());
            }
            return item;
        }

        String status = deriveStatus("FRESH", expiryDate);
        UserIngredient created = new UserIngredient(
                userId, req.ingredientName(), normalizedName, req.quantity(), req.unit(),
                expiryDate, source, status);
        return repo.save(created);
    }

    private LocalDate defaultExpiryDate(String rawIngredientName) {
        Optional<IngredientEntry> entry = catalog.lookup(rawIngredientName);
        Integer shelfDays = entry.map(IngredientEntry::shelfLifeDays).orElse(null);
        if (shelfDays == null) {
            String category = entry.map(IngredientEntry::category).orElse(null);
            shelfDays = DefaultShelfLifeTable.forCategory(category);
        }
        return LocalDate.now().plusDays(shelfDays);
    }

    private static BigDecimal sumQuantity(BigDecimal existing, BigDecimal incoming) {
        if (existing == null) {
            return incoming;
        }
        if (incoming == null) {
            return existing;
        }
        return existing.add(incoming);
    }

    /** Derived status — không ghi xuống DB khi chỉ đọc; chỉ persist lúc tạo mới hoặc đổi expiry_date/status. */
    private static String deriveStatus(String storedStatus, LocalDate expiryDate) {
        if ("USED".equals(storedStatus)) {
            return "USED";
        }
        if (expiryDate == null) {
            return storedStatus;
        }
        LocalDate today = LocalDate.now();
        if (expiryDate.isBefore(today)) {
            return "EXPIRED";
        }
        if (!expiryDate.isAfter(today.plusDays(NEAR_EXPIRY_DAYS))) {
            return "EXPIRING_SOON";
        }
        return "FRESH";
    }

    private InventoryItemResponse toResponse(UserIngredient item) {
        String displayStatus = deriveStatus(item.getStatus(), item.getExpiryDate());
        return new InventoryItemResponse(
                item.getId(), item.getIngredientName(), item.getNormalizedName(),
                item.getQuantity(), item.getUnit(), item.getExpiryDate(),
                item.getSource(), displayStatus);
    }
}
