package com.cloudopsguard.support;

import com.cloudopsguard.domain.approval.ApprovalRepository;
import com.cloudopsguard.domain.audit.AuditLogRepository;
import com.cloudopsguard.domain.auth.RefreshTokenRepository;
import com.cloudopsguard.domain.changerequest.ChangeRequest;
import com.cloudopsguard.domain.changerequest.ChangeRequestRepository;
import com.cloudopsguard.domain.comment.CommentRepository;
import com.cloudopsguard.domain.common.ChangeRequestStatus;
import com.cloudopsguard.domain.common.Environment;
import com.cloudopsguard.domain.common.IacType;
import com.cloudopsguard.domain.common.Role;
import com.cloudopsguard.domain.execution.ExecutionRepository;
import com.cloudopsguard.domain.execution.PostExecutionHealthCheckRepository;
import com.cloudopsguard.domain.execution.PreExecutionCheckRepository;
import com.cloudopsguard.domain.policy.PolicyRuleRepository;
import com.cloudopsguard.domain.policy.PolicyViolationRepository;
import com.cloudopsguard.domain.risk.RiskAssessmentFindingRepository;
import com.cloudopsguard.domain.risk.RiskAssessmentRepository;
import com.cloudopsguard.domain.user.User;
import com.cloudopsguard.domain.user.UserRepository;
import com.cloudopsguard.security.AppUserPrincipal;
import org.junit.jupiter.api.AfterEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * 結合テストの基盤。本番同等の PostgreSQL（Testcontainers）で Flyway を実行し、方言差を避ける。
 *
 * <p>各テストはサービスを実コミットさせ、{@link #cleanup()} で実 DB を掃除する（テスト間の独立性）。
 * 単一トランザクションでロールバックする方式は、reuse 検知など「例外で TX を rollback-only にする」
 * 経路の検証が崩れるため採らない。
 *
 * <p><b>シングルトンコンテナ方式</b>：コンテナは static 初期化で一度だけ起動し、{@code @Container} で
 * 止めない（Ryuk が JVM 終了時に回収）。{@code @SpringBootTest} のコンテキストはクラス間でキャッシュ
 * 再利用されるため、クラスごとにコンテナを停止すると後続クラスの DataSource が死んだポートを指して
 * 接続失敗する。1コンテナを全テストで共有してこれを避ける。
 */
@SpringBootTest
@ActiveProfiles("test")
public abstract class AbstractIntegrationTest {

    @SuppressWarnings("resource") // 生存期間は JVM 全体（Ryuk が回収）。明示 close しない。
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:16")
                    .withDatabaseName("cloudops")
                    .withUsername("cloudops")
                    .withPassword("cloudops");

    static {
        POSTGRES.start();
    }

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired protected UserRepository userRepository;
    @Autowired protected ChangeRequestRepository changeRequestRepository;
    @Autowired protected ApprovalRepository approvalRepository;
    @Autowired protected AuditLogRepository auditLogRepository;
    @Autowired protected CommentRepository commentRepository;
    @Autowired protected RefreshTokenRepository refreshTokenRepository;
    @Autowired protected PolicyRuleRepository policyRuleRepository;
    @Autowired protected PolicyViolationRepository policyViolationRepository;
    @Autowired protected RiskAssessmentRepository riskAssessmentRepository;
    @Autowired protected RiskAssessmentFindingRepository riskAssessmentFindingRepository;
    @Autowired protected PreExecutionCheckRepository preExecutionCheckRepository;
    @Autowired protected PostExecutionHealthCheckRepository postExecutionHealthCheckRepository;
    @Autowired protected ExecutionRepository executionRepository;
    @Autowired protected PasswordEncoder passwordEncoder;

    @AfterEach
    void cleanup() {
        // FK 安全な順序で削除（子 → 親）。
        auditLogRepository.deleteAll();
        approvalRepository.deleteAll();
        commentRepository.deleteAll();
        riskAssessmentFindingRepository.deleteAll();   // risk_assessments の子
        riskAssessmentRepository.deleteAll();           // change_requests の子
        policyViolationRepository.deleteAll();           // change_requests / policy_rules の子
        executionRepository.deleteAll();                 // change_requests の子
        preExecutionCheckRepository.deleteAll();         // change_requests の子
        postExecutionHealthCheckRepository.deleteAll();  // change_requests の子
        changeRequestRepository.deleteAll();
        refreshTokenRepository.deleteAll();
        policyRuleRepository.deleteAll();
        userRepository.deleteAll();
    }

    // ---- テストデータのファクトリ ----

    protected User createUser(String username, Role role) {
        User u = new User();
        u.setUsername(username);
        u.setEmail(username + "@test.local");
        u.setPasswordHash(passwordEncoder.encode("pw"));
        u.setDisplayName(username);
        u.setRole(role);
        return userRepository.save(u);
    }

    protected AppUserPrincipal principal(User u) {
        return new AppUserPrincipal(u.getId(), u.getUsername(), u.getRole());
    }

    /** 指定状態の変更申請を直接用意する（テストの arrange 用。本文は必須項目を充足させておく）。 */
    protected ChangeRequest createChangeRequest(User requester, ChangeRequestStatus status) {
        ChangeRequest cr = new ChangeRequest();
        cr.setTitle("RDS インスタンスクラスの変更");
        cr.setTargetEnvironment(Environment.STAGING);
        cr.setIacType(IacType.TERRAFORM);
        cr.setTargetAwsService("RDS");
        cr.setTargetResourceName("app-db");
        cr.setChangeReason("性能要件のためインスタンスタイプを引き上げる");
        cr.setChangeSummary("db.t3.medium → db.t3.large");
        cr.setDiffText("- instance_class = db.t3.medium\n+ instance_class = db.t3.large");
        cr.setStatus(status);
        cr.setRequesterId(requester.getId());
        return changeRequestRepository.save(cr);
    }
}
