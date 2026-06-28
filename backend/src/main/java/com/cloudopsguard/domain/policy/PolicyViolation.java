package com.cloudopsguard.domain.policy;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;

/**
 * ポリシー違反の記録（ER図.md §2-5・policy_violations）。
 * リスク判定時に該当した統制を1件ずつ残し、監査・表示に使う。
 */
@Entity
@Table(name = "policy_violations")
@Getter
@Setter
@NoArgsConstructor
public class PolicyViolation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "change_request_id", nullable = false)
    private Long changeRequestId;

    @Column(name = "policy_rule_id", nullable = false)
    private Long policyRuleId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private PolicyEffect effect;

    @Column(nullable = false, columnDefinition = "text")
    private String message;

    @CreationTimestamp
    @Column(name = "detected_at", nullable = false, updatable = false)
    private OffsetDateTime detectedAt;
}
