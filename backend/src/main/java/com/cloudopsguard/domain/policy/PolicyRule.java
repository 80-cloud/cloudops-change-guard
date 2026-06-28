package com.cloudopsguard.domain.policy;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;

/**
 * ポリシー定義（ER図.md §2-5）。Phase 2 は seed 投入と（将来の）一覧表示が役割。
 * 条件評価（effect の適用）は Phase 3 の評価器が行う。environment_scope / effect は値域が広いため
 * 文字列で保持し、enum 化は評価器実装時に検討する。
 */
@Entity
@Table(name = "policy_rules")
@Getter
@Setter
@NoArgsConstructor
public class PolicyRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String code;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(nullable = false, columnDefinition = "text")
    private String description;

    /** development/staging/production/ALL。 */
    @Column(name = "environment_scope", nullable = false, length = 20)
    private String environmentScope;

    /** BLOCK / REQUIRE_DUAL_APPROVAL / REQUIRE_ADDITIONAL_APPROVAL / REQUIRE_REASON / REQUIRE_MAINTENANCE_WINDOW。 */
    @Column(nullable = false, length = 40)
    private String effect;

    @Column(nullable = false)
    private boolean enabled = true;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;
}
