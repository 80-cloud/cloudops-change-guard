package com.cloudopsguard.domain.comment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** コメント投稿。 */
public record CreateComment(
        @NotBlank @Size(max = 5000) String body) {
}
