package com.cloudopsguard.domain.changerequest;

import com.cloudopsguard.common.ApiResponse;
import com.cloudopsguard.common.PageResponse;
import com.cloudopsguard.domain.changerequest.dto.*;
import com.cloudopsguard.domain.common.ChangeRequestStatus;
import com.cloudopsguard.domain.common.Environment;
import com.cloudopsguard.domain.common.RiskLevel;
import com.cloudopsguard.domain.common.TransitionAction;
import com.cloudopsguard.security.AppUserPrincipal;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * 変更申請 API（/api/v1/change-requests）。業務ロジックは {@link ChangeRequestService} に委譲する。
 * status を直接 PUT するエンドポイントは設けず、遷移は専用 POST で状態機械を通す（A-9）。
 */
@RestController
@RequestMapping("/api/v1/change-requests")
public class ChangeRequestController {

    private final ChangeRequestService service;

    public ChangeRequestController(ChangeRequestService service) {
        this.service = service;
    }

    /** 一覧（filter: environment/status/risk/requesterId・page/size/sort）。REQUESTER は自分のみ。 */
    @GetMapping
    public ApiResponse<PageResponse<ChangeRequestSummary>> list(
            @AuthenticationPrincipal AppUserPrincipal actor,
            @RequestParam(required = false) String environment,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String risk,
            @RequestParam(required = false) Long requesterId,
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {
        Page<ChangeRequest> page = service.list(actor,
                parseEnvironment(environment), parseStatus(status), parseRisk(risk),
                requesterId, pageable);
        return ApiResponse.of(PageResponse.from(page, ChangeRequestSummary::from));
    }

    /** 作成（DRAFT）。 */
    @PostMapping
    @PreAuthorize("hasRole('REQUESTER')")
    public ResponseEntity<ApiResponse<ChangeRequestResponse>> create(
            @AuthenticationPrincipal AppUserPrincipal actor,
            @Valid @RequestBody CreateChangeRequest req) {
        ChangeRequest cr = service.create(actor, req);
        return ResponseEntity.status(201).body(detail(cr, actor));
    }

    /** 詳細。 */
    @GetMapping("/{id}")
    public ApiResponse<ChangeRequestResponse> get(
            @AuthenticationPrincipal AppUserPrincipal actor, @PathVariable Long id) {
        ChangeRequest cr = service.getViewable(actor, id);
        return detail(cr, actor);
    }

    /** 編集（DRAFT/RETURNED の所有者のみ）。 */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('REQUESTER')")
    public ApiResponse<ChangeRequestResponse> update(
            @AuthenticationPrincipal AppUserPrincipal actor, @PathVariable Long id,
            @Valid @RequestBody UpdateChangeRequest req) {
        ChangeRequest cr = service.update(actor, id, req);
        return detail(cr, actor);
    }

    // ---- 状態遷移エンドポイント ----

    @PostMapping("/{id}/submit")
    @PreAuthorize("hasRole('REQUESTER')")
    public ApiResponse<ChangeRequestResponse> submit(
            @AuthenticationPrincipal AppUserPrincipal actor, @PathVariable Long id,
            @RequestBody(required = false) TransitionRequest body) {
        return transition(actor, id, TransitionAction.SUBMIT, body);
    }

    /** 取消は所有者/ADMIN（実施系は OPERATOR/ADMIN）。ロールは状態機械が状態ごとに判定する。 */
    @PostMapping("/{id}/cancel")
    public ApiResponse<ChangeRequestResponse> cancel(
            @AuthenticationPrincipal AppUserPrincipal actor, @PathVariable Long id,
            @RequestBody(required = false) TransitionRequest body) {
        return transition(actor, id, TransitionAction.CANCEL, body);
    }

    @PostMapping("/{id}/review-start")
    @PreAuthorize("hasRole('REVIEWER')")
    public ApiResponse<ChangeRequestResponse> reviewStart(
            @AuthenticationPrincipal AppUserPrincipal actor, @PathVariable Long id,
            @RequestBody(required = false) TransitionRequest body) {
        return transition(actor, id, TransitionAction.REVIEW_START, body);
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasRole('REVIEWER')")
    public ApiResponse<ChangeRequestResponse> approve(
            @AuthenticationPrincipal AppUserPrincipal actor, @PathVariable Long id,
            @RequestBody(required = false) TransitionRequest body) {
        return transition(actor, id, TransitionAction.APPROVE, body);
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasRole('REVIEWER')")
    public ApiResponse<ChangeRequestResponse> reject(
            @AuthenticationPrincipal AppUserPrincipal actor, @PathVariable Long id,
            @RequestBody(required = false) TransitionRequest body) {
        return transition(actor, id, TransitionAction.REJECT, body);
    }

    @PostMapping("/{id}/return")
    @PreAuthorize("hasRole('REVIEWER')")
    public ApiResponse<ChangeRequestResponse> returnToRequester(
            @AuthenticationPrincipal AppUserPrincipal actor, @PathVariable Long id,
            @RequestBody(required = false) TransitionRequest body) {
        return transition(actor, id, TransitionAction.RETURN_, body);
    }

    @PostMapping("/{id}/schedule")
    @PreAuthorize("hasRole('OPERATOR')")
    public ApiResponse<ChangeRequestResponse> schedule(
            @AuthenticationPrincipal AppUserPrincipal actor, @PathVariable Long id,
            @RequestBody(required = false) TransitionRequest body) {
        return transition(actor, id, TransitionAction.SCHEDULE, body);
    }

    @PostMapping("/{id}/start")
    @PreAuthorize("hasRole('OPERATOR')")
    public ApiResponse<ChangeRequestResponse> start(
            @AuthenticationPrincipal AppUserPrincipal actor, @PathVariable Long id,
            @RequestBody(required = false) TransitionRequest body) {
        return transition(actor, id, TransitionAction.START, body);
    }

    @PostMapping("/{id}/complete")
    @PreAuthorize("hasRole('OPERATOR')")
    public ApiResponse<ChangeRequestResponse> complete(
            @AuthenticationPrincipal AppUserPrincipal actor, @PathVariable Long id,
            @RequestBody(required = false) TransitionRequest body) {
        return transition(actor, id, TransitionAction.COMPLETE, body);
    }

    @PostMapping("/{id}/fail")
    @PreAuthorize("hasRole('OPERATOR')")
    public ApiResponse<ChangeRequestResponse> fail(
            @AuthenticationPrincipal AppUserPrincipal actor, @PathVariable Long id,
            @RequestBody(required = false) TransitionRequest body) {
        return transition(actor, id, TransitionAction.FAIL, body);
    }

    @PostMapping("/{id}/rollback")
    @PreAuthorize("hasRole('OPERATOR')")
    public ApiResponse<ChangeRequestResponse> rollback(
            @AuthenticationPrincipal AppUserPrincipal actor, @PathVariable Long id,
            @RequestBody(required = false) TransitionRequest body) {
        return transition(actor, id, TransitionAction.ROLLBACK, body);
    }

    // ---- 内部ヘルパ ----

    private ApiResponse<ChangeRequestResponse> transition(AppUserPrincipal actor, Long id,
                                                          TransitionAction action, TransitionRequest body) {
        ChangeRequest cr = service.transition(actor, id, action, body);
        return detail(cr, actor);
    }

    private ApiResponse<ChangeRequestResponse> detail(ChangeRequest cr, AppUserPrincipal actor) {
        return ApiResponse.of(ChangeRequestResponse.from(cr, service.allowedActions(cr, actor)));
    }

    private Environment parseEnvironment(String v) {
        return (v == null || v.isBlank()) ? null : Environment.fromWire(v);
    }

    private ChangeRequestStatus parseStatus(String v) {
        return (v == null || v.isBlank()) ? null : ChangeRequestStatus.valueOf(v.trim().toUpperCase());
    }

    private RiskLevel parseRisk(String v) {
        return (v == null || v.isBlank()) ? null : RiskLevel.valueOf(v.trim().toUpperCase());
    }
}
