package com.cloudopsguard.domain.policy;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.List;

/**
 * policy-rules.json を読み込み、評価器が参照する定義一覧にする（RiskRuleCatalog と同じデータ駆動方針）。
 * DB の policy_rules テーブルは一覧・seed 用の写しで、評価ロジックはこの JSON を正とする。
 */
@Component
public class PolicyCatalog {

    private static final String RESOURCE = "data/policy-rules.json";

    private final List<PolicyRuleDefinition> definitions;

    public PolicyCatalog(ObjectMapper objectMapper) {
        try (InputStream in = new ClassPathResource(RESOURCE).getInputStream()) {
            this.definitions = objectMapper.readValue(in,
                    objectMapper.getTypeFactory()
                            .constructCollectionType(List.class, PolicyRuleDefinition.class));
        } catch (IOException e) {
            throw new UncheckedIOException("policy-rules.json の読み込みに失敗しました", e);
        }
    }

    /** 全定義（enabled の判定は評価器側で行う）。 */
    public List<PolicyRuleDefinition> all() {
        return definitions;
    }
}
