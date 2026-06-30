package com.cloudopsguard.risk;

import com.cloudopsguard.domain.risk.IaCChangeProvider;
import com.cloudopsguard.domain.risk.MockIaCChangeProvider;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 取り込み口の契約テスト（実 AWS 不要）。Mock は常に empty＝resolveDiffText は手貼り fallback を返す（現挙動維持）。
 * sourceRef があり取得できる stub では取得テキストを優先する。
 */
class MockIaCChangeProviderTest {

    private final IaCChangeProvider mock = new MockIaCChangeProvider();

    @Test
    void Mockはfetchで常にemptyを返す() {
        assertThat(mock.fetchPlanText("s3://bucket/plan.txt")).isEmpty();
    }

    @Test
    void sourceRefがnullや空ならfallbackをそのまま使う() {
        assertThat(mock.resolveDiffText(null, "manual")).isEqualTo("manual");
        assertThat(mock.resolveDiffText("  ", "manual")).isEqualTo("manual");
    }

    @Test
    void Mockはソースを持たないのでsourceRefがあってもfallback() {
        assertThat(mock.resolveDiffText("s3://bucket/plan.txt", "manual")).isEqualTo("manual");
    }

    @Test
    void 取得できるProviderではsourceRef優先_取得できなければfallback() {
        IaCChangeProvider stub = ref -> "s3://has".equals(ref) ? Optional.of("REAL-PLAN") : Optional.empty();
        assertThat(stub.resolveDiffText("s3://has", "manual")).isEqualTo("REAL-PLAN");
        assertThat(stub.resolveDiffText("s3://missing", "manual")).isEqualTo("manual");
        assertThat(stub.resolveDiffText(null, "manual")).isEqualTo("manual");
    }
}
