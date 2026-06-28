package com.cloudopsguard.domain.audit;

import com.cloudopsguard.domain.common.AuditAction;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;

/**
 * 監査ログ（追記専用・ER図.md §2-12）。「誰が(actor)・いつ(createdAt)・どの申請に(changeRequestId)・
 * 何を(action)・前後の状態(before/after)」を記録する。UPDATE/DELETE は一切しない（改ざん不可）。
 */
@Entity
@Table(name = "audit_logs")
@Getter
@Setter
@NoArgsConstructor
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 対象変更申請。ポリシー定義操作等は NULL 可。 */
    @Column(name = "change_request_id")
    private Long changeRequestId;

    @Column(name = "actor_id", nullable = false)
    private Long actorId;

    @Enumerated(EnumType.STRING)
    @Column(name = "action_type", nullable = false, length = 30)
    private AuditAction actionType;

    @Column(name = "before_status", length = 20)
    private String beforeStatus;

    @Column(name = "after_status", length = 20)
    private String afterStatus;

    @Column(name = "comment")
    private String comment;

    /** 変更内容の要約。 */
    @Column(name = "summary")
    private String summary;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;
}
