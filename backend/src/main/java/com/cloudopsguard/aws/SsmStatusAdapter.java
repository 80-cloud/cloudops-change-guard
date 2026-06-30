package com.cloudopsguard.aws;

import com.cloudopsguard.domain.execution.HealthResult;
import com.cloudopsguard.domain.execution.InstanceStatusProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.ssm.SsmClient;
import software.amazon.awssdk.services.ssm.model.DescribeInstanceInformationRequest;
import software.amazon.awssdk.services.ssm.model.InstanceInformation;
import software.amazon.awssdk.services.ssm.model.InstanceInformationStringFilter;
import software.amazon.awssdk.services.ssm.model.PingStatus;

import java.util.List;
import java.util.Optional;

/**
 * 実 SSM インスタンス状態の取り込み口（aws プロファイル）。instanceRef をインスタンス ID として
 * DescribeInstanceInformation（read-only）し、PingStatus を HealthResult へ写像する：
 * いずれか CONNECTION_LOST→UNHEALTHY／INACTIVE→WARNING／すべて ONLINE→HEALTHY／該当なし→empty（手入力 fallback）。
 * 書込/削除 API は持たない（誤操作防止・docs/AWS・IaC連携方針.md §5）。
 */
@Component
@Profile("aws")
public class SsmStatusAdapter implements InstanceStatusProvider {

    private static final Logger log = LoggerFactory.getLogger(SsmStatusAdapter.class);

    private final SsmClient ssm;

    public SsmStatusAdapter(SsmClient ssm) {
        this.ssm = ssm;
    }

    @Override
    public Optional<HealthResult> fetchInstanceHealth(String instanceRef) {
        if (instanceRef == null || instanceRef.isBlank()) {
            return Optional.empty();
        }
        try {
            List<InstanceInformation> infos = ssm.describeInstanceInformation(
                    DescribeInstanceInformationRequest.builder()
                            .filters(InstanceInformationStringFilter.builder()
                                    .key("InstanceIds").values(instanceRef).build())
                            .build()).instanceInformationList();
            if (infos.isEmpty()) {
                return Optional.empty();
            }
            if (infos.stream().anyMatch(i -> i.pingStatus() == PingStatus.CONNECTION_LOST)) {
                return Optional.of(HealthResult.UNHEALTHY);
            }
            if (infos.stream().anyMatch(i -> i.pingStatus() == PingStatus.INACTIVE)) {
                return Optional.of(HealthResult.WARNING);
            }
            return Optional.of(HealthResult.HEALTHY);
        } catch (Exception e) {
            log.warn("SSM インスタンス情報取得失敗 instanceRef={}: {}", instanceRef, e.toString());
            return Optional.empty();
        }
    }
}
