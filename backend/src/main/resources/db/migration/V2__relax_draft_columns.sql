-- V2: 下書き(DRAFT)を最小項目で作成できるよう、本文系カラムの NOT NULL を解除する。
-- 提出(SUBMIT)時の必須充足は状態機械が検証するため、DB の NOT NULL には依存しない。
ALTER TABLE change_requests
    ALTER COLUMN target_aws_service DROP NOT NULL,
    ALTER COLUMN target_resource_name DROP NOT NULL,
    ALTER COLUMN change_reason DROP NOT NULL,
    ALTER COLUMN change_summary DROP NOT NULL,
    ALTER COLUMN diff_text DROP NOT NULL;
