package com.cloudopsguard.domain.changerequest;

import com.cloudopsguard.common.exception.IllegalStateTransitionException;
import com.cloudopsguard.common.exception.NotFoundException;
import com.cloudopsguard.common.exception.PolicyBlockedException;
import com.cloudopsguard.common.exception.ValidationException;
import com.cloudopsguard.domain.approval.Approval;
import com.cloudopsguard.domain.approval.ApprovalFlowMatrix;
import com.cloudopsguard.domain.approval.ApprovalRepository;
import com.cloudopsguard.domain.approval.ApprovalRequirement;
import com.cloudopsguard.domain.audit.AuditService;
import com.cloudopsguard.domain.changerequest.ChangeRequestStateMachine.TransitionContext;
import com.cloudopsguard.domain.changerequest.dto.CreateChangeRequest;
import com.cloudopsguard.domain.changerequest.dto.TransitionRequest;
import com.cloudopsguard.domain.changerequest.dto.UpdateChangeRequest;
import com.cloudopsguard.domain.common.*;
import com.cloudopsguard.domain.execution.ExecutionService;
import com.cloudopsguard.domain.changerequest.dto.ChangeRequestDetailResponse;
import com.cloudopsguard.domain.changerequest.dto.ChangeRequestResponse;
import com.cloudopsguard.domain.policy.PolicyEffect;
import com.cloudopsguard.domain.policy.PolicyOutcome;
import com.cloudopsguard.domain.risk.AssessmentOutcome;
import com.cloudopsguard.domain.risk.BlockReason;
import com.cloudopsguard.domain.risk.RiskAssessmentService;
import com.cloudopsguard.domain.risk.RiskFinding;
import com.cloudopsguard.security.AppUserPrincipal;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * 変更申請のユースケース。Controller は業務ロジックを持たずここへ委譲する（M-3）。
 *
 * <p>遷移は必ず {@link ChangeRequestStateMachine#transition} 経由（status を直接 set しない・A-9）。
 * 遷移 + 付随レコード(承認/実施) + 監査ログを<b>同一トランザクション</b>で書く（B5・原子性）。
 * 認可はロール（@PreAuthorize）＋所有者検証（本サービス）の二重で強制し、IDOR は 404 で秘匿する。
 */
@Service
public class ChangeRequestService {

    private final ChangeRequestRepository repository;
    private final ChangeRequestStateMachine stateMachine;
    private final ApprovalRepository approvalRepository;
    private final AuditService auditService;
    private final RiskAssessmentService riskAssessmentService;
    private final ApprovalFlowMatrix approvalFlowMatrix;
    private final ExecutionService executionService;

    public ChangeRequestService(ChangeRequestRepository repository,
                                ChangeRequestStateMachine stateMachine,
                                ApprovalRepository approvalRepository,
                                AuditService auditService,
                                RiskAssessmentService riskAssessmentService,
                                ApprovalFlowMatrix approvalFlowMatrix,
                                ExecutionService executionService) {
        this.repository = repository;
        this.stateMachine = stateMachine;
        this.approvalRepository = approvalRepository;
        this.auditService = auditService;
        this.riskAssessmentService = riskAssessmentService;
        this.approvalFlowMatrix = approvalFlowMatrix;
        this.executionService = executionService;
    }

    // ---- 作成・編集 ----

    /** 作成（DRAFT）。requester_id は principal から強制（クライアント指定を信用しない）。 */
    @Transactional
    public ChangeRequest create(AppUserPrincipal actor, CreateChangeRequest req) {
        ChangeRequest cr = new ChangeRequest();
        cr.setTitle(req.title());
        cr.setTargetEnvironment(req.targetEnvironment());
        cr.setIacType(req.iacType());
        cr.setTargetAwsService(req.targetAwsService());
        cr.setTargetResourceName(req.targetResourceName());
        cr.setChangeReason(req.changeReason());
        cr.setChangeSummary(req.changeSummary());
        cr.setDiffText(req.diffText());
        cr.setScheduledAt(req.scheduledAt());
        cr.setRollbackProcedure(req.rollbackProcedure());
        cr.setStatus(ChangeRequestStatus.DRAFT);
        cr.setRequesterId(actor.userId());
        ChangeRequest saved = repository.save(cr);
        auditService.record(actor, saved.getId(), AuditAction.CREATE, null, saved.getStatus(),
                null, "変更申請を作成");
        return saved;
    }

    /** 編集（DRAFT / RETURNED の所有者のみ・受入 A-1/A-3）。 */
    @Transactional
    public ChangeRequest update(AppUserPrincipal actor, Long id, UpdateChangeRequest req) {
        ChangeRequest cr = loadOrNotFound(id);
        // 他人の申請は存在を秘匿して 404（受入 A-1・IDOR）。
        if (!cr.isOwnedBy(actor.userId())) {
            throw new NotFoundException();
        }
        // 承認済み以降は編集不可（受入 A-3）。編集可能なのは DRAFT / RETURNED のみ。
        if (!cr.getStatus().isEditable()) {
            throw new IllegalStateTransitionException(
                    "この状態では編集できません",
                    cr.getStatus().name() + " の申請は編集できません（編集可能なのは下書き/差し戻しのみ）",
                    cr.getStatus().name(),
                    stateMachine.allowedActions(cr, actor));
        }
        cr.setTitle(req.title());
        cr.setTargetEnvironment(req.targetEnvironment());
        cr.setIacType(req.iacType());
        cr.setTargetAwsService(req.targetAwsService());
        cr.setTargetResourceName(req.targetResourceName());
        cr.setChangeReason(req.changeReason());
        cr.setChangeSummary(req.changeSummary());
        cr.setDiffText(req.diffText());
        cr.setScheduledAt(req.scheduledAt());
        cr.setRollbackProcedure(req.rollbackProcedure());
        ChangeRequest saved = repository.save(cr);
        auditService.record(actor, saved.getId(), AuditAction.EDIT,
                saved.getStatus(), saved.getStatus(), null, "変更申請を編集");
        return saved;
    }

    // ---- 参照 ----

    /** 詳細取得（閲覧権限つき）。REQUESTER は自分の申請のみ（他は 404）。 */
    @Transactional(readOnly = true)
    public ChangeRequest getViewable(AppUserPrincipal actor, Long id) {
        ChangeRequest cr = loadOrNotFound(id);
        if (actor.role() == Role.REQUESTER && !cr.isOwnedBy(actor.userId())) {
            throw new NotFoundException();   // 存在秘匿（IDOR）
        }
        return cr;
    }

    /** 集約詳細（基本情報＋pre-check／health／最新 execution）。閲覧権限は getViewable に従う（API設計.md §2）。 */
    @Transactional(readOnly = true)
    public ChangeRequestDetailResponse getDetail(AppUserPrincipal actor, Long id) {
        ChangeRequest cr = getViewable(actor, id);
        ChangeRequestResponse base = ChangeRequestResponse.from(cr, allowedActions(cr, actor));
        return new ChangeRequestDetailResponse(base,
                executionService.listPreChecks(id),
                executionService.listHealthChecks(id),
                executionService.latestExecution(id));
    }

    /** 一覧（フィルタ＋ページング）。REQUESTER は requesterId を自分に強制上書き（P-2）。 */
    @Transactional(readOnly = true)
    public Page<ChangeRequest> list(AppUserPrincipal actor, Environment environment,
                                    ChangeRequestStatus status, RiskLevel risk,
                                    Long requesterId, Pageable pageable) {
        Long effectiveRequesterId = (actor.role() == Role.REQUESTER) ? actor.userId() : requesterId;
        return repository.findAll(
                ChangeRequestSpecifications.filter(environment, status, risk, effectiveRequesterId),
                pageable);
    }

    /** 現在の閲覧者から見た実行可能アクション（詳細応答の allowedActions 用）。 */
    public List<String> allowedActions(ChangeRequest cr, AppUserPrincipal actor) {
        return stateMachine.allowedActions(cr, actor);
    }

    // ---- 状態遷移（status を直接 set しない・必ずここを通す） ----

    @Transactional
    public ChangeRequest transition(AppUserPrincipal actor, Long id,
                                    TransitionAction action, TransitionRequest body) {
        ChangeRequest cr = loadOrNotFound(id);
        ChangeRequestStatus before = cr.getStatus();
        String comment = body == null ? null : body.comment();
        TransitionContext ctx = new TransitionContext(comment, body == null ? null : body.scheduledAt());

        // schedule 時は実施予定日時を反映してからガードに掛ける。
        if (action == TransitionAction.SCHEDULE && ctx.scheduledAt() != null) {
            cr.setScheduledAt(ctx.scheduledAt());
        }

        // 業務ガード・付随記録を伴う遷移は専用経路で処理する。
        switch (action) {
            case APPROVE -> { return handleApprove(cr, actor, ctx, before, comment); }
            case START -> { return handleStart(cr, actor, ctx, before, comment); }
            case COMPLETE -> { return handleComplete(cr, actor, ctx, before, comment); }
            case FAIL -> { return handleFail(cr, actor, ctx, before, comment); }
            case ROLLBACK -> { return handleRollback(cr, actor, ctx, before, comment); }
            default -> { /* 以降の汎用経路で処理 */ }
        }

        stateMachine.transition(cr, action, actor, ctx);

        // SUBMIT 時：リスク判定＋ポリシー評価を実行・永続化し、BLOCK なら遷移を拒否（受入 A-4/A-5）。
        // 例外は本メソッドの @Transactional をロールバックさせ、状態を DRAFT/RETURNED のまま戻す。
        if (action == TransitionAction.SUBMIT) {
            AssessmentOutcome outcome = riskAssessmentService.assess(cr);
            if (outcome.blocked()) {
                throw new PolicyBlockedException(
                        "ポリシーにより提出できません（危険な変更が検出されました）",
                        buildBlockDetails(outcome));
            }
        }

        // SCHEDULE 時：実施前チェックを CR ごとに生成（冪等・is_required は env×risk マトリクス）。
        // 同一 TX 内で実行し、START ゲートが参照する。
        if (action == TransitionAction.SCHEDULE) {
            executionService.instantiatePreChecks(cr);
        }

        // 却下・差し戻しは approvals に記録（UNIQUE で二重記録を防止）。
        if (action == TransitionAction.REJECT || action == TransitionAction.RETURN_) {
            recordApproval(cr, actor, action, comment);
        }

        ChangeRequest saved = repository.save(cr);   // 明示 save（ダーティチェック依存にしない・B5）
        auditService.record(actor, saved.getId(), auditActionFor(action),
                before, saved.getStatus(), comment, null);
        return saved;
    }

    /**
     * APPROVE：環境×リスクの承認要件（approval-flow.json）に基づく条件付き遷移。
     *
     * <p>承認前提（実施予定日時・ロールバック手順）を確認 → ロール/自己承認/遷移の妥当性を検証（状態は変えない）
     * → このレビュー者の票を記録 → 必要承認数（distinct）に達したら APPROVED へ、未達なら UNDER_REVIEW を維持。
     * 状態変更は必ず状態機械経由（A-9）。
     */
    private ChangeRequest handleApprove(ChangeRequest cr, AppUserPrincipal actor,
                                        TransitionContext ctx, ChangeRequestStatus before, String comment) {
        // 先に認可（ロール REVIEWER・自己承認禁止・遷移の妥当性）を検証する（状態は変えない）。
        // 業務要件（前提充足）より認可を優先し、非レビュー者に要件を露出させない。
        stateMachine.validate(cr, TransitionAction.APPROVE, actor, ctx);

        // 次に CR レベルの承認前提（状態遷移設計.md §4）。
        ApprovalRequirement req = approvalFlowMatrix.requirementFor(
                cr.getTargetEnvironment(), riskLevelOf(cr));
        if (req.requireScheduledAt() && cr.getScheduledAt() == null) {
            throw new ValidationException("この変更の承認には実施予定日時が必要です");
        }
        if (req.requireRollbackProcedure() && isBlank(cr.getRollbackProcedure())) {
            throw new ValidationException("この変更の承認にはロールバック手順が必要です");
        }

        // このレビュー者の票を記録（UNIQUE が同一レビュー者の二重承認を防止＝distinct を担保）。
        recordApproval(cr, actor, TransitionAction.APPROVE, comment);

        // 異なるレビュー者の承認数が要件に達したら APPROVED へ。
        long approvedCount = approvalRepository.countByChangeRequestIdAndDecision(
                cr.getId(), Decision.APPROVED);
        if (approvedCount >= req.requiredApprovals()) {
            stateMachine.transition(cr, TransitionAction.APPROVE, actor, ctx);
        }

        ChangeRequest saved = repository.save(cr);
        auditService.record(actor, saved.getId(), AuditAction.APPROVE,
                before, saved.getStatus(), comment, null);
        return saved;
    }

    /**
     * START（SCHEDULED→IN_PROGRESS）：必須 pre-check 完了ゲート（A-7）＋実施記録の開始。
     *
     * <p>認可・遷移の妥当性を先に検証（状態は変えない）→ env×risk が対象なら必須 pre-check 全完了を確認
     * （未完了は 409）→ 遷移 → execution を1行開始記録。すべて同一 TX（B5）。
     */
    private ChangeRequest handleStart(ChangeRequest cr, AppUserPrincipal actor,
                                      TransitionContext ctx, ChangeRequestStatus before, String comment) {
        stateMachine.validate(cr, TransitionAction.START, actor, ctx);

        if (!executionService.requiredPreChecksComplete(cr)) {
            throw new IllegalStateTransitionException(
                    "必須の実施前チェックが未完了です",
                    "本変更の開始には必須の実施前チェックをすべて完了する必要があります",
                    cr.getStatus().name(),
                    stateMachine.allowedActions(cr, actor));
        }

        stateMachine.transition(cr, TransitionAction.START, actor, ctx);
        executionService.startExecution(cr, actor);

        ChangeRequest saved = repository.save(cr);
        auditService.record(actor, saved.getId(), AuditAction.EXECUTION_START,
                before, saved.getStatus(), comment, null);
        return saved;
    }

    /**
     * COMPLETE（IN_PROGRESS→COMPLETED）：iac_apply_result=SUCCESS かつ service_health_confirmed の
     * 導出が true の時のみ（A-10）。満たさなければ 409。確定時に execution の確認済み・finished_at を記録。
     */
    private ChangeRequest handleComplete(ChangeRequest cr, AppUserPrincipal actor,
                                         TransitionContext ctx, ChangeRequestStatus before, String comment) {
        stateMachine.validate(cr, TransitionAction.COMPLETE, actor, ctx);

        if (!executionService.canComplete(cr)) {
            throw new IllegalStateTransitionException(
                    "完了条件を満たしていません",
                    "完了には IaC 適用が成功し、必須の実施後ヘルスチェックがすべて正常である必要があります",
                    cr.getStatus().name(),
                    stateMachine.allowedActions(cr, actor));
        }

        stateMachine.transition(cr, TransitionAction.COMPLETE, actor, ctx);
        executionService.markCompleted(cr);

        ChangeRequest saved = repository.save(cr);
        auditService.record(actor, saved.getId(), AuditAction.EXECUTION_COMPLETE,
                before, saved.getStatus(), comment, null);
        return saved;
    }

    /** FAIL（IN_PROGRESS→FAILED）：実施記録の finished_at を記録。 */
    private ChangeRequest handleFail(ChangeRequest cr, AppUserPrincipal actor,
                                     TransitionContext ctx, ChangeRequestStatus before, String comment) {
        stateMachine.transition(cr, TransitionAction.FAIL, actor, ctx);
        executionService.markFailed(cr);

        ChangeRequest saved = repository.save(cr);
        auditService.record(actor, saved.getId(), AuditAction.EXECUTION_FAIL,
                before, saved.getStatus(), comment, null);
        return saved;
    }

    /** ROLLBACK（FAILED→ROLLED_BACK）：rollback_performed・rollback_note(=comment) を記録。 */
    private ChangeRequest handleRollback(ChangeRequest cr, AppUserPrincipal actor,
                                         TransitionContext ctx, ChangeRequestStatus before, String comment) {
        stateMachine.transition(cr, TransitionAction.ROLLBACK, actor, ctx);
        executionService.recordRollback(cr, comment);

        ChangeRequest saved = repository.save(cr);
        auditService.record(actor, saved.getId(), AuditAction.ROLLBACK,
                before, saved.getStatus(), comment, null);
        return saved;
    }

    private RiskLevel riskLevelOf(ChangeRequest cr) {
        return cr.getRiskLevel() != null ? cr.getRiskLevel() : RiskLevel.LOW;
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    // ---- 内部ヘルパ ----

    /** ブロック理由（危険なリスク finding ＋ BLOCK ポリシー）を API レスポンス用の details に整形する。 */
    private List<Object> buildBlockDetails(AssessmentOutcome outcome) {
        List<Object> details = new ArrayList<>();
        for (RiskFinding f : outcome.risk().blockingFindings()) {
            details.add(new BlockReason("RISK", f.ruleCode(), f.whyDangerous(), f.recommendedAction()));
        }
        for (PolicyOutcome o : outcome.policy().outcomes()) {
            if (o.effect() == PolicyEffect.BLOCK) {
                details.add(new BlockReason("POLICY", o.policyCode(), o.message(), null));
            }
        }
        return details;
    }

    private void recordApproval(ChangeRequest cr, AppUserPrincipal actor,
                                TransitionAction action, String comment) {
        Approval approval = new Approval();
        approval.setChangeRequestId(cr.getId());
        approval.setReviewerId(actor.userId());
        approval.setDecision(switch (action) {
            case APPROVE -> Decision.APPROVED;
            case REJECT -> Decision.REJECTED;
            case RETURN_ -> Decision.RETURNED;
            default -> throw new IllegalArgumentException("not an approval action: " + action);
        });
        approval.setComment(comment);
        approvalRepository.save(approval);
    }

    private AuditAction auditActionFor(TransitionAction action) {
        return switch (action) {
            case SUBMIT -> AuditAction.SUBMIT;
            case CANCEL -> AuditAction.CANCEL;
            case REVIEW_START -> AuditAction.REVIEW_START;
            case APPROVE -> AuditAction.APPROVE;
            case REJECT -> AuditAction.REJECT;
            case RETURN_ -> AuditAction.RETURN;
            case SCHEDULE -> AuditAction.SCHEDULE;
            case START -> AuditAction.EXECUTION_START;
            case COMPLETE -> AuditAction.EXECUTION_COMPLETE;
            case FAIL -> AuditAction.EXECUTION_FAIL;
            case ROLLBACK -> AuditAction.ROLLBACK;
        };
    }

    private ChangeRequest loadOrNotFound(Long id) {
        return repository.findById(id).orElseThrow(NotFoundException::new);
    }
}
