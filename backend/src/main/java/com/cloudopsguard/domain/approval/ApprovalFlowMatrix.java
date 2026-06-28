package com.cloudopsguard.domain.approval;

import com.cloudopsguard.domain.common.Environment;
import com.cloudopsguard.domain.common.RiskLevel;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * approval-flow.json の承認要件マトリクス（環境×リスク＝全12セル）を読み込み、引けるようにする。
 * 承認段数・承認前提（実施予定日時・ロールバック手順）の単一データ源（状態遷移設計.md §5.5）。
 */
@Component
public class ApprovalFlowMatrix {

    private static final String RESOURCE = "data/approval-flow.json";

    private final Map<Environment, Map<RiskLevel, ApprovalRequirement>> matrix = new EnumMap<>(Environment.class);

    public ApprovalFlowMatrix(ObjectMapper objectMapper) {
        try (InputStream in = new ClassPathResource(RESOURCE).getInputStream()) {
            Config config = objectMapper.readValue(in, Config.class);
            for (Cell cell : config.matrix()) {
                matrix.computeIfAbsent(cell.environment(), e -> new EnumMap<>(RiskLevel.class))
                        .put(cell.riskLevel(), new ApprovalRequirement(
                                cell.requiredReviewerApprovals(),
                                cell.distinctApprovers(),
                                cell.requireScheduledAt(),
                                cell.requireRollbackProcedure(),
                                cell.requirePreChecksComplete()));
            }
        } catch (IOException e) {
            throw new UncheckedIOException("approval-flow.json の読み込みに失敗しました", e);
        }
    }

    /** 環境×リスクの承認要件（未定義のセルは設定ミスとして例外）。 */
    public ApprovalRequirement requirementFor(Environment environment, RiskLevel riskLevel) {
        ApprovalRequirement req = matrix.getOrDefault(environment, Map.of()).get(riskLevel);
        if (req == null) {
            throw new IllegalStateException(
                    "approval-flow.json に未定義のセル: " + environment + " × " + riskLevel);
        }
        return req;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record Config(List<Cell> matrix) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record Cell(
            Environment environment,
            RiskLevel riskLevel,
            int requiredReviewerApprovals,
            boolean distinctApprovers,
            boolean requireScheduledAt,
            boolean requireRollbackProcedure,
            boolean requirePreChecksComplete) {
    }
}
