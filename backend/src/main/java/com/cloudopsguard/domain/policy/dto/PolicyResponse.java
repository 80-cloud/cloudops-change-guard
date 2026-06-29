package com.cloudopsguard.domain.policy.dto;

import com.cloudopsguard.domain.policy.PolicyRule;

/**
 * ポリシー定義1件の応答（GET /policies・SCR-06）。effect / environment_scope は値域が広く
 * 文字列で保持しているため、そのまま文字列で返す（PolicyRule の方針に合わせる）。
 */
public record PolicyResponse(
        String code,
        String name,
        String environmentScope,
        String effect,
        boolean enabled) {

    public static PolicyResponse from(PolicyRule p) {
        return new PolicyResponse(
                p.getCode(), p.getName(), p.getEnvironmentScope(), p.getEffect(), p.isEnabled());
    }
}
