package com.cloudopsguard.aws;

import com.cloudopsguard.domain.execution.HealthResult;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.cloudwatch.CloudWatchClient;
import software.amazon.awssdk.services.cloudwatch.model.DescribeAlarmsRequest;
import software.amazon.awssdk.services.cloudwatch.model.DescribeAlarmsResponse;
import software.amazon.awssdk.services.cloudwatch.model.MetricAlarm;
import software.amazon.awssdk.services.cloudwatch.model.StateValue;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * アラーム状態→HealthResult の写像を mock で網羅（実 AWS 不要）。
 */
class CloudWatchMonitoringAdapterTest {

    private static DescribeAlarmsResponse withStates(StateValue... states) {
        List<MetricAlarm> alarms = Arrays.stream(states)
                .map(s -> MetricAlarm.builder().alarmName("a").stateValue(s).build())
                .toList();
        return DescribeAlarmsResponse.builder().metricAlarms(alarms).build();
    }

    private static CloudWatchMonitoringAdapter adapterReturning(DescribeAlarmsResponse resp) {
        CloudWatchClient cw = mock(CloudWatchClient.class);
        when(cw.describeAlarms(any(DescribeAlarmsRequest.class))).thenReturn(resp);
        return new CloudWatchMonitoringAdapter(cw);
    }

    @Test
    void すべてOKならHEALTHY() {
        assertThat(adapterReturning(withStates(StateValue.OK, StateValue.OK)).fetchAlarmHealth("x"))
                .contains(HealthResult.HEALTHY);
    }

    @Test
    void いずれかALARMならUNHEALTHY() {
        assertThat(adapterReturning(withStates(StateValue.OK, StateValue.ALARM)).fetchAlarmHealth("x"))
                .contains(HealthResult.UNHEALTHY);
    }

    @Test
    void ALARMなしINSUFFICIENTありならWARNING() {
        assertThat(adapterReturning(withStates(StateValue.OK, StateValue.INSUFFICIENT_DATA)).fetchAlarmHealth("x"))
                .contains(HealthResult.WARNING);
    }

    @Test
    void 該当アラームなしはempty() {
        assertThat(adapterReturning(withStates()).fetchAlarmHealth("x")).isEmpty();
    }

    @Test
    void refが空ならempty() {
        CloudWatchMonitoringAdapter adapter = new CloudWatchMonitoringAdapter(mock(CloudWatchClient.class));
        assertThat(adapter.fetchAlarmHealth(null)).isEmpty();
        assertThat(adapter.fetchAlarmHealth("  ")).isEmpty();
    }
}
