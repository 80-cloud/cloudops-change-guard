package com.cloudopsguard.domain.risk;

import com.cloudopsguard.domain.common.RiskLevel;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * リスク検知の明細レコード（ER図.md §2-3・risk_findings）。{@link RiskAssessment} に紐づく。
 * 値オブジェクト {@link RiskFinding}（エンジン出力）を永続化したもの。plainMeaning は表示用で列を持たない。
 */
@Entity
@Table(name = "risk_findings")
@Getter
@Setter
@NoArgsConstructor
public class RiskAssessmentFinding {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "risk_assessment_id", nullable = false)
    private Long riskAssessmentId;

    @Column(name = "rule_code", nullable = false, length = 50)
    private String ruleCode;

    @Column(name = "rule_name", nullable = false, length = 100)
    private String ruleName;

    @Enumerated(EnumType.STRING)
    @Column(name = "risk_level", nullable = false, length = 10)
    private RiskLevel riskLevel;

    @Column(name = "why_dangerous", nullable = false, columnDefinition = "text")
    private String whyDangerous;

    @Column(name = "expected_impact", nullable = false, columnDefinition = "text")
    private String expectedImpact;

    @Column(name = "recommended_action", nullable = false, columnDefinition = "text")
    private String recommendedAction;

    @Column(name = "is_block", nullable = false)
    private boolean block;

    @Column(name = "requires_additional_approval", nullable = false)
    private boolean requiresAdditionalApproval;
}
