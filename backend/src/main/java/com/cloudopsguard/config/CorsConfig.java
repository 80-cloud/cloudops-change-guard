package com.cloudopsguard.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * CORS 設定。フロント（Vite）は localhost:5177 固定（CLAUDE.md のポート規律）。
 * Cookie 認証のため {@code allowCredentials=true}。この場合オリジンに {@code "*"} は使えないため
 * 単一オリジンを明示する（SEC-11・ワイルドカード禁止）。許可 origin は env で外部化する。
 *
 * <p>注意（B3）：{@link SecurityConfig} 側で {@code http.cors(...)} を明示しないと本設定は効かない。
 */
@Configuration
public class CorsConfig {

    @Bean
    public CorsConfigurationSource corsConfigurationSource(
            @Value("${app.cors.allowed-origin:http://localhost:5177}") String allowedOrigin) {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of(allowedOrigin));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", config);
        return source;
    }
}
