package com.cloudopsguard.domain.execution;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;

/**
 * 1回の実施記録（executions）。START で1行生成し、結果記録・COMPLETE/FAIL/ROLLBACK で更新する。
 * iac_apply_result と service_health_confirmed は別概念で、COMPLETED は両方を満たす時のみ（A-10）。
 * CR あたり1行前提（再実行は将来課題）。
 */
@Entity
@Table(name = "executions")
@Getter
@Setter
@NoArgsConstructor
public class Execution {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "change_request_id", nullable = false)
    private Long changeRequestId;

    @Column(name = "operator_id", nullable = false)
    private Long operatorId;

    @Enumerated(EnumType.STRING)
    @Column(name = "iac_apply_result", length = 10)
    private IacApplyResult iacApplyResult;

    @Column(name = "service_health_confirmed", nullable = false)
    private boolean serviceHealthConfirmed;

    @CreationTimestamp
    @Column(name = "started_at", nullable = false, updatable = false)
    private OffsetDateTime startedAt;

    @Column(name = "finished_at")
    private OffsetDateTime finishedAt;

    @Column(name = "rollback_performed", nullable = false)
    private boolean rollbackPerformed;

    @Column(name = "rollback_note", columnDefinition = "text")
    private String rollbackNote;

    @Column(name = "apply_run_url", columnDefinition = "text")
    private String applyRunUrl;

    @Column(name = "plan_source_ref", columnDefinition = "text")
    private String planSourceRef;
}
