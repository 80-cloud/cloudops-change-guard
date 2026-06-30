package com.cloudopsguard.domain.risk;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * 既定の取り込み口（aws 以外の全プロファイル）。外部ソースを持たず常に empty を返す。
 * これにより resolveDiffText は手貼り fallback を返し、現挙動を維持する。
 * 実取得は aws プロファイルの実 Adapter（後続増分）が担う。
 */
@Component
@Profile("!aws")
public class MockIaCChangeProvider implements IaCChangeProvider {

    @Override
    public Optional<String> fetchPlanText(String sourceRef) {
        return Optional.empty();
    }
}
