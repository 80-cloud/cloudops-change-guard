package com.cloudopsguard.domain.dashboard.dto;

import com.cloudopsguard.domain.audit.dto.AuditLogResponse;
import com.cloudopsguard.domain.changerequest.dto.ChangeRequestSummary;

import java.util.List;
import java.util.Map;

/**
 * ダッシュボード集計（GET /dashboard/summary・SCR-01）。ロールで範囲を調整：
 * REQUESTER は自分の申請のみ集計し、最近の監査は全体を出さない（情報漏えい防止で空）。
 */
public record DashboardSummaryResponse(
        Map<String, Long> statusCounts,
        List<ChangeRequestSummary> highRisk,
        List<ChangeRequestSummary> pendingApproval,
        List<ChangeRequestSummary> scheduled,
        List<AuditLogResponse> recentAudit) {
}
