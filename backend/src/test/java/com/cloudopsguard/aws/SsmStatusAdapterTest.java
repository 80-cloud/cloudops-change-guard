package com.cloudopsguard.aws;

import com.cloudopsguard.domain.execution.HealthResult;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.ssm.SsmClient;
import software.amazon.awssdk.services.ssm.model.DescribeInstanceInformationRequest;
import software.amazon.awssdk.services.ssm.model.DescribeInstanceInformationResponse;
import software.amazon.awssdk.services.ssm.model.InstanceInformation;
import software.amazon.awssdk.services.ssm.model.PingStatus;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * PingStatus→HealthResult の写像を mock で網羅（実 AWS 不要）。
 * LocalStack community は managed instance を作れず DescribeInstanceInformation を意味あるテストにできないため結合は持たない。
 */
class SsmStatusAdapterTest {

    private static DescribeInstanceInformationResponse withPings(PingStatus... pings) {
        List<InstanceInformation> infos = Arrays.stream(pings)
                .map(p -> InstanceInformation.builder().instanceId("i-1").pingStatus(p).build())
                .toList();
        return DescribeInstanceInformationResponse.builder().instanceInformationList(infos).build();
    }

    private static SsmStatusAdapter adapterReturning(DescribeInstanceInformationResponse resp) {
        SsmClient ssm = mock(SsmClient.class);
        when(ssm.describeInstanceInformation(any(DescribeInstanceInformationRequest.class))).thenReturn(resp);
        return new SsmStatusAdapter(ssm);
    }

    @Test
    void すべてONLINEならHEALTHY() {
        assertThat(adapterReturning(withPings(PingStatus.ONLINE, PingStatus.ONLINE)).fetchInstanceHealth("i-1"))
                .contains(HealthResult.HEALTHY);
    }

    @Test
    void いずれかCONNECTION_LOSTならUNHEALTHY() {
        assertThat(adapterReturning(withPings(PingStatus.ONLINE, PingStatus.CONNECTION_LOST)).fetchInstanceHealth("i-1"))
                .contains(HealthResult.UNHEALTHY);
    }

    @Test
    void LOSTなしINACTIVEありならWARNING() {
        assertThat(adapterReturning(withPings(PingStatus.ONLINE, PingStatus.INACTIVE)).fetchInstanceHealth("i-1"))
                .contains(HealthResult.WARNING);
    }

    @Test
    void 該当インスタンスなしはempty() {
        assertThat(adapterReturning(withPings()).fetchInstanceHealth("i-1")).isEmpty();
    }

    @Test
    void refが空ならempty() {
        SsmStatusAdapter adapter = new SsmStatusAdapter(mock(SsmClient.class));
        assertThat(adapter.fetchInstanceHealth(null)).isEmpty();
        assertThat(adapter.fetchInstanceHealth("  ")).isEmpty();
    }
}
