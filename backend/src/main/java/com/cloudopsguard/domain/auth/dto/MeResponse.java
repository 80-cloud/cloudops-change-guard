package com.cloudopsguard.domain.auth.dto;

import com.cloudopsguard.domain.common.Role;
import com.cloudopsguard.domain.user.User;

/** 認証ユーザーの情報（GET /auth/me・ログイン応答 body）。トークンは body に載せない。 */
public record MeResponse(Long id, String username, String displayName, Role role) {

    public static MeResponse from(User u) {
        return new MeResponse(u.getId(), u.getUsername(), u.getDisplayName(), u.getRole());
    }
}
