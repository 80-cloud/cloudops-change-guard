package com.cloudopsguard.domain.approval;

import com.cloudopsguard.domain.common.Decision;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;

/**
 * 承認・却下・差し戻しの記録（ER図.md §2-7）。
 * {@code UNIQUE(change_request_id, reviewer_id)} で同一レビュー者の二重承認を防ぐ。
 * 自己承認禁止（reviewer_id ≠ requester_id）は状態機械のガードで担保する。
 */
@Entity
@Table(name = "approvals",
        uniqueConstraints = @UniqueConstraint(columnNames = {"change_request_id", "reviewer_id"}))
@Getter
@Setter
@NoArgsConstructor
public class Approval {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "change_request_id", nullable = false)
    private Long changeRequestId;

    @Column(name = "reviewer_id", nullable = false)
    private Long reviewerId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Decision decision;

    @Column(columnDefinition = "text")
    private String comment;

    @CreationTimestamp
    @Column(name = "decided_at", nullable = false, updatable = false)
    private OffsetDateTime decidedAt;
}
