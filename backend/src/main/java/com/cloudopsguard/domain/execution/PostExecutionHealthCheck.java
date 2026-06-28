package com.cloudopsguard.domain.execution;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;

/**
 * 実施後ヘルスチェック1件（post_execution_health_checks）。OPERATOR が変更対象環境の正常性を
 * 記録する追記専用レコード（A-8・UPDATE/DELETE なし）。
 */
@Entity
@Table(name = "post_execution_health_checks")
@Getter
@Setter
@NoArgsConstructor
public class PostExecutionHealthCheck {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "change_request_id", nullable = false)
    private Long changeRequestId;

    @Enumerated(EnumType.STRING)
    @Column(name = "check_item", nullable = false, length = 60)
    private HealthCheckItem checkItem;

    @Enumerated(EnumType.STRING)
    @Column(name = "result", nullable = false, length = 12)
    private HealthResult result;

    @Column(columnDefinition = "text")
    private String note;

    @Column(name = "recorded_by", nullable = false)
    private Long recordedBy;

    @CreationTimestamp
    @Column(name = "recorded_at", nullable = false, updatable = false)
    private OffsetDateTime recordedAt;
}
