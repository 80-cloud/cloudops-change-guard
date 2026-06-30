package com.cloudopsguard.execution;

import com.cloudopsguard.domain.execution.HealthResult;
import com.cloudopsguard.domain.execution.MockMonitoringStatusProvider;
import com.cloudopsguard.domain.execution.MonitoringStatusProvider;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 監視取り込み口の契約テスト（実 AWS 不要）。Mock は常に empty＝resolveResult は手入力 result を返す（現挙動維持）。
 * 取得できる stub では monitoringRef を優先する。
 */
class MockMonitoringStatusProviderTest {

    private final MonitoringStatusProvider mock = new MockMonitoringStatusProvider();

    @Test
    void Mockはfetchで常にemptyを返す() {
        assertThat(mock.fetchAlarmHealth("alarm-prefix")).isEmpty();
    }

    @Test
    void monitoringRefがnullや空なら手入力resultを使う() {
        assertThat(mock.resolveResult(null, HealthResult.WARNING)).isEqualTo(HealthResult.WARNING);
        assertThat(mock.resolveResult("  ", HealthResult.UNHEALTHY)).isEqualTo(HealthResult.UNHEALTHY);
    }

    @Test
    void Mockはソースを持たないのでrefがあっても手入力resultにfallback() {
        assertThat(mock.resolveResult("alarm-x", HealthResult.WARNING)).isEqualTo(HealthResult.WARNING);
    }

    @Test
    void 取得できるProviderではref優先_取得できなければfallback() {
        MonitoringStatusProvider stub = ref -> "ok".equals(ref) ? Optional.of(HealthResult.HEALTHY) : Optional.empty();
        assertThat(stub.resolveResult("ok", HealthResult.UNHEALTHY)).isEqualTo(HealthResult.HEALTHY);
        assertThat(stub.resolveResult("missing", HealthResult.UNHEALTHY)).isEqualTo(HealthResult.UNHEALTHY);
        assertThat(stub.resolveResult(null, HealthResult.WARNING)).isEqualTo(HealthResult.WARNING);
    }
}
