package com.dishcover.recipe.config;

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
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Khác Inventory: đọc công thức (GET) là FREE, không cần JWT (CLAUDE.md mục 8) —
 * chỉ ghi (POST/PATCH/DELETE) mới bắt buộc token hợp lệ (specs/recipe-service.md mục 3.2/5).
 */
@Configuration
@EnableConfigurationProperties(JwtProperties.class)
public class SecurityConfig {

    /**
     * @param props cấu hình JWT nạp từ application.yml
     * @return service dùng để tạo/xác thực JWT, dùng chung qua {@code common/security}
     */
    @Bean
    JwtService jwtService(JwtProperties props) {
        return new JwtService(props.secret(), props.expirationMinutes());
    }

    /**
     * @param jwtService service xác thực JWT
     * @return filter đọc JWT từ header Authorization và thiết lập authentication vào SecurityContext
     */
    @Bean
    JwtAuthFilter jwtAuthFilter(JwtService jwtService) {
        return new JwtAuthFilter(jwtService);
    }

    /**
     * Khai báo chuỗi filter bảo mật: tắt CSRF (API stateless), cho phép công khai health check,
     * Swagger UI và GET {@code /recipes/**}; mọi request khác bắt buộc JWT hợp lệ.
     *
     * @param http            builder cấu hình HTTP security của Spring Security
     * @param jwtAuthFilter   filter xác thực JWT được chèn trước {@code UsernamePasswordAuthenticationFilter}
     * @return chuỗi filter bảo mật đã cấu hình
     * @throws Exception nếu cấu hình HttpSecurity thất bại
     */
    @Bean
    SecurityFilterChain filterChain(HttpSecurity http, JwtAuthFilter jwtAuthFilter) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/actuator/health").permitAll()
                        .requestMatchers("/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/recipes/**").permitAll()
                        .anyRequest().authenticated())
                .exceptionHandling(e -> e.authenticationEntryPoint(
                        (req, res, ex) -> res.sendError(HttpStatus.UNAUTHORIZED.value())))
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
