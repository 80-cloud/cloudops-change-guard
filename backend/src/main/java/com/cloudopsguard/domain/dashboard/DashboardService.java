package com.cloudopsguard.domain.dashboard;

import com.cloudopsguard.domain.audit.AuditLogRepository;
import com.cloudopsguard.domain.audit.dto.AuditLogResponse;
import com.cloudopsguard.domain.changerequest.ChangeRequest;
import com.cloudopsguard.domain.changerequest.ChangeRequestRepository;
import com.cloudopsguard.domain.changerequest.ChangeRequestSpecifications;
import com.cloudopsguard.domain.changerequest.dto.ChangeRequestSummary;
import com.cloudopsguard.domain.common.ChangeRequestStatus;
import com.cloudopsguard.domain.common.RiskLevel;
import com.cloudopsguard.domain.common.Role;
import com.cloudopsguard.domain.dashboard.dto.DashboardSummaryResponse;
import com.cloudopsguard.security.AppUserPrincipal;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * ダッシュボード集計（SCR-01）。REQUESTER は自分の申請のみ、それ以外は全体を見る。
 * 件数はステータス別 count、各リストは上位 N 件に絞る（全件ロードしない・P-2）。
 */
@Service
public class DashboardService {

    private static final int LIST_LIMIT = 10;

    private final ChangeRequestRepository changeRequestRepository;
    private final AuditLogRepository auditLogRepository;

    public DashboardService(ChangeRequestRepository changeRequestRepository,
                            AuditLogRepository auditLogRepository) {
        this.changeRequestRepository = changeRequestRepository;
        this.auditLogRepository = auditLogRepository;
    }

    @Transactional(readOnly = true)
    public DashboardSummaryResponse summary(AppUserPrincipal actor) {
        Long scope = (actor.role() == Role.REQUESTER) ? actor.userId() : null;

        Map<String, Long> statusCounts = new LinkedHashMap<>();
        for (ChangeRequestStatus status : ChangeRequestStatus.values()) {
            long count = changeRequestRepository.count(
                    ChangeRequestSpecifications.filter(null, status, null, scope));
            statusCounts.put(status.name(), count);
        }

        List<ChangeRequestSummary> highRisk = changeRequestRepository.findAll(
                        highRiskSpec(scope),
                        PageRequest.of(0, LIST_LIMIT, Sort.by(Sort.Direction.DESC, "createdAt")))
                .map(ChangeRequestSummary::from).getContent();

        List<ChangeRequestSummary> pendingApproval = changeRequestRepository.findAll(
                        ChangeRequestSpecifications.filter(null, ChangeRequestStatus.UNDER_REVIEW, null, scope),
                        PageRequest.of(0, LIST_LIMIT, Sort.by(Sort.Direction.DESC, "createdAt")))
                .map(ChangeRequestSummary::from).getContent();

        List<ChangeRequestSummary> scheduled = changeRequestRepository.findAll(
                        ChangeRequestSpecifications.filter(null, ChangeRequestStatus.SCHEDULED, null, scope),
                        PageRequest.of(0, LIST_LIMIT, Sort.by(Sort.Direction.ASC, "scheduledAt")))
                .map(ChangeRequestSummary::from).getContent();

        List<AuditLogResponse> recentAudit = (scope != null)
                ? List.of()
                : auditLogRepository.findAllByOrderByCreatedAtDesc(PageRequest.of(0, LIST_LIMIT))
                .map(AuditLogResponse::from).getContent();

        return new DashboardSummaryResponse(statusCounts, highRisk, pendingApproval, scheduled, recentAudit);
    }

    private static Specification<ChangeRequest> highRiskSpec(Long scope) {
        return (root, query, cb) -> {
            var predicate = root.get("riskLevel").in(RiskLevel.HIGH, RiskLevel.CRITICAL);
            if (scope != null) {
                predicate = cb.and(predicate, cb.equal(root.get("requesterId"), scope));
            }
            return predicate;
        };
    }
}
