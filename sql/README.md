# お世話マスタの追加反映

根拠は `docs/02_Florie_お世話マスタ確定.md` です。今回はSQLを用意した段階で、実DBには実行していません。アプリを停止してから作業してください。既存の `01_create_tables.sql` は再実行しません。

## ファイルと追加内容

| ファイル | 役割 |
|---|---|
| 02_add_care_guidance.sql | 一度だけ実行するALTER TABLE。説明用2カラムとお世話重複防止の一意制約を追加 |
| 03_seed_care_masters.sql | 花8件・お世話16件を登録。既存の同名マスタはIDを保って承認内容に更新 |

追加カラムは `flower_types.care_guidance` と `care_templates.care_description`、どちらもVARCHAR(500)、NULL許可です。既存行を壊さず追加するためNULLを許可していますが、今回の初期データには全件説明を設定します。出典用カラムはありません。

花の表示順はガーベラ、バラ、チューリップ、カーネーション、ひまわり、ダリア、アネモネ、ラナンキュラスです。各花に「水を替える」（1日、表示順1）と「茎を確認して整える」（3日、表示順2）を設定します。説明は資料第4・5章のユーザー向け文章をそのまま保存します。

イラスト素材は今回作成しないため、新規のflower_types.illustration_pathは空文字です。画像を用意する工程で設定してください。再実行時に既存の画像パスを上書きしません。

## 1. 接続と事前確認

FlorieフォルダのPowerShellで実行します。パスワードは `-p` の後に書かず、MySQLの入力要求に応じて入力します。

```powershell
& 'C:\Program Files\MySQL\MySQL Server 8.4\bin\mysql.exe' --default-character-set=utf8mb4 -h localhost -P 3306 -u florie_app -p florie
```

MySQLで実行します。

```sql
SELECT DATABASE();
SELECT COUNT(*) AS users_before FROM users;
SHOW COLUMNS FROM flower_types;
SHOW COLUMNS FROM care_templates;
SHOW INDEX FROM care_templates;
SELECT flower_type_id, flower_name FROM flower_types;
SELECT flower_type_id, care_name, COUNT(*) AS duplicate_count
FROM care_templates
GROUP BY flower_type_id, care_name
HAVING COUNT(*) > 1;
```

接続先がflorieで、追加カラムとuk_care_templates_flower_careがまだなく、最後の重複確認が0行であることを確認します。既存マスタがある場合は今回の承認内容への更新対象になるため内容を確認してください。重複があれば削除せず作業を止めます。users_beforeの件数は後で比較します。

## 2. 構造変更を一度だけ実行

```sql
SOURCE sql/02_add_care_guidance.sql;
SHOW COLUMNS FROM flower_types LIKE 'care_guidance';
SHOW COLUMNS FROM care_templates LIKE 'care_description';
SHOW INDEX FROM care_templates WHERE Key_name = 'uk_care_templates_flower_care';
```

MySQLのALTER TABLEは暗黙にコミットされ、ROLLBACKでは戻せません。途中で失敗した場合、ファイル全体を再実行せず、SHOW COLUMNSとSHOW INDEXで実際の状態を確認し、未適用のALTER文だけを実行してください。care_templatesのカラムと一意制約は同じALTER文で追加します。権限不足ならDBを準備した管理者に相談してください。アプリの接続ユーザーをrootに変える必要はありません。

## 3. 初期マスタを登録し、確認して確定

```sql
SOURCE sql/03_seed_care_masters.sql;
```

同じ接続を開いたまま、エラーが一つもなく、表示された花8件・お世話16件の名前、周期、説明が正しいことを確認します。SOURCEはエラー後も後続の文を実行することがあるため、末尾の件数だけで成功と判断しないでください。

問題がなければ確定します。

```sql
COMMIT;
SELECT COUNT(*) AS users_after FROM users;
exit
```

エラーや不一致があれば、COMMITせず次で初期データの変更を取り消します。

```sql
ROLLBACK;
```

users_afterが事前の件数と同じことを確認してください。SQLにはusers・flowers・care_tasks・care_recordsへの書き込みや削除はありません。

データ登録SQLは一意制約とON DUPLICATE KEY UPDATEで重複を防止します。03だけの再実行は可能ですが、マスタの説明・周期・表示順をこの資料の承認値に戻します。既存の予定や履歴は更新しません。02の構造変更は再実行しません。DMLはトランザクションでまとめ、利用者が確認してからCOMMITする構成です。

## 4. Java側の確認

既存の方法でFLORIE_DB_PASSWORDを環境変数に設定したPowerShellで実行します。

```powershell
.\mvnw.cmd clean verify
```

Hibernateの自動作成はnone、SQL自動実行はneverのままです。このビルドだけでマスタの内容一致や実DBへの登録まで検証できるわけではないため、上記SQLの結果も確認してください。

## 今後の画面への引き継ぎ（今回は未実装）

- 登録当日の準備案内：花登録画面の説明欄に、資料第6.1節の固定文を表示する。
- 花別の水量・注意事項：登録画面とホームでFlowerType.careGuidanceを表示する。
- 共通の置き場所・水の状態の案内：ホームの短い説明欄に表示し、必要なら登録画面でも同じThymeleafフラグメントを使う。
- 延命剤の注意：登録画面とホームのお世話一覧の近くに第7章の注記を表示する。製品の案内に沿った手入れで完了できることも伝える。
- 茎の共通補足：茎のお世話説明の近くに第4.2節の固定文を表示し、「直前に切った場合は確認だけでよい」を省略しない。

共通文はThymeleafの共通フラグメントに一度だけ置く方法がシンプルです。DB、周期0日のタスク、完了履歴、新しい専用画面は追加しません。CareTaskへ説明をコピーするかマスタから参照するかは、花登録・表示の工程で決めます。
