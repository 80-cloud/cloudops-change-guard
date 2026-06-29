package com.cloudopsguard.domain.dashboard;

import com.cloudopsguard.common.ApiResponse;
import com.cloudopsguard.domain.dashboard.dto.DashboardSummaryResponse;
import com.cloudopsguard.security.AppUserPrincipal;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** ダッシュボード集計 API（GET /api/v1/dashboard/summary・認証必須・ロールで範囲調整・SCR-01）。 */
@RestController
@RequestMapping("/api/v1/dashboard")
public class DashboardController {

    private final DashboardService service;

    public DashboardController(DashboardService service) {
        this.service = service;
    }

    @GetMapping("/summary")
    public ApiResponse<DashboardSummaryResponse> summary(@AuthenticationPrincipal AppUserPrincipal actor) {
        return ApiResponse.of(service.summary(actor));
    }
}
