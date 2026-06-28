package com.cloudopsguard.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * シード設定（application.yml の app.seed.*）。
 *
 * @param enabled         初期データ投入の有効/無効（テストは false）
 * @param adminUsername   初期 ADMIN のログイン ID
 * @param adminPassword   初期 ADMIN のパスワード（env 駆動・本番は強い値に）
 * @param defaultPassword admin 以外の初期ユーザー（req1/rev1/rev2/op1）のパスワード
 */
@ConfigurationProperties(prefix = "app.seed")
public record SeedProperties(
        boolean enabled,
        String adminUsername,
        String adminPassword,
        String defaultPassword) {
}
