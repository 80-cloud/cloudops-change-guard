package com.cloudopsguard.auth;

import com.cloudopsguard.domain.auth.InvalidRefreshTokenException;
import com.cloudopsguard.domain.auth.RefreshTokenService;
import com.cloudopsguard.domain.common.Role;
import com.cloudopsguard.domain.user.User;
import com.cloudopsguard.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * T-9：refresh トークンの rotation と reuse 検知（SEC-7）。
 * 各 rotate は独立トランザクションでコミットされるため、状態は呼び出しをまたいで残る。
 */
class RefreshTokenRotationTest extends AbstractIntegrationTest {

    @Autowired
    RefreshTokenService refreshTokenService;

    @Test
    void rotateIssuesNewTokenAndRevokesOld() {
        User user = createUser("u", Role.REQUESTER);

        String t1 = refreshTokenService.issue(user.getId());
        RefreshTokenService.RotationResult r1 = refreshTokenService.rotate(t1);

        assertThat(r1.newRawToken()).isNotEqualTo(t1);
        // 新トークンは有効（再 rotate できる）。
        RefreshTokenService.RotationResult r2 = refreshTokenService.rotate(r1.newRawToken());
        assertThat(r2.newRawToken()).isNotBlank();
    }

    @Test
    void reuseOfRevokedTokenIsDetectedAndAllTokensRevoked() {
        User user = createUser("u", Role.REQUESTER);

        String t1 = refreshTokenService.issue(user.getId());
        RefreshTokenService.RotationResult r1 = refreshTokenService.rotate(t1); // t1 は失効

        // 失効済み t1 の再提示＝盗用 → 例外、かつ当該ユーザーの全トークンを失効。
        assertThatThrownBy(() -> refreshTokenService.rotate(t1))
                .isInstanceOf(InvalidRefreshTokenException.class);

        // reuse 検知後は、直近の有効トークン（r1）も失効しているため使えない。
        assertThatThrownBy(() -> refreshTokenService.rotate(r1.newRawToken()))
                .isInstanceOf(InvalidRefreshTokenException.class);
    }

    @Test
    void unknownTokenIsRejected() {
        assertThatThrownBy(() -> refreshTokenService.rotate("not-a-real-token"))
                .isInstanceOf(InvalidRefreshTokenException.class);
    }
}
