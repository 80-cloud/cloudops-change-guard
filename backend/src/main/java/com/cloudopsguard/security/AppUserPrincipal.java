package com.cloudopsguard.security;

import com.cloudopsguard.domain.common.Role;

/**
 * 認証済みユーザーの最小情報。SecurityContext の principal として保持し、
 * 所有者検証（IDOR）・ロール判定に使う。クライアント入力ではなく、検証済み access トークンから導出する。
 * コントローラでは {@code @AuthenticationPrincipal AppUserPrincipal} で受け取る。
 */
public record AppUserPrincipal(Long userId, String username, Role role) {
}
