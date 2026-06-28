package com.cloudopsguard.execution;

import com.cloudopsguard.domain.changerequest.ChangeRequest;
import com.cloudopsguard.domain.common.ChangeRequestStatus;
import com.cloudopsguard.domain.common.Environment;
import com.cloudopsguard.domain.common.RiskLevel;
import com.cloudopsguard.domain.common.Role;
import com.cloudopsguard.domain.execution.ExecutionService;
import com.cloudopsguard.domain.execution.HealthCheckItem;
import com.cloudopsguard.domain.execution.HealthResult;
import com.cloudopsguard.domain.execution.dto.CreateHealthCheck;
import com.cloudopsguard.domain.execution.dto.HealthCheckResponse;
import com.cloudopsguard.domain.execution.dto.PreCheckResponse;
import com.cloudopsguard.domain.user.User;
import com.cloudopsguard.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** Inc 2：pre-check 生成・完了・health 記録の結合テスト。 */
class ExecutionRecordTest extends AbstractIntegrationTest {

    @Autowired ExecutionService executionService;

    private ChangeRequest scheduledCr(Environment env, RiskLevel risk) {
        User requester = createUser("req", Role.REQUESTER);
        ChangeRequest cr = createChangeRequest(requester, ChangeRequestStatus.SCHEDULED);
        cr.setTargetEnvironment(env);
        cr.setRiskLevel(risk);
        return changeRequestRepository.save(cr);
    }

    @Test
    void 本番CRはpre_checkが全required() {
        ChangeRequest cr = scheduledCr(Environment.PRODUCTION, RiskLevel.HIGH);
        executionService.instantiatePreChecks(cr);

        List<PreCheckResponse> checks = executionService.listPreChecks(cr.getId());
        assertThat(checks).hasSize(7);
        assertThat(checks).allMatch(PreCheckResponse::required);
    }

    @Test
    void 開発CRはpre_checkがrequiredにならない() {
        ChangeRequest cr = scheduledCr(Environment.DEVELOPMENT, RiskLevel.LOW);
        executionService.instantiatePreChecks(cr);

        List<PreCheckResponse> checks = executionService.listPreChecks(cr.getId());
        assertThat(checks).hasSize(7);
        assertThat(checks).noneMatch(PreCheckResponse::required);
    }

    @Test
    void instantiateは冪等で重複生成しない() {
        ChangeRequest cr = scheduledCr(Environment.PRODUCTION, RiskLevel.HIGH);
        executionService.instantiatePreChecks(cr);
        executionService.instantiatePreChecks(cr);
        assertThat(executionService.listPreChecks(cr.getId())).hasSize(7);
    }

    @Test
    void pre_checkを完了できる() {
        User operator = createUser("op", Role.OPERATOR);
        ChangeRequest cr = scheduledCr(Environment.PRODUCTION, RiskLevel.HIGH);
        executionService.instantiatePreChecks(cr);
        Long checkId = executionService.listPreChecks(cr.getId()).get(0).id();

        PreCheckResponse done = executionService.completePreCheck(principal(operator), cr.getId(), checkId);

        assertThat(done.completed()).isTrue();
        assertThat(done.completedBy()).isEqualTo(operator.getId());
    }

    @Test
    void health_checkを記録して一覧できる() {
        User operator = createUser("op", Role.OPERATOR);
        ChangeRequest cr = scheduledCr(Environment.STAGING, RiskLevel.MEDIUM);

        HealthCheckResponse saved = executionService.recordHealthCheck(principal(operator), cr.getId(),
                new CreateHealthCheck(HealthCheckItem.HTTP_HEALTH, HealthResult.HEALTHY, "2xx"));

        assertThat(saved.result()).isEqualTo(HealthResult.HEALTHY);
        assertThat(executionService.listHealthChecks(cr.getId())).hasSize(1);
    }
}
