package com.cloudopsguard.domain.changerequest;

import com.cloudopsguard.domain.common.ChangeRequestStatus;
import com.cloudopsguard.domain.common.Environment;
import com.cloudopsguard.domain.common.IacType;
import com.cloudopsguard.domain.common.RiskLevel;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;

/**
 * 変更申請（中核・ER図.md §2-2）。ステータスは状態機械（{@link ChangeRequestStateMachine}）でのみ進める。
 * {@code @Version} で同時更新の競合を検知する（R-03）。時刻は Hibernate が自動設定（B1）。
 */
@Entity
@Table(name = "change_requests")
@Getter
@Setter
@NoArgsConstructor
public class ChangeRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String title;

    /** development/staging/production（小文字保持・EnvironmentConverter が変換）。 */
    @Column(name = "target_environment", nullable = false, length = 20)
    private Environment targetEnvironment;

    @Enumerated(EnumType.STRING)
    @Column(name = "iac_type", nullable = false, length = 20)
    private IacType iacType;

    @Column(name = "target_aws_service", length = 50)
    private String targetAwsService;

    @Column(name = "target_resource_name", length = 200)
    private String targetResourceName;

    @Column(name = "change_reason", columnDefinition = "text")
    private String changeReason;

    @Column(name = "change_summary", columnDefinition = "text")
    private String changeSummary;

    @Column(name = "diff_text", columnDefinition = "text")
    private String diffText;

    @Column(name = "scheduled_at")
    private OffsetDateTime scheduledAt;

    @Column(name = "rollback_procedure", columnDefinition = "text")
    private String rollbackProcedure;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ChangeRequestStatus status = ChangeRequestStatus.DRAFT;

    /** 最新リスク判定のキャッシュ（正本は risk_assessments・Phase 3）。 */
    @Enumerated(EnumType.STRING)
    @Column(name = "risk_level", length = 10)
    private RiskLevel riskLevel;

    @Column(name = "requester_id", nullable = false)
    private Long requesterId;

    @Version
    @Column(nullable = false)
    private Long version;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    /** 所有者判定（IDOR 対策）。 */
    public boolean isOwnedBy(Long userId) {
        return requesterId != null && requesterId.equals(userId);
    }
}
