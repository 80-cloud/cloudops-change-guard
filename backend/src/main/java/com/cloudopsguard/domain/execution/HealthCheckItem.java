package com.cloudopsguard.domain.execution;

/** 実施後ヘルスチェックの項目（post_execution_health_checks.check_item）。 */
public enum HealthCheckItem {
    IAC_APPLY, ALB_TARGET_HEALTH, EC2_SSM, HTTP_HEALTH,
    CW_ALARM, APP_REACHABILITY, DB_CONNECTION, NOTE
}
