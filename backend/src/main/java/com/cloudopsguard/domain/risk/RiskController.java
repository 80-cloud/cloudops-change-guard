package com.cloudopsguard.domain.risk;

import com.cloudopsguard.common.ApiResponse;
import com.cloudopsguard.domain.changerequest.ChangeRequestService;
import com.cloudopsguard.domain.changerequest.dto.CreateChangeRequest;
import com.cloudopsguard.domain.policy.dto.PolicyViolationResponse;
import com.cloudopsguard.domain.risk.dto.PreviewRiskResponse;
import com.cloudopsguard.domain.risk.dto.RiskAssessmentResponse;
import com.cloudopsguard.security.AppUserPrincipal;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * リスク判定・ポリシー違反の参照＋作成画面プレビュー API（API設計.md §2/§3）。
 * 参照は {@link ChangeRequestService#getViewable}（IDOR は 404）で先に閲覧権限を担保してから返す。
 */
@RestController
@RequestMapping("/api/v1/change-requests")
public class RiskController {

    private final ChangeRequestService changeRequestService;
    private final RiskQueryService riskQueryService;
    private final RiskAssessmentService riskAssessmentService;

    public RiskController(ChangeRequestService changeRequestService, RiskQueryService riskQueryService,
                          RiskAssessmentService riskAssessmentService) {
        this.changeRequestService = changeRequestService;
        this.riskQueryService = riskQueryService;
        this.riskAssessmentService = riskAssessmentService;
    }

    /** 作成画面用のリスク・ポリシーのプレビュー（非永続）。入力中の diff を判定だけして返す。 */
    @PostMapping("/preview-risk")
    @PreAuthorize("hasRole('REQUESTER')")
    public ApiResponse<PreviewRiskResponse> previewRisk(@Valid @RequestBody CreateChangeRequest req) {
        return ApiResponse.of(PreviewRiskResponse.from(riskAssessmentService.preview(req)));
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
