package com.cloudopsguard.domain.approval;

import com.cloudopsguard.common.ApiResponse;
import com.cloudopsguard.domain.approval.dto.ApprovalResponse;
import com.cloudopsguard.security.AppUserPrincipal;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** 承認履歴 API（GET /api/v1/change-requests/{id}/approvals）。SCR-04 の承認履歴表示に使う。 */
@RestController
@RequestMapping("/api/v1/change-requests/{id}/approvals")
public class ApprovalController {

    private final ApprovalService service;

    public ApprovalController(ApprovalService service) {
        this.service = service;
    }

    @GetMapping
    public ApiResponse<List<ApprovalResponse>> list(
            @AuthenticationPrincipal AppUserPrincipal actor, @PathVariable Long id) {
        List<ApprovalResponse> body = service.list(actor, id).stream()
                .map(ApprovalResponse::from).toList();
        return ApiResponse.of(body);
    }
}
