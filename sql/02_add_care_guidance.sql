-- 既存6テーブルに一度だけ適用する構造変更。
-- 先にsql/README.mdの事前確認を行う。01_create_tables.sqlは再実行しない。
-- MySQLのALTER TABLEは暗黙にコミットされるため、ROLLBACKでは戻せない。
-- エラー時はここで止まり、追加済みカラムを確認して未適用のALTERだけを実行する。
USE florie;

-- 既存マスタ行を失わず追加できるようNULLを許可する。初期8件には次のSQLで案内を設定する。
ALTER TABLE flower_types
    ADD COLUMN care_guidance VARCHAR(500) NULL COMMENT '花ごとの水量・注意事項';

-- 同じ花に同名のお世話を二重登録しない。
-- 既に重複行がある場合は制約追加が失敗するが、自動で既存データを削除しない。
ALTER TABLE care_templates
    ADD COLUMN care_description VARCHAR(500) NULL COMMENT 'ユーザー向けのお世話説明文',
    ADD CONSTRAINT uk_care_templates_flower_care UNIQUE (flower_type_id, care_name);
