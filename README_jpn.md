<p align="center">
  <img src="images/ARMOR_BANNER.svg" alt="ARMOR-ANDROID-CONTROL banner" width="100%">
</p>

# 📱 ARMOR-ANDROID-CONTROL

<p align="center">
  <a href="README.md">🇺🇸 English</a> |
  <a href="README_spa.md">🇪🇸 Español</a> |
  <a href="README_fra.md">🇫🇷 Français</a> |
  <a href="README_ita.md">🇮🇹 Italiano</a> |
  <a href="README_deu.md">🇩🇪 Deutsch</a> |
  <a href="README_zho.md">🇨🇳 简体中文</a> |
  🇯🇵 <b>日本語</b> |
  <a href="README_por.md">🇵🇹 Português</a>
</p>

### ARMOR-SERVER のモバイルオペレータークライアント：警報、カメラ、レーダー、エネルギー、ネットワーク、ノードの設定。8 言語対応

<p align="center">
  <img src="https://img.shields.io/badge/License-GPL%203.0-blue.svg" alt="GPL 3.0">
  <img src="https://img.shields.io/badge/Language-Kotlin-7f52ff.svg" alt="Language">
  <img src="https://img.shields.io/badge/UI-Jetpack%20Compose-4285f4.svg" alt="UI">
  <img src="https://img.shields.io/badge/Languages-8-00E5FF.svg" alt="Languages">
  <img src="https://img.shields.io/badge/Tests-324%20passing-2ea44f.svg" alt="Tests">
  <img src="https://img.shields.io/badge/Version-0.7.5-0f6cbd.svg" alt="Version">
  <img src="https://img.shields.io/badge/Maturity-established-2EA44F.svg" alt="Maturity">
</p>

---

**正直さのチェック - 今日動いているもの:** アドレスの安全性、警報の判断、警報ウォッチャー、翻訳、読み取り専用ロールのルールは 324 件のユニットテストでカバーされ、アプリはビルドできます（`gradlew testDebugUnitTest assembleDebug`）。実際のスマートフォンで CM5 テストベンチに対して動作し、サインイン、カメラモニター、警戒と警報、レーダー、Bluetooth でのノード設定、GitHub からのアップデートが使われています。**まだ**証明されていないもの：実機でのソーラー・電気・警報盤の画面（存在するのはレーダーノードのみ）、Firebase 経由のプッシュ（アプリには入っており、独自の Firebase プロジェクトが必要です。実機ではまだ試していません）、各メーカーのバッテリー規則下でのバックグラウンド警報監視。

---

## 📋 ひと目で

| | |
|---|---|
| バージョン | `0.7.5` |
| 成熟度 | 確立 |
| テスト | 324 (`gradlew testDebugUnitTest`) |
| 技術 | Kotlin / Jetpack Compose |
| 実機での確認 | 実際のスマートフォンで CM5 テストベンチに対して使用。ソーラー・電気・警報盤の画面は実ノードを待っています |

## 🎯 概要

* **サーバー自身のログインでサインイン：**IP、ポート、ユーザー、パスワード。パスワードは HttpOnly セッションを作ります。スマートフォンに保存されるのは **保存する** にチェックしたときだけです（既定はオフ。ユーザー名とパスワードは Keystore の専用鍵で暗号化され、そのサーバー専用で、チェックを外すと直ちに消去されます）。**viewer** ロールは見るだけで、何かを変更するボタンはすべてオフです。
* **端末のロックによる保護：** アプリは、警戒の開始・解除、サイレン停止、解錠などの重要な命令の前に、端末自体の指紋・顔・PINを求めます（指紋または顔認証がある端末でのみ初期設定でオン）。オンにすればアプリを開くときにも求めます。管理者はStudioから必須にできますが、あなたが選んだ設定を下げることはできません。画面ロックのない端末が締め出されることはなく、ロック画面にはいつでもパスワードで再ログインする手段があります。任意で（既定ではオフ、目立つ警告付き）、保存されたセッションを封じる鍵をスマートフォンのロック解除に結び付けられます。その場合、再起動後はロックを解除するまでバックグラウンドの警報監視が待機します。
* **カメラモニター：**1〜16 タイル、拡大表示、縦横比を保つライブ MJPEG、範囲を限った PTZ パッド、スナップショットと録画（録画にはビューアーと削除確認付き）。
* **警戒と解除：**まず**警戒前のチェック**（開いている、無応答、停止しているもの）を行い、解除には PIN。確認する**警報**（待機中のものにはバッジ）、**デバイス**（煙、ガス、浸水、ドア、窓、動き、気候、プラグ、照明、サイレン、鍵、シャッター、門扉・ガレージドアのモーター、給湯器）の状態とオン / オフ / 切り替え。
* **あなたに届く警報：**重大度ごとの音、任意の**音声警報**、ノードが HIGH になった、カメラが無応答、デバイス警報、（警戒中に）ノードがオフラインになった通知、そして**鳴っている警報盤はスマートフォンを起こします**。バックグラウンド監視は再起動後も残り、サーバー停止中は間隔を空けます。
* **ライブレーダー（2D と 3D）：**レーダータブは Studio で設計したサイトと、レーダーが捉えて動く人を描き、1.5 秒ごとに更新します。3D 表示はドラッグ、ピンチ、回転できます。
* **ソーラーと電気のエネルギー：**合計（太陽、消費、充電状態付きのバッテリー、系統の有無）、各インバーターとバッテリー（セルは要求に応じて）、電気ノードのチャンネルと警報。
* **警報盤：**各 ARMOR-NODE-ALARM ノードの状態、ゾーンとイベント、サーバーが許可した場合の警戒・解除コマンド。
* **ネットワーク：**家のネットワークのデバイス、インターネットの状態、プロバイダーと場所付きの**公開アドレス**（ネットワークノードの報告どおり）。
* **Bluetooth でフィールドノードを設定**（サインイン画面または*その他 > ノードを設定*から）：`ARMOR-xxxxxx` を発信するノードを見つけ、新しいノードの管理者を作るか（ノードの PIN で）サインインし、Wi-Fi ネットワークを検索して、ノード名、ルーターの Wi-Fi または固定アドレス、ブローカーを設定します。**ノードファインダー**はすでにネットワーク上にあるものを一覧します。
* **アシスタント：**文字と音声のコマンド（クローズドなリスト、8 言語、あなたに結び付いた確認）で、家の状態を尋ねたり、警戒、解除、消音、照明のオン・オフができます。
* **証拠ライブラリ、**境界とノードの状態、すべての警報・ノード・カメラ・デバイス・警報・モード変更の**履歴**、**天気マップ**、設備の**サービス**とアップデート。
* **8 言語**（英語、スペイン語、ドイツ語、フランス語、イタリア語、日本語、中国語、ポルトガル語）を*その他 > 設定*で選ぶか端末に従います。すべての文が 8 つの表にあることを検査します。
* **パスワードの扱い：**平文 HTTP は、プライベート LAN またはループバックの IPv4 *リテラル*にのみ許可されます。プライベートアドレスのように始まるだけのホスト名（`10.attacker.example`）や先頭にゼロのあるアドレスは拒否されます。見た目はアイコン中心で、ほぼ黒の面、シアンのアクセント、注意にはアンバーを使います。
* **スマホからのセキュリティ：**2 段階認証と復旧コード、開いているセッション、サーバーから見たスマホの確認、外からの重要な操作での再認証、そして管理者向けに VPN（設定付きの端末）とダイナミック DNS の名前。**ステータス画面にシステム全体、そして外からの接続：** ステータス画面は警報パネル、サービス、録画中のカメラ、セキュリティとリモートアクセスをまとめ、各カードがその画面を開きます。入力したアドレスが自宅の外で https でない場合は、サインインが先に進めない理由を示し、2つの安全な方法（VPN、または https の公開アドレス）をガイドが説明します。
* **ノードの状態と警戒モード:** 「その他」にノードの状態画面（稼働時間、再起動の理由、メモリ、回線と信号、ブローカー接続、再起動、最後のエラー、サーバーの警告）があり、警報盤画面にはモード選択（外出、在宅、夜間、独自）があって、ファームウェアが対応していれば選んだモードで警戒します。8 言語対応です。
* **履歴の接続一覧:** 履歴画面に読み取り専用の2つ目の一覧があり、サーバーに接続するデバイスとアプリ（Studio、このアプリ、ノードのパネル、音声サービス）を、最終確認時刻、開いているセッション、サインインと拒否とともに表示します。アプリは自分が何者かをサーバーに伝えるため、自分自身として表示されます。アドレスは管理者にだけ表示されます。
* **バッテリーのメンテナンス（管理者向け）：** Pylontechバッテリーの簡単な操作——時計、カウンタとログ、再起動、容量、MOSFETテスト、ヒューズ故障、出力——を、Studioと同じ2段階（サーバーがノードにバッテリーを読み取らせ、画面に実行内容を正確に表示し、シリアル番号を再入力して「今すぐ実行」）で行い、結果と変更内容を表示します。制限値、ファームウェア、エキスパートコンソールはStudioで行います。
* **ノードの制御（管理者）：** その他 > ノード制御では、Studio と同じ操作（再起動、一時停止、再開、スタンバイ（動作停止、接続は維持）、識別、診断、メンテナンスのオン／オフ）を、1 台または全ノードに対して行えます。ノード名（全ノードの場合は RESTART、PAUSE、STANDBY のいずれかの単語）の再入力と新たな本人確認を求め、各ノードの進み具合を表示し、警報パネルやレーダーが一時停止またはスタンバイのとき家が無防備になることを警告します。工場出荷時リセット、消去、ディープスリープはありません。
* **開いているアプリのアラームウィンドウ：** 確認待ちのアラームがある間、アラーム画面以外のすべての画面の上にバナーが点滅します（重大は赤で速く、それ以外は琥珀色、端末のアニメーションがオフなら静止）。最も重大なアラームと待機件数を示し、その下でアプリを使い続けられます。アラームを開く、バナーから確認する、次のアラームが来るまで閉じる、のいずれもできます。
* **外出先から：** 2 つのアドレス（自宅の Wi-Fi ではプライベートなもの、モバイルデータまたは VPN では外出先用）を端末の場所に応じて選び、一方が応答しなければもう一方を試し、何が失敗したかを表示します。インストールの証明書の鍵は初回に記憶（または Studio の QR のテキストでペアリング）し、異なる鍵は拒否します。一度 https で接続したサーバーへの平文 http は拒否します。ノードのパネルはサーバー経由で開けます。警報はデータのみのプッシュとして届くことがあり、プッシュでも監視でも 1 度だけ通知します。開いているアプリとバックグラウンド監視はサーバーの読み取りを共有します。カメラの映像は画面外で停止し、待ち時間を延ばしながら再開します。

## 🧩 A.R.M.O.R. での役割

ARMOR-ANDROID-CONTROL は [ARMOR-STUDIO](https://github.com/JuanenRac/ARMOR-STUDIO) と同じ [ARMOR-SERVER](https://github.com/JuanenRac/ARMOR-SERVER) へのスマートフォンの窓口で、読み取りも指示もサーバーを通してのみ行います。警報があなたを追いかけ（鳴っている警報盤はスマートフォンを起こします）、アシスタントに声で尋ねられ、現場で新しいノードの横から Bluetooth で設定するためのツールでもあります。

## 📂 リポジトリの構成

```text
ARMOR-ANDROID-CONTROL/
├── app/src/main/java/es/electrohobby3d/armor/
│   ├── ArmorActivity.kt (the shell), EntryScreens.kt, HomeScreens.kt, CameraScreens.kt, RadarScreens.kt, DevicePanels.kt, MoreScreens.kt
│   ├── SolarScreens.kt, ElectricalScreens.kt, AlarmPanelScreens.kt, NetworkScreens.kt, ServicesScreens.kt, NodeControlScreens.kt, WeatherScreens.kt, VoiceScreens.kt, MediaViewer.kt
│   ├── NodeBleClient.kt, NodeSetupScreen.kt, NodeFinderScreens.kt, NodePanel.kt   configure a node over Bluetooth (the protocol is in model/NodeBle.kt)
│   ├── ArmorViewModel.kt, Friendly.kt, AlarmPolicy.kt, AlarmBanner.kt, AlarmNotifier.kt, AlarmSpeaker.kt, AlarmWatcherService.kt, BootReceiver.kt
│   ├── Tr.kt (the eight languages), ArmorTheme.kt, ServerEndpoint.kt, SessionVault.kt, DeviceLock.kt, PhoneLock.kt, LockScreens.kt, PinDialog.kt, MjpegFeed.kt, AlarmCodes.kt
│   ├── network/   model/   update/
│   └── assets/i18n/   en, es (the source phrases), de, fr, it, ja, zh, pt
├── docs/CLIENT_BOUNDARY.md, PUSH_NOTIFICATIONS.md   (the Firebase steps, in eight languages)
└── app/src/test/   endpoint-safety, plain-words, node-protocol, solar-model, translation and role tests
```

## 🛠️ 開発環境

**必要なもの:** JDK 17+, Android SDK（compile SDK 37）, Android 8.0+ のスマートフォン（minSdk 26）

```powershell
.\gradlew.bat testDebugUnitTest assembleDebug   # 324 tests, then the debug APK
adb install -r app\build\outputs\apk\debug\app-debug.apk
```

リリースは GitHub で公開され、アプリ自身がそこから更新を確認します。APK は管理者自身のリリース用の鍵（リポジトリの外に保管）で署名されており、アプリはダウンロードしたファイルを Android のインストーラーに渡す前に、その署名証明書を自身のものと照合します。[クライアントの境界](docs/CLIENT_BOUNDARY.md)を参照してください。

## 🔗 関連プロジェクト

**A.R.M.O.R.**（Autonomous Radar & Multimodal Observation Range）は、独立したリポジトリで構成される周辺警備システムです。それぞれに独自のバージョン、テスト、README があります。ファミリーは次のとおりです：

* **[ARMOR-COMMON](https://github.com/JuanenRac/ARMOR-COMMON)** - メッセージ契約、検証器、適合性ベクトル、生成された型
* **[ARMOR-NODE-RADAR](https://github.com/JuanenRac/ARMOR-NODE-RADAR)** - ESP32-S3 用フィールドノードのファームウェア。レーダー 3 基と独自の Web パネル付き
* **[ARMOR-NODE-SOLAR](https://github.com/JuanenRac/ARMOR-NODE-SOLAR)** - 太陽光インバーターとバッテリーのプロトコル、およびゲートウェイノードのメッセージ
* **[ARMOR-NODE-ELECTRICAL](https://github.com/JuanenRac/ARMOR-NODE-ELECTRICAL)** - 電気ノード：電力量計、電力網の計測メッセージ、開閉のルール
* **[ARMOR-NODE-ALARM](https://github.com/JuanenRac/ARMOR-NODE-ALARM)** - 警報ノードと警報盤：警戒区域、警戒セット、遅延、サイレン、PIN。サーバーがあってもなくても
* **[ARMOR-NODE-HMI](https://github.com/JuanenRac/ARMOR-NODE-HMI)** - タッチパネル：壁面ディスプレイでのシステム状態表示、警戒・確認操作、音声アシスタントの拠点
* **[ARMOR-NETWORK](https://github.com/JuanenRac/ARMOR-NETWORK)** - ローカルネットワーク：機器、インターネット、そして変化
* **[ARMOR-SERVER](https://github.com/JuanenRac/ARMOR-SERVER)** - 中央コーディネーター：テレメトリ、アラーム、デバイス、太陽光の測定値、カメラ
* **[ARMOR-STUDIO](https://github.com/JuanenRac/ARMOR-STUDIO)** - Web コンソール：カメラ、レーダー、アラーム、太陽光発電、2D/3D サイト設計
* **ARMOR-ANDROID-CONTROL** (このリポジトリ) - リアルタイム 2D/3D レーダー付きの Android オペレータークライアント
* **[ARMOR-IOS-CONTROL](https://github.com/JuanenRac/ARMOR-IOS-CONTROL)** - iPhone オペレータークライアント
* **[ARMOR-SERVER-AI](https://github.com/JuanenRac/ARMOR-SERVER-AI)** - 判断を説明し、決して動作しない視覚推論ポリシー
* **[ARMOR-VISION](https://github.com/JuanenRac/ARMOR-VISION)** - Jetson Orin NX 向けのカメラのゾーン、マスク、検知ルール
* **[ARMOR-VOICE-AI](https://github.com/JuanenRac/ARMOR-VOICE-AI)** - 偽造できない確認を備えたオフライン音声インテント
* **[ARMOR-HARDWARE](https://github.com/JuanenRac/ARMOR-HARDWARE)** - 筐体、電子部品、ベンチ受け入れマトリクス
* **[ARMOR-DEVOPS](https://github.com/JuanenRac/ARMOR-DEVOPS)** - デプロイ、CM5 テストベンチ、バックアップ、TLS
* **[ARMOR-SIMULATOR](https://github.com/JuanenRac/ARMOR-SIMULATOR)** - 再現可能な故障を備えたオフラインのテレメトリシミュレーター
* **[ARMOR-UPDATER](https://github.com/JuanenRac/ARMOR-UPDATER)** - エコシステム自身のリポジトリを検出し、インストールし、更新する
* **[ARMOR-DOCS](https://github.com/JuanenRac/ARMOR-DOCS)** - アーキテクチャ、セキュリティ基準、機能マトリクス

## 📚 ドキュメントとコミュニティ

詳しくは：

* [機能マトリクス：実証済みのものとそうでないもの](https://github.com/JuanenRac/ARMOR-DOCS/blob/main/docs/CAPABILITY_MATRIX.md)
* [プロジェクト一覧：バージョンとリポジトリ間の依存関係](https://github.com/JuanenRac/ARMOR-DOCS/blob/main/docs/PROJECT_CATALOG.md)
* [このリポジトリの変更履歴](CHANGELOG.md)
* [ライセンス（GPL-3.0-or-later）](LICENSE)
* 質問・提案・報告：electrohobby3d@gmail.com

## 👤 作者

**JuanenRac (Electro Hobby 3D)** · electrohobby3d@gmail.com

## 📜 ライセンス

GPL-3.0-or-later - [LICENSE](LICENSE) を参照。
