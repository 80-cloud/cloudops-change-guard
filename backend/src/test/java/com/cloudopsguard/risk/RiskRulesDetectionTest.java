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
import com.cloudopsguard.domain.risk.rules.AlbDeleteRule;
import com.cloudopsguard.domain.risk.rules.AwsConfigStopDeleteRule;
import com.cloudopsguard.domain.risk.rules.CloudTrailStopDeleteRule;
import com.cloudopsguard.domain.risk.rules.CwAlarmDeleteRule;
import com.cloudopsguard.domain.risk.rules.IamAdminAccessRule;
import com.cloudopsguard.domain.risk.rules.IamWildcardRule;
import com.cloudopsguard.domain.risk.rules.ProdEc2ReplaceRule;
import com.cloudopsguard.domain.risk.rules.ProdEcsLargeChangeRule;
import com.cloudopsguard.domain.risk.rules.RdsDeleteOrReplaceRule;
import com.cloudopsguard.domain.risk.rules.S3BucketDeleteRule;
import com.cloudopsguard.domain.risk.rules.SgOpenWorldRule;
import com.cloudopsguard.domain.risk.rules.Ssh22OpenWorldRule;
import com.cloudopsguard.domain.risk.rules.TargetGroupDeleteRule;
import com.cloudopsguard.domain.risk.rules.TfDestroyBulkRule;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 全14ルールを配線したエンジンで、Increment 2 で追加した11ルールの検知/非検知を検証する
 * （リスク判定ルール.md §3/§5・受入 A-4/A-5）。ALB↔TargetGroup の取り違え防止、S3 の本番昇格、
 * EC2 の本番ゲートなど、紛らわしいケースを重点的に確認する。
 */
class RiskRulesDetectionTest {

    private RiskEngine engine;

    @BeforeEach
    void setUp() {
        RiskRuleCatalog catalog = new RiskRuleCatalog(new ObjectMapper());
        List<IacDiffParser> parsers = List.of(new TerraformDiffParser(), new CloudFormationDiffParser());
        List<RiskRule> rules = List.of(
                new RdsDeleteOrReplaceRule(), new Ssh22OpenWorldRule(), new TfDestroyBulkRule(),
                new S3BucketDeleteRule(), new SgOpenWorldRule(), new IamAdminAccessRule(),
                new IamWildcardRule(), new CloudTrailStopDeleteRule(), new AwsConfigStopDeleteRule(),
                new CwAlarmDeleteRule(), new AlbDeleteRule(), new TargetGroupDeleteRule(),
                new ProdEc2ReplaceRule(), new ProdEcsLargeChangeRule());
        engine = new RiskEngine(rules, parsers, catalog);
    }

    private RiskAssessmentResult assess(Environment env, IacType iac, String diff) {
        ChangeRequest c = new ChangeRequest();
        c.setTargetEnvironment(env);
        c.setIacType(iac);
        c.setDiffText(diff);
        return engine.assess(c);
    }

    private List<String> codes(RiskAssessmentResult r) {
        return r.findings().stream().map(f -> f.ruleCode()).toList();
    }

    @Test
    void s3削除は本番でCRITICALに昇格しブロック_開発ではHIGH非ブロック() {
        RiskAssessmentResult prod = assess(Environment.PRODUCTION, IacType.TERRAFORM,
                "# aws_s3_bucket.assets will be destroyed");
        assertThat(codes(prod)).contains("S3_BUCKET_DELETE");
        assertThat(prod.riskLevel()).isEqualTo(RiskLevel.CRITICAL);
        assertThat(prod.blocked()).isTrue();

        RiskAssessmentResult dev = assess(Environment.DEVELOPMENT, IacType.TERRAFORM,
                "# aws_s3_bucket.assets will be destroyed");
        assertThat(dev.riskLevel()).isEqualTo(RiskLevel.HIGH);
        assertThat(dev.blocked()).isFalse();
    }

    @Test
    void sgの全世界公開はブロック_ポート22以外でも検知() {
        RiskAssessmentResult r = assess(Environment.STAGING, IacType.TERRAFORM,
                "+ from_port = 443\n+ cidr_blocks = [\"0.0.0.0/0\"]");
        assertThat(codes(r)).contains("SG_OPEN_WORLD");
        assertThat(codes(r)).doesNotContain("SSH_22_OPEN_WORLD");
        assertThat(r.blocked()).isTrue();
    }

    @Test
    void iam管理者権限は追加承認かつ本番ブロック() {
        RiskAssessmentResult r = assess(Environment.PRODUCTION, IacType.TERRAFORM,
                "+ policy_arn = \"arn:aws:iam::aws:policy/AdministratorAccess\"");
        assertThat(codes(r)).contains("IAM_ADMIN_ACCESS");
        assertThat(r.requiresAdditionalApproval()).isTrue();
        assertThat(r.blocked()).isTrue();
    }

    @Test
    void iamワイルドカードはHIGH非ブロックで追加承認() {
        RiskAssessmentResult r = assess(Environment.STAGING, IacType.TERRAFORM,
                "+ \"Action\": \"*\"\n+ \"Resource\": \"*\"");
        assertThat(codes(r)).contains("IAM_WILDCARD");
        assertThat(r.riskLevel()).isEqualTo(RiskLevel.HIGH);
        assertThat(r.blocked()).isFalse();
        assertThat(r.requiresAdditionalApproval()).isTrue();
    }

    @Test
    void cloudtrail削除はブロック_config削除は非ブロック() {
        RiskAssessmentResult ct = assess(Environment.PRODUCTION, IacType.TERRAFORM,
                "# aws_cloudtrail.main will be destroyed");
        assertThat(codes(ct)).contains("CLOUDTRAIL_STOP_DELETE");
        assertThat(ct.blocked()).isTrue();

        RiskAssessmentResult cfg = assess(Environment.PRODUCTION, IacType.TERRAFORM,
                "# aws_config_configuration_recorder.main will be destroyed");
        assertThat(codes(cfg)).contains("AWSCONFIG_STOP_DELETE");
        assertThat(cfg.blocked()).isFalse();
    }

    @Test
    void cwアラーム削除はMEDIUM単独検知() {
        RiskAssessmentResult r = assess(Environment.PRODUCTION, IacType.TERRAFORM,
                "# aws_cloudwatch_metric_alarm.cpu_high will be destroyed");
        assertThat(codes(r)).containsExactly("CW_ALARM_DELETE");
        assertThat(r.riskLevel()).isEqualTo(RiskLevel.MEDIUM);
        assertThat(r.blocked()).isFalse();
    }

    @Test
    void albとtargetGroupは取り違えない() {
        RiskAssessmentResult alb = assess(Environment.PRODUCTION, IacType.TERRAFORM,
                "# aws_lb.public will be destroyed");
        assertThat(codes(alb)).contains("ALB_DELETE");
        assertThat(codes(alb)).doesNotContain("TARGET_GROUP_DELETE");

        RiskAssessmentResult tg = assess(Environment.PRODUCTION, IacType.TERRAFORM,
                "# aws_lb_target_group.app will be destroyed");
        assertThat(codes(tg)).contains("TARGET_GROUP_DELETE");
        assertThat(codes(tg)).doesNotContain("ALB_DELETE");
    }

    @Test
    void ec2置換は本番のみ検知_開発では検知しない() {
        RiskAssessmentResult prod = assess(Environment.PRODUCTION, IacType.TERRAFORM,
                "# aws_instance.web must be replaced");
        assertThat(codes(prod)).contains("PROD_EC2_REPLACE");

        RiskAssessmentResult dev = assess(Environment.DEVELOPMENT, IacType.TERRAFORM,
                "# aws_instance.web must be replaced");
        assertThat(codes(dev)).doesNotContain("PROD_EC2_REPLACE");
    }

    @Test
    void ecs大規模変更は本番で検知() {
        RiskAssessmentResult r = assess(Environment.PRODUCTION, IacType.TERRAFORM,
                "# aws_ecs_service.app will be updated in-place\n~ desired_count = 2 -> 10");
        assertThat(codes(r)).contains("PROD_ECS_LARGE_CHANGE");
        assertThat(r.riskLevel()).isEqualTo(RiskLevel.HIGH);
    }

    @Test
    void cloudformationのrds削除も検知できる() {
        RiskAssessmentResult r = assess(Environment.PRODUCTION, IacType.CLOUDFORMATION,
                "AWS::RDS::DBInstance  Remove");
        assertThat(codes(r)).contains("RDS_DELETE_OR_REPLACE");
        assertThat(r.blocked()).isTrue();
    }
}
