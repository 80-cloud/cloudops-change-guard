package com.cloudopsguard.domain.auth;

import com.cloudopsguard.common.ApiResponse;
import com.cloudopsguard.domain.auth.dto.LoginRequest;
import com.cloudopsguard.domain.auth.dto.MeResponse;
import com.cloudopsguard.domain.user.User;
import com.cloudopsguard.domain.user.UserRepository;
import com.cloudopsguard.security.AppUserPrincipal;
import com.cloudopsguard.security.AuthCookies;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * 認証 API（/api/v1/auth）。トークンは HttpOnly Cookie で受け渡し、レスポンス body には載せない。
 */
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;
    private final AuthCookies cookies;
    private final UserRepository userRepository;

    public AuthController(AuthService authService, AuthCookies cookies, UserRepository userRepository) {
        this.authService = authService;
        this.cookies = cookies;
        this.userRepository = userRepository;
    }

    /** ログイン。access(短命)＋refresh を HttpOnly Cookie で発行する。 */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<MeResponse>> login(@Valid @RequestBody LoginRequest request) {
        AuthService.LoginResult result = authService.login(request.username(), request.password());
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookies.access(result.accessToken()).toString())
                .header(HttpHeaders.SET_COOKIE, cookies.refresh(result.refreshToken()).toString())
                .body(ApiResponse.of(MeResponse.from(result.user())));
    }

    /** リフレッシュ（access 再発行 + refresh rotation）。 */
    @PostMapping("/refresh")
    public ResponseEntity<Void> refresh(
            @CookieValue(name = AuthCookies.REFRESH, required = false) String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new InvalidRefreshTokenException("missing refresh token");
        }
        AuthService.RefreshResult result = authService.refresh(refreshToken);
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, cookies.access(result.accessToken()).toString())
                .header(HttpHeaders.SET_COOKIE, cookies.refresh(result.refreshToken()).toString())
                .build();
    }

    /** ログアウト（refresh 失効 + Cookie 削除）。 */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @CookieValue(name = AuthCookies.REFRESH, required = false) String refreshToken) {
        authService.logout(refreshToken);
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, cookies.clearAccess().toString())
                .header(HttpHeaders.SET_COOKIE, cookies.clearRefresh().toString())
                .build();
    }

    /** 認証確認（現在のログインユーザー）。未認証なら SecurityConfig が 401 を返す。 */
    @GetMapping("/me")
    public ApiResponse<MeResponse> me(@AuthenticationPrincipal AppUserPrincipal principal) {
        User user = userRepository.findById(principal.userId())
                .orElseThrow(() -> new IllegalStateException("authenticated user not found"));
        return ApiResponse.of(MeResponse.from(user));
    }
}
