package com.dishcover.user;

import com.dishcover.user.security.TurnstileClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthFlowIntegrationTest {

    @Autowired
    MockMvc mvc;
    @Autowired
    ObjectMapper mapper;
    /** Mock để không phụ thuộc mạng thật tới Cloudflare — test riêng TurnstileClient đã cover verify() thật. */
    @MockitoBean
    TurnstileClient turnstileClient;

    private String loginBody(String email, String password) {
        return "{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}";
    }

    /**
     * Đăng ký và trả token — token giờ chỉ đi qua cookie httpOnly {@code auth_token}, không
     * còn trong JSON body (xem AuthController). Test vẫn dùng Bearer header để gọi endpoint
     * bảo vệ vì JwtAuthFilter chấp nhận cả 2 đường, không cần MockMvc mô phỏng cookie jar.
     */
    private String register(String email, String pass) throws Exception {
        var response = mvc.perform(post("/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"" + pass + "\",\"fullName\":\"Test\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse();
        var cookie = response.getCookie("auth_token");
        org.junit.jupiter.api.Assertions.assertNotNull(cookie, "auth_token cookie phải được đặt sau register");
        return cookie.getValue();
    }

    @Test
    void registerThenLoginThenAccessProtected() throws Exception {
        String token = register("a@b.com", "secret1");

        // Đăng nhập đặt cookie httpOnly, body trả thẳng hồ sơ user (cùng shape /users/me)
        mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"a@b.com\",\"password\":\"secret1\"}"))
                .andExpect(status().isOk())
                .andExpect(cookie().exists("auth_token"))
                .andExpect(cookie().httpOnly("auth_token", true))
                .andExpect(jsonPath("$.plan").value("FREE"))
                .andExpect(jsonPath("$.email").value("a@b.com"));

        // Không token → 401
        mvc.perform(get("/users/me")).andExpect(status().isUnauthorized());

        // Có token → 200
        mvc.perform(get("/users/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("a@b.com"));
    }

    @Test
    void logoutClearsCookie() throws Exception {
        mvc.perform(post("/auth/logout"))
                .andExpect(status().isNoContent())
                .andExpect(cookie().maxAge("auth_token", 0));
    }

    @Test
    void duplicateEmailReturns409() throws Exception {
        register("dup@b.com", "secret1");
        mvc.perform(post("/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"dup@b.com\",\"password\":\"secret1\",\"fullName\":\"X\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("EMAIL_EXISTS"))
                .andExpect(jsonPath("$.traceId").isNotEmpty());
    }

    @Test
    void wrongPasswordReturns401() throws Exception {
        register("c@b.com", "secret1");
        mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"c@b.com\",\"password\":\"wrongpass\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));
    }

    @Test
    void invalidEmailFailsValidation() throws Exception {
        mvc.perform(post("/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"not-an-email\",\"password\":\"secret1\"}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void dietaryPreferenceCrudScopedToOwner() throws Exception {
        String token = register("diet@b.com", "secret1");

        String created = mvc.perform(post("/users/me/dietary-preferences")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"type\":\"ALLERGY\",\"value\":\"hải sản\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.type").value("ALLERGY"))
                .andReturn().getResponse().getContentAsString();
        long prefId = mapper.readTree(created).get("id").asLong();

        mvc.perform(get("/users/me/dietary-preferences").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].value").value("hải sản"));

        mvc.perform(delete("/users/me/dietary-preferences/" + prefId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());

        mvc.perform(get("/users/me/dietary-preferences").header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void addingSameDietaryPreferenceTwiceDoesNotDuplicate() throws Exception {
        String token = register("diet-dup@b.com", "secret1");
        String body = "{\"type\":\"ALLERGY\",\"value\":\"hải sản\"}";

        mvc.perform(post("/users/me/dietary-preferences")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated());
        mvc.perform(post("/users/me/dietary-preferences")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated());

        mvc.perform(get("/users/me/dietary-preferences").header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void badTypeRejectedByValidation() throws Exception {
        String token = register("bad@b.com", "secret1");
        mvc.perform(post("/users/me/dietary-preferences")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"type\":\"INVALID\",\"value\":\"x\"}"))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void calorieGoalDefaultsToUnsetThenUpsertReplacesValues() throws Exception {
        String token = register("goal@b.com", "secret1");

        // Chưa đặt mục tiêu -> 200 body rỗng (opt-in, không phải lỗi)
        mvc.perform(get("/users/me/calorie-goal").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(content().string(""));

        mvc.perform(put("/users/me/calorie-goal")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"calorieTarget\":2500,\"proteinTarget\":150,\"carbTarget\":300,\"fatTarget\":70}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.calorieTarget").value(2500))
                .andExpect(jsonPath("$.proteinTarget").value(150));

        mvc.perform(get("/users/me/calorie-goal").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.calorieTarget").value(2500));

        // Upsert lần 2 -> ghi đè, không tạo dòng mới
        mvc.perform(put("/users/me/calorie-goal")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"calorieTarget\":1800,\"proteinTarget\":140,\"carbTarget\":150,\"fatTarget\":50}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.calorieTarget").value(1800));
    }

    @Test
    void calorieGoalRejectsNonPositiveValues() throws Exception {
        String token = register("badgoal@b.com", "secret1");
        mvc.perform(put("/users/me/calorie-goal")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"calorieTarget\":-1,\"proteinTarget\":150,\"carbTarget\":300,\"fatTarget\":70}"))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void calorieGoalWithoutTokenReturns401() throws Exception {
        mvc.perform(get("/users/me/calorie-goal")).andExpect(status().isUnauthorized());
    }

    @Test
    void loginRequiresCaptchaAfterThreeFailures() throws Exception {
        register("captcha@b.com", "secret1");
        for (int i = 0; i < 3; i++) {
            mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                            .content(loginBody("captcha@b.com", "wrongpass")))
                    .andExpect(status().isUnauthorized());
        }

        // Lần 4 — dù đúng mật khẩu, thiếu captchaToken vẫn bị chặn TRƯỚC khi chạm AuthService.
        mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody("captcha@b.com", "secret1")))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("CAPTCHA_REQUIRED"));
    }

    @Test
    void loginSucceedsWithValidCaptchaAfterThreeFailures() throws Exception {
        when(turnstileClient.verify(any(), any())).thenReturn(true);
        register("captchaok@b.com", "secret1");
        for (int i = 0; i < 3; i++) {
            mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                            .content(loginBody("captchaok@b.com", "wrongpass")))
                    .andExpect(status().isUnauthorized());
        }

        mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"captchaok@b.com\",\"password\":\"secret1\",\"captchaToken\":\"any-token\"}"))
                .andExpect(status().isOk())
                .andExpect(cookie().exists("auth_token"));
    }

    @Test
    void loginLockedAfterFiveFailures() throws Exception {
        when(turnstileClient.verify(any(), any())).thenReturn(true);
        register("locked@b.com", "secret1");
        for (int i = 0; i < 5; i++) {
            mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                            .content("{\"email\":\"locked@b.com\",\"password\":\"wrongpass\",\"captchaToken\":\"any-token\"}"))
                    .andExpect(status().isUnauthorized());
        }

        // Lần 6 — dù đúng mật khẩu, đã khoá cứng, không cho thử nữa.
        mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody("locked@b.com", "secret1")))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.code").value("TOO_MANY_ATTEMPTS"))
                .andExpect(header().string("Retry-After", "900"));
    }

    @Test
    void successfulLoginResetsFailureCounterBelowCaptchaThreshold() throws Exception {
        register("reset@b.com", "secret1");

        mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody("reset@b.com", "wrongpass")))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody("reset@b.com", "secret1")))
                .andExpect(status().isOk());

        // Sau khi đăng nhập đúng, bộ đếm về 0 — sai tiếp 2 lần (dưới ngưỡng CAPTCHA) vẫn không bị bắt captcha.
        for (int i = 0; i < 2; i++) {
            mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                            .content(loginBody("reset@b.com", "wrongpass")))
                    .andExpect(status().isUnauthorized());
        }
        mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody("reset@b.com", "secret1")))
                .andExpect(status().isOk());
    }
}
