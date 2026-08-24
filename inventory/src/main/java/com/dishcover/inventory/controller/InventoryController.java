package com.dishcover.inventory.controller;

import com.dishcover.common.security.AuthenticatedUser;
import com.dishcover.inventory.dto.InventoryDtos.AddItemRequest;
import com.dishcover.inventory.dto.InventoryDtos.BatchAddRequest;
import com.dishcover.inventory.dto.InventoryDtos.InventoryItemResponse;
import com.dishcover.inventory.dto.InventoryDtos.UpdateItemRequest;
import com.dishcover.inventory.service.InventoryService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * userId luôn lấy từ JWT (specs/inventory-service.md mục 3) — không bao giờ nhận từ client.
 *
 * <p>Mọi endpoint chỉ yêu cầu JWT hợp lệ. Trước đây cả nhóm bị chặn bằng {@code @RequiresPlan("PRO")}
 * theo mô hình Freemium; mô hình này đã bị gỡ khỏi phạm vi đề tài cùng với Payment Service
 * (2026-08-17), nên tủ lạnh ảo giờ mở cho mọi người dùng đã đăng nhập.</p>
 */
@RestController
@RequestMapping("/inventory/items")
public class InventoryController {

    private final InventoryService service;

    public InventoryController(InventoryService service) {
        this.service = service;
    }

    /**
     * Liệt kê nguyên liệu trong tủ lạnh ảo của người dùng hiện tại, có thể lọc theo trạng thái.
     * Trạng thái trả về là giá trị đã được tính lại (derived) theo hạn dùng tại thời điểm gọi,
     * không phải đọc thẳng từ DB.
     *
     * @param me người dùng đã xác thực, lấy từ JWT
     * @param status trạng thái lọc (FRESH/EXPIRING_SOON/EXPIRED/USED), null nếu không lọc
     * @return danh sách nguyên liệu của người dùng
     */
    @GetMapping
    public List<InventoryItemResponse> list(@AuthenticationPrincipal AuthenticatedUser me,
                                            @RequestParam(required = false) String status) {
        return service.list(me.userId(), status);
    }

    /**
     * Lấy chi tiết một dòng nguyên liệu, chỉ khi thuộc sở hữu của người dùng hiện tại.
     *
     * @param me người dùng đã xác thực, lấy từ JWT
     * @param id id dòng nguyên liệu cần lấy
     * @return chi tiết nguyên liệu
     * @throws com.dishcover.common.exception.ResourceNotFoundException nếu không tồn tại hoặc không thuộc người dùng này
     */
    @GetMapping("/{id}")
    public InventoryItemResponse getOne(@AuthenticationPrincipal AuthenticatedUser me, @PathVariable Long id) {
        return service.getOne(me.userId(), id);
    }

    /**
     * Thêm một nguyên liệu nhập tay vào tủ lạnh ảo. Nếu đã tồn tại lô hàng cùng
     * (userId, normalized_name, expiry_date), số lượng sẽ được cộng dồn vào dòng đó thay vì
     * tạo dòng mới; khác lô (khác hạn dùng) thì tạo dòng riêng.
     *
     * @param me người dùng đã xác thực, lấy từ JWT
     * @param req thông tin nguyên liệu cần thêm
     * @return nguyên liệu sau khi thêm/gộp lô, HTTP 201
     */
    @PostMapping
    public ResponseEntity<InventoryItemResponse> add(@AuthenticationPrincipal AuthenticatedUser me,
                                                      @Valid @RequestBody AddItemRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.addItem(me.userId(), req));
    }

    /**
     * Thêm nhiều nguyên liệu cùng lúc, dùng sau bước người dùng xác nhận kết quả nhận diện ảnh
     * (nguồn được đánh dấu {@code IMAGE_RECOGNITION}). Áp dụng cùng quy tắc upsert theo lô như
     * {@link #add}.
     *
     * @param me người dùng đã xác thực, lấy từ JWT
     * @param req danh sách nguyên liệu cần thêm
     * @return danh sách nguyên liệu sau khi thêm/gộp lô, HTTP 201
     */
    @PostMapping("/batch")
    public ResponseEntity<List<InventoryItemResponse>> addBatch(@AuthenticationPrincipal AuthenticatedUser me,
                                                                 @Valid @RequestBody BatchAddRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.addBatch(me.userId(), req.items()));
    }

    /**
     * Cập nhật một phần một dòng nguyên liệu (số lượng, đơn vị, hạn dùng, trạng thái), chỉ khi
     * thuộc sở hữu người dùng hiện tại. Field null trong request giữ nguyên giá trị cũ.
     *
     * @param me người dùng đã xác thực, lấy từ JWT
     * @param id id dòng nguyên liệu cần cập nhật
     * @param req các field cần cập nhật
     * @return nguyên liệu sau khi cập nhật
     * @throws com.dishcover.common.exception.ResourceNotFoundException nếu không tồn tại hoặc không thuộc người dùng này
     */
    @PatchMapping("/{id}")
    public InventoryItemResponse update(@AuthenticationPrincipal AuthenticatedUser me, @PathVariable Long id,
                                        @Valid @RequestBody UpdateItemRequest req) {
        return service.update(me.userId(), id, req);
    }

    /**
     * Xóa một dòng nguyên liệu khỏi tủ lạnh ảo, chỉ khi thuộc sở hữu người dùng hiện tại.
     *
     * @param me người dùng đã xác thực, lấy từ JWT
     * @param id id dòng nguyên liệu cần xóa
     */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal AuthenticatedUser me, @PathVariable Long id) {
        service.delete(me.userId(), id);
    }
}
