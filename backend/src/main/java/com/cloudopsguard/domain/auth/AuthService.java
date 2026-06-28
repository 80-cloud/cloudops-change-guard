package com.cloudopsguard.domain.auth;

import com.cloudopsguard.domain.user.User;
import com.cloudopsguard.domain.user.UserRepository;
import com.cloudopsguard.security.JwtService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 認証フローのオーケストレーション。トークンの生成・rotation は専用サービスに委譲し、
 * ここは「誰を認証したか」を担う。
 */
@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder,
                       JwtService jwtService, RefreshTokenService refreshTokenService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.refreshTokenService = refreshTokenService;
    }

    /** ログイン。失敗は存在を漏らさない汎用エラー（BadCredentials）。 */
    @Transactional
    public LoginResult login(String username, String rawPassword) {
        User user = userRepository.findByUsername(username).orElseThrow(BadCredentialsException::new);
        if (!passwordEncoder.matches(rawPassword, user.getPasswordHash())) {
            throw new BadCredentialsException();
        }
        return issueSession(user);
    }

    /** リフレッシュ。rotation + reuse 検知は RefreshTokenService が担う。 */
    @Transactional
    public RefreshResult refresh(String rawRefreshToken) {
        RefreshTokenService.RotationResult rotation = refreshTokenService.rotate(rawRefreshToken);
        User user = userRepository.findById(rotation.userId())
                .orElseThrow(() -> new InvalidRefreshTokenException("user not found"));
        String access = jwtService.issueAccessToken(user.getId(), user.getUsername(), user.getRole());
        return new RefreshResult(access, rotation.newRawToken());
    }

    /** ログアウト（refresh 失効）。 */
    @Transactional
    public void logout(String rawRefreshToken) {
        if (rawRefreshToken != null && !rawRefreshToken.isBlank()) {
            refreshTokenService.revoke(rawRefreshToken);
        }
    }

    private LoginResult issueSession(User user) {
        String access = jwtService.issueAccessToken(user.getId(), user.getUsername(), user.getRole());
        String refresh = refreshTokenService.issue(user.getId());
        return new LoginResult(user, access, refresh);
    }

    public record LoginResult(User user, String accessToken, String refreshToken) {
    }

    public record RefreshResult(String accessToken, String refreshToken) {
    }
}
