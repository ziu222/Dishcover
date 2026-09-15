package com.dishcover.user.admin;

import com.dishcover.common.security.JwtService;
import com.dishcover.user.TestOtpStoreConfig;
import com.dishcover.user.entity.User;
import com.dishcover.user.mail.EmailSender;
import com.dishcover.user.repository.UserRepository;
import com.dishcover.user.security.TurnstileClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestOtpStoreConfig.class)
class AdminStatsFlowTest {

    @Autowired
    MockMvc mvc;
    @Autowired
    UserRepository users;
    @Autowired
    JwtService jwt;
    @MockitoBean
    TurnstileClient turnstileClient;
    @MockitoBean
    EmailSender emailSender;

    private User admin;

    @BeforeEach
    void setUp() {
        users.deleteAll();
        admin = users.save(newUser("admin@test.com", "ADMIN"));
        users.save(newUser("user1@test.com", "USER"));
        User locked = newUser("user2@test.com", "USER");
        locked.setLocked(true);
        users.save(locked);
    }

    private static User newUser(String email, String role) {
        User u = new User(email, "$2a$10$abcdefghijklmnopqrstuv", "Test");
        u.setEmailVerified(true);
        u.setRole(role);
        return u;
    }

    private String tokenFor(User u) {
        return jwt.issue(u.getId(), u.getEmail(), u.getPlan(), u.getRole());
    }

    @Test
    void userThuongKhongXemDuocSoLieu() throws Exception {
        mvc.perform(get("/admin/stats")
                        .header("Authorization", "Bearer " + jwt.issue(99L, "x@test.com", "FREE", "USER")))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminXemDuocSoLieuDungTheoDuLieuThat() throws Exception {
        mvc.perform(get("/admin/stats").header("Authorization", "Bearer " + tokenFor(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalUsers").value(3))
                .andExpect(jsonPath("$.adminCount").value(1))
                .andExpect(jsonPath("$.lockedCount").value(1))
                .andExpect(jsonPath("$.newUsersLast7Days").value(3));
    }
}
