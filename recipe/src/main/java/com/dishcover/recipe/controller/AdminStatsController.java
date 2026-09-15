package com.dishcover.recipe.controller;

import com.dishcover.recipe.dto.AdminStatsResponse;
import com.dishcover.recipe.service.AdminStatsService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Số liệu tổng quan công thức cho màn /admin/so-lieu. Quyền ADMIN gác ở SecurityConfig ({@code /admin/**}). */
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
