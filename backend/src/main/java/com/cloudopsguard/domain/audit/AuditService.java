package com.cloudopsguard.domain.audit;

import com.cloudopsguard.domain.common.AuditAction;
import com.cloudopsguard.domain.common.ChangeRequestStatus;
import com.cloudopsguard.security.AppUserPrincipal;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 監査ログの記録と閲覧（受入 A-8）。
 *
 * <p>記録は呼び出し元の {@code @Transactional} に参加する（{@code MANDATORY}）。これにより
 * 「操作がコミットされたときだけ監査行も残る」を保証し、ログだけ残る/操作だけ進むを防ぐ（B5）。
 * actor はクライアント入力ではなく principal（検証済み JWT）から導出する。
 */
@Service
public class AuditService {

    private final AuditLogRepository repository;

    public AuditService(AuditLogRepository repository) {
        this.repository = repository;
    }

    /** 監査行を記録する。認可チェックを通過した操作と同一トランザクションで呼ぶこと。 */
    @Transactional(propagation = Propagation.MANDATORY)
    public void record(AppUserPrincipal actor, Long changeRequestId, AuditAction action,
                       ChangeRequestStatus before, ChangeRequestStatus after,
                       String comment, String summary) {
        AuditLog log = new AuditLog();
        log.setActorId(actor.userId());
        log.setChangeRequestId(changeRequestId);
        log.setActionType(action);
        log.setBeforeStatus(before == null ? null : before.name());
        log.setAfterStatus(after == null ? null : after.name());
        log.setComment(comment);
        log.setSummary(summary);
        repository.save(log);
    }

    /** 当該申請の監査ログ（新しい順）。 */
    @Transactional(readOnly = true)
    public List<AuditLog> listForChangeRequest(Long changeRequestId) {
        return repository.findByChangeRequestIdOrderByCreatedAtDesc(changeRequestId);
    }

    /** 全監査ログ（ADMIN・ページング）。 */
    @Transactional(readOnly = true)
    public Page<AuditLog> listAll(Pageable pageable) {
        return repository.findAllByOrderByCreatedAtDesc(pageable);
    }
}
