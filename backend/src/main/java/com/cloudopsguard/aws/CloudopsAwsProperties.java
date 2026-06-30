package com.cloudopsguard.aws;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * AWS 連携設定（application.yml の cloudops.aws.*・env 駆動）。
 *
 * <p>SDK は Adapter 層（このパッケージ）に閉じ込め、Service/Controller からは直接呼ばない
 * （docs/AWS・IaC連携方針.md §1）。実 AWS への接続は aws プロファイル時のみ有効になる。
 *
 * @param region           AWS リージョン（既定 ap-northeast-1・infra/variables.tf と一致）
 * @param endpointOverride 空＝実 AWS。LocalStack 検証時のみ http://localhost:4566 を指定する切替点
 * @param planBucket       実 plan(text) を置く S3 バケット名（取込元・空なら取込しない）
 */
@ConfigurationProperties(prefix = "cloudops.aws")
public record CloudopsAwsProperties(String region, String endpointOverride, String planBucket) {
}
