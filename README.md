# Florie

一輪挿しの花のお世話を支援するWebアプリです。現在はDB基盤とユーザー新規登録・ログイン・ログアウトまで実装しています。花登録・お世話・お花畑は未実装です。SQLのDBへの反映は手動で行います。

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

次は本ページ末尾の手順で、実DBを使ったユーザー登録・ログイン・ログアウトを確認します。パスワード等はGitへ保存しません。花のお世話の具体的な初期設定は引き続き未確定です。

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

SQLはプロジェクト直下のsqlフォルダに置き、自動実行の対象にしていません。`ddl-auto=none`と`spring.sql.init.mode=never`も維持しています。初期データのINSERTはありません。

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

外部キーはON DELETE RESTRICTとし、JPAにも削除の連動設定は付けていません。親を削除して履歴が連鎖的に消えることを防ぎます。ただし全レコードの削除禁止を保証する仕組みではありません。JpaRepositoryの標準削除メソッドは使わず、今後のServiceで終了を状態更新として実装します。一輪制限や同時操作対策もService工程で実装します。

ニックネームはVARCHAR(20) NOT NULLとCHECK制約で空・半角空白のみを拒否します。ユーザーのニックネームは今回Form・Serviceで入力検証を追加しています。花の入力画面は今後実装します。SQLの文字数はMySQLのCHAR_LENGTHを基準とし、今回のJava入力検証の数え方は次節に記載します。

## ユーザー登録・ログイン・ログアウト

### 処理とファイルの役割

- `controller/AuthController.java`：ログイン・登録画面の表示、登録結果の画面への受け渡し。
- `form/UserRegisterForm.java`：登録入力と必須・形式・長さ・確認一致の検証。確認パスワードはDBには渡しません。
- `service/UserService.java`：重複確認、ハッシュ化、ユーザー保存。登録全体をトランザクションで管理します。
- `service/EmailAlreadyRegisteredException.java`：メール重複をControllerへ伝えます。
- `service/CustomUserDetailsService.java`：UserRepositoryでメールアドレスから認証用ユーザー情報を取得します。
- `config/SecurityConfig.java`：Spring Securityのログイン・ログアウト・アクセス制御とBCryptの設定。
- `controller/HomeController.java`：認証済みユーザー名を取得する最小限の遷移先。花の状態は判定しません。
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

画面はUI画像のベージュ背景、白いラベル、入力欄・茶色いボタンの幅と角丸、下部の画面切替リンクに合わせています。確認パスワード欄を補った登録画面は縦にスクロールできます。ロゴは文字とオリジナルの葉のSVGによる近似で、手書き文字の見た目は端末のフォントで変わります。ホームは今回の認証確認用の最小画面です。
