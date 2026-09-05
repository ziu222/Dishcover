package com.dishcover.notification.controller;

import com.dishcover.common.security.JwtService;
import com.dishcover.notification.entity.Notification;
import com.dishcover.notification.repository.NotificationRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class NotificationControllerTest {

    private static final String SECRET = "test-secret-at-least-32-chars-long-000";

    @Autowired
    MockMvc mvc;
    @Autowired
    NotificationRepository repository;

    private String auth(long userId) {
        return "Bearer " + new JwtService(SECRET, 120).issue(userId, "user" + userId + "@test.com", "FREE");
    }

    @Test
    void noTokenReturns401() throws Exception {
        mvc.perform(get("/notifications")).andExpect(status().isUnauthorized());
    }

    @Test
    void listReturnsOwnNotificationsWithUnreadCount() throws Exception {
        long uid = System.nanoTime();
        repository.save(new Notification(uid, "INGREDIENT_EXPIRING_SOON", "t", "m", "/goi-y", 1L));

        mvc.perform(get("/notifications").header("Authorization", auth(uid)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.unreadCount").value(1));
    }

    @Test
    void markReadOnOtherUsersNotificationReturns404() throws Exception {
        long owner = System.nanoTime();
        long other = owner + 1;
        Notification n = repository.save(new Notification(owner, "INGREDIENT_EXPIRING_SOON", "t", "m", "/goi-y", 1L));

        mvc.perform(patch("/notifications/{id}/read", n.getId()).header("Authorization", auth(other)))
                .andExpect(status().isNotFound());
    }

    @Test
    void markReadThenListShowsRead() throws Exception {
        long uid = System.nanoTime();
        Notification n = repository.save(new Notification(uid, "INGREDIENT_EXPIRING_SOON", "t", "m", "/goi-y", 1L));

        mvc.perform(patch("/notifications/{id}/read", n.getId()).header("Authorization", auth(uid)))
                .andExpect(status().isOk());
        mvc.perform(get("/notifications").header("Authorization", auth(uid)))
                .andExpect(jsonPath("$.unreadCount").value(0));
    }

    /** size quá lớn phải bị chặn ở max-page-size (100, application.yml), không tải nguyên bảng. */
    @Test
    void oversizedPageSizeReturnsOkNotError() throws Exception {
        long uid = System.nanoTime();
        repository.save(new Notification(uid, "INGREDIENT_EXPIRING_SOON", "t", "m", "/goi-y", 1L));

        mvc.perform(get("/notifications").param("size", "100000").header("Authorization", auth(uid)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(1));
    }

    /** page âm trước đây làm PageRequest.of() ném IllegalArgumentException -> 500; Pageable tự kẹp về 0. */
    @Test
    void negativePageDoesNotCrash() throws Exception {
        long uid = System.nanoTime();
        repository.save(new Notification(uid, "INGREDIENT_EXPIRING_SOON", "t", "m", "/goi-y", 1L));

        mvc.perform(get("/notifications").param("page", "-1").header("Authorization", auth(uid)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(1));
    }

    @Test
    void markAllReadClearsOwnUnreadCountOnly() throws Exception {
        long uid = System.nanoTime();
        long other = uid + 1;
        repository.save(new Notification(uid, "INGREDIENT_EXPIRING_SOON", "t1", "m1", "/goi-y", 1L));
        repository.save(new Notification(uid, "INGREDIENT_EXPIRED", "t2", "m2", "/goi-y", 2L));
        repository.save(new Notification(other, "INGREDIENT_EXPIRED", "t3", "m3", "/goi-y", 3L));

        mvc.perform(patch("/notifications/read-all").header("Authorization", auth(uid)))
                .andExpect(status().isOk());

        mvc.perform(get("/notifications").header("Authorization", auth(uid)))
                .andExpect(jsonPath("$.unreadCount").value(0));
        mvc.perform(get("/notifications").header("Authorization", auth(other)))
                .andExpect(jsonPath("$.unreadCount").value(1));
    }
}
