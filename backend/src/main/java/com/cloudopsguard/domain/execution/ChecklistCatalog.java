package com.cloudopsguard.domain.execution;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * checklist-defaults.json を読み込む雛形カタログ（RiskRuleCatalog と同方式・起動時1回・不変）。
 * 実施前チェックの標準項目／COMPLETED 判定に必須なヘルス項目（requiredForCompletion）を供給する。
 */
@Component
public class ChecklistCatalog {

    private static final String RESOURCE = "data/checklist-defaults.json";

    private final List<PreCheckDefault> preChecks;
    private final List<HealthCheckDefault> healthChecks;
    private final Set<HealthCheckItem> requiredForCompletion;

    public ChecklistCatalog(ObjectMapper objectMapper) {
        try (InputStream in = new ClassPathResource(RESOURCE).getInputStream()) {
            Config config = objectMapper.readValue(in, Config.class);
            this.preChecks = List.copyOf(config.preExecutionChecks());
            this.healthChecks = List.copyOf(config.postExecutionHealthChecks());
            this.requiredForCompletion = config.requiredForCompletion().isEmpty()
                    ? EnumSet.noneOf(HealthCheckItem.class)
                    : EnumSet.copyOf(config.requiredForCompletion());
        } catch (IOException e) {
            throw new UncheckedIOException("checklist-defaults.json の読み込みに失敗しました", e);
        }
    }

    public List<PreCheckDefault> preChecks() {
        return preChecks;
    }

    public List<HealthCheckDefault> healthChecks() {
        return healthChecks;
    }

    /** COMPLETED 判定で全 HEALTHY を要求するヘルス項目（service_health_confirmed の母集合）。 */
    public Set<HealthCheckItem> requiredForCompletion() {
        return requiredForCompletion;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record PreCheckDefault(CheckType checkType, String label, boolean requiredInProd, String hint) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record HealthCheckDefault(HealthCheckItem checkItem, String label, String hint) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record Config(List<PreCheckDefault> preExecutionChecks,
                          List<HealthCheckDefault> postExecutionHealthChecks,
                          List<HealthCheckItem> requiredForCompletion) {
    }
}
