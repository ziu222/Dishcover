package com.dishcover.user.admin;

import com.dishcover.common.security.AuthenticatedUser;
import com.dishcover.user.admin.AdminUserDtos.AdminUserResponse;
import com.dishcover.user.admin.AdminUserDtos.LockRequest;
import com.dishcover.user.admin.AdminUserDtos.RoleRequest;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Quản trị tài khoản. Quyền ADMIN do SecurityConfig gác ở mức {@code /admin/**} — không rải
 * annotation từng method để chỉ có một chỗ duy nhất quyết định ai vào được khu này.
 */
@RestController
@RequestMapping("/admin/users")
public class AdminUserController {

    private final AdminUserService service;

    public AdminUserController(AdminUserService service) {
        this.service = service;
    }

    /**
     * @param email lọc theo chuỗi con của email, bỏ trống là lấy tất cả
     * @param pageable phân trang, mặc định 20 dòng, mới nhất trước
     * @return trang tài khoản
     */
    @GetMapping
    public Page<AdminUserResponse> list(
            @RequestParam(required = false) String email,
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {
        return service.list(email, pageable);
    }

    /**
     * Khoá hoặc mở khoá một tài khoản.
     *
     * @param me   admin đang thao tác, suy từ JWT
     * @param id   tài khoản bị tác động
     * @param req  trạng thái khoá mong muốn
     * @return tài khoản sau khi đổi
     * @throws SelfTargetException nếu tự khoá chính mình (409)
     */
    @PatchMapping("/{id}/lock")
    public AdminUserResponse setLocked(@AuthenticationPrincipal AuthenticatedUser me,
                                       @PathVariable Long id,
                                       @Valid @RequestBody LockRequest req) {
        return service.setLocked(me.userId(), id, req.locked());
    }

    /**
     * Đổi role một tài khoản.
     *
     * @param me  admin đang thao tác, suy từ JWT
     * @param id  tài khoản bị tác động
     * @param req role mới (USER hoặc ADMIN)
     * @return tài khoản sau khi đổi
     * @throws SelfTargetException nếu tự bỏ quyền quản trị của chính mình (409)
     */
    @PatchMapping("/{id}/role")
    public AdminUserResponse setRole(@AuthenticationPrincipal AuthenticatedUser me,
                                     @PathVariable Long id,
                                     @Valid @RequestBody RoleRequest req) {
        return service.setRole(me.userId(), id, req.role());
    }
}
