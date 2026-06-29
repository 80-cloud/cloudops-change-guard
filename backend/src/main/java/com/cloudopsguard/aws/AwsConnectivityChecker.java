package com.cloudopsguard.aws;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.sts.StsClient;
import software.amazon.awssdk.services.sts.model.GetCallerIdentityResponse;

/**
 * 起動時に一度だけ STS GetCallerIdentity を呼び、AWS への疎通（資格情報・リージョン・エンドポイント）を確認する。
 * read-only でリソース操作はしない。aws プロファイル時のみ有効。
 *
 * <p>疎通失敗は WARN ログに留め、起動は止めない（AWS 未整備でもアプリ本体＝変更申請フローは動かすため）。
 */
@Component
@Profile("aws")
public class AwsConnectivityChecker implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AwsConnectivityChecker.class);

    private final StsClient stsClient;

    public AwsConnectivityChecker(StsClient stsClient) {
        this.stsClient = stsClient;
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            GetCallerIdentityResponse identity = stsClient.getCallerIdentity();
            log.info("[AWS疎通] 成功 account={} arn={}（read-only・リソース操作なし）",
                    identity.account(), identity.arn());
        } catch (Exception e) {
            log.warn("[AWS疎通] 失敗：STS GetCallerIdentity を呼べませんでした（資格情報/リージョン/エンドポイント未整備の可能性）: {}",
                    e.toString());
        }
    }
}
