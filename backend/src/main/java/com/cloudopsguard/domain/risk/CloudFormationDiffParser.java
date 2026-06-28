package com.cloudopsguard.domain.risk;

import com.cloudopsguard.domain.common.IacType;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * CloudFormation 変更セット相当テキストの正規化（リスク判定ルール.md §1）。
 * {@code AWS::RDS::DBInstance} のリソース型と、同一行の {@code Remove/Replace/Add/Modify}
 * からアクションを判定する。
 *
 * <p>MVP のヒューリスティック。アクションが同一行に無い場合は {@link ChangeAction#UNKNOWN}。
 */
@Component
public class CloudFormationDiffParser implements IacDiffParser {

    private static final Pattern CFN_TYPE = Pattern.compile("AWS::[A-Za-z0-9]+::[A-Za-z0-9]+");

    @Override
    public boolean supports(IacType iacType) {
        return iacType == IacType.CLOUDFORMATION;
    }

    @Override
    public List<NormalizedChange> parse(String diffText) {
        List<NormalizedChange> out = new ArrayList<>();
        if (diffText == null || diffText.isBlank()) {
            return out;
        }
        for (String raw : diffText.split("\\R")) {
            String line = raw.trim();
            Matcher m = CFN_TYPE.matcher(line);
            if (m.find()) {
                out.add(new NormalizedChange(m.group(), actionOf(line), line));
            }
        }
        return out;
    }

    private ChangeAction actionOf(String line) {
        String lower = line.toLowerCase();
        if (lower.contains("remove")) {
            return ChangeAction.DELETE;
        }
        if (lower.contains("replace")) {
            return ChangeAction.REPLACE;
        }
        if (lower.contains("add")) {
            return ChangeAction.CREATE;
        }
        if (lower.contains("modify")) {
            return ChangeAction.UPDATE;
        }
        return ChangeAction.UNKNOWN;
    }
}
