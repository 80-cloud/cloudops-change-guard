package com.cloudopsguard.domain.risk;

import com.cloudopsguard.common.ApiResponse;
import com.cloudopsguard.domain.changerequest.ChangeRequestService;
import com.cloudopsguard.domain.policy.dto.PolicyViolationResponse;
import com.cloudopsguard.domain.risk.dto.RiskAssessmentResponse;
import com.cloudopsguard.security.AppUserPrincipal;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * リスク判定・ポリシー違反の参照 API（API設計.md §3）。
 * 閲覧権限は {@link ChangeRequestService#getViewable}（所有/ロール・IDOR は 404）で先に担保してから返す。
 */
@RestController
@RequestMapping("/api/v1/change-requests")
public class RiskController {

    private final ChangeRequestService changeRequestService;
    private final RiskQueryService riskQueryService;

    public RiskController(ChangeRequestService changeRequestService, RiskQueryService riskQueryService) {
        this.changeRequestService = changeRequestService;
        this.riskQueryService = riskQueryService;
    }

    /** 最新のリスク判定結果（findings 付き）。 */
    @GetMapping("/{id}/risk-assessment")
    public ApiResponse<RiskAssessmentResponse> riskAssessment(
            @AuthenticationPrincipal AppUserPrincipal actor, @PathVariable Long id) {
        changeRequestService.getViewable(actor, id);   // 閲覧権限・IDOR を強制（404）
        return ApiResponse.of(riskQueryService.latestAssessment(id));
    }

    /** ポリシー違反一覧。 */
    @GetMapping("/{id}/policy-violations")
    public ApiResponse<List<PolicyViolationResponse>> policyViolations(
            @AuthenticationPrincipal AppUserPrincipal actor, @PathVariable Long id) {
        changeRequestService.getViewable(actor, id);
        return ApiResponse.of(riskQueryService.policyViolations(id));
    }
}
