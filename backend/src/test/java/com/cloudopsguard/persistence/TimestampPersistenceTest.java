package com.cloudopsguard.persistence;

import com.cloudopsguard.domain.changerequest.ChangeRequest;
import com.cloudopsguard.domain.common.ChangeRequestStatus;
import com.cloudopsguard.domain.common.Role;
import com.cloudopsguard.domain.user.User;
import com.cloudopsguard.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * T-8：時刻の自動設定を「実際に永続化したものを読み戻して」検証する（共通設計方針）。
 * モックでは初期化漏れを見逃すため、実 DB へ書き込み → 読み戻しで確認する。
 * 楽観ロックの初期 version も併せて確認する。
 */
class TimestampPersistenceTest extends AbstractIntegrationTest {

    @Test
    void userTimestampsArePersisted() {
        User created = createUser("req", Role.REQUESTER);

        User reloaded = userRepository.findById(created.getId()).orElseThrow();
        assertThat(reloaded.getCreatedAt()).isNotNull();
        assertThat(reloaded.getUpdatedAt()).isNotNull();
    }

    @Test
    void changeRequestTimestampsAndVersionArePersisted() {
        User requester = createUser("req", Role.REQUESTER);
        ChangeRequest cr = createChangeRequest(requester, ChangeRequestStatus.DRAFT);

        ChangeRequest reloaded = changeRequestRepository.findById(cr.getId()).orElseThrow();
        assertThat(reloaded.getCreatedAt()).isNotNull();
        assertThat(reloaded.getUpdatedAt()).isNotNull();
        assertThat(reloaded.getVersion()).isZero();   // @Version の初期値
    }
}
