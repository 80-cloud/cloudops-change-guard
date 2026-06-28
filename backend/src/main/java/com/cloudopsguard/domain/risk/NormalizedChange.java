package com.cloudopsguard.domain.risk;

/**
 * 正規化された1件の変更（リスク判定ルール.md §1）。
 * 差分テキストを {@link IacDiffParser} が解析し、リスクルールはこの構造化項目に対して述語評価する
 * （単純な文字列一致で終わらせない）。
 *
 * @param resourceType リソース型（例：terraform {@code aws_db_instance} / CFN {@code AWS::RDS::DBInstance}）
 * @param action       create/update/delete/replace
 * @param snippet      抽出元の生テキスト行（値ベースの補助判定・表示用）
 */
public record NormalizedChange(String resourceType, ChangeAction action, String snippet) {
}
