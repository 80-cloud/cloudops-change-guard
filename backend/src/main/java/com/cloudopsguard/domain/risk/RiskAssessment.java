package com.cloudopsguard.domain.risk;

import com.cloudopsguard.domain.common.RiskLevel;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;

/**
 * リスク判定の記録（ER図.md §2-3・risk_assessments）。1回の判定の集約結果を保持し、
 * 明細は {@link RiskAssessmentFinding}（risk_findings）に紐づく。change_requests.risk_level は最新判定のキャッシュ。
 */
@Entity
@Table(name = "risk_assessments")
@Getter
@Setter
@NoArgsConstructor
public class RiskAssessment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "change_request_id", nullable = false)
    private Long changeRequestId;

    @Enumerated(EnumType.STRING)
    @Column(name = "risk_level", nullable = false, length = 10)
    private RiskLevel riskLevel;

    @Column(name = "is_blocked", nullable = false)
    private boolean blocked;

    @Column(name = "requires_additional_approval", nullable = false)
    private boolean requiresAdditionalApproval;

    @CreationTimestamp
    @Column(name = "assessed_at", nullable = false, updatable = false)
    private OffsetDateTime assessedAt;
}
