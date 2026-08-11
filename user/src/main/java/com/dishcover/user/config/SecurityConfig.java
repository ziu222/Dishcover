package com.dishcover.user.config;

import com.dishcover.common.security.JwtAuthFilter;
import com.dishcover.common.security.JwtService;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Cấu hình Spring Security cho User Service: xác thực JWT stateless (không session),
 * công khai /auth/register + /auth/login + tài liệu Swagger, còn lại yêu cầu JWT hợp lệ.
 */
@Configuration
@EnableConfigurationProperties(JwtProperties.class)
public class SecurityConfig {

    @Bean
    JwtService jwtService(JwtProperties props) {
        return new JwtService(props.secret(), props.expirationMinutes());
    }

    @Bean
    JwtAuthFilter jwtAuthFilter(JwtService jwtService) {
        return new JwtAuthFilter(jwtService);
    }

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http, JwtAuthFilter jwtAuthFilter) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // Công khai: đăng ký, đăng nhập, health. Còn lại cần JWT hợp lệ.
                        // (Endpoint /internal đổi plan để dành cho luồng Payment mục 10 — chưa làm,
                        //  vì Gateway StripPrefix có thể để lộ /internal ra ngoài, cần chặn ở Gateway trước.)
                        .requestMatchers(HttpMethod.POST, "/auth/register", "/auth/login").permitAll()
                        .requestMatchers("/actuator/health").permitAll()
                        // Swagger UI + OpenAPI docs công khai (chỉ tài liệu, không lộ dữ liệu)
                        .requestMatchers("/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**").permitAll()
                        .anyRequest().authenticated())
                // Chưa xác thực → 401 (mặc định của Spring Security là 403); frontend phân biệt
                // 401 (đăng nhập lại) với 402/403 (paywall/không đủ quyền).
                .exceptionHandling(e -> e.authenticationEntryPoint(
                        (req, res, ex) -> res.sendError(HttpStatus.UNAUTHORIZED.value())))
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
