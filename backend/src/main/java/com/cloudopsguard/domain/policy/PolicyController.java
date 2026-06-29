package com.cloudopsguard.domain.policy;

import com.cloudopsguard.common.ApiResponse;
import com.cloudopsguard.domain.policy.dto.PolicyResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** ポリシー一覧 API（GET /api/v1/policies・ADMIN のみ・MVP は閲覧のみ）。 */
@RestController
@RequestMapping("/api/v1/policies")
public class PolicyController {

    private final PolicyService service;

    public PolicyController(PolicyService service) {
        this.service = service;
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<List<PolicyResponse>> list() {
        List<PolicyResponse> body = service.listAll().stream().map(PolicyResponse::from).toList();
        return ApiResponse.of(body);
    }
}
