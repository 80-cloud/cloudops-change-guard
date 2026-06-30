package com.cloudopsguard.aws;

import com.cloudopsguard.domain.execution.HealthResult;
import com.cloudopsguard.domain.execution.MonitoringStatusProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.cloudwatch.CloudWatchClient;
import software.amazon.awssdk.services.cloudwatch.model.DescribeAlarmsRequest;
import software.amazon.awssdk.services.cloudwatch.model.MetricAlarm;
import software.amazon.awssdk.services.cloudwatch.model.StateValue;

import java.util.List;
import java.util.Optional;

/**
 * 実 CloudWatch アラーム状態の取り込み口（aws プロファイル）。monitoringRef をアラーム名プレフィックスとして
 * DescribeAlarms（read-only）し、状態を HealthResult へ写像する：
 * いずれか ALARM→UNHEALTHY／INSUFFICIENT_DATA→WARNING／すべて OK→HEALTHY／該当なし→empty（手入力 fallback）。
 * 書込/削除 API は持たない（誤操作防止・docs/AWS・IaC連携方針.md §5）。
 */
@Component
@Profile("aws")
public class CloudWatchMonitoringAdapter implements MonitoringStatusProvider {

    private static final Logger log = LoggerFactory.getLogger(CloudWatchMonitoringAdapter.class);

    private final CloudWatchClient cloudWatch;

    public CloudWatchMonitoringAdapter(CloudWatchClient cloudWatch) {
        this.cloudWatch = cloudWatch;
    }

    @Override
    public Optional<HealthResult> fetchAlarmHealth(String monitoringRef) {
        if (monitoringRef == null || monitoringRef.isBlank()) {
            return Optional.empty();
        }
        try {
            List<MetricAlarm> alarms = cloudWatch.describeAlarms(
                    DescribeAlarmsRequest.builder().alarmNamePrefix(monitoringRef).build()).metricAlarms();
            if (alarms.isEmpty()) {
                return Optional.empty();
            }
            if (alarms.stream().anyMatch(a -> a.stateValue() == StateValue.ALARM)) {
                return Optional.of(HealthResult.UNHEALTHY);
            }
            if (alarms.stream().anyMatch(a -> a.stateValue() == StateValue.INSUFFICIENT_DATA)) {
                return Optional.of(HealthResult.WARNING);
            }
            return Optional.of(HealthResult.HEALTHY);
        } catch (Exception e) {
            log.warn("CloudWatch アラーム取得失敗 prefix={}: {}", monitoringRef, e.toString());
            return Optional.empty();
        }
    }
}
