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
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Khu quản trị tài khoản. Trọng tâm là hai chốt an toàn (không tự khoá, không tự hạ quyền) và
 * việc tài khoản bị khoá không đăng nhập được — hỏng cái nào cũng dẫn tới mất quyền quản trị
 * hoặc khoá chặn không có tác dụng.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestOtpStoreConfig.class)
class AdminUserFlowTest {

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
    private User victim;

    @BeforeEach
    void setUp() {
        users.deleteAll();
        admin = users.save(newUser("admin@test.com", "ADMIN"));
        victim = users.save(newUser("victim@test.com", "USER"));
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
    void userThuongKhongVaoDuocKhuQuanTri() throws Exception {
        mvc.perform(get("/admin/users").header("Authorization", "Bearer " + tokenFor(victim)))
                .andExpect(status().isForbidden());
    }

    @Test
    void khongCoTokenThiKhongVaoDuoc() throws Exception {
        mvc.perform(get("/admin/users")).andExpect(status().isUnauthorized());
    }

    @Test
    void adminXemDuocDanhSachVaLocTheoEmail() throws Exception {
        mvc.perform(get("/admin/users").param("email", "victim")
                        .header("Authorization", "Bearer " + tokenFor(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].email").value("victim@test.com"));
    }

    @Test
    void adminKhoaDuocTaiKhoanKhac() throws Exception {
        mvc.perform(patch("/admin/users/" + victim.getId() + "/lock")
                        .header("Authorization", "Bearer " + tokenFor(admin))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"locked\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.locked").value(true));

        assertThat(users.findById(victim.getId()).orElseThrow().getLocked()).isTrue();
    }

    @Test
    void adminKhongTuKhoaDuocChinhMinh() throws Exception {
        mvc.perform(patch("/admin/users/" + admin.getId() + "/lock")
                        .header("Authorization", "Bearer " + tokenFor(admin))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"locked\":true}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("SELF_TARGET"));

        assertThat(users.findById(admin.getId()).orElseThrow().getLocked()).isFalse();
    }

    @Test
    void adminKhongTuHaQuyenChinhMinh() throws Exception {
        mvc.perform(patch("/admin/users/" + admin.getId() + "/role")
                        .header("Authorization", "Bearer " + tokenFor(admin))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"role\":\"USER\"}"))
                .andExpect(status().isConflict());

        assertThat(users.findById(admin.getId()).orElseThrow().getRole()).isEqualTo("ADMIN");
    }

    @Test
    void adminPhongDuocQuyenChoNguoiKhac() throws Exception {
        mvc.perform(patch("/admin/users/" + victim.getId() + "/role")
                        .header("Authorization", "Bearer " + tokenFor(admin))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"role\":\"ADMIN\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("ADMIN"));
    }

    @Test
    void roleLaGiaTriLaBiTuChoiNgayTaiBien() throws Exception {
        mvc.perform(patch("/admin/users/" + victim.getId() + "/role")
                        .header("Authorization", "Bearer " + tokenFor(admin))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"role\":\"SUPERUSER\"}"))
                // 422 chu khong phai 400 — quy uoc loi validation cua du an (CLAUDE.md muc 9)
                .andExpect(status().isUnprocessableEntity());

        assertThat(users.findById(victim.getId()).orElseThrow().getRole()).isEqualTo("USER");
    }

    @Test
    void saiMatKhauKhongLoRaViecTaiKhoanDangBiKhoa() throws Exception {
        victim.setLocked(true);
        users.save(victim);

        mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"victim@test.com\",\"password\":\"sai-mat-khau\"}"))
                .andExpect(status().isUnauthorized());
    }
}
