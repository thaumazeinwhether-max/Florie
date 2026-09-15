# Florie

一輪挿しの花のお世話を支援するWebアプリです。現在はDB基盤、認証、花登録、今日のお世話一覧、お世話完了、お別れ、お花畑と思い出表示まで実装しています。SQLのDBへの反映は手動で行います。

## 開発環境

- Java 21（JDK）
- Spring Boot 4.1.1
- Maven 3.9.16（Maven Wrapperから取得）

Javaのインストール先をJAVA_HOMEに設定し、JavaとGitをPATHから利用できる環境で実行してください。初回はMavenと依存ライブラリのダウンロードにインターネット接続が必要です。Mavenの手動インストールは不要です。

## ビルドとテスト

PowerShellで、このREADMEがあるフォルダから実行します。
テストはSpring Boot全体を初期化するため、MySQLが起動しており、次節の環境変数が設定されている必要があります。

```powershell
.\mvnw.cmd clean verify
```

Javaコードのコンパイル、テスト、実行用JARの作成を行います。結果はtargetフォルダに出力します。

テストだけを実行する場合：

```powershell
.\mvnw.cmd test
```

## 起動と終了

MySQL84サービスが起動していることと、florie_appがflorieデータベースを利用できることを前提とします。PowerShellで以下を実行し、表示される入力欄にアプリ用ユーザーのパスワードを入力してください。入力した値は画面やコマンド履歴に残しません。環境変数はこのPowerShellと、そこから起動するアプリにだけ引き継がれます。

```powershell
$florieCredential = Get-Credential -UserName 'florie_app' -Message 'Florie用DBパスワードを入力してください'
if ($null -eq $florieCredential) { throw 'パスワード入力をキャンセルしました。' }
$env:FLORIE_DB_PASSWORD = $florieCredential.GetNetworkCredential().Password
Remove-Variable florieCredential
```

同じPowerShellで起動します。

```powershell
.\mvnw.cmd spring-boot:run
```

ログに「Started FlorieApplication」が表示されれば起動完了です。ブラウザで http://localhost:8080/login を開いてください。

終了するには、起動したターミナルでCtrl+Cを押します。

接続確認では、ログの「HikariPool-1 - Start completed.」と「Started FlorieApplication」を確認します。接続成功を確認してから、同じPowerShellで ` .\mvnw.cmd test ` を実行してください。終了後、環境変数が不要になったら以下で削除します。

```powershell
Remove-Item Env:FLORIE_DB_PASSWORD
```

接続先は `jdbc:mysql://localhost:3306/florie`、ユーザー名は `florie_app` です。パスワードは `FLORIE_DB_PASSWORD` から取得し、既定値は設けません。rootは使用しません。Hibernateの自動テーブル作成・更新は `ddl-auto=none`、SQLスクリプトの自動実行は `spring.sql.init.mode=never` で無効にしています。

パスワードがない状態でコンパイルとJAR作成だけを確認する場合は、次を使います。これはDB接続やテストの成功を確認するコマンドではありません。

```powershell
.\mvnw.cmd -DskipTests package
```

## ファイルの役割

| ファイル | 役割 |
|---|---|
| pom.xml | Java・Spring Bootのバージョン、依存ライブラリ、ビルド方法 |
| mvnw / mvnw.cmd | Mavenを実行する公式スクリプト。Windowsではmvnw.cmdを使用 |
| .mvn/wrapper/maven-wrapper.properties | 使用するMavenのバージョンと取得先 |
| src/main/java/com/florie/FlorieApplication.java | アプリを起動する入口 |
| src/main/resources/application.properties | アプリ名とDB接続設定。パスワード自体は保存しない |
| src/test/java/com/florie/FlorieApplicationTests.java | Spring Bootの初期化を確認するテスト |
| .gitignore / .gitattributes | Gitで除外するファイルと改行の扱い |

Web用とテスト用の依存関係に加え、Spring Data JPA、MySQLドライバー、Thymeleaf、Spring Security、Validationを使用しています。認証のテストにはspring-boot-starter-security-testを使用します。バージョンはSpring Bootの管理に合わせます。

.gitignoreでは.env、ローカル用設定、ログ、targetを除外しています。ただし、通常のapplication.propertiesはGit管理対象なので、パスワードを直接記載してはいけません。

## 仕様の根拠と次の工程

[開発ルール](AGENTS.md)、[確定仕様](docs/01_Florie_実装仕様確定.md)、[実装前整理資料](docs/00_Florie_実装前整理資料.md)と設計書を参照してください。

お世話の初期設定はdocs/02_Florie_お世話マスタ確定.mdで確定しました。現在の開発環境では、説明カラムと花8件・お世話16件の反映はユーザー側で確認済みです。新しいDBを用意する場合は[追加SQLの手順](sql/README.md)も参照してください。花登録の確認でSQLの再実行は不要です。パスワード等はGitへ保存しません。

## DBスキーマの手動反映

現在の開発環境では、SQL反映と従来の起動テストはユーザー側で確認済みです。以下は新しい空のDBを用意する場合の手順です。既存DBで再実行する必要はありません。

Florieフォルダを開いたPowerShellで、次のコマンドを実行します。`-p`の後ろにはパスワードを書かず、MySQLの入力要求に応じて入力してください。

```powershell
& 'C:\Program Files\MySQL\MySQL Server 8.4\bin\mysql.exe' --default-character-set=utf8mb4 -h localhost -P 3306 -u florie_app -p florie
```

MySQLのプロンプトで、まず接続先と既存テーブルを確認します。

```sql
SELECT DATABASE();
SHOW TABLES;
```

データベースがflorieで、テーブルがないことを確認してから実行します。

```sql
SOURCE sql/01_create_tables.sql;
SHOW TABLES;
SHOW CREATE TABLE flowers\G
SHOW CREATE TABLE care_records\G
exit
```

SQLは空のDBに一度だけ適用する初期作成用です。既存テーブルがある場合や実行中にエラーが出た場合は、再実行や削除をせずエラー内容を確認してください。MySQLのCREATE TABLEは途中まで成功した分が残ることがあります。CREATE権限が不足する場合は、DBを準備した管理者にスキーマ反映を依頼してください。アプリ用の接続設定をrootへ変更する必要はありません。

SQLはプロジェクト直下のsqlフォルダに置き、自動実行の対象にしていません。`ddl-auto=none`と`spring.sql.init.mode=never`も維持しています。01には初期データはありません。新しい空のDBでは01の後に[02・03の手順](sql/README.md)も実行します。既存6テーブルがあるDBでは01を再実行しません。

反映後、前述の方法でFLORIE_DB_PASSWORDを設定した同じPowerShellで実行します。

```powershell
.\mvnw.cmd clean verify
```

これはコンパイル・アプリ初期化・Repositoryの検索定義の確認です。`ddl-auto=none`なので、これだけでSQLとEntityの全カラム一致や実際の登録・更新まで確認したことにはなりません。それらのDB操作テストは後続工程で行います。

## EntityとRepository

| テーブル | Entity | Repository | 役割 |
|---|---|---|---|
| users | User | UserRepository | ユーザー情報 |
| flower_types | FlowerType | FlowerTypeRepository | 花種類マスター |
| care_templates | CareTemplate | CareTemplateRepository | 花種類ごとのお世話設定 |
| flowers | Flower | FlowerRepository | 登録した一輪。花ニックネームと状態を保持 |
| care_tasks | CareTask | CareTaskRepository | 各花の現在のお世話予定 |
| care_records | CareRecord | CareRecordRepository | 完了日の履歴 |

Javaファイルはsrc/main/java/com/florie/entityとrepositoryに配置しています。FlowerStatusはACTIVEとENDEDを表すenumで、DBにはVARCHARとして保存します。完了日はLocalDateとDATEで対応させています。

関連は、FlowerからUser・FlowerType、CareTemplateからFlowerType、CareTaskからFlower、CareRecordからCareTaskへの単方向ManyToOneです。親側に一覧フィールドを持たせず、一覧はRepositoryで検索します。DTO・継承・抽象クラスは追加していません。

外部キーはON DELETE RESTRICTとし、JPAにも削除の連動設定は付けていません。親を削除して履歴が連鎖的に消えることを防ぎます。ただし全レコードの削除禁止を保証する仕組みではありません。JpaRepositoryの標準削除メソッドは使わず、今後のServiceで終了を状態更新として実装します。一輪制限と同時登録対策はFlowerServiceで実装しています。

ニックネームはVARCHAR(20) NOT NULLとCHECK制約で空・半角空白のみを拒否します。ユーザーと花のニックネームは、それぞれ別のForm・Serviceで入力検証します。SQLの文字数はMySQLのCHAR_LENGTHを基準とし、Java入力検証の数え方は次節に記載します。

## ユーザー登録・ログイン・ログアウト

### 処理とファイルの役割

- `controller/AuthController.java`：ログイン・登録画面の表示、登録結果の画面への受け渡し。
- `form/UserRegisterForm.java`：登録入力と必須・形式・長さ・確認一致の検証。確認パスワードはDBには渡しません。
- `service/UserService.java`：重複確認、ハッシュ化、ユーザー保存。登録全体をトランザクションで管理します。
- `service/EmailAlreadyRegisteredException.java`：メール重複をControllerへ伝えます。
- `service/CustomUserDetailsService.java`：UserRepositoryでメールアドレスから認証用ユーザー情報を取得します。
- `config/SecurityConfig.java`：Spring Securityのログイン・ログアウト・アクセス制御とBCryptの設定。
- `controller/HomeController.java`：認証済みユーザー名と自分のACTIVEな花を取得し、花あり・花なしのホームを表示します。
- `templates/login.html`、`register.html`、`home.html`、`error.html`、`fragments/logo.html`：Thymeleaf画面。
- `static/css/auth.css`：ログイン・登録画面を中心とした共通デザイン。

Javaのパスは `src/main/java/com/florie`、HTML・CSSは `src/main/resources` を基準とします。
UserRepositoryの既存のfindByEmail・existsByEmailと標準のsaveAndFlushを利用し、Repositoryの変更はありません。

登録はController → Service → Repository → usersの順に処理します。成功時は `/login?registered` へ移動し、自動ログインしません。
ログインPOSTはSpring Securityが受け取り、CustomUserDetailsService → Repositoryでユーザーを探してBCryptで照合します。成功時は `/home`、失敗時は `/login?error` へ移動します。存在しないメールと誤ったパスワードは同じエラー表示です。

セッションにはSpring Security標準のSecurityContextを保持し、独自のユーザーセッション管理は追加していません。認証成功後はパスワード・ハッシュが認証オブジェクトから消去され、セッションIDが変更されます。POST `/logout` でセッションを無効化し、JSESSIONID Cookieを削除して `/login?logout` へ戻ります。未ログインの `/home` はログイン画面へ誘導します。POSTにはCSRFトークンが必要で、Thymeleafがフォームに追加します。

### 入力と保存

| 項目 | チェック |
|---|---|
| ユーザーニックネーム | 必須、空白のみ不可、1～20文字。重複可 |
| メール | 必須、メール形式、255文字以内、重複不可 |
| パスワード | 必須、8文字以上、UTF-8で72バイト以内。文字種の強制なし |
| 確認パスワード | 必須、パスワードと一致 |

入力の細部として、メールは前後の空白を除去し、小文字に統一して登録・ログインの両方で扱います。パスワードは空白を除去せずそのまま照合します。JavaのSize検証とHTMLのmaxlengthはUTF-16の長さなので、一部の絵文字などは2文字分です。通常の日本語は1文字分です。

BCrypt（計算コスト12）で、毎回ランダムなソルトを使ったハッシュだけをusers.password_hashへ保存します。BCryptの上限に合わせて72バイトを超える入力を拒否し、切り詰めません。日本語は通常1文字3バイトです。エラー画面にはニックネーム・メールのみ再表示し、パスワード欄は空にします。HTML出力はThymeleafでエスケープし、内部例外やSQLをエラー画面に表示しません。

### 認証の自動テスト

DBのパスワードなしで、今回のService・画面・認証だけを検証する場合：

```powershell
.\mvnw.cmd "-Dtest=UserServiceTest,AuthWebTest" verify
```

UserServiceTestは入力拒否・重複・ハッシュの保存内容を確認します。AuthWebTestはDB部分だけをMockitoで置き換え、実際のController、Service、Spring Security、Thymeleafで登録・ログイン・セッション・ログアウト・CSRFを確認します。画面確認用HTMLをtarget/ui-previewへ出力します。これらは実DBの接続や保存を検証するテストではありません。

全テストは、環境変数とMySQLを準備して ` .\mvnw.cmd clean verify ` で実行します。既存のFlorieApplicationTestsはDBを含むアプリ全体の初期化確認です。

### 実DBを使った確認手順

1. このREADMEの起動手順に従ってFLORIE_DB_PASSWORDを設定し、同じPowerShellで ` .\mvnw.cmd clean verify ` を実行します。SQLの再実行は不要です。
2. ` .\mvnw.cmd spring-boot:run ` で起動し、http://localhost:8080/register を開きます。
3. テスト用のニックネーム・未登録メール・8文字以上のパスワード・同じ確認値で登録します。ログイン画面へ戻り、登録完了が表示されることを確認します。
4. 同じメールで再登録し、重複エラーになることを確認します。空欄、短いパスワード、確認不一致も確認します。エラー後にパスワードが残っていないことも確認します。
5. 登録したメールとパスワードでログインし、ホームに自分のニックネームが表示されることを確認します。再読み込みしてもログインが維持されることを確認します。
6. ログアウトし、再び `/home` を開くとログイン画面へ戻ることを確認します。誤ったパスワードと未登録メールが同じエラーになることを確認します。

必要なら、florie_appでMySQLに接続して次のSQLで登録結果を確認できます。パスワードハッシュそのものは表示しません。

```sql
SELECT user_id, nickname, email, created_at,
       (LEFT(password_hash, 4) = '$2a$' AND CHAR_LENGTH(password_hash) = 60) AS bcrypt_format
FROM users;
```

画面はUI画像のベージュ背景、白いラベル、入力欄・茶色いボタンの幅と角丸、下部の画面切替リンクに合わせています。確認パスワード欄を補った登録画面は縦にスクロールできます。ロゴは文字とオリジナルの葉のSVGによる近似で、手書き文字の見た目は端末のフォントで変わります。

## 花の登録

### 処理と構成

`FlowerController`はGET `/flowers/register`で画面を表示し、POST `/flowers`で`FlowerRegisterForm`を検証して`FlowerService`へ渡します。ユーザーID・日付・状態は入力させず、ログイン情報とサーバーの日付から決めます。

Serviceの登録処理は次の順です。

1. 入力を再検証し、ログインユーザーのusers行をロックする。
2. ACTIVEの花があれば拒否する。
3. 選択した花種類がDBに存在し、承認されたお世話設定2件と説明が揃っていることを確認する。
4. 花のニックネーム、ログインユーザー、花種類、登録日、ACTIVEをflowersへ保存する。終了日はNULL。
5. マスタの名前・周期をコピーしてcare_tasksを2件保存する。予定日は同じ登録日からplusDays(intervalDays)で計算する。
6. 成功したらホームへリダイレクトする。

`@Transactional`によって花と予定をまとめて保存します。保存失敗は例外をServiceの外へ返して全体をロールバックします。usersのロックはUserRepository.findByEmailForUpdateのPESSIMISTIC_WRITEで取得します。花が0件でも存在するユーザー行を使うので、同じユーザーが2つのタブから登録しても順番に処理されます。後の処理は先の保存後にACTIVEの存在を調べます。今後、花を作成する別経路を追加する場合も同じロック規則が必要です。直接SQLを実行した場合まで一輪制限を保証するDB制約ではありません。

日付はDateTimeConfigで日本時間（Asia/Tokyo）を採用しています。9月14日の登録なら、水替えは9月15日、茎の確認は9月17日です。登録中に日付が変わってもずれないよう、一度取得した登録日から両方を計算します。

説明文はcare_tasksにコピーせずcare_templatesに保持します。今日のお世話一覧では花種類・お世話名から参照します。説明の履歴保存、マスタやDBスキーマの変更はありません。

### 入力と画面

花種類は必須・正の数・DBに存在するID、花のニックネームは必須・空白のみ不可・20文字以内です。文字数はユーザー登録と同じJavaのSize検証なので、一部の絵文字は2文字分です。不正なIDの形式にも日本語でエラーを表示します。エラー時は入力内容と選択を保ちます。

DBの花一覧を表示順で取得します。花の選択はラジオボタンで1つに限定し、選択した花の常時案内をCSSで表示します。JavaScriptは追加していません。8種類と準備案内を収めるため縦スクロールにしています。ホームは自分の花だけを表示し、花がいない場合だけNew Flowerへ進めます。未実装のOwakare・Gardenは無効なボタンです。今日のお世話を装ったダミーの完了ボタンはありません。花の絵は共通の仮SVGで、最終素材は後工程です。

### ビルドと自動テスト

DBへ接続せず、登録処理・画面・既存認証を確認する場合：

```powershell
.\mvnw.cmd "-Dtest=UserServiceTest,AuthWebTest,FlowerServiceTest,FlowerWebTest" verify
```

FlowerServiceTestはRepositoryを置き換え、正常登録、空白・長すぎる名前、未選択・不存在ID、一輪制限、マスタ不足、予定2件と日付、保存例外の伝播を確認します。FlowerWebTestはServiceを置き換え、画面表示、入力チェック、認証・CSRF、入力からユーザー情報を指定できないこと、遷移を確認します。これらのテストでは実MySQLのロック競合やロールバックは再現していません。

### 実DB・ブラウザで確認する手順

1. MySQLを起動し、既存の方法でFLORIE_DB_PASSWORDを設定したPowerShellで `.\mvnw.cmd clean verify` を実行する。
2. `.\mvnw.cmd spring-boot:run` で起動し、http://localhost:8080/login でログインする。SQLやマスタを再投入する必要はありません。
3. ACTIVEの花がないユーザーでホームのNew Flowerを開く。8種類と準備案内があり、花を選び替えるとその花の水量・注意事項が表示されることを確認する。
4. 未選択・空欄で送信し、エラーになることを確認する。20文字を超える入力はブラウザでも制限します。サーバー側の21文字拒否は自動テストで確認します。
5. 花と1～20文字のニックネームを指定して登録する。ホームに選んだ花種類と花のニックネームが表示されることを確認する。
6. `/flowers/register` を直接開いても、新しい花を登録できないことを確認する。ブラウザの戻る操作から再送信しても拒否されることを確認する。
7. 別のテストユーザーでログインすると、先のユーザーの花が表示されないことを確認する。

同時操作は、まだ花を持たない同一ユーザーで登録画面を2タブ開いてから、両方で登録を送信して確認できます。結果がACTIVE 1件・予定2件であることを確認してください。既存の花やユーザーを削除して確認する必要はありません。

florie_appでMySQLに接続した後、以下のSELECTで確認できます。パスワードやハッシュは表示しません。

```sql
SELECT f.flower_id, f.user_id, ft.flower_name, f.flower_nickname,
       f.started_on, f.ended_on, f.status
FROM flowers f JOIN flower_types ft ON ft.flower_type_id = f.flower_type_id;

SELECT f.flower_id, c.care_name, c.interval_days, c.next_care_date,
       DATEDIFF(c.next_care_date, f.started_on) AS days_after_registration
FROM flowers f JOIN care_tasks c ON c.flower_id = f.flower_id
ORDER BY f.flower_id, c.interval_days;

SELECT user_id, COUNT(*) AS active_count
FROM flowers WHERE status = 'ACTIVE' GROUP BY user_id;

SELECT flower_id, COUNT(*) AS task_count
FROM care_tasks GROUP BY flower_id;
```

登録直後はdays_after_registrationが1と3、active_countが各1、task_countが各2になることを確認します。全体初期化テストだけでは実際の花の保存と予定の内容までは検証できません。実DBでの同時登録・失敗時のロールバック確認は残っています。お別れが未実装なので、登録した花は今回の画面から終了できません。

## 今日のお世話一覧

HomeController → CareService → UserRepository・CareTaskRepository → DBの順に読み取ります。ログイン情報からユーザーを特定し、次の条件をすべて満たすcare_tasksだけを検索します。

- 花の所有者がログイン中ユーザーである。
- 花の状態がACTIVEである。
- next_care_dateが今日以前（<=）である。

今日の日付は既存DateTimeConfigのClockから日本時間で取得します。1回の画面表示で取得する「今日」は一つにして、検索と表示の間で日付がずれないようにしています。予定日の古い順、同じ日ならcare_task_id順です。1行を1件として読み取り、遅延日数分の行を生成しません。

花がいない場合は従来の花なしホーム、花がいて予定が0件なら「今日のお世話はありません。」を表示します。各カードにはお世話名、予定日、「今日が予定日」または「予定日を過ぎています」を表示します。カード内の「できた」から完了を記録できます。説明を開くと、花種類とお世話名に対応するcare_templatesの説明が読めます。茎の共通補足と延命剤の注意も表示します。説明用のDTOやJavaScriptは追加していません。

追加・変更した主なファイルはCareService.java、HomeController.java、CareTaskRepository.java、home.html、flower.cssです。検索には既存のfindDueTasksを利用し、同日内の並び順だけを追加しました。DBスキーマ、既存マスタ、予定日、完了履歴を変更する処理はありません。

### テスト

```powershell
.\mvnw.cmd "-Dtest=UserServiceTest,AuthWebTest,FlowerServiceTest,FlowerWebTest,CareServiceTest,HomeCareWebTest" verify
```

CareServiceTestはテスト専用H2のメモリ内DBで実際のRepositoryを動かし、今日・期限超過・未来・他ユーザー・終了済み・同日順序・説明取得を検証します。HomeCareWebTestはホームの表示、件数、空状態、未認証アクセスを確認します。既存の認証・花登録テストも実行します。

H2はpom.xmlのtestスコープなので本番アプリには入りません。テストクラス内だけにH2接続とcreate-dropを指定し、その一時DBを作成・破棄します。通常のapplication.propertiesのMySQL接続、ddl-auto=none、SQL自動実行無効の設定は維持しています。H2での成功はMySQL固有の動作検証の代わりにはなりません。

上記は一覧表示までの40テストです。完了機能を含むテストの実行方法は次節に記載します。MySQLの環境変数を設定した環境では、通常の `.\mvnw.cmd clean verify` で全件を実行してください。

### 実ブラウザとMySQLでの確認

1. 既存の方法でFLORIE_DB_PASSWORDを設定し、`.\mvnw.cmd clean verify`、続いて `.\mvnw.cmd spring-boot:run` を実行します。SQLの再投入は不要です。
2. ログインして http://localhost:8080/home を開きます。花がいる場合は今日以前の予定だけが表示されることを確認します。登録当日など全予定が未来なら、0件のメッセージになります。
3. 同じ画面を再読み込みしても、同じお世話が増えないことを確認します。完了前の再読み込みでは予定や履歴が変更されないことを確認します。
4. 花がいない別ユーザーで従来の空状態を確認します。他ユーザーの予定が見えないことも確認します。
5. ログアウト後に/homeを直接開き、ログイン画面へ戻ることを確認します。

MySQLにflorie_appで接続し、以下の読み取り用SELECTと画面を比較できます。メールの例はログイン中ユーザーのものに置き換えます。パスワードやハッシュは取得しません。

```sql
SELECT DATE(UTC_TIMESTAMP() + INTERVAL 9 HOUR) AS today_in_japan;

SELECT t.care_task_id, t.care_name, t.next_care_date,
       CASE
         WHEN t.next_care_date < DATE(UTC_TIMESTAMP() + INTERVAL 9 HOUR) THEN '期限超過'
         WHEN t.next_care_date = DATE(UTC_TIMESTAMP() + INTERVAL 9 HOUR) THEN '今日'
         ELSE '未来・表示対象外'
       END AS expected_display
FROM care_tasks t
JOIN flowers f ON f.flower_id = t.flower_id
JOIN users u ON u.user_id = f.user_id
WHERE u.email = '自分のメールアドレス'
  AND f.status = 'ACTIVE'
ORDER BY t.next_care_date, t.care_task_id;
```

上の結果のうち「未来・表示対象外」を除いた件数・名前が、画面と一致することを確認してください。期限超過のデータがまだない場合は、予定日の翌日以降に再表示して確認できます。確認のために既存の予定日を書き換えたり、PC時計を変更したりする必要はありません。日付別の条件は自動テストでも検証します。

未完了の予定は期限を過ぎても表示され続けます。「できた」で完了すると履歴を1件追加し、次回予定日を更新します。

## お世話完了

### 処理と変更ファイル

ホームのお世話カードの「できた」からPOST `/care-tasks/{taskId}/complete` を送信します。ThymeleafのフォームがCSRFトークンを付け、Spring Securityが検証します。GETでは更新しません。

CareController → CareService.completeCare → UserRepository・CareTaskRepository・CareRecordRepository → DBの順に処理します。新規ファイルはCareController.java、CareCompletionException.java、CareCompletionTest.java、CareCompletionWebTest.javaです。CareService、CareTaskRepository、home.html、flower.cssと既存HomeCareWebTestを更新しています。Entity・DB定義・マスタ・SQL・依存ライブラリは変更していません。

1. ログインユーザーのusers行をロックする（花登録と同じ順序）。
2. task IDとユーザーIDの両方で対象タスクを検索し、PESSIMISTIC_WRITEでロックする。
3. 花がACTIVE、予定日が今日以前、周期が1日以上であることを確認する。
4. 既存Clockによる日本時間の今日をcompleted_onとして、対象タスクとともにcare_recordsへ1件保存する。
5. 実際の完了日＋interval_daysをnext_care_dateへ保存する。
6. 成功メッセージを付けてホームへリダイレクトする。未来になった予定は今日の一覧から消える。

対象が存在しない、他ユーザー、ENDED、未来の予定は更新しません。他ユーザーと不存在については同じメッセージです。完了日やユーザーIDはブラウザから受け取りません。アプリの状態名はACTIVE / ENDEDで、ENDという状態はありません。

Service全体を@Transactionalで囲みます。履歴のINSERTと予定のUPDATEをそれぞれflushしますが、flushはコミットではありません。どちらかに失敗したら例外をService外へ返し、両方をロールバックします。Controllerは内部SQLを表示せず、ホームに簡潔なエラーを表示します。メッセージはSpring MVCのFlash属性で一度だけ表示します。

同時送信はロック待ちとなり、後の処理は更新後の予定日を再確認します。1回目で次回日が未来に移るため、2回目は拒否されます。古い予定日から日数を足したり、遅延回数分の履歴を追加したりしません。次回の予定日を迎えれば、その日の新しいお世話として再度完了できます。

### 自動テスト

```powershell
.\mvnw.cmd "-Dtest=UserServiceTest,AuthWebTest,FlowerServiceTest,FlowerWebTest,CareServiceTest,HomeCareWebTest,CareCompletionTest,CareCompletionWebTest" verify
```

実MySQLを使わない54件を実行します。MySQLでの既存アプリ初期化テスト1件も含めると合計55件です。MySQL用環境変数を設定した環境では `.\mvnw.cmd clean verify` で全件実行できます。

CareCompletionTestの9件は、専用H2でServiceの実トランザクションを実行します。正常完了、日本時間の実日付、期限超過、履歴1件、次回日、一覧からの除外、未来・他ユーザー・ENDED・不存在の拒否、再送、履歴INSERT後の失敗時ロールバック、2要求の同時完了を確認します。テスト自体はトランザクションで囲まず、Serviceの処理後に別の読み取りで永続化結果を確認します。同時要求の再現にのみ2スレッドを使い、アプリ本体にマルチスレッド処理は追加していません。

CareCompletionWebTestの5件は、POST・CSRF・認証、ブラウザからの日付やユーザー指定を採用しないこと、成功とエラーの通知、GET・不正形式IDの拒否を確認します。既存HomeCareWebTestは完了フォームを検証するように更新しています。

H2はテスト専用であり、実MySQLのテストガーベラ・ユーザー・マスタに接続しません。MySQL固有のロック動作は実環境で別途確認が必要です。

### 実ブラウザ・実MySQLの確認手順

1. 既存の方法でFLORIE_DB_PASSWORDを設定して `.\mvnw.cmd clean verify` を実行し、`.\mvnw.cmd spring-boot:run` で起動します。SQLを再投入しません。
2. 自分のアカウントでホームを開き、実際にお世話を済ませたタスクだけ「できた」を押します。花別説明や延命剤製品の案内に沿った手入れで完了できます。
3. 記録完了メッセージが表示され、そのタスクが今日の一覧から消えることを確認します。他の未完了タスクは残ります。
4. MySQLで以下の読み取り専用SELECTを実行します。メールは自分のものへ置き換え、必要なら完了前にも実行して件数を比較してください。パスワード・ハッシュは取得しません。

```sql
SELECT t.care_task_id, t.care_name, t.interval_days, t.next_care_date,
       COUNT(r.care_record_id) AS record_count,
       MAX(r.completed_on) AS last_completed_on,
       DATE_ADD(MAX(r.completed_on), INTERVAL t.interval_days DAY) AS expected_next_date
FROM care_tasks t
JOIN flowers f ON f.flower_id = t.flower_id
JOIN users u ON u.user_id = f.user_id
LEFT JOIN care_records r ON r.care_task_id = t.care_task_id
WHERE u.email = '自分のメールアドレス'
  AND f.status = 'ACTIVE'
GROUP BY t.care_task_id, t.care_name, t.interval_days, t.next_care_date;
```

完了したタスクのrecord_countが1だけ増え、last_completed_onが日本時間の今日、next_care_dateとexpected_next_dateが一致することを確認します。水替えなら翌日、茎なら3日後です。期限超過でも現在日基準です。

二重送信は、完了前に同じホームを2タブ開き、一つ目で完了した後、古い二つ目の画面でも同じタスクの「できた」を押して確認できます。二つ目は拒否され、履歴件数と次回日は増えません。ログアウトした後、古い画面から送信しても更新されないことを確認します。

未来や他ユーザーのIDを直接送信するケース、ロールバックは自動テストで検証済みです。確認のために実DBの予定日や所有者を書き換える必要はありません。実MySQLでは、ブラウザでユーザーが「できた」を押した対象だけを通常の完了処理で更新します。お別れ・終了・お花畑・履歴一覧・編集は今回未実装です。


## 花とのお別れ

ホームのOwakare → GET `/flowers/{flowerId}/end`（確認のみ） → POST `/flowers/{flowerId}/end`（確定） → 花なしホーム、という流れです。確認画面のHomeはキャンセル用の通常リンクです。GETやキャンセルでは保存処理を呼びません。「お花畑に送る」は確定資料・UI06の表記です。お花畑の画面自体は次工程です。

FlowerController → FlowerService → 既存UserRepository・FlowerRepository → DBの構造です。ログイン情報からユーザーを特定し、花IDとユーザーIDの両方で検索します。ACTIVEだけを終了でき、他人・不存在・ENDEDは拒否します。確認表示時だけでなく確定時にも再確認します。

`endFlower`の@Transactional内で、最初にusers行をロックします。花登録・お世話完了も同じロックを使うため、同じ利用者の更新は順番に進みます。後から来た再送はENDEDを検出し、終了日を上書きしません。既存Clockによる日本時間の今日をended_onへ、ENDEDをstatusへ同時に保存します。ブラウザから終了日や所有者は受け取りません。

Flower、care_tasks、care_recordsは削除しません。予定日・完了履歴も更新しません。既存のホーム取得条件がACTIVEのため、終了後の花と予定はホームから外れ、新しい花を登録できます。スキーマ・Entity・Repository・SQL・マスタ・依存関係の変更はありません。

追加ファイル：FlowerEndException.java、flower-end.html、FlowerEndTest.java、FlowerEndWebTest.java。
変更ファイル：FlowerService.java、FlowerController.java、home.html、flower.css、本README。

### テスト

```powershell
.\mvnw.cmd "-Dtest=UserServiceTest,AuthWebTest,FlowerServiceTest,FlowerWebTest,CareServiceTest,HomeCareWebTest,CareCompletionTest,CareCompletionWebTest,FlowerEndTest,FlowerEndWebTest" verify
```

上記は実MySQLを使わない68件です。追加14件は確認表示・キャンセル導線・確定・日本時間の終了日・データ保持・今日のお世話からの除外・新しい花の登録・他人/不存在/再終了の拒否・保存失敗時ロールバック・未ログイン/CSRFなしの拒否を確認します。FlowerEndTestは専用の一時H2を使い、実MySQLの花を変更しません。既存のMySQL初期化テスト1件を含めた全件数は69件です。

### 実ブラウザ・実MySQLの確認

1. 既存の方法で環境変数を設定したPowerShellで `.\mvnw.cmd clean verify`、続いて `.\mvnw.cmd spring-boot:run` を実行します。SQLは再投入しません。
2. ログインし、以下のSELECTで対象のflower_id・status・ended_onと予定・履歴件数を控えます。
3. ホームのOwakareから確認画面を開き、種類とニックネームを確認します。Homeでキャンセルし、状態・終了日・件数が変化しないことを確認します。
4. 再度確認画面を開き、「お花畑に送る」で確定します。完了メッセージと花なしホームが表示され、以前のお世話が表示されないことを確認します。
5. 同じSELECTで、元の花がENDED、日本時間の今日がended_onに保存され、予定・履歴の件数が変わっていないことを確認します。
6. 元の確認URLを再度開いてもホームに戻され、終了日が上書きされないことを確認します。New Flowerから新しい花を登録でき、元の花が残っていることも確認できます。
7. ログアウト後は確認URLがログイン画面へ戻ることを確認します。

```sql
SELECT f.flower_id, f.flower_nickname, f.status, f.started_on, f.ended_on,
       (SELECT COUNT(*) FROM care_tasks t WHERE t.flower_id = f.flower_id) AS task_count,
       (SELECT COUNT(*) FROM care_records r
        JOIN care_tasks t ON t.care_task_id = r.care_task_id
        WHERE t.flower_id = f.flower_id) AS record_count
FROM flowers f
JOIN users u ON u.user_id = f.user_id
WHERE u.email = '自分のメールアドレス'
ORDER BY f.flower_id;
```

実MySQLでの終了操作とロックの動作確認は利用者の環境で行います。お花畑・思い出・復元・編集・履歴一覧はまだ実装していません。

今回の検証結果：上記68件はすべて成功（失敗0・エラー0）、Maven verifyはBUILD SUCCESSでした。実MySQLを使用する初期化テスト1件は今回実行していません。確認画面のHTMLをChromeで表示し、幅320・390・1280pxで横はみ出しがないことを確認しました。


## お花畑・思い出表示

ホームのGarden → GET `/garden` → 花を選択 → GET `/garden/{flowerId}` の順に表示します。GardenController → FlowerService → 既存Repository → DBの構造で、ユーザーはログイン情報から特定します。

一覧は既存の`findByUserUserIdAndStatusOrderByEndedOnDesc`で、自分のENDEDだけを終了日の新しい順に取得します。同日の並び順は固定していません。詳細は花IDとユーザーIDで取得し、さらにENDEDを確認します。他人・ACTIVE・不存在は同じメッセージでお花畑へ戻します。未ログインは既存Spring Securityによりログイン画面へ戻ります。

今回の「Figmaを最優先する」という指定に従い、一覧はUI07の3列配置、種類・ニックネーム・既存の共通仮SVGとし、日付はUI09のMemoryカードへまとめました。詳細設計5.12の一覧日付とUI07の差は、確定資料8.2で保留されていた表示差分です。一緒に過ごした日数は追加していません。空の場合はUI08の「まだお花がいません」「お世話を終えたお花がここに並びます」を表示します。多数の花は次の段へ並べ、縦スクロールします。

思い出には種類・ニックネーム・共通仮SVG・お世話を始めた日・お世話を終えた日を表示します。日付は既存DATEをyyyy/MM/ddで表示します。メモ、写真、予定一覧、完了履歴一覧、編集・復元は実装しません。

読み取り用Serviceは`@Transactional(readOnly = true)`とし、検索だけを行います。readOnly指定だけに依存せず、Controller・Serviceとも更新・削除・Entityへの値設定を呼ばず、POSTの入口も設けていません。CareTask・CareRecordのRepositoryもこの機能では使いません。DBスキーマ、SQL、マスタ、Entity、Repository、依存ライブラリ、docsは変更していません。

追加：GardenController.java、garden.html、memory.html、GardenTest.java。
変更：FlowerService.java、home.html、flower.css、README.md。

### テスト方法

```powershell
.\mvnw.cmd "-Dtest=UserServiceTest,AuthWebTest,FlowerServiceTest,FlowerWebTest,CareServiceTest,HomeCareWebTest,CareCompletionTest,CareCompletionWebTest,FlowerEndTest,FlowerEndWebTest,GardenTest" verify
```

GardenTestは10件。専用H2と実Service・Repository・Controller・Thymeleaf・Securityを使用し、取得条件・終了日順・空状態・詳細日付・HTMLエスケープ・直接アクセス拒否・未ログイン・GETの繰り返しでのデータ保持・遷移・POST不存在を確認します。実MySQLを使わないテストは既存68件と合わせて78件です。MySQL用の既存初期化テスト1件を含めると全79件です。

### 実ブラウザ・実MySQLでの確認

1. 既存の環境変数を設定済みのPowerShellで `.\mvnw.cmd clean verify`、続いて `.\mvnw.cmd spring-boot:run` を実行します。SQLは再投入しません。
2. ログインし、ホームのGardenからお花畑へ移動します。終了済み「テストガーベラ」が表示され、ACTIVEの花が表示されないことを確認します。
3. 花を選び、種類・ニックネーム・開始日・終了日が下記SELECTと一致することを確認します。Gardenで一覧へ、Homeでホームへ戻れます。
4. 一覧・思い出を数回再読み込みし、下記SELECTの結果と件数が変わらないことを確認します。
5. 終了済みの花がない別アカウントでは空状態になることを確認します。そこで元の花の詳細URLを直接開いても、表示されずお花畑へ戻ることを確認します。
6. 自分のACTIVEのIDや存在しないIDでも思い出が開かないこと、ログアウト後は両画面がログイン画面へ戻ることを確認します。確認用に実データを編集・削除する必要はありません。

```sql
SELECT f.flower_id, ft.flower_name, f.flower_nickname,
       f.status, f.started_on, f.ended_on
FROM flowers f
JOIN flower_types ft ON ft.flower_type_id = f.flower_type_id
JOIN users u ON u.user_id = f.user_id
WHERE u.email = '自分のメールアドレス'
ORDER BY f.ended_on DESC;

SELECT COUNT(*) AS flower_count FROM flowers;
SELECT COUNT(*) AS task_count FROM care_tasks;
SELECT COUNT(*) AS record_count FROM care_records;
```

最終的な8種類の花イラストと正式フォント等の調整は別工程です。今回の仮SVGは共通なので、種類は併記した花名で区別します。

今回の検証結果：78件すべて成功（失敗0・エラー0）、Maven verifyはBUILD SUCCESS。実MySQL用の初期化テスト1件は今回実行していません。一覧・空状態・思い出の3画面をChromeで確認し、320・390・1280px幅で横はみ出しがないことを確認しました。
