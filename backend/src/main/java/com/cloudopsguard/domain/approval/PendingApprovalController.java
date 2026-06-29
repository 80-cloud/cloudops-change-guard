package com.cloudopsguard.domain.approval;

import com.cloudopsguard.common.ApiResponse;
import com.cloudopsguard.common.PageResponse;
import com.cloudopsguard.domain.changerequest.dto.ChangeRequestSummary;
import com.cloudopsguard.security.AppUserPrincipal;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 承認待ち一覧 API（GET /api/v1/change-requests/pending-approval・REVIEWER のみ・SCR-05）。 */
@RestController
@RequestMapping("/api/v1/change-requests/pending-approval")
public class PendingApprovalController {

    private final PendingApprovalService service;

    public PendingApprovalController(PendingApprovalService service) {
        this.service = service;
    }

    @GetMapping
    @PreAuthorize("hasRole('REVIEWER')")
    public ApiResponse<PageResponse<ChangeRequestSummary>> list(
            @AuthenticationPrincipal AppUserPrincipal actor,
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {
        return ApiResponse.of(PageResponse.from(
                service.listFor(actor, pageable), ChangeRequestSummary::from));
    }
}
