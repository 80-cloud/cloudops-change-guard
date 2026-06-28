package com.cloudopsguard.domain.comment.dto;

import com.cloudopsguard.domain.comment.Comment;

import java.time.OffsetDateTime;

/** コメント応答 DTO。 */
public record CommentResponse(Long id, Long changeRequestId, Long authorId,
                              String body, OffsetDateTime createdAt) {

    public static CommentResponse from(Comment c) {
        return new CommentResponse(c.getId(), c.getChangeRequestId(), c.getAuthorId(),
                c.getBody(), c.getCreatedAt());
    }
}
