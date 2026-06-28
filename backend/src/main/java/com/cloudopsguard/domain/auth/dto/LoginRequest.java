package com.cloudopsguard.domain.auth.dto;

import jakarta.validation.constraints.NotBlank;

/** ログイン要求（username + password）。 */
public record LoginRequest(
        @NotBlank String username,
        @NotBlank String password) {
}
