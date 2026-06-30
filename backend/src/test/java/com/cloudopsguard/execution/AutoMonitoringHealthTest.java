package com.cloudopsguard.execution;

import com.cloudopsguard.domain.changerequest.ChangeRequest;
import com.cloudopsguard.domain.common.ChangeRequestStatus;
import com.cloudopsguard.domain.common.Role;
import com.cloudopsguard.domain.execution.ExecutionService;
import com.cloudopsguard.domain.execution.HealthCheckItem;
import com.cloudopsguard.domain.execution.HealthResult;
import com.cloudopsguard.domain.execution.MonitoringStatusProvider;
import com.cloudopsguard.domain.execution.dto.CreateHealthCheck;
import com.cloudopsguard.domain.execution.dto.HealthCheckResponse;
import com.cloudopsguard.domain.user.User;
import com.cloudopsguard.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * recordHealthCheck 時に monitoringRef があれば MonitoringStatusProvider 解決の結果を保存する配線テスト。
 * Provider は @MockBean で差し替え（実 AWS 不要）。
 */
class AutoMonitoringHealthTest extends AbstractIntegrationTest {

    @MockBean
    private MonitoringStatusProvider monitoringStatusProvider;

    @Autowired
    private ExecutionService executionService;

    @Test
    void monitoringRefありならProvider解決のヘルスを保存する() {
        when(monitoringStatusProvider.resolveResult(anyString(), any())).thenReturn(HealthResult.HEALTHY);
        User operator = createUser("opMon", Role.OPERATOR);
        ChangeRequest cr = createChangeRequest(operator, ChangeRequestStatus.DRAFT);

        HealthCheckResponse resp = executionService.recordHealthCheck(
                principal(operator), cr.getId(),
                new CreateHealthCheck(HealthCheckItem.CW_ALARM, HealthResult.UNHEALTHY, "manual", "alarm-prefix"));

        assertThat(resp.result()).isEqualTo(HealthResult.HEALTHY);
        verify(monitoringStatusProvider).resolveResult("alarm-prefix", HealthResult.UNHEALTHY);
    }
}
