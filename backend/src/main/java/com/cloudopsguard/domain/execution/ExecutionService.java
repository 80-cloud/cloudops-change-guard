package com.cloudopsguard.domain.execution;

import com.cloudopsguard.common.exception.NotFoundException;
import com.cloudopsguard.domain.approval.ApprovalFlowMatrix;
import com.cloudopsguard.domain.approval.ApprovalRequirement;
import com.cloudopsguard.domain.changerequest.ChangeRequest;
import com.cloudopsguard.domain.common.RiskLevel;
import com.cloudopsguard.domain.execution.dto.CreateHealthCheck;
import com.cloudopsguard.domain.execution.dto.ExecutionResponse;
import com.cloudopsguard.domain.execution.dto.HealthCheckResponse;
import com.cloudopsguard.domain.execution.dto.PreCheckResponse;
import com.cloudopsguard.domain.execution.dto.RecordExecutionResult;
import com.cloudopsguard.security.AppUserPrincipal;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 実施前チェック・実施後ヘルスチェック・実施記録(execution)のユースケース（Phase 4）。
 * 閲覧権限（所有/ロール・IDOR）は呼び出し側が {@code ChangeRequestService.getViewable} で先に担保する。
 * 状態遷移（START/COMPLETE 等）は {@code ChangeRequestService} が担当し、ここは記録と判定のみ。
 */
@Service
public class ExecutionService {

    private final PreExecutionCheckRepository preCheckRepository;
    private final PostExecutionHealthCheckRepository healthCheckRepository;
    private final ExecutionRepository executionRepository;
    private final ChecklistCatalog checklistCatalog;
    private final ApprovalFlowMatrix approvalFlowMatrix;
    private final MonitoringStatusProvider monitoringStatusProvider;

    public ExecutionService(PreExecutionCheckRepository preCheckRepository,
                            PostExecutionHealthCheckRepository healthCheckRepository,
                            ExecutionRepository executionRepository,
                            ChecklistCatalog checklistCatalog,
                            ApprovalFlowMatrix approvalFlowMatrix,
                            MonitoringStatusProvider monitoringStatusProvider) {
        this.preCheckRepository = preCheckRepository;
        this.healthCheckRepository = healthCheckRepository;
        this.executionRepository = executionRepository;
        this.checklistCatalog = checklistCatalog;
        this.approvalFlowMatrix = approvalFlowMatrix;
        this.monitoringStatusProvider = monitoringStatusProvider;
    }

    // ---- 実施前チェック ----

    /**
     * SCHEDULE 時に実施前チェックを CR ごとに生成する（冪等）。
     * is_required は env×risk のマトリクス（requirePreChecksComplete）で決まる。
     * dev など対象外セルでは全項目 required=false で生成され、START ゲートは素通りする。
     */
    @Transactional
    public void instantiatePreChecks(ChangeRequest cr) {
        if (preCheckRepository.existsByChangeRequestId(cr.getId())) {
            return;   // 既に生成済み（再 SCHEDULE でも重複させない）
        }
        ApprovalRequirement req = approvalFlowMatrix.requirementFor(
                cr.getTargetEnvironment(), riskLevelOf(cr));
        for (ChecklistCatalog.PreCheckDefault def : checklistCatalog.preChecks()) {
            PreExecutionCheck c = new PreExecutionCheck();
            c.setChangeRequestId(cr.getId());
            c.setCheckType(def.checkType());
            c.setRequired(req.requirePreChecksComplete() && def.requiredInProd());
            c.setCompleted(false);
            preCheckRepository.save(c);
        }
    }

    /**
     * START ゲート判定（A-7）：env×risk が対象なら必須 pre-check が全完了か。
     * 対象外（dev 等・requirePreChecksComplete=false）は常に true。例外送出・409 整形は呼び出し側。
     */
    @Transactional(readOnly = true)
    public boolean requiredPreChecksComplete(ChangeRequest cr) {
        ApprovalRequirement req = approvalFlowMatrix.requirementFor(
                cr.getTargetEnvironment(), riskLevelOf(cr));
        if (!req.requirePreChecksComplete()) {
            return true;
        }
        return preCheckRepository.findByChangeRequestIdOrderByIdAsc(cr.getId()).stream()
                .filter(PreExecutionCheck::isRequired)
                .allMatch(PreExecutionCheck::isCompleted);
    }

    @Transactional(readOnly = true)
    public List<PreCheckResponse> listPreChecks(Long changeRequestId) {
        return preCheckRepository.findByChangeRequestIdOrderByIdAsc(changeRequestId).stream()
                .map(PreCheckResponse::from)
                .toList();
    }

    /** チェック完了（OPERATOR）。当該 CR に属さない checkId は 404。完了済みは冪等に無変更で返す。 */
    @Transactional
    public PreCheckResponse completePreCheck(AppUserPrincipal actor, Long changeRequestId, Long checkId) {
        PreExecutionCheck c = preCheckRepository.findByIdAndChangeRequestId(checkId, changeRequestId)
                .orElseThrow(NotFoundException::new);
        if (!c.isCompleted()) {
            c.setCompleted(true);
            c.setCompletedBy(actor.userId());
            c.setCompletedAt(OffsetDateTime.now());
            preCheckRepository.save(c);
        }
        return PreCheckResponse.from(c);
    }

    // ---- 実施後ヘルスチェック ----

    @Transactional(readOnly = true)
    public List<HealthCheckResponse> listHealthChecks(Long changeRequestId) {
        return healthCheckRepository.findByChangeRequestIdOrderByRecordedAtAsc(changeRequestId).stream()
                .map(HealthCheckResponse::from)
                .toList();
    }

    /** ヘルスチェック記録（OPERATOR・追記専用）。 */
    @Transactional
    public HealthCheckResponse recordHealthCheck(AppUserPrincipal actor, Long changeRequestId,
                                                 CreateHealthCheck req) {
        PostExecutionHealthCheck h = new PostExecutionHealthCheck();
        h.setChangeRequestId(changeRequestId);
        h.setCheckItem(req.checkItem());
        h.setResult(monitoringStatusProvider.resolveResult(req.monitoringRef(), req.result()));
        h.setNote(req.note());
        h.setRecordedBy(actor.userId());
        return HealthCheckResponse.from(healthCheckRepository.save(h));
    }

    /**
     * service_health_confirmed の導出（決定3）：requiredForCompletion の各項目について、
     * 最新の記録結果が HEALTHY なら true。1つでも未記録/WARNING/UNHEALTHY なら false。
     * requiredForCompletion が空の設定なら常に true（設定上のオプトアウト）。
     */
    @Transactional(readOnly = true)
    public boolean serviceHealthConfirmed(Long changeRequestId) {
        Set<HealthCheckItem> required = checklistCatalog.requiredForCompletion();
        if (required.isEmpty()) {
            return true;
        }
        Map<HealthCheckItem, HealthResult> latest = new EnumMap<>(HealthCheckItem.class);
        for (PostExecutionHealthCheck h :
                healthCheckRepository.findByChangeRequestIdOrderByRecordedAtAsc(changeRequestId)) {
            latest.put(h.getCheckItem(), h.getResult());   // 後勝ち＝最新
        }
        for (HealthCheckItem item : required) {
            if (latest.get(item) != HealthResult.HEALTHY) {
                return false;
            }
        }
        return true;
    }

    // ---- 実施記録(execution) ----

    /** START 時に実施記録を1行開始する（operator_id・started_at）。CR あたり1行前提。 */
    @Transactional
    public void startExecution(ChangeRequest cr, AppUserPrincipal actor) {
        Execution e = new Execution();
        e.setChangeRequestId(cr.getId());
        e.setOperatorId(actor.userId());
        executionRepository.save(e);
    }

    /** 実行結果（IaC 適用結果）を最新 execution に設定する（決定A）。実施未開始は 404。 */
    @Transactional
    public ExecutionResponse recordExecutionResult(Long changeRequestId, RecordExecutionResult req) {
        Execution e = latestOrThrow(changeRequestId);
        e.setIacApplyResult(req.iacApplyResult());
        return ExecutionResponse.from(executionRepository.save(e));
    }

    /** COMPLETE 可否判定（A-10）：iac_apply_result=SUCCESS かつ service_health_confirmed の導出が true。 */
    @Transactional(readOnly = true)
    public boolean canComplete(ChangeRequest cr) {
        Execution e = executionRepository
                .findTopByChangeRequestIdOrderByStartedAtDesc(cr.getId()).orElse(null);
        if (e == null || e.getIacApplyResult() != IacApplyResult.SUCCESS) {
            return false;
        }
        return serviceHealthConfirmed(cr.getId());
    }

    /** COMPLETE 確定：service_health_confirmed=true・finished_at を記録（canComplete 成立後に呼ぶ）。 */
    @Transactional
    public void markCompleted(ChangeRequest cr) {
        Execution e = latestOrThrow(cr.getId());
        e.setServiceHealthConfirmed(true);
        e.setFinishedAt(OffsetDateTime.now());
        executionRepository.save(e);
    }

    /** FAIL 確定：finished_at を記録。 */
    @Transactional
    public void markFailed(ChangeRequest cr) {
        executionRepository.findTopByChangeRequestIdOrderByStartedAtDesc(cr.getId())
                .ifPresent(e -> {
                    e.setFinishedAt(OffsetDateTime.now());
                    executionRepository.save(e);
                });
    }

    /** ROLLBACK 記録：rollback_performed=true・rollback_note。 */
    @Transactional
    public void recordRollback(ChangeRequest cr, String note) {
        executionRepository.findTopByChangeRequestIdOrderByStartedAtDesc(cr.getId())
                .ifPresent(e -> {
                    e.setRollbackPerformed(true);
                    e.setRollbackNote(note);
                    executionRepository.save(e);
                });
    }

    /** 最新の実施記録の応答（詳細集約用）。未実施なら null。 */
    @Transactional(readOnly = true)
    public ExecutionResponse latestExecution(Long changeRequestId) {
        return executionRepository.findTopByChangeRequestIdOrderByStartedAtDesc(changeRequestId)
                .map(ExecutionResponse::from)
                .orElse(null);
    }

    private Execution latestOrThrow(Long changeRequestId) {
        return executionRepository.findTopByChangeRequestIdOrderByStartedAtDesc(changeRequestId)
                .orElseThrow(NotFoundException::new);
    }

    private RiskLevel riskLevelOf(ChangeRequest cr) {
        return cr.getRiskLevel() != null ? cr.getRiskLevel() : RiskLevel.LOW;
    }
}
