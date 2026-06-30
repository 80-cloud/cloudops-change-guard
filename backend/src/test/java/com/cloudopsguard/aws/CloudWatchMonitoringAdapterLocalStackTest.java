package com.cloudopsguard.aws;

import com.cloudopsguard.domain.execution.HealthResult;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.cloudwatch.CloudWatchClient;
import software.amazon.awssdk.services.cloudwatch.model.ComparisonOperator;
import software.amazon.awssdk.services.cloudwatch.model.PutMetricAlarmRequest;
import software.amazon.awssdk.services.cloudwatch.model.Statistic;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * CloudWatchMonitoringAdapter の結合テスト（LocalStack の CloudWatch・実 AWS/課金なし）。
 * 実 DescribeAlarms の疎通を検証：存在するアラームは present（状態値は LocalStack 評価依存のため present のみ）、
 * 該当なしは empty。状態→HealthResult の写像は単体テスト（mock）で網羅済み。
 */
@Testcontainers
class CloudWatchMonitoringAdapterLocalStackTest {

    @Container
    static final LocalStackContainer LOCALSTACK = new LocalStackContainer(
            DockerImageName.parse("localstack/localstack:3"))
            .withServices(LocalStackContainer.Service.CLOUDWATCH);

    private static CloudWatchClient cw;
    private static CloudWatchMonitoringAdapter adapter;

    @BeforeAll
    static void setUp() {
        cw = CloudWatchClient.builder()
                .endpointOverride(LOCALSTACK.getEndpoint())
                .region(Region.of(LOCALSTACK.getRegion()))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(LOCALSTACK.getAccessKey(), LOCALSTACK.getSecretKey())))
                .build();
        cw.putMetricAlarm(PutMetricAlarmRequest.builder()
                .alarmName("live-alarm").namespace("Custom").metricName("Errors")
                .comparisonOperator(ComparisonOperator.GREATER_THAN_THRESHOLD)
                .threshold(1.0).evaluationPeriods(1).period(60).statistic(Statistic.SUM)
                .build());
        adapter = new CloudWatchMonitoringAdapter(cw);
    }

    @AfterAll
    static void tearDown() {
        if (cw != null) {
            cw.close();
        }
    }

    @Test
    void 存在するアラームをDescribeできる() {
        Optional<HealthResult> result = adapter.fetchAlarmHealth("live-alarm");
        assertThat(result).isPresent();
    }

    @Test
    void 該当アラームなしはemptyを返す() {
        assertThat(adapter.fetchAlarmHealth("no-such-alarm")).isEmpty();
    }
}
