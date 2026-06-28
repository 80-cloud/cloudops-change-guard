package com.cloudopsguard.domain.changerequest;

import com.cloudopsguard.common.exception.IllegalStateTransitionException;
import com.cloudopsguard.common.exception.NotFoundException;
import com.cloudopsguard.domain.approval.Approval;
import com.cloudopsguard.domain.approval.ApprovalRepository;
import com.cloudopsguard.domain.audit.AuditService;
import com.cloudopsguard.domain.changerequest.ChangeRequestStateMachine.TransitionContext;
import com.cloudopsguard.domain.changerequest.dto.CreateChangeRequest;
import com.cloudopsguard.domain.changerequest.dto.TransitionRequest;
import com.cloudopsguard.domain.changerequest.dto.UpdateChangeRequest;
import com.cloudopsguard.domain.common.*;
import com.cloudopsguard.security.AppUserPrincipal;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 変更申請のユースケース。Controller は業務ロジックを持たずここへ委譲する（M-3）。
 *
 * <p>遷移は必ず {@link ChangeRequestStateMachine#transition} 経由（status を直接 set しない・A-9）。
 * 遷移 + 付随レコード(承認) + 監査ログを<b>同一トランザクション</b>で書く（B5・原子性）。
 * 認可はロール（@PreAuthorize）＋所有者検証（本サービス）の二重で強制し、IDOR は 404 で秘匿する。
 */
@Service
public class ChangeRequestService {

    private final ChangeRequestRepository repository;
    private final ChangeRequestStateMachine stateMachine;
    private final ApprovalRepository approvalRepository;
    private final AuditService auditService;

    public ChangeRequestService(ChangeRequestRepository repository,
                                ChangeRequestStateMachine stateMachine,
                                ApprovalRepository approvalRepository,
                                AuditService auditService) {
        this.repository = repository;
        this.stateMachine = stateMachine;
        this.approvalRepository = approvalRepository;
        this.auditService = auditService;
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

        stateMachine.transition(cr, action, actor, ctx);

        // 承認系は approvals に記録（UNIQUE で二重承認を防止）。
        if (action == TransitionAction.APPROVE
                || action == TransitionAction.REJECT
                || action == TransitionAction.RETURN_) {
            recordApproval(cr, actor, action, comment);
        }

        ChangeRequest saved = repository.save(cr);   // 明示 save（ダーティチェック依存にしない・B5）
        auditService.record(actor, saved.getId(), auditActionFor(action),
                before, saved.getStatus(), comment, null);
        return saved;
    }

    // ---- 内部ヘルパ ----

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
