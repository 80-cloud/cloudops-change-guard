package com.cloudopsguard.risk;

import com.cloudopsguard.domain.changerequest.ChangeRequest;
import com.cloudopsguard.domain.common.Environment;
import com.cloudopsguard.domain.common.IacType;
import com.cloudopsguard.domain.common.RiskLevel;
import com.cloudopsguard.domain.risk.CloudFormationDiffParser;
import com.cloudopsguard.domain.risk.IacDiffParser;
import com.cloudopsguard.domain.risk.RiskAssessmentResult;
import com.cloudopsguard.domain.risk.RiskEngine;
import com.cloudopsguard.domain.risk.RiskRule;
import com.cloudopsguard.domain.risk.RiskRuleCatalog;
import com.cloudopsguard.domain.risk.TerraformDiffParser;
import com.cloudopsguard.domain.risk.rules.RdsDeleteOrReplaceRule;
import com.cloudopsguard.domain.risk.rules.Ssh22OpenWorldRule;
import com.cloudopsguard.domain.risk.rules.TfDestroyBulkRule;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * RiskEngine の純ロジック単体テスト（Spring/DB 非依存・risk-rules.json を classpath から読む）。
 * 受入 A-4/A-5：該当差分で検知・非該当で不検知・環境による level/ブロック変化（リスク判定ルール.md §5）。
 */
class RiskEngineTest {

    private RiskEngine engine;

    @BeforeEach
    void setUp() {
        RiskRuleCatalog catalog = new RiskRuleCatalog(new ObjectMapper());
        List<IacDiffParser> parsers = List.of(new TerraformDiffParser(), new CloudFormationDiffParser());
        List<RiskRule> rules = List.of(
                new RdsDeleteOrReplaceRule(), new Ssh22OpenWorldRule(), new TfDestroyBulkRule());
        engine = new RiskEngine(rules, parsers, catalog);
    }

    private ChangeRequest cr(Environment env, IacType iac, String diff) {
        ChangeRequest c = new ChangeRequest();
        c.setTargetEnvironment(env);
        c.setIacType(iac);
        c.setDiffText(diff);
        return c;
    }

    @Test
    void rds削除は本番でCRITICALかつブロック() {
        RiskAssessmentResult r = engine.assess(cr(Environment.PRODUCTION, IacType.TERRAFORM,
                "# aws_db_instance.main will be destroyed"));

        assertThat(r.riskLevel()).isEqualTo(RiskLevel.CRITICAL);
        assertThat(r.blocked()).isTrue();
        assertThat(r.findings()).extracting("ruleCode").containsExactly("RDS_DELETE_OR_REPLACE");
    }

    @Test
    void rds削除は開発ではCRITICALだがブロックされない() {
        RiskAssessmentResult r = engine.assess(cr(Environment.DEVELOPMENT, IacType.TERRAFORM,
                "# aws_db_instance.main will be destroyed"));

        assertThat(r.riskLevel()).isEqualTo(RiskLevel.CRITICAL);
        assertThat(r.blocked()).isFalse();   // isBlockInProd は本番のみ
    }

    @Test
    void ssh22の全世界公開はブロック対象() {
        String diff = """
                + resource "aws_security_group_rule" "ssh" {
                +   type        = "ingress"
                +   from_port   = 22
                +   to_port     = 22
                +   cidr_blocks = ["0.0.0.0/0"]
                + }
                """;
        RiskAssessmentResult r = engine.assess(cr(Environment.STAGING, IacType.TERRAFORM, diff));

        assertThat(r.blocked()).isTrue();
        assertThat(r.findings()).extracting("ruleCode").contains("SSH_22_OPEN_WORLD");
    }

    @Test
    void 一括destroyは本番でブロック() {
        String diff = """
                # aws_s3_bucket.logs will be destroyed
                # aws_instance.web will be destroyed
                """;
        RiskAssessmentResult r = engine.assess(cr(Environment.PRODUCTION, IacType.TERRAFORM, diff));

        assertThat(r.findings()).extracting("ruleCode").contains("TF_DESTROY_BULK");
        assertThat(r.blocked()).isTrue();
    }

    @Test
    void 無害なタグ追加は検知なしでLOW() {
        RiskAssessmentResult r = engine.assess(cr(Environment.PRODUCTION, IacType.TERRAFORM,
                "# aws_instance.web will be updated in-place\n+ tags = { Name = \"web\" }"));

        assertThat(r.findings()).isEmpty();
        assertThat(r.riskLevel()).isEqualTo(RiskLevel.LOW);
        assertThat(r.blocked()).isFalse();
    }
}
