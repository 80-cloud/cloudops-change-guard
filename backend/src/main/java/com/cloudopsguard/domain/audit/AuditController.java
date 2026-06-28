package com.cloudopsguard.domain.audit;

import com.cloudopsguard.common.ApiResponse;
import com.cloudopsguard.common.PageResponse;
import com.cloudopsguard.domain.audit.dto.AuditLogResponse;
import com.cloudopsguard.domain.changerequest.ChangeRequestService;
import com.cloudopsguard.security.AppUserPrincipal;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 監査ログ閲覧 API。作成系以外（PUT/DELETE）は設けない（改ざん不可・受入 A-8）。
 */
@RestController
@RequestMapping("/api/v1")
public class AuditController {

    private final AuditService auditService;
    private final ChangeRequestService changeRequestService;

    public AuditController(AuditService auditService, ChangeRequestService changeRequestService) {
        this.auditService = auditService;
        this.changeRequestService = changeRequestService;
    }

    /** 当該申請の監査ログ。閲覧権限は変更申請の getViewable に委譲（REQUESTER は自分のみ・他は 404）。 */
    @GetMapping("/change-requests/{id}/audit-logs")
    public ApiResponse<List<AuditLogResponse>> forChangeRequest(
            @AuthenticationPrincipal AppUserPrincipal actor, @PathVariable Long id) {
        changeRequestService.getViewable(actor, id);
        List<AuditLogResponse> body = auditService.listForChangeRequest(id).stream()
                .map(AuditLogResponse::from).toList();
        return ApiResponse.of(body);
    }

    /** 全監査ログ（ADMIN のみ・ページング）。 */
    @GetMapping("/audit-logs")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<PageResponse<AuditLogResponse>> all(
            @PageableDefault(size = 50, sort = "createdAt") Pageable pageable) {
        return ApiResponse.of(PageResponse.from(
                auditService.listAll(pageable), AuditLogResponse::from));
    }
}
