-- 根拠：docs/02_Florie_お世話マスタ確定.md 第4・5章。説明文は原文のまま保存する。
-- 先に02_add_care_guidance.sqlを適用する。詳しい手順はsql/README.md。
-- アプリを停止し、同じ接続で実行する。結果確認後に手動でCOMMITする。
-- エラーが一つでも出た場合はCOMMITせずROLLBACKする。
-- 再実行は既存IDを保って承認済みの内容へ更新する。users・花・予定・履歴は変更しない。
USE florie;
SET NAMES utf8mb4;
START TRANSACTION;

-- 花8種類。イラストは未作成なので新規行のパスは空文字とし、架空の画像を指定しない。
-- 再実行時は、後から設定されたイラストのパスを上書きしない。
-- 1. ガーベラ
INSERT INTO flower_types (flower_name, illustration_path, display_order, care_guidance)
VALUES ('ガーベラ', '', 1, '水は少なめに。茎先が3～5cmほどつかる量を目安にし、水切れに気をつけましょう。')
ON DUPLICATE KEY UPDATE
    display_order = 1,
    care_guidance = '水は少なめに。茎先が3～5cmほどつかる量を目安にし、水切れに気をつけましょう。';

-- 2. バラ
INSERT INTO flower_types (flower_name, illustration_path, display_order, care_guidance)
VALUES ('バラ', '', 2, '水はやや多めに。花瓶の半分程度を目安にし、水につかる葉は取り除きましょう。')
ON DUPLICATE KEY UPDATE
    display_order = 2,
    care_guidance = '水はやや多めに。花瓶の半分程度を目安にし、水につかる葉は取り除きましょう。';

-- 3. チューリップ
INSERT INTO flower_types (flower_name, illustration_path, display_order, care_guidance)
VALUES ('チューリップ', '', 3, '水は少なめから始め、茎先が水につかる状態を保ちましょう。水が減っていないか、こまめに見てください。')
ON DUPLICATE KEY UPDATE
    display_order = 3,
    care_guidance = '水は少なめから始め、茎先が水につかる状態を保ちましょう。水が減っていないか、こまめに見てください。';

-- 4. カーネーション
INSERT INTO flower_types (flower_name, illustration_path, display_order, care_guidance)
VALUES ('カーネーション', '', 4, '水は少なめにし、葉がつからないようにしましょう。茎のぬめりはやさしく洗い流してください。')
ON DUPLICATE KEY UPDATE
    display_order = 4,
    care_guidance = '水は少なめにし、葉がつからないようにしましょう。茎のぬめりはやさしく洗い流してください。';

-- 5. ひまわり
INSERT INTO flower_types (flower_name, illustration_path, display_order, care_guidance)
VALUES ('ひまわり', '', 5, '水は少なめに。茎先が3～5cmほどつかる量を目安にし、水につかる葉は取り除きましょう。')
ON DUPLICATE KEY UPDATE
    display_order = 5,
    care_guidance = '水は少なめに。茎先が3～5cmほどつかる量を目安にし、水につかる葉は取り除きましょう。';

-- 6. ダリア
INSERT INTO flower_types (flower_name, illustration_path, display_order, care_guidance)
VALUES ('ダリア', '', 6, '普段は浅めの水から始め、水切れに注意しましょう。余分な葉を減らし、茎を折らないように扱ってください。')
ON DUPLICATE KEY UPDATE
    display_order = 6,
    care_guidance = '普段は浅めの水から始め、水切れに注意しましょう。余分な葉を減らし、茎を折らないように扱ってください。';

-- 7. アネモネ
INSERT INTO flower_types (flower_name, illustration_path, display_order, care_guidance)
VALUES ('アネモネ', '', 7, '普段は浅めの水にし、水切れに気をつけましょう。柔らかい茎をやさしく扱い、涼しい場所に飾ってください。')
ON DUPLICATE KEY UPDATE
    display_order = 7,
    care_guidance = '普段は浅めの水にし、水切れに気をつけましょう。柔らかい茎をやさしく扱い、涼しい場所に飾ってください。';

-- 8. ラナンキュラス
INSERT INTO flower_types (flower_name, illustration_path, display_order, care_guidance)
VALUES ('ラナンキュラス', '', 8, '普段は浅めの水にし、水切れに気をつけましょう。柔らかい茎をやさしく扱い、涼しい場所に飾ってください。')
ON DUPLICATE KEY UPDATE
    display_order = 8,
    care_guidance = '普段は浅めの水にし、水切れに気をつけましょう。柔らかい茎をやさしく扱い、涼しい場所に飾ってください。';

-- お世話16件。固定IDを仮定せず、花名から外部キーを取得する。
-- ガーベラ：水を替える
INSERT INTO care_templates
    (flower_type_id, care_name, interval_days, display_order, care_description)
SELECT flower_type_id, '水を替える', 1, 1, '古い水を捨て、花瓶を洗って新しい水に替えましょう。茎をやさしくすすぎ、ぬめりや傷みも確認してください。'
FROM flower_types
WHERE flower_name = 'ガーベラ'
ON DUPLICATE KEY UPDATE
    interval_days = 1,
    display_order = 1,
    care_description = '古い水を捨て、花瓶を洗って新しい水に替えましょう。茎をやさしくすすぎ、ぬめりや傷みも確認してください。';

-- ガーベラ：茎を確認して整える
INSERT INTO care_templates
    (flower_type_id, care_name, interval_days, display_order, care_description)
SELECT flower_type_id, '茎を確認して整える', 3, 2, '茎先を確認し、清潔でよく切れるハサミで少し切って切り口を新しくしましょう。茎をつぶさず、まっすぐ横に切ってください。'
FROM flower_types
WHERE flower_name = 'ガーベラ'
ON DUPLICATE KEY UPDATE
    interval_days = 3,
    display_order = 2,
    care_description = '茎先を確認し、清潔でよく切れるハサミで少し切って切り口を新しくしましょう。茎をつぶさず、まっすぐ横に切ってください。';

-- バラ：水を替える
INSERT INTO care_templates
    (flower_type_id, care_name, interval_days, display_order, care_description)
SELECT flower_type_id, '水を替える', 1, 1, '古い水を捨て、花瓶を洗って新しい水に替えましょう。水につかる葉が残っていないか、茎に傷みがないかも確認してください。'
FROM flower_types
WHERE flower_name = 'バラ'
ON DUPLICATE KEY UPDATE
    interval_days = 1,
    display_order = 1,
    care_description = '古い水を捨て、花瓶を洗って新しい水に替えましょう。水につかる葉が残っていないか、茎に傷みがないかも確認してください。';

-- バラ：茎を確認して整える
INSERT INTO care_templates
    (flower_type_id, care_name, interval_days, display_order, care_description)
SELECT flower_type_id, '茎を確認して整える', 3, 2, '清潔な水の中で、茎先をよく切れるハサミで少し斜めに切りましょう。トゲに気をつけ、花首を強く持たないでください。'
FROM flower_types
WHERE flower_name = 'バラ'
ON DUPLICATE KEY UPDATE
    interval_days = 3,
    display_order = 2,
    care_description = '清潔な水の中で、茎先をよく切れるハサミで少し斜めに切りましょう。トゲに気をつけ、花首を強く持たないでください。';

-- チューリップ：水を替える
INSERT INTO care_templates
    (flower_type_id, care_name, interval_days, display_order, care_description)
SELECT flower_type_id, '水を替える', 1, 1, '古い水を捨て、花瓶を洗って新しい水に替えましょう。茎先が水につかっていることと、茶色い傷みがないことも確認してください。'
FROM flower_types
WHERE flower_name = 'チューリップ'
ON DUPLICATE KEY UPDATE
    interval_days = 1,
    display_order = 1,
    care_description = '古い水を捨て、花瓶を洗って新しい水に替えましょう。茎先が水につかっていることと、茶色い傷みがないことも確認してください。';

-- チューリップ：茎を確認して整える
INSERT INTO care_templates
    (flower_type_id, care_name, interval_days, display_order, care_description)
SELECT flower_type_id, '茎を確認して整える', 3, 2, '茎先の変色や傷みを確認し、必要に応じて少し切り戻しましょう。柔らかい茎をつぶさないよう、よく切れるハサミを使ってください。'
FROM flower_types
WHERE flower_name = 'チューリップ'
ON DUPLICATE KEY UPDATE
    interval_days = 3,
    display_order = 2,
    care_description = '茎先の変色や傷みを確認し、必要に応じて少し切り戻しましょう。柔らかい茎をつぶさないよう、よく切れるハサミを使ってください。';

-- カーネーション：水を替える
INSERT INTO care_templates
    (flower_type_id, care_name, interval_days, display_order, care_description)
SELECT flower_type_id, '水を替える', 1, 1, '古い水を捨て、花瓶を洗って新しい水に替えましょう。茎のぬめりはやさしく洗い流し、茎先が茶色くなっていないか確認してください。'
FROM flower_types
WHERE flower_name = 'カーネーション'
ON DUPLICATE KEY UPDATE
    interval_days = 1,
    display_order = 1,
    care_description = '古い水を捨て、花瓶を洗って新しい水に替えましょう。茎のぬめりはやさしく洗い流し、茎先が茶色くなっていないか確認してください。';

-- カーネーション：茎を確認して整える
INSERT INTO care_templates
    (flower_type_id, care_name, interval_days, display_order, care_description)
SELECT flower_type_id, '茎を確認して整える', 3, 2, '茎先を確認し、必要に応じて少し斜めに切り戻しましょう。茶色く傷んだ部分があれば、その部分より上で切ってください。'
FROM flower_types
WHERE flower_name = 'カーネーション'
ON DUPLICATE KEY UPDATE
    interval_days = 3,
    display_order = 2,
    care_description = '茎先を確認し、必要に応じて少し斜めに切り戻しましょう。茶色く傷んだ部分があれば、その部分より上で切ってください。';

-- ひまわり：水を替える
INSERT INTO care_templates
    (flower_type_id, care_name, interval_days, display_order, care_description)
SELECT flower_type_id, '水を替える', 1, 1, '古い水を捨て、花瓶を洗って新しい水に替えましょう。水につかる葉を取り除き、茎のぬめりや傷みも確認してください。'
FROM flower_types
WHERE flower_name = 'ひまわり'
ON DUPLICATE KEY UPDATE
    interval_days = 1,
    display_order = 1,
    care_description = '古い水を捨て、花瓶を洗って新しい水に替えましょう。水につかる葉を取り除き、茎のぬめりや傷みも確認してください。';

-- ひまわり：茎を確認して整える
INSERT INTO care_templates
    (flower_type_id, care_name, interval_days, display_order, care_description)
SELECT flower_type_id, '茎を確認して整える', 3, 2, '茎先を確認し、清潔でよく切れるハサミで少し切って切り口を新しくしましょう。茎をつぶさず、まっすぐ横に切ってください。'
FROM flower_types
WHERE flower_name = 'ひまわり'
ON DUPLICATE KEY UPDATE
    interval_days = 3,
    display_order = 2,
    care_description = '茎先を確認し、清潔でよく切れるハサミで少し切って切り口を新しくしましょう。茎をつぶさず、まっすぐ横に切ってください。';

-- ダリア：水を替える
INSERT INTO care_templates
    (flower_type_id, care_name, interval_days, display_order, care_description)
SELECT flower_type_id, '水を替える', 1, 1, '古い水を捨て、花瓶を洗って新しい水に替えましょう。茎のぬめりをやさしく洗い流し、茎先の傷みも確認してください。'
FROM flower_types
WHERE flower_name = 'ダリア'
ON DUPLICATE KEY UPDATE
    interval_days = 1,
    display_order = 1,
    care_description = '古い水を捨て、花瓶を洗って新しい水に替えましょう。茎のぬめりをやさしく洗い流し、茎先の傷みも確認してください。';

-- ダリア：茎を確認して整える
INSERT INTO care_templates
    (flower_type_id, care_name, interval_days, display_order, care_description)
SELECT flower_type_id, '茎を確認して整える', 3, 2, '茎先を確認し、よく切れるハサミで少し切り戻しましょう。傷んだ部分はその上で切り、空洞のある茎をつぶさないようにしてください。'
FROM flower_types
WHERE flower_name = 'ダリア'
ON DUPLICATE KEY UPDATE
    interval_days = 3,
    display_order = 2,
    care_description = '茎先を確認し、よく切れるハサミで少し切り戻しましょう。傷んだ部分はその上で切り、空洞のある茎をつぶさないようにしてください。';

-- アネモネ：水を替える
INSERT INTO care_templates
    (flower_type_id, care_name, interval_days, display_order, care_description)
SELECT flower_type_id, '水を替える', 1, 1, '古い水を捨て、花瓶を洗って新しい水に替えましょう。柔らかい茎を折らないように扱い、茎先の傷みも確認してください。'
FROM flower_types
WHERE flower_name = 'アネモネ'
ON DUPLICATE KEY UPDATE
    interval_days = 1,
    display_order = 1,
    care_description = '古い水を捨て、花瓶を洗って新しい水に替えましょう。柔らかい茎を折らないように扱い、茎先の傷みも確認してください。';

-- アネモネ：茎を確認して整える
INSERT INTO care_templates
    (flower_type_id, care_name, interval_days, display_order, care_description)
SELECT flower_type_id, '茎を確認して整える', 3, 2, '清潔な水の中で、茎先をよく切れるハサミで少し切り戻しましょう。柔らかく空洞のある茎を、つぶしたり折ったりしないようにしてください。'
FROM flower_types
WHERE flower_name = 'アネモネ'
ON DUPLICATE KEY UPDATE
    interval_days = 3,
    display_order = 2,
    care_description = '清潔な水の中で、茎先をよく切れるハサミで少し切り戻しましょう。柔らかく空洞のある茎を、つぶしたり折ったりしないようにしてください。';

-- ラナンキュラス：水を替える
INSERT INTO care_templates
    (flower_type_id, care_name, interval_days, display_order, care_description)
SELECT flower_type_id, '水を替える', 1, 1, '古い水を捨て、花瓶を洗って新しい水に替えましょう。柔らかい茎を折らないように扱い、茎先の傷みも確認してください。'
FROM flower_types
WHERE flower_name = 'ラナンキュラス'
ON DUPLICATE KEY UPDATE
    interval_days = 1,
    display_order = 1,
    care_description = '古い水を捨て、花瓶を洗って新しい水に替えましょう。柔らかい茎を折らないように扱い、茎先の傷みも確認してください。';

-- ラナンキュラス：茎を確認して整える
INSERT INTO care_templates
    (flower_type_id, care_name, interval_days, display_order, care_description)
SELECT flower_type_id, '茎を確認して整える', 3, 2, '清潔な水の中で、茎先をよく切れるハサミで少し切り戻しましょう。柔らかく空洞のある茎を、つぶしたり折ったりしないようにしてください。'
FROM flower_types
WHERE flower_name = 'ラナンキュラス'
ON DUPLICATE KEY UPDATE
    interval_days = 3,
    display_order = 2,
    care_description = '清潔な水の中で、茎先をよく切れるハサミで少し切り戻しましょう。柔らかく空洞のある茎を、つぶしたり折ったりしないようにしてください。';

-- 以下の結果を確認する。このファイルではCOMMITしない。
SELECT flower_type_id, flower_name, display_order, care_guidance
FROM flower_types ORDER BY display_order;

SELECT f.flower_name, c.care_name, c.interval_days, c.display_order, c.care_description
FROM care_templates c
JOIN flower_types f ON f.flower_type_id = c.flower_type_id
ORDER BY f.display_order, c.display_order;

SELECT COUNT(*) AS flower_type_count FROM flower_types;
SELECT COUNT(*) AS care_template_count FROM care_templates;

-- 元が空のマスタなら8件・16件になる。既存の別データは削除しないため、件数が違えば確認する。
-- 全文にエラーがなく、内容・件数が正しい場合だけ、手動で COMMIT; を実行する。
-- エラーや不一致があれば ROLLBACK; を実行する。接続を閉じても未確定の変更は取り消される。
