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
  <a href="README_jpn.md">🇯🇵 日本語</a>
</p>

### ARMOR-SERVER 的移动端操作员客户端

<p align="center">
  <img src="https://img.shields.io/badge/License-GPL%203.0-blue.svg" alt="GPL 3.0">
  <img src="https://img.shields.io/badge/Language-Kotlin-7f52ff.svg" alt="Language">
  <img src="https://img.shields.io/badge/UI-Jetpack%20Compose-4285f4.svg" alt="UI">
  <img src="https://img.shields.io/badge/Maturity-functional%20baseline-00E5FF.svg" alt="Maturity">
</p>

---

**诚实性检查 - 今天真正能运行的部分:** 地址安全和报警决策规则有单元测试（55 个），应用可以构建。它**尚未在手机上对着服务器运行过**，因此通知权限流程和后台服务未经验证。布防、报警和设备遵循服务器的路由，但没有在真实手机上试过。

---

## 🎯 概述

* **使用服务器自己的登录：** IP、端口、用户名和密码；密码创建 HttpOnly 会话，绝不会保存在手机上。
* **摄像头监控：** 1 到 16 个画面块、最大化视图、保持画面比例的实时 MJPEG、受限的 PTZ 控制盘、快照和录像。
* **布防与撤防，** 需先确认；待**确认的报警**，未处理的带徽标；**设备**（烟雾、燃气、水浸、门、窗、移动、气候、插座、灯、警笛、门锁）及其状态和 开 / 关 / 切换。
* **实时雷达，2D 和 3D：** 雷达标签页按 Studio 中设计的样子绘制场地（地面、建筑、树木、立柱、雷达和摄像头的探测范围）以及雷达看到移动的人，每一秒半刷新一次；可拖动、捏合和旋转 3D 视图。摆放遵循 Studio 的规则，但从未显示过真实雷达。
* **太阳能：** “Más”中的一项（以及状态页的一个磁贴）显示合计（太阳、用电、带电量和流向的电池、是否有市电），以及服务器上报的每台逆变器和电池的数值；电池的电芯可按需展开，并标出最高和最低；尚在等待数据的设备，以及示例读数或无信号的设备都有标记。打开时每五秒刷新一次；已针对服务器的响应格式测试，并在模拟器中对着带示例读数的本地服务器试过，从未用真实设备试过。太阳能警报（逆变器故障、电池电量低或处于保护状态、设备无应答）会像设备警报一样提醒手机。
* **通过蓝牙配置现场节点**（在登录界面或 *Más > Configurar un nodo* 中）：找到广播 `ARMOR-xxxxxx` 的节点，为新节点创建管理员或登录，搜索 Wi-Fi 网络，并设置节点名称、路由器 Wi-Fi 或固定地址以及代理，适用于没有以太网线的节点。已编译并做过单元测试，从未对着节点或手机运行。
* **证据库、** 周界与节点状态，以及每一次警告、节点、摄像头、设备、报警和模式变化的**历史**。
* **报警通知：** 节点达到 HIGH、摄像头不再响应、设备报警（烟雾、燃气、水浸、紧急按钮任何时候；门、窗或移动传感器仅在布防时），以及布防时节点离线；可选的后台监视使用当前会话，并在其结束时提示。
* **谨慎处理密码：** 仅允许对私有局域网或回环 IPv4 *字面量* 使用明文 HTTP。只是开头像私有地址的主机名（`10.attacker.example`）或带前导零的地址（某些解析器会把 `010.0.0.1` 读成公网的 `8.0.0.1`）会被拒绝。
* **电网：** “更多”中的一项（以及状态屏幕上的一块卡片）显示电网入口的功率（取电或向电网送电）、电气节点测量的每个通道（电压、电流、功率、电能、频率、功率因数、开关状态）和电表的告警；电气节点的告警（电表告警、市电电压超出范围、电网中断、节点不再应答）与太阳能告警一样会通知操作员。逆变器的卡片还会显示其第二路光伏输入和并联系统的单元。
* **由图标构成的外观：** 近乎黑色的界面、青色强调、琥珀色表示需要注意、少字的大图标、底部栏、关于页面和退出按钮。

## 📂 仓库结构

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

## 🛠️ 开发环境

```powershell
.\gradlew.bat testDebugUnitTest assembleDebug
adb install -r app\build\outputs\apk\debug\app-debug.apk
```

调试版 APK 未为发布签名。参见[客户端边界](docs/CLIENT_BOUNDARY.md)。

## 🔗 相关项目

**A.R.M.O.R.**（Autonomous Radar & Multimodal Observation Range）是由若干独立仓库组成的周界安防系统。每个仓库都有自己的版本、测试和 README；家族成员如下：

* **[ARMOR-COMMON](https://github.com/JuanenRac/ARMOR-COMMON)** - 消息契约、验证器、一致性向量和生成的类型
* **[ARMOR-RADAR](https://github.com/JuanenRac/ARMOR-RADAR)** - 适用于 ESP32-S3 的现场节点固件，带三个雷达和自带网页面板
* **[ARMOR-SOLAR](https://github.com/JuanenRac/ARMOR-SOLAR)** - 太阳能逆变器与电池的协议，以及网关节点的消息
* **[ARMOR-ELECTRICAL](https://github.com/JuanenRac/ARMOR-ELECTRICAL)** - 电气节点：电表、电网读数消息和开关规则
* **[ARMOR-NETWORK](https://github.com/JuanenRac/ARMOR-NETWORK)** - 本地网络：其设备、互联网以及变化
* **[ARMOR-SERVER](https://github.com/JuanenRac/ARMOR-SERVER)** - 中央协调器：遥测、报警、设备、太阳能读数和摄像头
* **[ARMOR-STUDIO](https://github.com/JuanenRac/ARMOR-STUDIO)** - 网页控制台：摄像头、雷达、报警、太阳能和 2D/3D 场地设计器
* **ARMOR-ANDROID-CONTROL** (本仓库) - 带实时 2D/3D 雷达的 Android 操作员客户端
* **[ARMOR-SERVER-AI](https://github.com/JuanenRac/ARMOR-SERVER-AI)** - 会解释决策且从不执行动作的视觉推理策略
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
