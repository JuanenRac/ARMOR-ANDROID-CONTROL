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
  <img src="https://img.shields.io/badge/License-GPL%203.0-blue.svg" alt="GPL 3.0">
  <img src="https://img.shields.io/badge/Language-Kotlin-7f52ff.svg" alt="Language">
  <img src="https://img.shields.io/badge/UI-Jetpack%20Compose-4285f4.svg" alt="UI">
  <img src="https://img.shields.io/badge/Languages-8-00E5FF.svg" alt="Languages">
  <img src="https://img.shields.io/badge/Tests-207%20passing-2ea44f.svg" alt="Tests">
  <img src="https://img.shields.io/badge/Version-0.7.2-0f6cbd.svg" alt="Version">
  <img src="https://img.shields.io/badge/Maturity-established-2EA44F.svg" alt="Maturity">
</p>

---

**诚实性检查 - 今天真正能运行的部分:** 地址安全、报警决策、报警监视器、翻译和只读角色的规则由 207 个单元测试覆盖，应用可以构建（`gradlew testDebugUnitTest assembleDebug`）。它在真实手机上对照 CM5 测试台运行：登录、摄像头监视器、布防和报警、雷达、通过蓝牙设置节点以及从 GitHub 更新都在使用中。**尚未**证明的是：使用真实设备的太阳能、电气和报警主机界面（目前只有雷达节点）、通过 Firebase 的推送（应用中已包含，需要自己的 Firebase 项目；尚未在手机上试过），以及在各手机厂商电池规则下的后台报警监视。

---

## 📋 速览

| | |
|---|---|
| 版本 | `0.7.2` |
| 成熟度 | 成熟 |
| 测试 | 207 (`gradlew testDebugUnitTest`) |
| 技术栈 | Kotlin / Jetpack Compose |
| 真实硬件 | 在真实手机上对照 CM5 测试台使用；太阳能、电气和报警主机界面在等待真实节点 |

## 🎯 概述

* **使用服务器自己的登录登录：**IP、端口、用户名和密码；密码创建一个 HttpOnly 会话；只有勾选 **记住我** 时才会保存在手机上（默认关闭：用户名和密码用 Keystore 专属密钥加密，仅用于该服务器，取消勾选后立即清除）。**viewer** 角色只能查看：所有会修改内容的按钮都被关闭。
* **摄像头监视器：**1 到 16 个画面、放大视图、保持画面比例的实时 MJPEG、有限定范围的云台面板、快照和录像，录像带有查看器和删除确认。
* **布防和撤防：**先做**布防前检查**（哪些是打开、沉默或故障的），撤防需要 PIN；要确认的**报警**，等待中的显示角标；**设备**（烟雾、燃气、漏水、门、窗、移动、气候、插座、灯、警笛、门锁、卷帘、大门和车库门电机、热水器）及其状态和开 / 关 / 切换。
* **能找到你的报警：**每种严重级别有声音、可选的**语音报警**、节点达到高级、摄像头沉默、设备报警和（布防时）节点离线的通知，**响起的报警主机会唤醒手机**；后台监视在重启后仍然存在，并在服务器宕机时退避。
* **实时雷达，2D 和 3D：**雷达标签页按 Studio 中的设计绘制场地，以及雷达看到的人在其上移动，每秒半刷新一次；可拖动、捏合和旋转 3D 视图。
* **太阳能与电气能源：**合计（阳光、用电、带电量的电池、电网是否存在）、每个逆变器和电池（可按需查看电芯），以及电气节点的通道和报警。
* **报警主机：**每个 ARMOR-ALARM 节点的状态、其防区和事件，以及服务器允许时的布防 / 撤防命令。
* **网络：**住宅网络的设备、互联网状态以及带运营商和地点的**公网地址**，均由网络节点报告。
* **通过蓝牙设置现场节点**（从登录界面或*更多 > 设置节点*）：找到广播 `ARMOR-xxxxxx` 的节点，为新节点创建管理员或登录（使用节点的 PIN），搜索 Wi-Fi 网络并设置节点名称、路由器 Wi-Fi 或固定地址以及代理；**节点查找器**列出网络上已有的节点。
* **助手：**文字和语音命令（封闭列表，八种语言，确认与你绑定），可询问住宅状态，或布防、撤防、静音以及开关灯。
* **证据库、**周界和节点状态、每次警报、节点、摄像头、设备、报警和模式变化的**历史**、**天气地图**，以及系统的**服务**和更新。
* **八种语言**（英语、西班牙语、德语、法语、意大利语、日语、中文和葡萄牙语），在*更多 > 设置*中选择或跟随手机；会检查每一句话都在八张表中。
* **谨慎对待密码：**明文 HTTP 只允许用于私有局域网或回环的 IPv4 *字面量*；仅看起来像私有地址的主机名（`10.attacker.example`）或带前导零的地址会被拒绝。界面由图标构成：近黑色表面、青色强调、琥珀色表示注意。
* **在手机上管理安全：**第二重验证及其恢复码、已打开的会话、检查服务器如何看待手机、从外部执行关键操作时的重新身份验证，以及面向管理员的 VPN（带配置的设备）和动态 DNS 名称。**状态屏幕展示整个系统，以及从外部进入的方式：** 状态屏幕汇总报警主机、服务、正在录像的摄像头以及安全与远程访问，每张卡片都会打开对应屏幕；当输入的地址在家外且没有 https 时，登录会说明为何无法继续，并由向导讲解两种安全方式（VPN，或带 https 的公网地址）。
* **节点健康与布防模式：** “更多”中有节点健康画面（每个节点的运行时间、重启原因、内存、连接与信号、代理连接、重启、最后错误以及服务器发出的警告），报警主机画面有模式选择（离家、在家、夜间和自定义），主机固件支持时按所选模式布防。支持八种语言。
* **历史中的连接：** 历史页面新增了第二个只读列表，列出连接服务器的设备和应用（Studio、本应用、节点面板、语音服务），以及各自最后出现的时间、已打开的会话、登录与被拒次数；应用现在会向服务器表明自己的身份，因此会以自己的名义显示。地址只显示给管理员。

## 🧩 在 A.R.M.O.R. 中的角色

ARMOR-ANDROID-CONTROL 是手机上通往 [ARMOR-STUDIO](https://github.com/JuanenRac/ARMOR-STUDIO) 所用的同一个 [ARMOR-SERVER](https://github.com/JuanenRac/ARMOR-SERVER) 的窗口：它只通过服务器读取和下达命令。报警会跟着你（响起的报警主机会唤醒手机），你可以用语音询问助手，也可以在现场站在新节点旁通过蓝牙设置它。

## 📂 仓库结构

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

## 🛠️ 开发环境

**环境要求:** JDK 17+, the Android SDK (compile SDK 37), an Android 8.0+ phone (minSdk 26)

```powershell
.\gradlew.bat testDebugUnitTest assembleDebug   # 219 tests, then the debug APK
adb install -r app\build\outputs\apk\debug\app-debug.apk
```

版本发布在 GitHub 上，应用自己会从那里检查更新；APK 使用维护者自己的发布密钥签名（密钥保存在仓库之外），应用在把文件交给 Android 安装程序之前，会先把下载文件的签名证书与自己的比对。参见[客户端边界](docs/CLIENT_BOUNDARY.md)。

## 🔗 相关项目

**A.R.M.O.R.**（Autonomous Radar & Multimodal Observation Range）是由若干独立仓库组成的周界安防系统。每个仓库都有自己的版本、测试和 README；家族成员如下：

* **[ARMOR-COMMON](https://github.com/JuanenRac/ARMOR-COMMON)** - 消息契约、验证器、一致性向量和生成的类型
* **[ARMOR-RADAR](https://github.com/JuanenRac/ARMOR-RADAR)** - 适用于 ESP32-S3 的现场节点固件，带三个雷达和自带网页面板
* **[ARMOR-SOLAR](https://github.com/JuanenRac/ARMOR-SOLAR)** - 太阳能逆变器与电池的协议，以及网关节点的消息
* **[ARMOR-ELECTRICAL](https://github.com/JuanenRac/ARMOR-ELECTRICAL)** - 电气节点：电表、电网读数消息和开关规则
* **[ARMOR-ALARM](https://github.com/JuanenRac/ARMOR-ALARM)** - 报警节点与报警主机：防区、布防、延时、警笛和 PIN，有无服务器均可
* **[ARMOR-HMI](https://github.com/JuanenRac/ARMOR-HMI)** - 触摸面板：墙面屏幕上的系统状态、布防与确认，以及语音助手的所在
* **[ARMOR-NETWORK](https://github.com/JuanenRac/ARMOR-NETWORK)** - 本地网络：其设备、互联网以及变化
* **[ARMOR-SERVER](https://github.com/JuanenRac/ARMOR-SERVER)** - 中央协调器：遥测、报警、设备、太阳能读数和摄像头
* **[ARMOR-STUDIO](https://github.com/JuanenRac/ARMOR-STUDIO)** - 网页控制台：摄像头、雷达、报警、太阳能和 2D/3D 场地设计器
* **ARMOR-ANDROID-CONTROL** (本仓库) - 带实时 2D/3D 雷达的 Android 操作员客户端
* **[ARMOR-SERVER-AI](https://github.com/JuanenRac/ARMOR-SERVER-AI)** - 会解释决策且从不执行动作的视觉推理策略
* **[ARMOR-VISION](https://github.com/JuanenRac/ARMOR-VISION)** - Jetson Orin NX 上的摄像头区域、遮罩与检测规则
* **[ARMOR-VOICE-AI](https://github.com/JuanenRac/ARMOR-VOICE-AI)** - 带无法伪造确认的离线语音意图
* **[ARMOR-HARDWARE](https://github.com/JuanenRac/ARMOR-HARDWARE)** - 外壳、电子器件和台架验收矩阵
* **[ARMOR-DEVOPS](https://github.com/JuanenRac/ARMOR-DEVOPS)** - 部署、CM5 测试台、备份与 TLS
* **[ARMOR-SIMULATOR](https://github.com/JuanenRac/ARMOR-SIMULATOR)** - 带可重复故障的离线遥测模拟器
* **[ARMOR-UPDATER](https://github.com/JuanenRac/ARMOR-UPDATER)** - 发现、安装并更新生态系统自身的仓库
* **[ARMOR-DOCS](https://github.com/JuanenRac/ARMOR-DOCS)** - 架构、安全基线和能力矩阵

## 📚 文档与社区

更多阅读：

* [能力矩阵：哪些已被证实，哪些没有](https://github.com/JuanenRac/ARMOR-DOCS/blob/main/docs/CAPABILITY_MATRIX.md)
* [项目目录：版本以及各仓库之间的依赖](https://github.com/JuanenRac/ARMOR-DOCS/blob/main/docs/PROJECT_CATALOG.md)
* [本仓库的变更记录](CHANGELOG.md)
* [许可证（GPL-3.0-or-later）](LICENSE)
* 问题、想法与反馈：electrohobby3d@gmail.com

## 👤 作者

**JuanenRac (Electro Hobby 3D)** · electrohobby3d@gmail.com

## 📜 许可证

GPL-3.0-or-later - 见 [LICENSE](LICENSE)。
