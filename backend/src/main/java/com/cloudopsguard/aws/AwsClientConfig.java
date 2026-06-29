package com.cloudopsguard.aws;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sts.StsClient;

import java.net.URI;

/**
 * 実 AWS クライアントの組み立て。aws プロファイル時のみ有効（既定/local/test では bean を作らない）。
 *
 * <p>資格情報は {@link DefaultCredentialsProvider}（環境変数→プロファイル→IAM ロールを自動解決）で取得し、
 * キーをコードや DB に置かない（CLAUDE.md §5）。endpointOverride を設定すると LocalStack 等へ向け替える。
 */
@Configuration
@Profile("aws")
public class AwsClientConfig {

    @Bean
    public StsClient stsClient(CloudopsAwsProperties props) {
        var builder = StsClient.builder()
                .region(Region.of(props.region()))
                .credentialsProvider(DefaultCredentialsProvider.create());
        if (props.endpointOverride() != null && !props.endpointOverride().isBlank()) {
            builder = builder.endpointOverride(URI.create(props.endpointOverride()));
        }
        return builder.build();
    }
}
