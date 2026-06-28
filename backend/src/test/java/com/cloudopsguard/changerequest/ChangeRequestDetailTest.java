package com.cloudopsguard.changerequest;

import com.cloudopsguard.domain.changerequest.ChangeRequest;
import com.cloudopsguard.domain.changerequest.ChangeRequestService;
import com.cloudopsguard.domain.changerequest.dto.ChangeRequestDetailResponse;
import com.cloudopsguard.domain.common.ChangeRequestStatus;
import com.cloudopsguard.domain.common.Environment;
import com.cloudopsguard.domain.common.RiskLevel;
import com.cloudopsguard.domain.common.Role;
import com.cloudopsguard.domain.execution.ExecutionService;
import com.cloudopsguard.domain.execution.HealthCheckItem;
import com.cloudopsguard.domain.execution.HealthResult;
import com.cloudopsguard.domain.execution.dto.CreateHealthCheck;
import com.cloudopsguard.domain.user.User;
import com.cloudopsguard.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;

/** Inc 5：詳細応答の集約（pre-check / health / 最新 execution）。 */
class ChangeRequestDetailTest extends AbstractIntegrationTest {

    @Autowired ChangeRequestService changeRequestService;
    @Autowired ExecutionService executionService;

    @Test
    void 詳細にpreCheckとhealthと最新executionが含まれる() {
        User requester = createUser("req", Role.REQUESTER);
        User operator = createUser("op", Role.OPERATOR);
        ChangeRequest cr = createChangeRequest(requester, ChangeRequestStatus.SCHEDULED);
        cr.setTargetEnvironment(Environment.PRODUCTION);
        cr.setRiskLevel(RiskLevel.HIGH);
        cr = changeRequestRepository.save(cr);
        executionService.instantiatePreChecks(cr);
        executionService.startExecution(cr, principal(operator));
        executionService.recordHealthCheck(principal(operator), cr.getId(),
                new CreateHealthCheck(HealthCheckItem.HTTP_HEALTH, HealthResult.HEALTHY, null));

        ChangeRequestDetailResponse detail = changeRequestService.getDetail(principal(operator), cr.getId());

        assertThat(detail.changeRequest().id()).isEqualTo(cr.getId());
        assertThat(detail.preChecks()).hasSize(7);
        assertThat(detail.healthChecks()).hasSize(1);
        assertThat(detail.execution()).isNotNull();
    }

    @Test
    void 未実施なら詳細のexecutionはnullでpreCheckも空() {
        User requester = createUser("req", Role.REQUESTER);
        ChangeRequest cr = createChangeRequest(requester, ChangeRequestStatus.DRAFT);

        ChangeRequestDetailResponse detail = changeRequestService.getDetail(principal(requester), cr.getId());

        assertThat(detail.execution()).isNull();
        assertThat(detail.preChecks()).isEmpty();
        assertThat(detail.healthChecks()).isEmpty();
    }
}
