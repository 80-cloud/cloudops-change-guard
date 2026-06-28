package com.cloudopsguard.domain.risk.rules;

import com.cloudopsguard.domain.changerequest.ChangeRequest;
import com.cloudopsguard.domain.risk.ChangeAction;
import com.cloudopsguard.domain.risk.NormalizedChange;

import java.util.List;
import java.util.function.Predicate;

/**
 * リスクルール共通の検知ヘルパ（パッケージ内専用）。
 * 値ベース判定の正規化と、型×アクションの一致判定をまとめる。各ルールの述語を短く保つ。
 */
final class Detections {

    private Detections() {
    }

    /** diff_text を小文字化（空白は保持）。トークンの contains 判定用。null は空文字。 */
    static String lower(ChangeRequest cr) {
        return cr.getDiffText() == null ? "" : cr.getDiffText().toLowerCase();
    }

    /** diff_text を小文字化し空白を全除去。{@code "action":"*"} のような構文一致用。 */
    static String compact(ChangeRequest cr) {
        return lower(cr).replaceAll("\\s+", "");
    }

    /** resourceType（小文字化済を渡す）が typeMatch に合致し、action が指定集合に含まれる変更が1件でもあるか。 */
    static boolean anyChange(List<NormalizedChange> changes,
                             Predicate<String> typeMatch, ChangeAction... actions) {
        return changes.stream().anyMatch(c ->
                c.resourceType() != null
                        && typeMatch.test(c.resourceType().toLowerCase())
                        && in(actions, c.action()));
    }

    private static boolean in(ChangeAction[] actions, ChangeAction a) {
        for (ChangeAction x : actions) {
            if (x == a) {
                return true;
            }
        }
        return false;
    }
}
