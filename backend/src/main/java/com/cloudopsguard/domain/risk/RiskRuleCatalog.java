package com.cloudopsguard.domain.risk;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * risk-rules.json を読み込み、ruleCode で引ける定義カタログにする。
 * 説明文・統制はここから供給し、検知ロジック（{@link RiskRule}）と分離する（データ駆動・ADR-0003）。
 *
 * <p>起動時に1回読み込む（不変）。Spring Bean としても、テストの手動生成としても使える。
 */
@Component
public class RiskRuleCatalog {

    private static final String RESOURCE = "data/risk-rules.json";

    private final Map<String, RiskRuleDefinition> byCode;

    public RiskRuleCatalog(ObjectMapper objectMapper) {
        try (InputStream in = new ClassPathResource(RESOURCE).getInputStream()) {
            List<RiskRuleDefinition> defs = objectMapper.readValue(in,
                    objectMapper.getTypeFactory()
                            .constructCollectionType(List.class, RiskRuleDefinition.class));
            this.byCode = defs.stream().collect(Collectors.toUnmodifiableMap(
                    RiskRuleDefinition::ruleCode, Function.identity()));
        } catch (IOException e) {
            throw new UncheckedIOException("risk-rules.json の読み込みに失敗しました", e);
        }
    }

    /** ruleCode から定義を引く（未登録は設定ミスとして例外）。 */
    public RiskRuleDefinition byCode(String ruleCode) {
        RiskRuleDefinition def = byCode.get(ruleCode);
        if (def == null) {
            throw new IllegalStateException("risk-rules.json に未定義の ruleCode: " + ruleCode);
        }
        return def;
    }
}
