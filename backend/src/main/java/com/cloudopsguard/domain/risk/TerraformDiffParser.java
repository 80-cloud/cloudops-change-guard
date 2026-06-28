package com.cloudopsguard.domain.risk;

import com.cloudopsguard.domain.common.IacType;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Terraform plan テキストの正規化（リスク判定ルール.md §1）。
 * {@code # aws_db_instance.main will be destroyed} のようなプラン構文から
 * アクション（destroy/replace/create/update）とリソース型（{@code aws_*}）を抽出する。
 *
 * <p>MVP の素朴なヒューリスティックで、行単位にアクション行だけを拾う。値ベースの判定
 * （0.0.0.0/0・ポート22 等）は各ルールが {@code diff_text} 全体を参照して補う。
 */
@Component
public class TerraformDiffParser implements IacDiffParser {

    private static final Pattern TF_TYPE = Pattern.compile("aws_[a-z0-9_]+");

    @Override
    public boolean supports(IacType iacType) {
        return iacType == IacType.TERRAFORM;
    }

    @Override
    public List<NormalizedChange> parse(String diffText) {
        List<NormalizedChange> out = new ArrayList<>();
        if (diffText == null || diffText.isBlank()) {
            return out;
        }
        for (String raw : diffText.split("\\R")) {
            String line = raw.trim();
            String lower = line.toLowerCase();
            ChangeAction action = null;
            if (lower.contains("will be destroyed")) {
                action = ChangeAction.DELETE;
            } else if (lower.contains("must be replaced") || lower.contains("forces replacement")
                    || line.startsWith("-/+")) {
                action = ChangeAction.REPLACE;
            } else if (lower.contains("will be created")) {
                action = ChangeAction.CREATE;
            } else if (lower.contains("will be updated in-place")) {
                action = ChangeAction.UPDATE;
            }
            if (action != null) {
                out.add(new NormalizedChange(extractType(line), action, line));
            }
        }
        return out;
    }

    /** 行から最初の {@code aws_*} トークンをリソース型として取り出す（無ければ空文字）。 */
    private String extractType(String line) {
        Matcher m = TF_TYPE.matcher(line);
        return m.find() ? m.group() : "";
    }
}
