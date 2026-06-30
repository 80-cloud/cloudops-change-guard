-- V3: 実施記録に「外部で実行された apply の証跡」を残す列を追加する（A-2b）。
-- Backend は apply を実行しない。CI/Worker/手動が実行した結果へのリンク(apply_run_url)と、
-- 取り込んだ plan の参照(plan_source_ref)を記録するための任意項目（後方互換のため nullable）。
ALTER TABLE executions ADD COLUMN apply_run_url   TEXT;
ALTER TABLE executions ADD COLUMN plan_source_ref TEXT;
