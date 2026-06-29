package com.cloudopsguard.config;

import com.cloudopsguard.domain.changerequest.ChangeRequest;
import com.cloudopsguard.domain.changerequest.ChangeRequestRepository;
import com.cloudopsguard.domain.changerequest.ChangeRequestService;
import com.cloudopsguard.domain.changerequest.dto.CreateChangeRequest;
import com.cloudopsguard.domain.common.Environment;
import com.cloudopsguard.domain.common.IacType;
import com.cloudopsguard.domain.common.Role;
import com.cloudopsguard.domain.common.TransitionAction;
import com.cloudopsguard.domain.policy.PolicyRule;
import com.cloudopsguard.domain.policy.PolicyRuleRepository;
import com.cloudopsguard.domain.user.User;
import com.cloudopsguard.domain.user.UserRepository;
import com.cloudopsguard.security.AppUserPrincipal;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.util.List;

/**
 * 初期データ投入（app.seed.enabled=true 時のみ）。
 *
 * <p>初期ユーザー：admin(ADMIN) / req1(REQUESTER) / rev1・rev2(REVIEWER) / op1(OPERATOR)。
 * rev1/rev2 の2名は CRITICAL 二重承認テスト（Phase 3）に必要なため Phase 2 から作る。
 * policy_rules は data/policy-rules.json から投入する（未投入時のみ）。
 * デモ用の変更申請も、空のときだけ実サービス経由で投入する（risk 判定・承認履歴・監査も本物になる）。
 * いずれも冪等。パスワードは env（app.seed.*）から取得し、コードに直書きしない。
 */
@Component
public class DataSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private final SeedProperties props;
    private final UserRepository userRepository;
    private final PolicyRuleRepository policyRuleRepository;
    private final ChangeRequestRepository changeRequestRepository;
    private final ChangeRequestService changeRequestService;
    private final PasswordEncoder passwordEncoder;
    private final ObjectMapper objectMapper;

    public DataSeeder(SeedProperties props, UserRepository userRepository,
                      PolicyRuleRepository policyRuleRepository,
                      ChangeRequestRepository changeRequestRepository,
                      ChangeRequestService changeRequestService,
                      PasswordEncoder passwordEncoder, ObjectMapper objectMapper) {
        this.props = props;
        this.userRepository = userRepository;
        this.policyRuleRepository = policyRuleRepository;
        this.changeRequestRepository = changeRequestRepository;
        this.changeRequestService = changeRequestService;
        this.passwordEncoder = passwordEncoder;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) throws Exception {
        if (!props.enabled()) {
            return;
        }
        seedUsers();
        seedPolicies();
        seedChangeRequests();
    }

    private void seedUsers() {
        if (userRepository.count() > 0) {
            return;
        }
        userRepository.save(newUser(props.adminUsername(), "管理者", Role.ADMIN, props.adminPassword()));
        userRepository.save(newUser("req1", "申請 太郎", Role.REQUESTER, props.defaultPassword()));
        userRepository.save(newUser("rev1", "査閲 一郎", Role.REVIEWER, props.defaultPassword()));
        userRepository.save(newUser("rev2", "査閲 二郎", Role.REVIEWER, props.defaultPassword()));
        userRepository.save(newUser("op1", "実施 花子", Role.OPERATOR, props.defaultPassword()));
        log.info("[seed] 初期ユーザー5名を作成しました");
    }

    private User newUser(String username, String displayName, Role role, String rawPassword) {
        User u = new User();
        u.setUsername(username);
        u.setEmail(username + "@cloudops.local");
        u.setPasswordHash(passwordEncoder.encode(rawPassword));
        u.setDisplayName(displayName);
        u.setRole(role);
        return u;
    }

    private void seedPolicies() throws Exception {
        if (policyRuleRepository.count() > 0) {
            return;
        }
        try (InputStream in = new ClassPathResource("data/policy-rules.json").getInputStream()) {
            List<PolicyRuleSeed> seeds = objectMapper.readValue(in,
                    objectMapper.getTypeFactory().constructCollectionType(List.class, PolicyRuleSeed.class));
            for (PolicyRuleSeed s : seeds) {
                if (policyRuleRepository.existsByCode(s.code())) {
                    continue;
                }
                PolicyRule rule = new PolicyRule();
                rule.setCode(s.code());
                rule.setName(s.name());
                rule.setDescription(s.description());
                rule.setEnvironmentScope(s.environmentScope());
                rule.setEffect(s.effect());
                rule.setEnabled(s.enabled());
                policyRuleRepository.save(rule);
            }
            log.info("[seed] policy_rules を {} 件投入しました", seeds.size());
        }
    }

    /**
     * デモ用の変更申請を投入（空のときだけ）。状態を直接 set せず実サービス経由で遷移させ、
     * リスク判定・承認履歴・監査ログまで本物にする。ダッシュボード/承認待ち/一覧が初回から賑わう。
     */
    private void seedChangeRequests() {
        if (changeRequestRepository.count() > 0) {
            return;
        }
        User req1 = userRepository.findByUsername("req1").orElse(null);
        User rev1 = userRepository.findByUsername("rev1").orElse(null);
        if (req1 == null || rev1 == null) {
            return;
        }
        AppUserPrincipal requester = principal(req1);
        AppUserPrincipal reviewer = principal(rev1);

        // 1) DRAFT（低）：下書きのまま。
        changeRequestService.create(requester, new CreateChangeRequest(
                "開発S3バケットにコスト配賦タグを追加", Environment.DEVELOPMENT, IacType.TERRAFORM,
                "S3", "dev-logs-bucket", "コスト可視化のためのタグ付与", "タグのみ追加する低リスク変更",
                "# aws_s3_bucket.logs will be updated in-place\n+ tags = { CostCenter = \"dev\" }",
                null, null));

        // 2) UNDER_REVIEW（中）：検証環境のアラーム削除。提出→査閲開始で承認待ちに残す。
        Long id2 = changeRequestService.create(requester, new CreateChangeRequest(
                "検証環境の不要なCloudWatchアラームを削除", Environment.STAGING, IacType.TERRAFORM,
                "CloudWatch", "staging-cpu-alarm", "誤検知が続くアラームの整理", "アラーム1件を削除",
                "# aws_cloudwatch_metric_alarm.cpu will be destroyed",
                null, null)).getId();
        changeRequestService.transition(requester, id2, TransitionAction.SUBMIT, null);
        changeRequestService.transition(reviewer, id2, TransitionAction.REVIEW_START, null);

        // 3) APPROVED（低）：dev のタグ統一。提出→査閲開始→承認（低リスクは1名で確定）。
        Long id3 = changeRequestService.create(requester, new CreateChangeRequest(
                "開発EC2のNameタグを命名規約に合わせる", Environment.DEVELOPMENT, IacType.TERRAFORM,
                "EC2", "dev-app-1", "命名規約の統一", "Nameタグを規約に合わせて更新",
                "# aws_instance.app will be updated in-place\n+ tags = { Name = \"dev-app-1\" }",
                null, null)).getId();
        changeRequestService.transition(requester, id3, TransitionAction.SUBMIT, null);
        changeRequestService.transition(reviewer, id3, TransitionAction.REVIEW_START, null);
        changeRequestService.transition(reviewer, id3, TransitionAction.APPROVE, null);

        // 4) UNDER_REVIEW（重大）：開発RDS削除（dev は非ブロックだが CRITICAL）。承認待ち＆高リスクの見本。
        Long id4 = changeRequestService.create(requester, new CreateChangeRequest(
                "開発RDSインスタンスの再作成（削除を含む）", Environment.DEVELOPMENT, IacType.TERRAFORM,
                "RDS", "dev-app-db", "パラメータ刷新のための作り直し", "既存インスタンスを削除して再作成",
                "# aws_db_instance.main will be destroyed",
                null, "削除前スナップショットから復元する")).getId();
        changeRequestService.transition(requester, id4, TransitionAction.SUBMIT, null);
        changeRequestService.transition(reviewer, id4, TransitionAction.REVIEW_START, null);

        log.info("[seed] デモ変更申請を投入しました（DRAFT / UNDER_REVIEW×2 / APPROVED）");
    }

    private AppUserPrincipal principal(User u) {
        return new AppUserPrincipal(u.getId(), u.getUsername(), u.getRole());
    }

    /**
     * policy-rules.json の1要素。policy_rules テーブルに載る項目のみ取り、その他
     * （appliesToRuleCodes / appliesToRiskLevels / message＝Phase 3 評価器用）は無視する
     * （Spring の ObjectMapper は未知プロパティを既定で無視する）。
     */
    private record PolicyRuleSeed(String code, String name, String description,
                                  String environmentScope, String effect, boolean enabled) {
    }
}
