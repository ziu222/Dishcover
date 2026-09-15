package com.dishcover.user.admin;

import com.dishcover.user.admin.AdminStatsDtos.AdminStatsResponse;
import com.dishcover.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;

/** Đếm trực tiếp qua {@code count(*)} tại thời điểm gọi — không cache, dữ liệu chỉ vài nghìn dòng. */
@Service
public class AdminStatsService {

    private final UserRepository users;

    public AdminStatsService(UserRepository users) {
        this.users = users;
    }

    @Transactional(readOnly = true)
    public AdminStatsResponse stats() {
        Instant sevenDaysAgo = Instant.now().minus(Duration.ofDays(7));
        return new AdminStatsResponse(
                users.count(),
                users.countByRole("ADMIN"),
                users.countByLocked(true),
                users.countByCreatedAtAfter(sevenDaysAgo));
    }
}
