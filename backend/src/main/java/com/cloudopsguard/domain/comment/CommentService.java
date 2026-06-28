package com.cloudopsguard.domain.comment;

import com.cloudopsguard.domain.audit.AuditService;
import com.cloudopsguard.domain.changerequest.ChangeRequest;
import com.cloudopsguard.domain.changerequest.ChangeRequestService;
import com.cloudopsguard.domain.common.AuditAction;
import com.cloudopsguard.security.AppUserPrincipal;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * コメントのユースケース。閲覧権限は {@link ChangeRequestService#getViewable} に委譲し
 * （REQUESTER は自分の申請のみ・他は 404）、投稿時は監査ログ(COMMENT)を同一 TX で記録する（受入 A-8）。
 */
@Service
public class CommentService {

    private final CommentRepository repository;
    private final ChangeRequestService changeRequestService;
    private final AuditService auditService;

    public CommentService(CommentRepository repository,
                          ChangeRequestService changeRequestService,
                          AuditService auditService) {
        this.repository = repository;
        this.changeRequestService = changeRequestService;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public List<Comment> list(AppUserPrincipal actor, Long changeRequestId) {
        changeRequestService.getViewable(actor, changeRequestId);   // 閲覧権限の検証（無ければ 404）
        return repository.findByChangeRequestIdOrderByCreatedAtAsc(changeRequestId);
    }

    @Transactional
    public Comment create(AppUserPrincipal actor, Long changeRequestId, String body) {
        ChangeRequest cr = changeRequestService.getViewable(actor, changeRequestId);
        Comment comment = new Comment();
        comment.setChangeRequestId(cr.getId());
        comment.setAuthorId(actor.userId());
        comment.setBody(body);
        Comment saved = repository.save(comment);
        auditService.record(actor, cr.getId(), AuditAction.COMMENT, null, null, body, "コメントを投稿");
        return saved;
    }
}
