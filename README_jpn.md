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
  <img src="https://img.shields.io/badge/Tests-123%20passing-2ea44f.svg" alt="Tests">
  <img src="https://img.shields.io/badge/Version-0.5.8-0f6cbd.svg" alt="Version">
  <img src="https://img.shields.io/badge/Maturity-established-2EA44F.svg" alt="Maturity">
</p>

---

**正直さのチェック - 今日動いているもの:** アドレスの安全性、警報の判断、警報ウォッチャー、翻訳、読み取り専用ロールのルールは 123 件のユニットテストでカバーされ、アプリはビルドできます（`gradlew testDebugUnitTest assembleRelease`）。実際のスマートフォンで CM5 テストベンチに対して動作し、サインイン、カメラモニター、警戒と警報、レーダー、Bluetooth でのノード設定、GitHub からのアップデートが使われています。**まだ**証明されていないもの：実機でのソーラー・電気・警報盤の画面（存在するのはレーダーノードのみ）、Firebase 経由のプッシュ（独自の Firebase プロジェクトが必要で、アプリにはまだありません）、各メーカーのバッテリー規則下でのバックグラウンド警報監視。

---

## 📋 ひと目で

| | |
|---|---|
| バージョン | `0.5.8` |
| 成熟度 | 確立 |
| テスト | 123 (`gradlew testDebugUnitTest`) |
| 技術 | Kotlin / Jetpack Compose |
| 実機での確認 | 実際のスマートフォンで CM5 テストベンチに対して使用。ソーラー・電気・警報盤の画面は実ノードを待っています |

## 🎯 概要

* **サーバー自身のログインでサインイン：**IP、ポート、ユーザー、パスワード。パスワードは HttpOnly セッションを作り、スマートフォンには保存されません。**viewer** ロールは見るだけで、何かを変更するボタンはすべてオフです。
* **カメラモニター：**1〜16 タイル、拡大表示、縦横比を保つライブ MJPEG、範囲を限った PTZ パッド、スナップショットと録画（録画にはビューアーと削除確認付き）。
* **警戒と解除：**まず**警戒前のチェック**（開いている、無応答、停止しているもの）を行い、解除には PIN。確認する**警報**（待機中のものにはバッジ）、**デバイス**（煙、ガス、浸水、ドア、窓、動き、気候、プラグ、照明、サイレン、鍵、シャッター、門扉・ガレージドアのモーター、給湯器）の状態とオン / オフ / 切り替え。
* **あなたに届く警報：**重大度ごとの音、任意の**音声警報**、ノードが HIGH になった、カメラが無応答、デバイス警報、（警戒中に）ノードがオフラインになった通知、そして**鳴っている警報盤はスマートフォンを起こします**。バックグラウンド監視は再起動後も残り、サーバー停止中は間隔を空けます。
* **ライブレーダー（2D と 3D）：**レーダータブは Studio で設計したサイトと、レーダーが捉えて動く人を描き、1.5 秒ごとに更新します。3D 表示はドラッグ、ピンチ、回転できます。
* **ソーラーと電気のエネルギー：**合計（太陽、消費、充電状態付きのバッテリー、系統の有無）、各インバーターとバッテリー（セルは要求に応じて）、電気ノードのチャンネルと警報。
* **警報盤：**各 ARMOR-ALARM ノードの状態、ゾーンとイベント、サーバーが許可した場合の警戒・解除コマンド。
* **ネットワーク：**家のネットワークのデバイス、インターネットの状態、プロバイダーと場所付きの**公開アドレス**（ネットワークノードの報告どおり）。
* **Bluetooth でフィールドノードを設定**（サインイン画面または*その他 > ノードを設定*から）：`ARMOR-xxxxxx` を発信するノードを見つけ、新しいノードの管理者を作るか（ノードの PIN で）サインインし、Wi-Fi ネットワークを検索して、ノード名、ルーターの Wi-Fi または固定アドレス、ブローカーを設定します。**ノードファインダー**はすでにネットワーク上にあるものを一覧します。
* **アシスタント：**文字と音声のコマンド（クローズドなリスト、8 言語、あなたに結び付いた確認）で、家の状態を尋ねたり、警戒、解除、消音、照明のオン・オフができます。
* **証拠ライブラリ、**境界とノードの状態、すべての警報・ノード・カメラ・デバイス・警報・モード変更の**履歴**、**天気マップ**、設備の**サービス**とアップデート。
* **8 言語**（英語、スペイン語、ドイツ語、フランス語、イタリア語、日本語、中国語、ポルトガル語）を*その他 > 設定*で選ぶか端末に従います。すべての文が 8 つの表にあることを検査します。
* **パスワードの扱い：**平文 HTTP は、プライベート LAN またはループバックの IPv4 *リテラル*にのみ許可されます。プライベートアドレスのように始まるだけのホスト名（`10.attacker.example`）や先頭にゼロのあるアドレスは拒否されます。見た目はアイコン中心で、ほぼ黒の面、シアンのアクセント、注意にはアンバーを使います。

## 🧩 A.R.M.O.R. での役割

ARMOR-ANDROID-CONTROL は [ARMOR-STUDIO](https://github.com/JuanenRac/ARMOR-STUDIO) と同じ [ARMOR-SERVER](https://github.com/JuanenRac/ARMOR-SERVER) へのスマートフォンの窓口で、読み取りも指示もサーバーを通してのみ行います。警報があなたを追いかけ（鳴っている警報盤はスマートフォンを起こします）、アシスタントに声で尋ねられ、現場で新しいノードの横から Bluetooth で設定するためのツールでもあります。

## 📂 リポジトリの構成

```text
ARMOR-ANDROID-CONTROL/
├── app/src/main/java/es/electrohobby3d/armor/
│   ├── ArmorActivity.kt (the shell), EntryScreens.kt, HomeScreens.kt, CameraScreens.kt, RadarScreens.kt, DevicePanels.kt, MoreScreens.kt
│   ├── SolarScreens.kt, ElectricalScreens.kt, AlarmPanelScreens.kt, NetworkScreens.kt, ServicesScreens.kt, WeatherScreens.kt, VoiceScreens.kt, MediaViewer.kt
│   ├── NodeBleClient.kt, NodeSetupScreen.kt, NodeFinderScreens.kt, NodePanel.kt   configure a node over Bluetooth (the protocol is in model/NodeBle.kt)
│   ├── ArmorViewModel.kt, Friendly.kt, AlarmPolicy.kt, AlarmNotifier.kt, AlarmSpeaker.kt, AlarmWatcherService.kt, BootReceiver.kt
│   ├── Tr.kt (the eight languages), ArmorTheme.kt, ServerEndpoint.kt, SessionVault.kt, PinDialog.kt, MjpegFeed.kt
│   ├── network/   model/   update/
│   └── assets/i18n/   en, es (the source phrases), de, fr, it, ja, zh, pt
├── docs/CLIENT_BOUNDARY.md, PUSH_NOTIFICATIONS.md   (the Firebase steps, in eight languages)
└── app/src/test/   endpoint-safety, plain-words, node-protocol, solar-model, translation and role tests
```

## 🛠️ 開発環境

**必要なもの:** JDK 17+, the Android SDK (compile SDK 36), an Android 8.0+ phone (minSdk 26)

```powershell
.\gradlew.bat testDebugUnitTest assembleRelease   # 123 tests, then the release APK
adb install -r app\build\outputs\apk\release\app-release.apk
```

リリースビルドは管理者の鍵で署名されて GitHub で公開され、アプリ自身がそこから更新を確認します。[クライアントの境界](docs/CLIENT_BOUNDARY.md)を参照してください。

## 🔗 関連プロジェクト

**A.R.M.O.R.**（Autonomous Radar & Multimodal Observation Range）は、独立したリポジトリで構成される周辺警備システムです。それぞれに独自のバージョン、テスト、README があります。ファミリーは次のとおりです：

* **[ARMOR-COMMON](https://github.com/JuanenRac/ARMOR-COMMON)** - メッセージ契約、検証器、適合性ベクトル、生成された型
* **[ARMOR-RADAR](https://github.com/JuanenRac/ARMOR-RADAR)** - ESP32-S3 用フィールドノードのファームウェア。レーダー 3 基と独自の Web パネル付き
* **[ARMOR-SOLAR](https://github.com/JuanenRac/ARMOR-SOLAR)** - 太陽光インバーターとバッテリーのプロトコル、およびゲートウェイノードのメッセージ
* **[ARMOR-ELECTRICAL](https://github.com/JuanenRac/ARMOR-ELECTRICAL)** - 電気ノード：電力量計、電力網の計測メッセージ、開閉のルール
* **[ARMOR-ALARM](https://github.com/JuanenRac/ARMOR-ALARM)** - 警報ノードと警報盤：警戒区域、警戒セット、遅延、サイレン、PIN。サーバーがあってもなくても
* **[ARMOR-HMI](https://github.com/JuanenRac/ARMOR-HMI)** - タッチパネル：壁面ディスプレイでのシステム状態表示、警戒・確認操作、音声アシスタントの拠点
* **[ARMOR-NETWORK](https://github.com/JuanenRac/ARMOR-NETWORK)** - ローカルネットワーク：機器、インターネット、そして変化
* **[ARMOR-SERVER](https://github.com/JuanenRac/ARMOR-SERVER)** - 中央コーディネーター：テレメトリ、アラーム、デバイス、太陽光の測定値、カメラ
* **[ARMOR-STUDIO](https://github.com/JuanenRac/ARMOR-STUDIO)** - Web コンソール：カメラ、レーダー、アラーム、太陽光発電、2D/3D サイト設計
* **ARMOR-ANDROID-CONTROL** (このリポジトリ) - リアルタイム 2D/3D レーダー付きの Android オペレータークライアント
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
