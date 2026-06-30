package com.cloudopsguard.domain.execution;

import com.cloudopsguard.common.ApiResponse;
import com.cloudopsguard.domain.changerequest.ChangeRequestService;
import com.cloudopsguard.domain.execution.dto.CreateHealthCheck;
import com.cloudopsguard.domain.execution.dto.ExecutionResponse;
import com.cloudopsguard.domain.execution.dto.HealthCheckResponse;
import com.cloudopsguard.domain.execution.dto.PreCheckResponse;
import com.cloudopsguard.domain.execution.dto.RecordExecutionResult;
import com.cloudopsguard.security.AppUserPrincipal;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 実施前チェック・実施後ヘルスチェック・実行結果の API（API設計.md §4）。
 * 閲覧権限は {@link ChangeRequestService#getViewable}（IDOR は 404）で先に担保してから処理する。
 */
@RestController
@RequestMapping("/api/v1/change-requests")
public class ExecutionController {

    private final ChangeRequestService changeRequestService;
    private final ExecutionService executionService;

    public ExecutionController(ChangeRequestService changeRequestService,
                               ExecutionService executionService) {
        this.changeRequestService = changeRequestService;
        this.executionService = executionService;
    }

    @GetMapping("/{id}/pre-checks")
    public ApiResponse<List<PreCheckResponse>> preChecks(
            @AuthenticationPrincipal AppUserPrincipal actor, @PathVariable Long id) {
        changeRequestService.getViewable(actor, id);
        return ApiResponse.of(executionService.listPreChecks(id));
    }

    @PostMapping("/{id}/pre-checks/{checkId}/complete")
    @PreAuthorize("hasRole('OPERATOR')")
    public ApiResponse<PreCheckResponse> completePreCheck(
            @AuthenticationPrincipal AppUserPrincipal actor,
            @PathVariable Long id, @PathVariable Long checkId) {
        changeRequestService.getViewable(actor, id);
        return ApiResponse.of(executionService.completePreCheck(actor, id, checkId));
    }

    @GetMapping("/{id}/health-checks")
    public ApiResponse<List<HealthCheckResponse>> healthChecks(
            @AuthenticationPrincipal AppUserPrincipal actor, @PathVariable Long id) {
        changeRequestService.getViewable(actor, id);
        return ApiResponse.of(executionService.listHealthChecks(id));
    }

    @PostMapping("/{id}/health-checks")
    @PreAuthorize("hasRole('OPERATOR')")
    public ResponseEntity<ApiResponse<HealthCheckResponse>> recordHealthCheck(
            @AuthenticationPrincipal AppUserPrincipal actor, @PathVariable Long id,
            @Valid @RequestBody CreateHealthCheck req) {
        changeRequestService.getViewable(actor, id);
        return ResponseEntity.status(201).body(
                ApiResponse.of(executionService.recordHealthCheck(actor, id, req)));
    }

    /** 実行結果（IaC 適用結果）を記録する（決定A）。COMPLETE はこの事実を検証して進む。 */
    @PostMapping("/{id}/execution-result")
    @PreAuthorize("hasRole('OPERATOR')")
    public ApiResponse<ExecutionResponse> recordExecutionResult(
            @AuthenticationPrincipal AppUserPrincipal actor, @PathVariable Long id,
            @Valid @RequestBody RecordExecutionResult req) {
        changeRequestService.getViewable(actor, id);
        return ApiResponse.of(executionService.recordExecutionResult(actor, id, req));
    }
}
