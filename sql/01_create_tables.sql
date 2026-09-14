-- 空のflorieデータベースへ手動で一度だけ実行する。
-- 既存テーブルの削除・変更、初期データの登録は行わない。
USE florie;

-- ユーザー自身のニックネームと、ハッシュ化したパスワードを保存する。
CREATE TABLE users (
    user_id BIGINT NOT NULL AUTO_INCREMENT,
    nickname VARCHAR(20) NOT NULL,
    email VARCHAR(255) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    created_at DATETIME NOT NULL,
    PRIMARY KEY (user_id),
    CONSTRAINT uk_users_email UNIQUE (email),
    CONSTRAINT ck_users_nickname CHECK (CHAR_LENGTH(TRIM(nickname)) BETWEEN 1 AND 20)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 花種類の情報。今回、8種類のデータ投入はまだ行わない。
CREATE TABLE flower_types (
    flower_type_id BIGINT NOT NULL AUTO_INCREMENT,
    flower_name VARCHAR(50) NOT NULL,
    illustration_path VARCHAR(255) NOT NULL,
    display_order INT NOT NULL,
    PRIMARY KEY (flower_type_id),
    CONSTRAINT uk_flower_types_name UNIQUE (flower_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 花種類別のお世話設定。具体的な名前・周期には既定値を設けない。
CREATE TABLE care_templates (
    care_template_id BIGINT NOT NULL AUTO_INCREMENT,
    flower_type_id BIGINT NOT NULL,
    care_name VARCHAR(50) NOT NULL,
    interval_days INT NOT NULL,
    display_order INT NOT NULL,
    PRIMARY KEY (care_template_id),
    CONSTRAINT fk_care_templates_flower_type FOREIGN KEY (flower_type_id)
        REFERENCES flower_types (flower_type_id) ON DELETE RESTRICT,
    CONSTRAINT ck_care_templates_interval CHECK (interval_days >= 1)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- お世話中と終了済みの花を同じテーブルに残す。思い出メモは作らない。
-- 一人一輪の制限は、今後Serviceで同時操作も考慮して実装する。
CREATE TABLE flowers (
    flower_id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    flower_type_id BIGINT NOT NULL,
    flower_nickname VARCHAR(20) NOT NULL,
    started_on DATE NOT NULL,
    ended_on DATE NULL,
    status VARCHAR(20) NOT NULL,
    PRIMARY KEY (flower_id),
    CONSTRAINT fk_flowers_user FOREIGN KEY (user_id)
        REFERENCES users (user_id) ON DELETE RESTRICT,
    CONSTRAINT fk_flowers_flower_type FOREIGN KEY (flower_type_id)
        REFERENCES flower_types (flower_type_id) ON DELETE RESTRICT,
    CONSTRAINT ck_flowers_nickname CHECK (CHAR_LENGTH(TRIM(flower_nickname)) BETWEEN 1 AND 20),
    CONSTRAINT ck_flowers_status CHECK (
        (BINARY status = 'ACTIVE' AND ended_on IS NULL)
        OR (BINARY status = 'ENDED' AND ended_on IS NOT NULL)
    )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 登録時のマスターから名前と周期をコピーする保存先。
-- 元のマスターが変更されても、登録済みの予定の周期を保てる。
CREATE TABLE care_tasks (
    care_task_id BIGINT NOT NULL AUTO_INCREMENT,
    flower_id BIGINT NOT NULL,
    care_name VARCHAR(50) NOT NULL,
    interval_days INT NOT NULL,
    next_care_date DATE NOT NULL,
    PRIMARY KEY (care_task_id),
    CONSTRAINT fk_care_tasks_flower FOREIGN KEY (flower_id)
        REFERENCES flowers (flower_id) ON DELETE RESTRICT,
    CONSTRAINT ck_care_tasks_interval CHECK (interval_days >= 1)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 完了日は時刻を持たないDATE。終了した花の履歴も残す。
CREATE TABLE care_records (
    care_record_id BIGINT NOT NULL AUTO_INCREMENT,
    care_task_id BIGINT NOT NULL,
    completed_on DATE NOT NULL,
    PRIMARY KEY (care_record_id),
    CONSTRAINT fk_care_records_task FOREIGN KEY (care_task_id)
        REFERENCES care_tasks (care_task_id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
