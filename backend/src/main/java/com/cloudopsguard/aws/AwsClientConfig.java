package com.cloudopsguard.aws;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.cloudwatch.CloudWatchClient;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.sts.StsClient;

import java.net.URI;

/**
 * 実 AWS クライアントの組み立て。aws プロファイル時のみ有効（既定/local/test では bean を作らない）。
 *
 * <p>資格情報は {@link DefaultCredentialsProvider}（環境変数→プロファイル→IAM ロールを自動解決）で取得し、
 * キーをコードや DB に置かない（CLAUDE.md §5）。endpointOverride を設定すると LocalStack 等へ向け替える
 * （その場合 S3 は path-style を強制＝LocalStack/MinIO 互換）。
 */
@Configuration
@Profile("aws")
public class AwsClientConfig {

    @Bean
    public StsClient stsClient(CloudopsAwsProperties props) {
        var builder = StsClient.builder()
                .region(Region.of(props.region()))
                .credentialsProvider(DefaultCredentialsProvider.create());
        if (hasOverride(props)) {
            builder = builder.endpointOverride(URI.create(props.endpointOverride()));
        }
        return builder.build();
    }

    @Bean
    public S3Client s3Client(CloudopsAwsProperties props) {
        var builder = S3Client.builder()
                .region(Region.of(props.region()))
                .credentialsProvider(DefaultCredentialsProvider.create());
        if (hasOverride(props)) {
            builder = builder.endpointOverride(URI.create(props.endpointOverride()))
                    .forcePathStyle(true);
        }
        return builder.build();
    }

    @Bean
    public CloudWatchClient cloudWatchClient(CloudopsAwsProperties props) {
        var builder = CloudWatchClient.builder()
                .region(Region.of(props.region()))
                .credentialsProvider(DefaultCredentialsProvider.create());
        if (hasOverride(props)) {
            builder = builder.endpointOverride(URI.create(props.endpointOverride()));
        }
        return builder.build();
    }

    private static boolean hasOverride(CloudopsAwsProperties props) {
        return props.endpointOverride() != null && !props.endpointOverride().isBlank();
    }
}
