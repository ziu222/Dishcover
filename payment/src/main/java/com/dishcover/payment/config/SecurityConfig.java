package com.dishcover.payment.config;

import com.dishcover.common.security.JwtAuthFilter;
import com.dishcover.common.security.JwtService;
import com.dishcover.common.security.RequiresPlanAspect;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Bảo mật Payment Service: xác thực JWT stateless, không session.
 *
 * <p>Khác 5 service trước ở một chỗ quan trọng: <b>endpoint nhận IPN phải để công khai</b>. Cổng
 * thanh toán gọi server-to-server, không mang JWT của người dùng — bắt xác thực ở đó thì IPN
 * không bao giờ vào được và gói PRO không bao giờ kích hoạt. Bù lại, IPN tự bảo vệ bằng chữ ký
 * HMAC-SHA512: request không verify được chữ ký sẽ bị loại ngay (CLAUDE.md mục 8/11).</p>
 */
@Configuration
@EnableConfigurationProperties({JwtProperties.class, VnpayProperties.class})
public class SecurityConfig {

    @Bean
    JwtService jwtService(JwtProperties props) {
        return new JwtService(props.secret(), props.expirationMinutes());
    }

    @Bean
    JwtAuthFilter jwtAuthFilter(JwtService jwtService) {
        return new JwtAuthFilter(jwtService);
    }

    /**
     * Aspect ở {@code common} không đánh {@code @Component} (service con không component-scan
     * package đó) nên mỗi service tự đăng ký — giống Inventory/Matching/RAG.
     */
    @Bean
    RequiresPlanAspect requiresPlanAspect() {
        return new RequiresPlanAspect();
    }

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http, JwtAuthFilter jwtAuthFilter) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/actuator/health").permitAll()
                        .requestMatchers("/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**").permitAll()
                        // IPN: cổng thanh toán gọi, không có JWT — bảo vệ bằng chữ ký, không bằng token.
                        .requestMatchers("/payments/vnpay/ipn").permitAll()
                        // Bảng giá gói để công khai: người chưa nâng cấp phải xem được giá mới quyết định mua.
                        .requestMatchers("/payments/plans").permitAll()
                        .anyRequest().authenticated())
                .exceptionHandling(e -> e.authenticationEntryPoint(
                        (req, res, ex) -> res.sendError(HttpStatus.UNAUTHORIZED.value())))
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
