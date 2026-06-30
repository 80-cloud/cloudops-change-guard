package com.cloudopsguard.domain.execution;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * 既定の監視取り込み口（aws 以外の全プロファイル）。外部監視を持たず常に empty を返す。
 * これにより resolveResult は手入力 result を返し、現挙動を維持する。
 * 実取得は aws プロファイルの実 Adapter（後続増分）が担う。
 */
@Component
@Profile("!aws")
public class MockMonitoringStatusProvider implements MonitoringStatusProvider {

    @Override
    public Optional<HealthResult> fetchAlarmHealth(String monitoringRef) {
        return Optional.empty();
    }
}
