package com.cloudopsguard.domain.approval;

import com.cloudopsguard.domain.changerequest.ChangeRequestService;
import com.cloudopsguard.security.AppUserPrincipal;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 承認履歴の参照ユースケース。閲覧権限は {@link ChangeRequestService#getViewable} に委譲する
 * （REQUESTER は自分の申請のみ・他は 404）。読み取り専用（承認の記録は承認遷移側で行う）。
 */
@Service
public class ApprovalService {

    private final ApprovalRepository repository;
    private final ChangeRequestService changeRequestService;

    public ApprovalService(ApprovalRepository repository,
                           ChangeRequestService changeRequestService) {
        this.repository = repository;
        this.changeRequestService = changeRequestService;
    }

    @Transactional(readOnly = true)
    public List<Approval> list(AppUserPrincipal actor, Long changeRequestId) {
        changeRequestService.getViewable(actor, changeRequestId);   // 閲覧権限の検証（無ければ 404）
        return repository.findByChangeRequestIdOrderByDecidedAtAsc(changeRequestId);
    }
}
