package com.cloudopsguard.config;

import com.cloudopsguard.common.ApiError;
import com.cloudopsguard.observability.MdcLoggingFilter;
import com.cloudopsguard.security.JwtCookieAuthFilter;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.io.IOException;

/**
 * セキュリティの中核。RBAC + ステートレス認可。
 *
 * <ul>
 *   <li>セッションは持たない（STATELESS）。認証は JWT in HttpOnly Cookie（{@link JwtCookieAuthFilter}）。</li>
 *   <li>CSRF はステートレス + SameSite=Strict 前提で無効化。</li>
 *   <li>ロール別の細かい認可はメソッドレベル（{@code @PreAuthorize}）で行う（{@link EnableMethodSecurity}）。</li>
 *   <li>未認証は 401・認可不可は 403 を統一 JSON で返す。</li>
 * </ul>
 */
@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    private final ObjectMapper objectMapper;

    public SecurityConfig(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http,
                                           JwtCookieAuthFilter jwtFilter,
                                           MdcLoggingFilter mdcFilter) throws Exception {
        http
            // B3：CorsConfig を効かせるため明示。これが無いと preflight が落ちる。
            .cors(Customizer.withDefaults())
            .csrf(csrf -> csrf.disable())
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(
                        "/actuator/health", "/actuator/health/**", "/actuator/info",
                        "/api/v1/auth/login", "/api/v1/auth/refresh").permitAll()
                .anyRequest().authenticated())
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint((req, res, e) ->
                        writeError(res, HttpStatus.UNAUTHORIZED, "UNAUTHENTICATED", "ログインが必要です"))
                .accessDeniedHandler((req, res, e) ->
                        writeError(res, HttpStatus.FORBIDDEN, "FORBIDDEN", "この操作を行う権限がありません")))
            .httpBasic(basic -> basic.disable())
            .formLogin(form -> form.disable())
            .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
            // JWT の後段で MDC に requestId/userId を載せる（SecurityContext 確立後）。
            .addFilterAfter(mdcFilter, JwtCookieAuthFilter.class);
        return http.build();
    }

    private void writeError(HttpServletResponse res, HttpStatus status, String code, String message)
            throws IOException {
        res.setStatus(status.value());
        res.setContentType(MediaType.APPLICATION_JSON_VALUE);
        res.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(res.getWriter(), ApiError.of(code, message));
    }

    /** パスワードは bcrypt でハッシュ化（コスト 10 以上＝BCrypt 既定）。 */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
