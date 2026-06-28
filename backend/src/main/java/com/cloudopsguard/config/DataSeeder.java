package com.cloudopsguard.config;

import com.cloudopsguard.domain.common.Role;
import com.cloudopsguard.domain.policy.PolicyRule;
import com.cloudopsguard.domain.policy.PolicyRuleRepository;
import com.cloudopsguard.domain.user.User;
import com.cloudopsguard.domain.user.UserRepository;
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
 * policy_rules は data/policy-rules.json から投入する（未投入時のみ）。いずれも冪等。
 * パスワードは env（app.seed.*）から取得し、コードに直書きしない。
 */
@Component
public class DataSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private final SeedProperties props;
    private final UserRepository userRepository;
    private final PolicyRuleRepository policyRuleRepository;
    private final PasswordEncoder passwordEncoder;
    private final ObjectMapper objectMapper;

    public DataSeeder(SeedProperties props, UserRepository userRepository,
                      PolicyRuleRepository policyRuleRepository, PasswordEncoder passwordEncoder,
                      ObjectMapper objectMapper) {
        this.props = props;
        this.userRepository = userRepository;
        this.policyRuleRepository = policyRuleRepository;
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
     * policy-rules.json の1要素。policy_rules テーブルに載る項目のみ取り、その他
     * （appliesToRuleCodes / appliesToRiskLevels / message＝Phase 3 評価器用）は無視する
     * （Spring の ObjectMapper は未知プロパティを既定で無視する）。
     */
    private record PolicyRuleSeed(String code, String name, String description,
                                  String environmentScope, String effect, boolean enabled) {
    }
}
