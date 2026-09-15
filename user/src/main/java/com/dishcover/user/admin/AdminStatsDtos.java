package com.dishcover.user.admin;

/** Số liệu tổng quan tài khoản cho màn /admin/so-lieu. */
public final class AdminStatsDtos {

    private AdminStatsDtos() {
    }

    public record AdminStatsResponse(
            long totalUsers,
            long adminCount,
            long lockedCount,
            long newUsersLast7Days
    ) {
    }
}
