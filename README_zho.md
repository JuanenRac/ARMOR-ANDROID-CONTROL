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
  🇨🇳 <b>简体中文</b> |
  <a href="README_jpn.md">🇯🇵 日本語</a> |
  <a href="README_por.md">🇵🇹 Português</a>
</p>

### ARMOR-SERVER 的移动操作端：报警、摄像头、雷达、能源、网络和节点设置，支持八种语言

<p align="center">
  <img src="https://img.shields.io/badge/License-GPL%203.0--or--later-blue.svg" alt="GPL 3.0 or later">
  <img src="https://img.shields.io/badge/Language-Kotlin-7f52ff.svg" alt="Language">
  <img src="https://img.shields.io/badge/UI-Jetpack%20Compose-4285f4.svg" alt="UI">
  <img src="https://img.shields.io/badge/Languages-8-00E5FF.svg" alt="Languages">
  <img src="https://img.shields.io/badge/Tests-376%20passing-2ea44f.svg" alt="Tests">
  <img src="https://img.shields.io/badge/Version-0.7.8-0f6cbd.svg" alt="Version">
  <img src="https://img.shields.io/badge/Maturity-established-2EA44F.svg" alt="Maturity">
</p>

---

**诚实性检查 - 今天真正能运行的部分：** 地址安全、报警决策、报警监视器、翻译和只读角色的规则由 376 个单元测试覆盖，应用可以构建（`gradlew testDebugUnitTest assembleDebug`）。它在真实手机上对照 CM5 测试台运行：登录、摄像头监视器、布防和报警、雷达、通过蓝牙设置节点以及从 GitHub 更新都在使用中。**尚未**证明的是：使用真实设备的太阳能、电气和报警主机界面（目前只有雷达节点）、通过 Firebase 的推送（应用中已包含，需要自己的 Firebase 项目；尚未在手机上试过），以及在各手机厂商电池规则下的后台报警监视。

---

## 📋 速览

| | |
|---|---|
| 版本 | `0.7.8` |
| 成熟度 | 成熟 |
| 角色 | ARMOR-SERVER 的 Android 操作客户端（家族：Clients，上级：ARMOR-SERVER） |
| 技术栈 | Kotlin / Jetpack Compose, Android 8.0+ |
| 测试 | 376 (`gradlew testDebugUnitTest`) |
| 语言 | 8（每种语言 1345 条，每条都在所有表中检查） |
| 运行位置 | Android 8.0+ 手机（minSdk 26） |
| 真实硬件 | 在真实手机上对照 CM5 测试台使用；太阳能、电气和报警主机界面在等待真实节点 |
| 许可证 | GPL-3.0-or-later |

**跳转到：** [概述](#-概述) · [架构](#-架构) · [安全](#-安全与防护) · [接口](#-接口) · [配置](#-配置) · [快速开始](#-快速开始) · [测试](#-测试) · [故障排除](#-故障排除) · [结构](#-仓库结构) · [相关项目](#-相关项目)

## 🚦 项目状态

每一行是本仓库的一个部分，灯号表示它今天做到了哪一步。内容依据仓库能够展示的证据（测试、构建、运行），而不是计划。

🟢 已完成并验证 · 🟡 可用，有已知限制 · 🟠 部分完成或尚未验证 · 🔴 未完成或不可用

| 领域 | 状态 | 在这里的含义 |
|---|---|---|
| 软件 | 🟢 | 应用可以构建（`gradlew testDebugUnitTest assembleDebug`），并在真实手机上对 CM5 测试台运行。 |
| 测试 | 🟢 | 376 项单元测试通过（`gradlew testDebugUnitTest`）：地址安全、报警判定、报警监视、翻译和只读角色。 |
| 界面 | 🟡 | 登录、摄像头监视、布防与报警和雷达已在真实手机上使用；通过蓝牙设置节点尚未在真实节点上确认，光伏、电气和报警面板界面也尚未接触真实设备。 |
| 硬件 | 🟡 | 已在真实手机上对 CM5 测试台使用；各手机厂商电池规则下的后台报警监视尚未证明。 |
| 安全 | 🟡 | 地址安全和只读角色规则有单元测试覆盖；没有经过独立审查。 |
| 更新 | 🟡 | 来自 GitHub 的更新已在手机上使用。 |
| 集成 | 🟠 | 通过 Firebase 的推送已在应用中，但需要自己的 Firebase 项目，尚未在手机上试过。 |
| 文档 | 🟢 | 八种语言和更新日志均为最新。 |

## 🎯 概述

* **使用服务器自己的登录登录：**IP、端口、用户名和密码；密码创建一个 HttpOnly 会话；只有勾选 **记住我** 时才会保存在手机上（默认关闭：用户名和密码用 Keystore 专属密钥加密，仅用于该服务器，取消勾选后立即清除）。**viewer** 角色只能查看：所有会修改内容的按钮都被关闭。
* **使用手机锁定：** 在布防、撤防、关闭警报器、解锁以及其他关键指令之前，应用会要求验证手机自身的指纹、面部或 PIN（仅当手机有指纹或面部识别时默认开启）；如果你开启，打开应用时也会要求验证。管理员可以在 Studio 中强制要求，但不能降低你的选择；没有屏幕锁定的手机永远不会被拒之门外，锁定界面也始终提供改用密码重新登录。可选（默认关闭，并有醒目的提示）：可将封存已保存会话的密钥与手机解锁绑定；开启后，手机重启后后台报警监控会等到您解锁手机才恢复。
* **摄像头监视器：**1 到 16 个画面、放大视图、保持画面比例的实时 MJPEG、有限定范围的云台面板、快照和录像，录像带有查看器和删除确认。
* **布防和撤防：**先做**布防前检查**（哪些是打开、沉默或故障的），撤防需要 PIN；要确认的**报警**，等待中的显示角标；**设备**（烟雾、燃气、漏水、门、窗、移动、气候、插座、灯、警笛、门锁、卷帘、大门和车库门电机、热水器）及其状态和开 / 关 / 切换。
* **能找到你的报警：**每种严重级别有声音、可选的**语音报警**、节点达到高级、摄像头沉默、设备报警和（布防时）节点离线的通知，**响起的报警主机会唤醒手机**；后台监视在重启后仍然存在，并在服务器宕机时退避。
* **实时雷达，2D 和 3D：**雷达标签页按 Studio 中的设计绘制场地，以及雷达看到的人在其上移动，每秒半刷新一次；可拖动、捏合和旋转 3D 视图。
* **太阳能与电气能源：**合计（阳光、用电、带电量的电池、电网是否存在）、每个逆变器和电池（可按需查看电芯），以及电气节点的通道和报警。
* **报警主机：**每个 ARMOR-NODE-ALARM 节点的状态、其防区和事件，以及服务器允许时的布防 / 撤防命令。
* **网络：**住宅网络的设备、互联网状态以及带运营商和地点的**公网地址**，均由网络节点报告。
* **通过蓝牙设置现场节点**（从登录界面或*更多 > 设置节点*）：找到广播 `ARMOR-xxxxxx` 的节点，为新节点创建管理员或登录（使用节点的 PIN），搜索 Wi-Fi 网络并设置节点名称、路由器 Wi-Fi 或固定地址以及代理；**节点查找器**列出网络上已有的节点。
* **助手：**文字和语音命令（封闭列表，八种语言，确认与你绑定），可询问住宅状态，或布防、撤防、静音以及开关灯。
* **证据库、**周界和节点状态、每次警报、节点、摄像头、设备、报警和模式变化的**历史**、**天气地图**，以及系统的**服务**和更新。
* **八种语言**（英语、西班牙语、德语、法语、意大利语、日语、中文和葡萄牙语），在*更多 > 设置*中选择或跟随手机；会检查每一句话都在八张表中。
* **谨慎对待密码：**明文 HTTP 只允许用于私有局域网或回环的 IPv4 *字面量*；仅看起来像私有地址的主机名（`10.attacker.example`）或带前导零的地址会被拒绝。界面由图标构成：近黑色表面、青色强调、琥珀色表示注意。
* **在手机上管理安全：**第二重验证及其恢复码、已打开的会话、检查服务器如何看待手机、从外部执行关键操作时的重新身份验证，以及面向管理员的 VPN（带配置的设备）和动态 DNS 名称。
* **状态屏幕展示整个系统，以及从外部进入的方式：** 状态屏幕汇总报警主机、服务、正在录像的摄像头以及安全与远程访问，每张卡片都会打开对应屏幕；当输入的地址在家外且没有 https 时，登录会说明为何无法继续，并由向导讲解两种安全方式（VPN，或带 https 的公网地址）。
* **节点健康与布防模式：** “更多”中有节点健康画面（每个节点的运行时间、重启原因、内存、连接与信号、代理连接、重启、最后错误以及服务器发出的警告），报警主机画面有模式选择（离家、在家、夜间和自定义），主机固件支持时按所选模式布防。支持八种语言。
* **历史中的连接：** 历史页面新增了第二个只读列表，列出连接服务器的设备和应用（Studio、本应用、节点面板、语音服务），以及各自最后出现的时间、已打开的会话、登录与被拒次数；应用现在会向服务器表明自己的身份，因此会以自己的名义显示。地址只显示给管理员。
* **电池维护（管理员）：** 对 Pylontech 电池的简单操作——时钟、计数器和日志、重启、容量、MOSFET 测试、保险丝故障、输出——与 Studio 相同的两步（服务器让节点读取电池，界面准确显示将要执行的内容，再输入序列号点击“立即执行”），并显示结果和变化；限值、固件和专家控制台仍在 Studio 中操作。
* **节点控制（管理员）：** 更多 > 节点控制提供与 Studio 相同的操作——重启、暂停、恢复、待机（停止工作，保持连接）、识别、诊断以及开启或关闭维护——可用于一个节点或全部节点；需要再次输入节点名称（对所有节点则输入单词 RESTART、PAUSE 或 STANDBY）并重新验证身份，显示每个节点进行到哪一步，并警告报警主机或雷达处于暂停或待机时家中将失去保护。没有恢复出厂设置、擦除或深度睡眠。
* **已打开应用中的报警窗口：** 有报警等待人处理时，除报警页面外，每个页面上方都会出现脉动的横幅（严重报警为红色且更快，其余为琥珀色，手机关闭动画时保持静止），说明最严重的报警和等待的数量，并允许在其下继续使用应用；可打开报警、直接在横幅上确认，或关闭到下一个报警到来。
* **在家外使用：** 两个地址（家中 Wi-Fi 使用内网地址，移动数据或 VPN 使用外出地址），按手机所在位置选择；一个没有响应时尝试另一个，并提示哪里出了问题；安装证书的密钥在首次连接时记住（或用 Studio 二维码的文本配对），不同的密钥会被拒绝，对曾通过 https 访问过的服务器拒绝普通 http；可以通过服务器打开节点的面板；警报可以作为仅含数据的推送到达，无论经由推送还是后台监控，只播报一次；打开的应用与后台监控共用一次服务器读取；摄像头画面离开屏幕即停止，并以逐渐增长的等待时间恢复。
* **可自选声音与语音的通知:** 在设置中可以静音所有通知、不带声音和振动地显示（也不朗读），并按严重程度（高、低）或类型（雷达警报、无响应的摄像头、离线或沉默的节点，以及设备、太阳能、电气、网络和报警面板的报警）关闭；除非所有通知都被静音，否则严重报警一定会通知。每种严重程度和每种通知类型都可选声音，来自手机自带声音或自己的文件（小于 3 MB），各带播放按钮；朗读报警的语音（女声、男声或任意；儿童、成人或老人；平实、动作英雄或黑暗领主；语速；以及是否也朗读低严重程度的通知）带有试听按钮。
* **直达正确屏幕的横幅与更安静的监视:** 报警横幅会打开处理它的屏幕（摄像头、设备、逆变器或电池、电气屏幕、网络或报警面板）；手机在注册推送时告诉服务器它的语言，通知就用该语言书写，新的 Firebase 令牌会立刻从后台送达服务器。同一路流中有两个镜头的摄像头在摄像头列表中是两个窗口。推送提醒正常工作时，后台监视每 120 秒而不是每 10 秒询问一次服务器（*设置 > 推送提醒下的监视*：10、60、120、300 或 600 秒）。
* **服务器的布防模式（离家、在家、夜间）：** 当服务器启用了布防模式（Studio 的 配置 > 安全）时，状态页的布防按钮会打开选择器，可按模式布防、切换或撤防；未分配任何内容的模式需要确认，切换到保护范围更小的模式需要像撤防一样输入 PIN，状态页会显示当前生效的模式。从撤防状态布防时，与普通布防一样，会先执行布防前检查。模式关闭时，应用的布防和撤防与以往相同。

## 🧩 在 A.R.M.O.R. 中的角色

ARMOR-ANDROID-CONTROL 是手机上通往 [ARMOR-STUDIO](https://github.com/JuanenRac/ARMOR-STUDIO) 所用的同一个 [ARMOR-SERVER](https://github.com/JuanenRac/ARMOR-SERVER) 的窗口：它只通过服务器读取和下达命令。报警会跟着你（响起的报警主机会唤醒手机），你可以用语音询问助手，也可以在现场站在新节点旁通过蓝牙设置它。

| 名称 | 方式 | 用途 |
|---|---|---|
| ARMOR-SERVER | HTTP `/api/v1` (55 paths), MJPEG | 应用读取和下达的一切 |
| 现场节点 | Bluetooth (`ARMOR-xxxxxx`) | 在新节点旁边完成设置 |
| GitHub | HTTPS | 检查并下载应用自身的签名更新 |
| Open-Meteo | HTTPS | 天气地图使用的公开预报、空气质量和地点搜索 |

## 🏛️ 架构

<p align="center">
  <img src="images/architecture.svg" alt="ARMOR-ANDROID-CONTROL 的架构：谁调用它、它包含什么、它依赖什么" width="100%">
</p>

## 🔒 安全与防护

| 威胁 | 防御 |
|---|---|
| 保存在手机上的密码 | 仅在勾选*记住我*时（默认关闭）：用户名和密码用专属 Keystore 密钥加密，仅用于该服务器，取消勾选立即清除；不备份（`allowBackup=false`） |
| 面向互联网的明文 HTTP | 仅允许私有局域网或回环的 IPv4 *字面量*；`10.attacker.example` 之类的主机或带前导零的地址（`010.0.0.1` 会被当作八进制）会被拒绝；曾通过 https 访问的主机不允许改用明文 http |
| https 上的中间人 | 信任的是系统的证书颁发机构以及你在手机上安装的那些；首次记住安装环境证书的密钥（或用 Studio 二维码文本配对），不同的密钥会被拒绝 |
| 手机落入他人之手时的关键操作 | 在布防、撤防、静音警笛、解锁等关键操作前要求手机指纹、面部或 PIN（仅在手机有指纹或面部时默认开启）；管理员可在 Studio 中强制要求，但不能降低你的选择；没有屏幕锁的手机永远不会被拒之门外 |
| 查看者修改了内容 | *查看者*角色能看到一切，但所有会修改内容的按钮都被禁用（有角色测试） |
| 错误的撤防 PIN | PIN 仅在撤防时发送给服务器；`wrong_pin`、`pin_locked`、`pin_not_set` 和 `elevation_required` 会被翻译成通俗文字 |
| 伪造的应用更新 | 在调用 Android 安装程序之前，会比较已安装应用与候选版本的签名证书（历史）SHA-256；若没有任何共同项，会明确提示 |
| 手机没看到的报警 | 前台服务（`AlarmWatcherService`）每 10 秒轮询一次，等待手机解锁 15 秒，退避最多 60 秒，失败 6 次后提示*无法连接*，重启后依然运行；超过 15 分钟的报警不会播报 |
| 手机上的摄像头密码 | 摄像头通过服务器的视频流观看：应用没有摄像头密码，也没有 RTSP 地址 |

### 权限

`INTERNET`、`ACCESS_NETWORK_STATE`：服务器。`USE_BIOMETRIC`：手机锁。`POST_NOTIFICATIONS`：报警。`FOREGROUND_SERVICE`（+ `DATA_SYNC`、`SPECIAL_USE`）和 `RECEIVE_BOOT_COMPLETED`：报警监视。`REQUEST_INSTALL_PACKAGES`：应用更新。`BLUETOOTH_SCAN`、`BLUETOOTH_CONNECT`：设置节点；`BLUETOOTH`、`BLUETOOTH_ADMIN` 和 `ACCESS_FINE_LOCATION` 仅限 Android 11 及以下（`maxSdkVersion=30`），用于蓝牙扫描。

没有独立的安全审查。请阅读 [docs/CLIENT_BOUNDARY.md](docs/CLIENT_BOUNDARY.md) 和 [SECURITY.md](SECURITY.md)。

## 🔌 接口

| 部分 | 作用 |
|---|---|
| ARMOR-SERVER | `network/ArmorApiClient.kt` 中有 55 条不同的 `/api/v1` 路径（接口在 `network/ArmorApi.kt`）；雷达每 1.5 秒读取一次；摄像头经服务器以 MJPEG 方式观看 |
| 两个地址 | 家庭 Wi-Fi 下用私有地址，外出时（移动数据或 VPN）用外出地址，按手机所在位置选择；一个无响应就尝试另一个，并说明失败原因 |
| Bluetooth | 查找广播 `ARMOR-xxxxxx` 的节点，创建新节点的管理员或用节点 PIN 登录，搜索 Wi-Fi 并设置名称、Wi-Fi 或固定地址和代理；协议在 `model/NodeBle.kt`；节点查找器会列出已在网络上的节点 |
| 语音 | `POST /api/v1/voice/command`：八种语言的封闭命令列表，并带有与你绑定的确认（`model/Voice.kt`、`VoiceScreens.kt`） |
| Push | `PushService`：报警以仅含数据的消息送达，无论经推送还是监视，只播报一次；Firebase 步骤见 [docs/PUSH_NOTIFICATIONS.md](docs/PUSH_NOTIFICATIONS.md)（八种语言） |

## ⚙️ 配置

| 名称 | 值 | 作用 |
|---|---|---|
| `compileSdk` / `targetSdk` / `minSdk` | 37 / 37 / 26 | 手机需 Android 8.0 或更高 |
| `versionName` | = `armor.project.json` | 清单与版本不一致时构建失败 |
| JDK / Android SDK | 17+ / 37 | 用于构建应用和运行测试 |

应用内（不在文件中）：语言（八种语言或跟随手机）、两个地址、*记住我*、手机锁、语音报警和通知，位于*更多 > 设置*。代码中的限制：监视每 10 秒轮询一次，最多退避到 60 秒，并忽略超过 15 分钟的报警。

## 🚀 快速开始

**要求:** JDK 17+、Android SDK（compile SDK 37）、Android 8.0+ 手机（minSdk 26）。

```powershell
.\gradlew.bat testDebugUnitTest assembleDebug   # 376 tests, then the debug APK
adb install -r app\build\outputs\apk\debug\app-debug.apk
```

版本发布在 GitHub 上，应用自己会从那里检查更新；APK 使用维护者自己的发布密钥签名（密钥保存在仓库之外），应用在把文件交给 Android 安装程序之前，会先把下载文件的签名证书与自己的比对。参见[客户端边界](docs/CLIENT_BOUNDARY.md)。

## 🧪 测试

```powershell
.\gradlew.bat testDebugUnitTest
python -I tools/armor_ci_validate.py
```

| 部分 | 测试数 | 覆盖内容 |
|---|---|---|
| 报警 | 67 | `AlarmPolicy`、`AlarmBanner`、`AlarmPanels`、`AlarmCodes`、`AlarmRoute`、`NoticeFilter`、`PushEvents`、`DevicesAndAlarms` |
| 服务器连接、解析、测试数据 | 59 | `ApiClient`, `ApiError`, `ServerFixtures`, `ParsersTolerant`, `SavePayload`, `JsonText`, `Preflight`, `SharedFeed`, `Connections` |
| 视图模型与角色 | 41 | `ArmorViewModel`, `Viewer`, `ArmProfiles`, `NodeCensus`, `NodeHealth` |
| 锁、会话保险库、密钥 | 38 | `DeviceLock`, `SessionVault`, `CredentialVault`, `KeyBinding`, `Security` |
| 地址安全、证书 | 32 | `ServerEndpoint`, `ServerAddresses`, `Literals`, `NodeAddress`, `CertificatePins`, `RemoteAccess` |
| 能源与网络 | 34 | `Solar`, `Battery`, `Electrical`, `Network` |
| 蓝牙节点与节点控制 | 26 | `NodeBle`, `NodeControl` |
| 来自 GitHub 的更新 | 17 | `GitHubReleaseUpdaterHttpFailure`, `SignerCheck`, `SemanticVersion`, `ReleaseMetadataParser` |
| 雷达与摄像头 | 14 | `RadarGeometry`, `CameraTraits`, `CameraLenses` |
| 翻译、通俗措辞、语音 | 14 | `Tr`, `Friendly`, `Voice` |

47 个文件共 376 个测试，用 `testDebugUnitTest` 运行；由模拟服务器（`FakeServer.kt`）和从真实服务器获取的数据（`tools/capture_fixtures.mjs`）驱动。还会检查每条短语都在八张语言表中。

## 🏗️ 构建与发布

版本发布在 GitHub，应用自己从那里检查更新；应用查找资源 `ARMOR-ANDROID-CONTROL-release.apk`。APK 使用维护者自己的发布密钥签名（密钥保存在仓库之外），应用在把文件交给 Android 安装程序之前，会比较下载文件的签名证书与自身证书。每个 APK 之前都要提高 `versionCode`，否则 Android 不会把它当作更新。

## 🩺 故障排除

| 现象 | 原因 | 处理 |
|---|---|---|
| `wrong_pin`, `pin_locked`, `pin_not_set`, `elevation_required` | 服务器拒绝撤防 PIN 或要求重新验证身份 | 重新输入 PIN、等待锁定结束或确认身份 |
| 登录提示无法继续 | 地址在家庭之外且没有 https（明文 http 被拒绝） | 按指南操作：使用 VPN，或带 https 的公网地址 |
| 原本可用的连接被拒绝 | 安装环境证书的密钥发生了变化 | 若是有意更改，请用 Studio 二维码文本重新配对 |
| 后台报警延迟或收不到 | 手机厂商的电池策略会停止监视；推送需要自己的 Firebase 项目 | 将应用排除在省电限制之外；按 [docs/PUSH_NOTIFICATIONS.md](docs/PUSH_NOTIFICATIONS.md) 操作 |
| 更新被拒绝 | 下载文件的签名证书与已安装应用没有任何共同项 | 不要安装；请从本仓库的发布版本获取 APK |

## 📂 仓库结构

<p align="center">
  <img src="images/repository-structure.svg" alt="ARMOR-ANDROID-CONTROL 的仓库结构" width="100%">
</p>

<details>
<summary>目录树的文本版</summary>

```text
ARMOR-ANDROID-CONTROL/
├── app/src/main/java/es/electrohobby3d/armor/   应用：界面、规则、网络与更新
├── app/src/main/assets/                         八种语言表、报警代码、天气地图
├── app/src/test/                                376 个测试和一个模拟服务器
├── docs/                                        本仓库的指南
├── tools/                                       共享的 CI 检查和项目工具
├── .github/                                     CI 工作流以及 Issue 和 Pull Request 模板
├── images/                                      横幅、图标和本 README 的两幅图
├── build.sh build.bat                           通过共享的项目工作流构建
├── test.sh test.bat                             通过共享工作流运行测试
├── run.sh run.bat                               通过共享的项目工作流启动
├── armor.project.json                           清单：版本、角色、成熟度、构建命令
├── CHANGELOG.md                                 每个版本的变化
├── SECURITY.md SUPPORT.md LICENSE               安全策略、支持和许可证
├── CONTRIBUTING.md CODE_OF_CONDUCT.md           如何贡献与行为准则
├── build.gradle.kts settings.gradle.kts         项目的 Gradle 构建
├── gradle.properties                            Gradle 设置
├── gradlew gradlew.bat gradle/                  Gradle 包装器
├── build-test.sh build-test.bat                 通过共享工作流运行测试
└── .gitattributes .gitignore                    Git 设置
```

</details>

## 🗺️ 路线图与已知限制

**目前已知的限制**

* 太阳能、电气和报警面板的界面尚未接触过真实设备（目前只有一个雷达节点）。
* 节点的蓝牙设置尚未在真实节点上确认，Firebase 推送也未在手机上试过。
* 在各手机厂商的电池策略下，后台报警监视尚未得到验证。
* 没有独立的安全审查。

**下一步**

* 用真实节点试验太阳能、电气和报警面板界面以及蓝牙设置。
* 用某个安装环境自己的项目试验 Firebase 推送，并在多家手机厂商的设备上试验报警监视。

## 🌍 语言

README 和文本提供英语、西班牙语、法语、意大利语、德语、汉语、日语和欧洲葡萄牙语版本。

## 🔗 相关项目

**A.R.M.O.R.**（Autonomous Radar & Multimodal Observation Range）是一个由相互独立的仓库组成的周界安全系统。每个仓库都有自己的版本、自己的测试和自己的 README；这是整个系列：

* **[ARMOR-COMMON](https://github.com/JuanenRac/ARMOR-COMMON)** - 消息契约、验证器、一致性向量和生成的类型
* **[ARMOR-NODE-RADAR](https://github.com/JuanenRac/ARMOR-NODE-RADAR)** - 面向 ESP32-S3 的现场节点固件，配三个雷达和自带的网页面板
* **[ARMOR-NODE-SOLAR](https://github.com/JuanenRac/ARMOR-NODE-SOLAR)** - 太阳能逆变器和电池的协议，以及网关节点的消息
* **[ARMOR-NODE-ELECTRICAL](https://github.com/JuanenRac/ARMOR-NODE-ELECTRICAL)** - 电气节点：电表、电网读数的消息和切换规则
* **[ARMOR-NODE-ALARM](https://github.com/JuanenRac/ARMOR-NODE-ALARM)** - 报警节点与面板：防区、布防、延时、警笛和 PIN，可配合服务器也可独立使用
* **[ARMOR-NODE-HMI](https://github.com/JuanenRac/ARMOR-NODE-HMI)** - 触摸面板：壁挂屏幕上的系统状态、布防与确认，以及语音助手的所在
* **[ARMOR-NETWORK](https://github.com/JuanenRac/ARMOR-NETWORK)** - 本地网络：其设备、互联网以及变化
* **[ARMOR-SERVER](https://github.com/JuanenRac/ARMOR-SERVER)** - 中央协调器：遥测、报警、设备、太阳能读数和摄像头
* **[ARMOR-STUDIO](https://github.com/JuanenRac/ARMOR-STUDIO)** - 网页控制台：摄像头、雷达、报警、太阳能以及 2D/3D 场地设计器
* **ARMOR-ANDROID-CONTROL**（本仓库） - 带实时 2D/3D 雷达的 Android 操作员客户端
* **[ARMOR-IOS-CONTROL](https://github.com/JuanenRac/ARMOR-IOS-CONTROL)** - iPhone 操作员客户端
* **[ARMOR-SERVER-AI](https://github.com/JuanenRac/ARMOR-SERVER-AI)** - 视觉推理策略，会解释其决策且从不驱动设备
* **[ARMOR-VISION](https://github.com/JuanenRac/ARMOR-VISION)** - 面向 Jetson Orin NX 的摄像头区域、遮罩和检测规则
* **[ARMOR-VOICE-AI](https://github.com/JuanenRac/ARMOR-VOICE-AI)** - 离线语音意图，附带无法伪造的确认
* **[ARMOR-HARDWARE](https://github.com/JuanenRac/ARMOR-HARDWARE)** - 外壳、电子器件和台架验收矩阵
* **[ARMOR-DEVOPS](https://github.com/JuanenRac/ARMOR-DEVOPS)** - 部署、CM5 测试台、备份和 TLS
* **[ARMOR-SIMULATOR](https://github.com/JuanenRac/ARMOR-SIMULATOR)** - 带可重复故障的离线遥测模拟器
* **[ARMOR-UPDATER](https://github.com/JuanenRac/ARMOR-UPDATER)** - 检测、安装和更新生态系统自身的仓库
* **[ARMOR-DOCS](https://github.com/JuanenRac/ARMOR-DOCS)** - 架构、安全基线和能力矩阵

## 📚 文档

* [客户端边界](docs/CLIENT_BOUNDARY.md)：客户端做什么、绝不做什么
* [推送通知](docs/PUSH_NOTIFICATIONS.md)：Firebase 步骤（八种语言）
* [台架测试](docs/BENCH_TESTS.md)：用真机进行的检查
* [能力矩阵：哪些已被证明，哪些没有](https://github.com/JuanenRac/ARMOR-DOCS/blob/main/docs/CAPABILITY_MATRIX.md)
* [项目目录：版本以及各仓库之间如何相互依赖](https://github.com/JuanenRac/ARMOR-DOCS/blob/main/docs/PROJECT_CATALOG.md)
* [本仓库的更新日志](CHANGELOG.md)

## 🤝 参与贡献与社区

* 提交 Pull Request 之前请运行 `.\gradlew.bat testDebugUnitTest` and `python -I tools/armor_ci_validate.py`：必须通过。参见 [CONTRIBUTING.md](CONTRIBUTING.md) 和[行为准则](CODE_OF_CONDUCT.md)。
* README 提供八种语言，由 ARMOR-DOCS 中的源文件生成：文本的修改在那里进行。
* 组件之间消息的变更从 ARMOR-COMMON 开始。
* 不要把仅仅构建过的东西描述为已在板子上运行。
* 请私下报告漏洞：[SECURITY.md](SECURITY.md)。问题、想法和报告：electrohobby3d@gmail.com（[SUPPORT.md](SUPPORT.md)）。

## 👤 作者

**JuanenRac (Electro Hobby 3D)** · electrohobby3d@gmail.com

## 📜 许可证

GPL-3.0-or-later - 见 [LICENSE](LICENSE)。
