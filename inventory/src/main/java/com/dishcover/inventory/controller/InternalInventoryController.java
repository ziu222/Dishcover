package com.dishcover.inventory.controller;

import com.dishcover.common.security.InternalSecretGuard;
import com.dishcover.inventory.repository.UserIngredientRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoint nội bộ để User Service dọn dữ liệu khi xoá tài khoản.
 *
 * <p>Không dùng JWT vì lời gọi này xuất phát từ thao tác quản trị chạy phía server, không nằm
 * trong một request có JWT sống của chính người dùng bị xoá — cùng lý do với
 * {@code GET /internal/users/{id}} bên User Service.
 */
@RestController
@RequestMapping("/internal/users")
public class InternalInventoryController {

    private final UserIngredientRepository repo;
    private final String internalSecret;

    public InternalInventoryController(UserIngredientRepository repo,
                                        @Value("${internal.service-secret}") String internalSecret) {
        this.repo = repo;
        this.internalSecret = internalSecret;
    }

    /**
     * Xoá toàn bộ nguyên liệu của một user. Idempotent: user không có gì vẫn trả 204, nên bấm
     * xoá lại sau khi lỗi giữa chừng là an toàn.
     *
     * @param id     user cần dọn
     * @param secret header X-Internal-Secret
     */
    @DeleteMapping("/{id}/items")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Transactional
    public void deleteAllForUser(@PathVariable Long id,
                                  @RequestHeader(value = "X-Internal-Secret", required = false) String secret) {
        InternalSecretGuard.verify(internalSecret, secret);
        repo.deleteByUserId(id);
    }
}
