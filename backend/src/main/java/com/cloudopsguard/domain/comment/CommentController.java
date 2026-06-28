package com.cloudopsguard.domain.comment;

import com.cloudopsguard.common.ApiResponse;
import com.cloudopsguard.domain.comment.dto.CommentResponse;
import com.cloudopsguard.domain.comment.dto.CreateComment;
import com.cloudopsguard.security.AppUserPrincipal;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** コメント API（/api/v1/change-requests/{id}/comments）。 */
@RestController
@RequestMapping("/api/v1/change-requests/{id}/comments")
public class CommentController {

    private final CommentService service;

    public CommentController(CommentService service) {
        this.service = service;
    }

    @GetMapping
    public ApiResponse<List<CommentResponse>> list(
            @AuthenticationPrincipal AppUserPrincipal actor, @PathVariable Long id) {
        List<CommentResponse> body = service.list(actor, id).stream()
                .map(CommentResponse::from).toList();
        return ApiResponse.of(body);
    }

    @PostMapping
    public ResponseEntity<ApiResponse<CommentResponse>> create(
            @AuthenticationPrincipal AppUserPrincipal actor, @PathVariable Long id,
            @Valid @RequestBody CreateComment req) {
        Comment saved = service.create(actor, id, req.body());
        return ResponseEntity.status(201).body(ApiResponse.of(CommentResponse.from(saved)));
    }
}
