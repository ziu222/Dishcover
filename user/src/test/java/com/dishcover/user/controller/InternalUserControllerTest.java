package com.dishcover.user.controller;

import com.dishcover.user.entity.User;
import com.dishcover.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class InternalUserControllerTest {

    @Autowired
    MockMvc mvc;
    @Autowired
    UserRepository userRepository;

    @Test
    void correctSecretReturnsUser() throws Exception {
        User user = userRepository.save(new User("noti-" + System.nanoTime() + "@test.com", "hash", "Test User"));

        mvc.perform(get("/internal/users/{id}", user.getId())
                        .header("X-Internal-Secret", "test-internal-secret"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(user.getId()))
                .andExpect(jsonPath("$.email").value(user.getEmail()));
    }

    @Test
    void wrongSecretReturns401() throws Exception {
        mvc.perform(get("/internal/users/{id}", 1L).header("X-Internal-Secret", "sai-secret"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void missingUserReturns404() throws Exception {
        mvc.perform(get("/internal/users/{id}", 999999L)
                        .header("X-Internal-Secret", "test-internal-secret"))
                .andExpect(status().isNotFound());
    }
}
