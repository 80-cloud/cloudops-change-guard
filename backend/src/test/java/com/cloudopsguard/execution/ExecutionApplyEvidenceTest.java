package com.cloudopsguard.execution;

import com.cloudopsguard.domain.audit.AuditLog;
import com.cloudopsguard.domain.changerequest.ChangeRequest;
import com.cloudopsguard.domain.common.AuditAction;
import com.cloudopsguard.domain.common.ChangeRequestStatus;
import com.cloudopsguard.domain.common.Role;
import com.cloudopsguard.domain.execution.ExecutionService;
import com.cloudopsguard.domain.execution.IacApplyResult;
import com.cloudopsguard.domain.execution.dto.ExecutionResponse;
import com.cloudopsguard.domain.execution.dto.RecordExecutionResult;
import com.cloudopsguard.domain.user.User;
import com.cloudopsguard.security.AppUserPrincipal;
import com.cloudopsguard.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** A-2b：apply 証跡（Run URL・plan 参照）の記録・監査・後方互換。ゲート不変は ExecutionCompleteTest が担保。 */
class ExecutionApplyEvidenceTest extends AbstractIntegrationTest {

    @Autowired ExecutionService executionService;

    private record Fixture(ChangeRequest cr, AppUserPrincipal op) {}

    private Fixture inProgress() {
        User requester = createUser("req", Role.REQUESTER);
        ChangeRequest cr = createChangeRequest(requester, ChangeRequestStatus.IN_PROGRESS);
        AppUserPrincipal op = principal(createUser("op", Role.OPERATOR));
        executionService.startExecution(cr, op);
        return new Fixture(cr, op);
    }

    @Test
    void apply証跡が永続化されレスポンスに出る() {
        Fixture f = inProgress();
        ExecutionResponse res = executionService.recordExecutionResult(f.op(), f.cr().getId(),
                new RecordExecutionResult(IacApplyResult.SUCCESS,
                        "https://ci.example/run/123", "plans/cr-1.txt"));

        assertThat(res.iacApplyResult()).isEqualTo(IacApplyResult.SUCCESS);
        assertThat(res.applyRunUrl()).isEqualTo("https://ci.example/run/123");
        assertThat(res.planSourceRef()).isEqualTo("plans/cr-1.txt");
    }

    @Test
    void 記録時にEXECUTION_RESULT_RECORD監査が残る() {
        Fixture f = inProgress();
        executionService.recordExecutionResult(f.op(), f.cr().getId(),
                new RecordExecutionResult(IacApplyResult.FAILED, null, null));

        List<AuditLog> logs =
                auditLogRepository.findByChangeRequestIdOrderByCreatedAtDesc(f.cr().getId());
        assertThat(logs).anyMatch(l -> l.getActionType() == AuditAction.EXECUTION_RESULT_RECORD
                && l.getActorId().equals(f.op().userId()));
    }

    @Test
    void 後方互換_1引数記録は証跡nullで動く() {
        Fixture f = inProgress();
        ExecutionResponse res = executionService.recordExecutionResult(f.op(), f.cr().getId(),
                new RecordExecutionResult(IacApplyResult.SUCCESS));

        assertThat(res.iacApplyResult()).isEqualTo(IacApplyResult.SUCCESS);
        assertThat(res.applyRunUrl()).isNull();
        assertThat(res.planSourceRef()).isNull();
    }
}
