package com.cloudopsguard.domain.changerequest;

import com.cloudopsguard.common.exception.ForbiddenException;
import com.cloudopsguard.common.exception.IllegalStateTransitionException;
import com.cloudopsguard.common.exception.ValidationException;
import com.cloudopsguard.domain.common.ChangeRequestStatus;
import com.cloudopsguard.domain.common.Role;
import com.cloudopsguard.domain.common.TransitionAction;
import com.cloudopsguard.security.AppUserPrincipal;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.cloudopsguard.domain.common.ChangeRequestStatus.*;
import static com.cloudopsguard.domain.common.TransitionAction.*;

/**
 * 変更申請の状態遷移を一元管理するステートマシン（状態遷移設計.md §2/§4）。
 *
 * <p>遷移は {@link #transition} 経由のみ。Controller / Service から status を直接 set しない（受入 A-9）。
 * 許可表にない遷移は 409（{@link IllegalStateTransitionException}・現在状態と許可操作付き）、
 * ロール不足は 403（{@link ForbiddenException}）で拒否する。
 *
 * <p>Phase 2 で実装するガード：所有者検証・自己承認禁止・承認後編集ロック・SUBMIT 必須項目・差し戻しコメント必須。
 * リスク/ポリシー連動（BLOCK・承認段数・本番の必須チェック完了）は<b>フック点のみ</b>用意し、本実装は Phase 3/4。
 */
@Component
public class ChangeRequestStateMachine {

    /** 遷移時に渡す追加情報（差し戻しコメント・実施予定日時など）。 */
    public record TransitionContext(String comment, java.time.OffsetDateTime scheduledAt) {
        public static TransitionContext empty() {
            return new TransitionContext(null, null);
        }

        public static TransitionContext withComment(String comment) {
            return new TransitionContext(comment, null);
        }
    }

    @FunctionalInterface
    private interface Guard {
        void check(ChangeRequest cr, AppUserPrincipal actor, TransitionContext ctx);
    }

    /** 1つの許可遷移：遷移先・許可ロール・ガード。 */
    private record Rule(ChangeRequestStatus to, Set<Role> roles, Guard guard) {
    }

    // ---- ガード（再利用する小さな述語） ----
    private static final Guard NO_GUARD = (cr, actor, ctx) -> { };

    /** REQUESTER は自分の申請のみ操作可（ADMIN 等はオーナー不問）。 */
    private static final Guard OWNER_IF_REQUESTER = (cr, actor, ctx) -> {
        if (actor.role() == Role.REQUESTER && !cr.isOwnedBy(actor.userId())) {
            // 存在を秘匿するため、所有不一致は 403 ではなく上位（Service）で 404 にする方針もあるが、
            // ここでは操作権限の不足として 403。閲覧経路の IDOR は Service 側で 404 にする。
            throw new ForbiddenException("自分の申請ではないため操作できません");
        }
    };

    /** 自己承認禁止（approver ≠ requester・受入 A-2）。 */
    private static final Guard NOT_SELF_APPROVAL = (cr, actor, ctx) -> {
        if (cr.isOwnedBy(actor.userId())) {
            throw new ForbiddenException("自分が申請した変更は承認できません");
        }
    };

    /** 差し戻しはコメント必須。 */
    private static final Guard RETURN_NEEDS_COMMENT = (cr, actor, ctx) -> {
        if (ctx.comment() == null || ctx.comment().isBlank()) {
            throw new ValidationException("差し戻しにはコメントが必要です");
        }
    };

    /** SUBMIT 時の必須項目充足（状態遷移設計.md §4）。所有者であることも併せて確認。 */
    private static final Guard SUBMIT_GUARD = (cr, actor, ctx) -> {
        OWNER_IF_REQUESTER.check(cr, actor, ctx);
        requireText(cr.getTitle(), "title");
        requireNonNull(cr.getTargetEnvironment(), "target_environment");
        requireNonNull(cr.getIacType(), "iac_type");
        requireText(cr.getTargetAwsService(), "target_aws_service");
        requireText(cr.getTargetResourceName(), "target_resource_name");
        requireText(cr.getChangeReason(), "change_reason");
        requireText(cr.getChangeSummary(), "change_summary");
        requireText(cr.getDiffText(), "diff_text");
        // フック点（Phase 3）：リスク判定の実行と BLOCK ポリシー該当時の PolicyBlockedException。
    };

    /**
     * 許可遷移表。{@code Map<現在状態, Map<アクション, ルール>>}。
     * cancel の許可ロールは状態で異なる（実施前は所有者/ADMIN、実施系は OPERATOR/ADMIN）。
     */
    private final Map<ChangeRequestStatus, Map<TransitionAction, Rule>> table = Map.of(
            DRAFT, Map.of(
                    SUBMIT, new Rule(SUBMITTED, Set.of(Role.REQUESTER), SUBMIT_GUARD),
                    CANCEL, new Rule(CANCELLED, Set.of(Role.REQUESTER, Role.ADMIN), OWNER_IF_REQUESTER)),
            RETURNED, Map.of(
                    // MVP：差し戻し後も submit を許可（API設計.md の DRAFT/RETURNED→SUBMITTED に準拠）。
                    SUBMIT, new Rule(SUBMITTED, Set.of(Role.REQUESTER), SUBMIT_GUARD),
                    CANCEL, new Rule(CANCELLED, Set.of(Role.REQUESTER, Role.ADMIN), OWNER_IF_REQUESTER)),
            SUBMITTED, Map.of(
                    REVIEW_START, new Rule(UNDER_REVIEW, Set.of(Role.REVIEWER), NO_GUARD),
                    CANCEL, new Rule(CANCELLED, Set.of(Role.REQUESTER, Role.ADMIN), OWNER_IF_REQUESTER)),
            UNDER_REVIEW, Map.of(
                    APPROVE, new Rule(APPROVED, Set.of(Role.REVIEWER), NOT_SELF_APPROVAL),
                    REJECT, new Rule(REJECTED, Set.of(Role.REVIEWER), NO_GUARD),
                    RETURN_, new Rule(RETURNED, Set.of(Role.REVIEWER), RETURN_NEEDS_COMMENT),
                    CANCEL, new Rule(CANCELLED, Set.of(Role.REQUESTER, Role.ADMIN), OWNER_IF_REQUESTER)),
            APPROVED, Map.of(
                    SCHEDULE, new Rule(SCHEDULED, Set.of(Role.OPERATOR), NO_GUARD),
                    CANCEL, new Rule(CANCELLED, Set.of(Role.OPERATOR, Role.ADMIN), NO_GUARD)),
            SCHEDULED, Map.of(
                    // フック点（Phase 3/4）：本番は必須 pre-check 完了・BLOCK 無しを START 前に検証。
                    START, new Rule(IN_PROGRESS, Set.of(Role.OPERATOR), NO_GUARD),
                    CANCEL, new Rule(CANCELLED, Set.of(Role.OPERATOR, Role.ADMIN), NO_GUARD)),
            IN_PROGRESS, Map.of(
                    // フック点（Phase 4）：COMPLETE は iac_apply=SUCCESS かつ service_health_confirmed を検証。
                    COMPLETE, new Rule(COMPLETED, Set.of(Role.OPERATOR), NO_GUARD),
                    FAIL, new Rule(FAILED, Set.of(Role.OPERATOR), NO_GUARD)),
            FAILED, Map.of(
                    ROLLBACK, new Rule(ROLLED_BACK, Set.of(Role.OPERATOR), NO_GUARD))
    );

    /**
     * 遷移を検証して新ステータスを適用する（cr.status を更新）。永続化・監査・付随レコード記録は Service が行う。
     *
     * @throws IllegalStateTransitionException 許可表にないアクション（飛び級/逆行/終端）→ 409
     * @throws ForbiddenException              ロール不足・所有不一致・自己承認 → 403
     * @throws ValidationException             SUBMIT 必須項目不足・差し戻しコメント不足 → 422
     */
    public void transition(ChangeRequest cr, TransitionAction action, AppUserPrincipal actor,
                           TransitionContext ctx) {
        Rule rule = ruleFor(cr, action, actor, ctx);
        cr.setStatus(rule.to());
    }

    /**
     * 遷移の妥当性（許可表・ロール・ガード）だけを検証し、<b>状態は変えない</b>。
     * 条件付き遷移（承認段数の定足数未達など、票は記録するが APPROVED にはしない場面）で使う。
     *
     * @throws IllegalStateTransitionException / ForbiddenException / ValidationException transition と同じ
     */
    public void validate(ChangeRequest cr, TransitionAction action, AppUserPrincipal actor,
                         TransitionContext ctx) {
        ruleFor(cr, action, actor, ctx);
    }

    /** 許可表からルールを引き、ロール・ガードを検証して返す（状態は変えない）。 */
    private Rule ruleFor(ChangeRequest cr, TransitionAction action, AppUserPrincipal actor,
                         TransitionContext ctx) {
        Map<TransitionAction, Rule> rules = table.getOrDefault(cr.getStatus(), Map.of());
        Rule rule = rules.get(action);
        if (rule == null) {
            throw illegal(cr, actor, action);
        }
        if (!rule.roles().contains(actor.role())) {
            throw new ForbiddenException("この状態でこの操作を行う権限がありません");
        }
        rule.guard().check(cr, actor, ctx);
        return rule;
    }

    /** 現在状態・ロール（＋所有）から、その利用者が実行可能なアクションの wire 名一覧を算出する。 */
    public List<String> allowedActions(ChangeRequest cr, AppUserPrincipal actor) {
        Map<TransitionAction, Rule> rules = table.getOrDefault(cr.getStatus(), Map.of());
        List<String> result = new ArrayList<>();
        for (var entry : rules.entrySet()) {
            Rule rule = entry.getValue();
            if (!rule.roles().contains(actor.role())) {
                continue;
            }
            // REQUESTER の所有限定アクションは、自分の申請のときだけ候補に含める。
            if (actor.role() == Role.REQUESTER && !cr.isOwnedBy(actor.userId())) {
                continue;
            }
            result.add(entry.getKey().wire());
        }
        return result;
    }

    private IllegalStateTransitionException illegal(ChangeRequest cr, AppUserPrincipal actor,
                                                   TransitionAction action) {
        String current = cr.getStatus().name();
        return new IllegalStateTransitionException(
                "この変更申請は現在の状態ではその操作ができません",
                current + " から " + action.wire() + " はできません（許可された遷移ではありません）",
                current,
                allowedActions(cr, actor));
    }

    private static void requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new ValidationException("必須項目が未入力です: " + field);
        }
    }

    private static void requireNonNull(Object value, String field) {
        if (value == null) {
            throw new ValidationException("必須項目が未入力です: " + field);
        }
    }
}
