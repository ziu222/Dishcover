package com.dishcover.user.admin;

import com.dishcover.user.admin.AdminStatsDtos.AdminStatsResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Số liệu tổng quan tài khoản cho màn /admin/so-lieu. Quyền ADMIN gác ở SecurityConfig ({@code /admin/**}). */
@RestController
@RequestMapping("/admin/stats")
public class AdminStatsController {

    private final AdminStatsService service;

    public AdminStatsController(AdminStatsService service) {
        this.service = service;
    }

    @GetMapping
    public AdminStatsResponse stats() {
        return service.stats();
    }
}
