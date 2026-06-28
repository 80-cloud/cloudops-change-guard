-- ============================================================================
-- V1__init.sql — CloudOps Change Guard 初期スキーマ（13テーブル）
-- ER図.md 準拠。Flyway は append-only / 後方互換で運用（既存 migration は変更しない）。
-- enum は VARCHAR で保持し、アプリ側 enum で検証する（CHECK 制約は後続 migration で追加可）。
-- 時刻はすべて TIMESTAMPTZ（UTC 前提・hibernate.jdbc.time_zone=UTC と対）。
-- ============================================================================

-- ===== users =====
CREATE TABLE users (
  id            BIGSERIAL PRIMARY KEY,
  username      VARCHAR(50)  NOT NULL UNIQUE,
  email         VARCHAR(255) NOT NULL UNIQUE,
  password_hash VARCHAR(255) NOT NULL,
  role          VARCHAR(20)  NOT NULL,   -- REQUESTER/REVIEWER/OPERATOR/ADMIN
  display_name  VARCHAR(100) NOT NULL,
  created_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
  updated_at    TIMESTAMPTZ  NOT NULL DEFAULT now()
);

-- ===== refresh_tokens（SEC-7：access 短命 + refresh DB rotation + reuse 検知） =====
-- 平文は保存せず SHA-256 hex(64) のみ保持。rotate のたびに revoked_at を打ち使い捨て。
-- 失効済みトークンの再提示＝盗用(reuse)とみなし、当該ユーザーの未失効を一括失効する。
CREATE TABLE refresh_tokens (
  id          BIGSERIAL PRIMARY KEY,
  user_id     BIGINT      NOT NULL REFERENCES users(id),
  token_hash  VARCHAR(64) NOT NULL UNIQUE,   -- SHA-256 hex（64文字）
  expires_at  TIMESTAMPTZ NOT NULL,
  revoked_at  TIMESTAMPTZ,                   -- 失効時刻。NULL=有効
  created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_rt_user ON refresh_tokens(user_id);

-- ===== change_requests（中核） =====
CREATE TABLE change_requests (
  id                   BIGSERIAL PRIMARY KEY,
  title                VARCHAR(200) NOT NULL,
  target_environment   VARCHAR(20)  NOT NULL,  -- development/staging/production
  iac_type             VARCHAR(20)  NOT NULL,  -- CLOUDFORMATION/TERRAFORM
  target_aws_service   VARCHAR(50)  NOT NULL,
  target_resource_name VARCHAR(200) NOT NULL,
  change_reason        TEXT NOT NULL,
  change_summary       TEXT NOT NULL,
  diff_text            TEXT NOT NULL,
  scheduled_at         TIMESTAMPTZ,
  rollback_procedure   TEXT,
  status               VARCHAR(20)  NOT NULL DEFAULT 'DRAFT',
  risk_level           VARCHAR(10),            -- LOW/MEDIUM/HIGH/CRITICAL（最新判定キャッシュ）
  requester_id         BIGINT NOT NULL REFERENCES users(id),
  version              BIGINT NOT NULL DEFAULT 0,   -- 楽観ロック(@Version)
  created_at           TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at           TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_cr_status    ON change_requests(status);
CREATE INDEX idx_cr_env       ON change_requests(target_environment);
CREATE INDEX idx_cr_risk      ON change_requests(risk_level);
CREATE INDEX idx_cr_requester ON change_requests(requester_id);
CREATE INDEX idx_cr_scheduled ON change_requests(scheduled_at);

-- ===== risk_assessments / risk_findings（Phase 3 で RiskEngine が書き込み） =====
CREATE TABLE risk_assessments (
  id                           BIGSERIAL PRIMARY KEY,
  change_request_id            BIGINT NOT NULL REFERENCES change_requests(id),
  risk_level                   VARCHAR(10) NOT NULL,
  is_blocked                   BOOLEAN NOT NULL DEFAULT false,
  requires_additional_approval BOOLEAN NOT NULL DEFAULT false,
  assessed_at                  TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_ra_cr ON risk_assessments(change_request_id);

CREATE TABLE risk_findings (
  id                           BIGSERIAL PRIMARY KEY,
  risk_assessment_id           BIGINT NOT NULL REFERENCES risk_assessments(id),
  rule_code                    VARCHAR(50)  NOT NULL,
  rule_name                    VARCHAR(100) NOT NULL,
  risk_level                   VARCHAR(10)  NOT NULL,
  why_dangerous                TEXT NOT NULL,
  expected_impact              TEXT NOT NULL,
  recommended_action           TEXT NOT NULL,
  is_block                     BOOLEAN NOT NULL DEFAULT false,
  requires_additional_approval BOOLEAN NOT NULL DEFAULT false
);
CREATE INDEX idx_rf_ra ON risk_findings(risk_assessment_id);

-- ===== policy_rules / policy_violations =====
CREATE TABLE policy_rules (
  id                BIGSERIAL PRIMARY KEY,
  code              VARCHAR(50)  NOT NULL UNIQUE,
  name              VARCHAR(150) NOT NULL,
  description       TEXT NOT NULL,
  environment_scope VARCHAR(20)  NOT NULL,  -- development/staging/production/ALL
  effect            VARCHAR(40)  NOT NULL,  -- BLOCK/REQUIRE_DUAL_APPROVAL/REQUIRE_ADDITIONAL_APPROVAL/REQUIRE_REASON/REQUIRE_MAINTENANCE_WINDOW
  enabled           BOOLEAN NOT NULL DEFAULT true,
  created_at        TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE policy_violations (
  id                BIGSERIAL PRIMARY KEY,
  change_request_id BIGINT NOT NULL REFERENCES change_requests(id),
  policy_rule_id    BIGINT NOT NULL REFERENCES policy_rules(id),
  effect            VARCHAR(40) NOT NULL,
  message           TEXT NOT NULL,
  detected_at       TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_pv_cr ON policy_violations(change_request_id);

-- ===== approvals =====
CREATE TABLE approvals (
  id                BIGSERIAL PRIMARY KEY,
  change_request_id BIGINT NOT NULL REFERENCES change_requests(id),
  reviewer_id       BIGINT NOT NULL REFERENCES users(id),
  decision          VARCHAR(20) NOT NULL,  -- APPROVED/REJECTED/RETURNED
  comment           TEXT,
  decided_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
  UNIQUE (change_request_id, reviewer_id)  -- 同一レビュー者の二重承認防止
);
CREATE INDEX idx_ap_cr ON approvals(change_request_id);

-- ===== pre_execution_checks =====
CREATE TABLE pre_execution_checks (
  id                BIGSERIAL PRIMARY KEY,
  change_request_id BIGINT NOT NULL REFERENCES change_requests(id),
  check_type        VARCHAR(50) NOT NULL,  -- BACKUP/ROLLBACK/MONITORING/IMPACT/STAKEHOLDER/WINDOW/APPROVAL
  is_required       BOOLEAN NOT NULL DEFAULT false,
  is_completed      BOOLEAN NOT NULL DEFAULT false,
  completed_by      BIGINT REFERENCES users(id),
  completed_at      TIMESTAMPTZ
);
CREATE INDEX idx_pec_cr ON pre_execution_checks(change_request_id);

-- ===== post_execution_health_checks =====
-- result 値は seed(checklist-defaults.json)・引き継ぎ§4 に合わせる：
--   HEALTHY / WARNING / UNHEALTHY / NOT_CHECKED
CREATE TABLE post_execution_health_checks (
  id                BIGSERIAL PRIMARY KEY,
  change_request_id BIGINT NOT NULL REFERENCES change_requests(id),
  check_item        VARCHAR(60) NOT NULL,  -- IAC_APPLY/ALB_TARGET_HEALTH/EC2_SSM/HTTP_HEALTH/CW_ALARM/APP_REACHABILITY/DB_CONNECTION/NOTE
  result            VARCHAR(12) NOT NULL,  -- HEALTHY/WARNING/UNHEALTHY/NOT_CHECKED
  note              TEXT,
  recorded_by       BIGINT NOT NULL REFERENCES users(id),
  recorded_at       TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_phc_cr ON post_execution_health_checks(change_request_id);

-- ===== executions =====
CREATE TABLE executions (
  id                       BIGSERIAL PRIMARY KEY,
  change_request_id        BIGINT NOT NULL REFERENCES change_requests(id),
  operator_id              BIGINT NOT NULL REFERENCES users(id),
  iac_apply_result         VARCHAR(10),  -- SUCCESS/FAILED
  service_health_confirmed BOOLEAN NOT NULL DEFAULT false,
  started_at               TIMESTAMPTZ NOT NULL DEFAULT now(),
  finished_at              TIMESTAMPTZ,
  rollback_performed       BOOLEAN NOT NULL DEFAULT false,
  rollback_note            TEXT
);
CREATE INDEX idx_ex_cr ON executions(change_request_id);

-- ===== comments =====
CREATE TABLE comments (
  id                BIGSERIAL PRIMARY KEY,
  change_request_id BIGINT NOT NULL REFERENCES change_requests(id),
  author_id         BIGINT NOT NULL REFERENCES users(id),
  body              TEXT NOT NULL,
  created_at        TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_cm_cr ON comments(change_request_id);

-- ===== audit_logs（追記専用・改ざん不可：UPDATE/DELETE するコードを書かない） =====
CREATE TABLE audit_logs (
  id                BIGSERIAL PRIMARY KEY,
  change_request_id BIGINT REFERENCES change_requests(id),
  actor_id          BIGINT NOT NULL REFERENCES users(id),
  action_type       VARCHAR(30) NOT NULL,  -- CREATE/EDIT/SUBMIT/APPROVE/REJECT/RETURN/CANCEL/EXECUTION_START/EXECUTION_COMPLETE/EXECUTION_FAIL/ROLLBACK/POLICY_VIOLATION/COMMENT
  before_status     VARCHAR(20),
  after_status      VARCHAR(20),
  comment           TEXT,
  summary           TEXT,                  -- 変更内容の要約
  created_at        TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_al_cr     ON audit_logs(change_request_id);
CREATE INDEX idx_al_actor  ON audit_logs(actor_id);
CREATE INDEX idx_al_action ON audit_logs(action_type);
