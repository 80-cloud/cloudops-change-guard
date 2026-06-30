package com.cloudopsguard.execution;

import com.cloudopsguard.domain.execution.HealthResult;
import com.cloudopsguard.domain.execution.InstanceStatusProvider;
import com.cloudopsguard.domain.execution.MockInstanceStatusProvider;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * インスタンス状態取り込み口の契約テスト（実 AWS 不要）。Mock は常に empty＝resolveResult は手入力 result を返す。
 * 取得できる stub では instanceRef を優先する。
 */
class MockInstanceStatusProviderTest {

    private final InstanceStatusProvider mock = new MockInstanceStatusProvider();

    @Test
    void Mockはfetchで常にemptyを返す() {
        assertThat(mock.fetchInstanceHealth("i-123")).isEmpty();
    }

    @Test
    void instanceRefがnullや空なら手入力resultを使う() {
        assertThat(mock.resolveResult(null, HealthResult.WARNING)).isEqualTo(HealthResult.WARNING);
        assertThat(mock.resolveResult("  ", HealthResult.UNHEALTHY)).isEqualTo(HealthResult.UNHEALTHY);
    }

    @Test
    void Mockはソースを持たないのでrefがあっても手入力resultにfallback() {
        assertThat(mock.resolveResult("i-123", HealthResult.WARNING)).isEqualTo(HealthResult.WARNING);
    }

    @Test
    void 取得できるProviderではref優先_取得できなければfallback() {
        InstanceStatusProvider stub = ref -> "i-ok".equals(ref) ? Optional.of(HealthResult.HEALTHY) : Optional.empty();
        assertThat(stub.resolveResult("i-ok", HealthResult.UNHEALTHY)).isEqualTo(HealthResult.HEALTHY);
        assertThat(stub.resolveResult("i-missing", HealthResult.UNHEALTHY)).isEqualTo(HealthResult.UNHEALTHY);
        assertThat(stub.resolveResult(null, HealthResult.WARNING)).isEqualTo(HealthResult.WARNING);
    }
}
