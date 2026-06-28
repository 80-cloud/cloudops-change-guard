package com.cloudopsguard.security;

import com.cloudopsguard.config.JwtProperties;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

/**
 * 認証 Cookie（HttpOnly + Secure + SameSite=Strict）の組み立て。
 * 素の {@code jakarta.servlet.http.Cookie} では SameSite を設定できないため
 * {@link ResponseCookie} を使う（B3 の罠回避）。SameSite=Strict で CSRF を緩和する。
 */
@Component
public class AuthCookies {

    public static final String ACCESS = "access_token";
    public static final String REFRESH = "refresh_token";
    /** refresh は認証エンドポイントにのみ送れば十分（露出面を狭める）。 */
    private static final String REFRESH_PATH = "/api/v1/auth";

    private final JwtProperties props;

    public AuthCookies(JwtProperties props) {
        this.props = props;
    }

    public ResponseCookie access(String token) {
        return base(ACCESS, token, "/", props.accessTtlSeconds());
    }

    public ResponseCookie refresh(String token) {
        return base(REFRESH, token, REFRESH_PATH, props.refreshTtlSeconds());
    }

    public ResponseCookie clearAccess() {
        return base(ACCESS, "", "/", 0);
    }

    public ResponseCookie clearRefresh() {
        return base(REFRESH, "", REFRESH_PATH, 0);
    }

    private ResponseCookie base(String name, String value, String path, long maxAgeSeconds) {
        return ResponseCookie.from(name, value)
                .httpOnly(true)
                .secure(props.cookieSecure())
                .sameSite("Strict")
                .path(path)
                .maxAge(maxAgeSeconds)
                .build();
    }
}
