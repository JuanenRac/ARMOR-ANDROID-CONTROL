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
  🇯🇵 <b>日本語</b>
</p>

### ARMOR-SERVER のモバイルオペレータークライアント

<p align="center">
  <img src="https://img.shields.io/badge/License-GPL%203.0-blue.svg" alt="GPL 3.0">
  <img src="https://img.shields.io/badge/Language-Kotlin-7f52ff.svg" alt="Language">
  <img src="https://img.shields.io/badge/UI-Jetpack%20Compose-4285f4.svg" alt="UI">
  <img src="https://img.shields.io/badge/Maturity-functional%20baseline-00E5FF.svg" alt="Maturity">
</p>

---

**正直さのチェック - 今日動いているもの:** エンドポイントの安全性とアラーム判断の規則には単体テスト（55 件）があり、アプリはビルドできます。**スマートフォンでサーバーに対して実行したことはない**ため、通知権限のフローとバックグラウンドサービスは未検証です。警戒、アラーム、デバイスはサーバーのルートに従いますが、実機では試していません。

---

## 🎯 概要

* **サーバー自身のログインでサインイン：** IP、ポート、ユーザー、パスワード。パスワードは HttpOnly セッションを作り、スマートフォンには保存されません。
* **カメラモニター：** 1 から 16 タイル、最大化表示、縦横比を保つライブ MJPEG、範囲を制限した PTZ パッド、スナップショットと録画。
* **警戒・解除**は確認のあとに行います。**アラーム**は確認応答でき、待機中のものはバッジで表示。**デバイス**（煙、ガス、浸水、ドア、窓、動き、気候、プラグ、照明、サイレン、鍵）は状態と オン / オフ / 切り替え 付き。
* **2D と 3D のライブレーダー：** レーダータブは Studio で設計したサイト（地面、建物、木、ポスト、レーダーとカメラの視野）と、レーダーが検出して動く人を 1.5 秒ごとに更新して描きます。3D 表示はドラッグ、ピンチ、回転できます。配置は Studio の規則に従いますが、実際のレーダーを表示したことはありません。
* **太陽光：** 「Más」の項目（と状態画面のタイル）に、合計（太陽、消費、充電量と流れを含むバッテリー、系統の有無）と、サーバーが報告するすべてのインバーターとバッテリーの数値を表示します。バッテリーのセルは必要に応じて表示し、最高と最低を強調し、まだデータを待っている機器、および例の値や無応答の機器には印が付きます。開いている間は 5 秒ごとに更新。サーバーの応答形式に対するテストと、例の値を持つローカルサーバーに対するエミュレーターでの動作確認を行いましたが、実機では一度も試していません。太陽光の警報（インバーターの故障、バッテリー残量低下や保護動作、応答しなくなった機器）は、機器の警報と同様にスマートフォンに通知されます。
* **Bluetooth でフィールドノードを設定**（ログイン画面または *Más > Configurar un nodo* から）：`ARMOR-xxxxxx` を発信しているノードを見つけ、新しいノードの管理者を作るかサインインし、Wi-Fi ネットワークを検索して、ノード名、ルーターの Wi-Fi または固定アドレス、ブローカーを設定します。イーサネットケーブルのないノード向けです。コンパイルと単体テストは済んでいますが、ノードや実機で動かしたことはありません。
* **証拠ライブラリ、** 周辺とノードの状態、およびすべての警告、ノード、カメラ、デバイス、アラーム、モード変更の**履歴**。
* **アラーム通知：** ノードが HIGH に達したとき、カメラが応答しなくなったとき、デバイスのアラーム（煙、ガス、浸水、パニックは常時。ドア、窓、動きセンサーは警戒中のみ）、そして警戒中にノードがオフラインになったとき。任意のバックグラウンド監視は現在のセッションを使い、終了するとそれを知らせます。
* **パスワードへの配慮：** 平文の HTTP は、プライベート LAN またはループバックの IPv4 *リテラル* にのみ許可されます。プライベートアドレスのように始まるだけのホスト名（`10.attacker.example`）や、先頭にゼロが付いたアドレス（リゾルバーによっては `010.0.0.1` を公開アドレスの `8.0.0.1` と読む）は拒否されます。
* **電力網：** 「その他」の項目（と状態画面のタイルの）が、系統入力の電力（受電か逆潮流か）、電気ノードが測定するすべてのチャンネル（電圧、電流、電力、電力量、周波数、力率、スイッチの状態）、電力量計の警報を表示します。電気ノードの警報（電力量計の警報、商用電圧の範囲外、系統の喪失、応答しなくなったノード）は、ソーラーの警報と同様にオペレーターに通知されます。インバーターのカードには、2 つ目の PV 入力と並列システムのユニットも表示されます。
* **アイコンで作られた見た目：** ほぼ黒の面、シアンのアクセント、注意を促すアンバー、言葉の少ない大きなアイコン、下部バー、情報ページ、サインアウトボタン。

## 📂 リポジトリの構成

```text
ARMOR-ANDROID-CONTROL/
├── app/src/main/java/es/electrohobby3d/armor/
│   ├── ArmorActivity.kt (the shell), EntryScreens.kt (splash, sign-in, account, About), HomeScreens.kt, CameraScreens.kt, RadarScreens.kt, DevicePanels.kt, MoreScreens.kt, SolarScreens.kt
│   ├── NodeBleClient.kt, NodeSetupScreen.kt   configure a radar, solar or electrical node over Bluetooth (the protocol is in model/NodeBle.kt)
│   ├── ArmorViewModel.kt, Friendly.kt, AlarmPolicy.kt, AlarmNotifier.kt, AlarmWatcherService.kt
│   ├── ArmorTheme.kt, ServerEndpoint.kt, MjpegFeed.kt
│   ├── network/   model/
├── docs/CLIENT_BOUNDARY.md
└── app/src/test/   endpoint-safety, plain-words, node-protocol and solar-model tests
```

## 🛠️ 開発環境

```powershell
.\gradlew.bat testDebugUnitTest assembleDebug
adb install -r app\build\outputs\apk\debug\app-debug.apk
```

デバッグ APK は配布用に署名されていません。[クライアントの境界](docs/CLIENT_BOUNDARY.md)を参照。

## 🔗 関連プロジェクト

**A.R.M.O.R.**（Autonomous Radar & Multimodal Observation Range）は、独立したリポジトリで構成される周辺警備システムです。それぞれに独自のバージョン、テスト、README があります。ファミリーは次のとおりです：

* **[ARMOR-COMMON](../ARMOR-COMMON)** - メッセージ契約、検証器、適合性ベクトル、生成された型
* **[ARMOR-RADAR](../ARMOR-RADAR)** - ESP32-S3 用フィールドノードのファームウェア。レーダー 3 基と独自の Web パネル付き
* **[ARMOR-SOLAR](../ARMOR-SOLAR)** - 太陽光インバーターとバッテリーのプロトコル、およびゲートウェイノードのメッセージ
* **[ARMOR-ELECTRICAL](../ARMOR-ELECTRICAL)** - 電気ノード：電力量計、電力網の計測メッセージ、開閉のルール
* **[ARMOR-NETWORK](../ARMOR-NETWORK)** - ローカルネットワーク：機器、インターネット、そして変化
* **[ARMOR-SERVER](../ARMOR-SERVER)** - 中央コーディネーター：テレメトリ、アラーム、デバイス、太陽光の測定値、カメラ
* **[ARMOR-STUDIO](../ARMOR-STUDIO)** - Web コンソール：カメラ、レーダー、アラーム、太陽光発電、2D/3D サイト設計
* **ARMOR-ANDROID-CONTROL** (このリポジトリ) - リアルタイム 2D/3D レーダー付きの Android オペレータークライアント
* **[ARMOR-SERVER-AI](../ARMOR-SERVER-AI)** - 判断を説明し、決して動作しない視覚推論ポリシー
* **[ARMOR-VOICE-AI](../ARMOR-VOICE-AI)** - 偽造できない確認を備えたオフライン音声インテント
* **[ARMOR-HARDWARE](../ARMOR-HARDWARE)** - 筐体、電子部品、ベンチ受け入れマトリクス
* **[ARMOR-DEVOPS](../ARMOR-DEVOPS)** - デプロイ、CM5 テストベンチ、バックアップ、TLS
* **[ARMOR-SIMULATOR](../ARMOR-SIMULATOR)** - 再現可能な故障を備えたオフラインのテレメトリシミュレーター
* **[ARMOR-UPDATER](../ARMOR-UPDATER)** - エコシステム自身のリポジトリを検出し、インストールし、更新する
* **[ARMOR-DOCS](../ARMOR-DOCS)** - アーキテクチャ、セキュリティ基準、機能マトリクス

## 📚 ドキュメントとコミュニティ

詳しくは：

* [機能マトリクス：実証済みのものとそうでないもの](../ARMOR-DOCS/docs/CAPABILITY_MATRIX.md)
* [プロジェクト一覧：バージョンとリポジトリ間の依存関係](../ARMOR-DOCS/docs/PROJECT_CATALOG.md)
* [このリポジトリの変更履歴](CHANGELOG.md)
* [ライセンス（GPL-3.0-or-later）](LICENSE)
* 質問・提案・報告：electrohobby3d@gmail.com

## 👤 作者

**JuanenRac (Electro Hobby 3D)** · electrohobby3d@gmail.com

## 📜 ライセンス

GPL-3.0-or-later - [LICENSE](LICENSE) を参照。
