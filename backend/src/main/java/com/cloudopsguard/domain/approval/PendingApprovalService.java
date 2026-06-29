package com.cloudopsguard.domain.approval;

import com.cloudopsguard.domain.changerequest.ChangeRequest;
import com.cloudopsguard.domain.changerequest.ChangeRequestRepository;
import com.cloudopsguard.domain.common.ChangeRequestStatus;
import com.cloudopsguard.security.AppUserPrincipal;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 承認待ち一覧（SCR-05）のユースケース。レビュー者が「自分が承認できる」変更だけを返す：
 * UNDER_REVIEW かつ 自分が申請者でない かつ 自分が未投票（承認/却下/差し戻し済みは除外）。
 * 自己承認禁止・二重投票防止（approvals の UNIQUE）と整合する絞り込み。
 */
@Service
public class PendingApprovalService {

    private final ChangeRequestRepository changeRequestRepository;
    private final ApprovalRepository approvalRepository;

    public PendingApprovalService(ChangeRequestRepository changeRequestRepository,
                                  ApprovalRepository approvalRepository) {
        this.changeRequestRepository = changeRequestRepository;
        this.approvalRepository = approvalRepository;
    }

    @Transactional(readOnly = true)
    public Page<ChangeRequest> listFor(AppUserPrincipal actor, Pageable pageable) {
        Long reviewerId = actor.userId();
        List<Long> votedIds = approvalRepository.findByReviewerId(reviewerId).stream()
                .map(Approval::getChangeRequestId).toList();

        Specification<ChangeRequest> spec = (root, query, cb) -> {
            var predicate = cb.equal(root.get("status"), ChangeRequestStatus.UNDER_REVIEW);
            predicate = cb.and(predicate, cb.notEqual(root.get("requesterId"), reviewerId));
            if (!votedIds.isEmpty()) {
                predicate = cb.and(predicate, cb.not(root.get("id").in(votedIds)));
            }
            return predicate;
        };
        return changeRequestRepository.findAll(spec, pageable);
    }
}
