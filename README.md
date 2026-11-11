# SleepTown - 睡眠小镇

> 和好友一起放下手机，共同建造小镇。

SleepTown 是一款睡眠监督 App。设定睡眠时段后，在规定时间内不能离开 App，否则正在建造的房屋会倒塌。坚持到底就能随机获得一栋建筑，日积月累建成专属小镇。支持联机模式，多人共同建造同一栋建筑，一人失败全员受罚。

## 功能特性

- **单人睡眠**：设定入睡/起床时间，倒计时建造房屋，切出 App 超时则倒塌
- **联机睡眠**：房主创建房间，好友输入房间号加入，所有人同步建造同一栋建筑
- **建筑图鉴**：10 种建筑，按稀有度加权随机（普通→稀有→史诗→传说）
- **息屏智能检测**：使用 `PowerManager.isInteractive()` 区分息屏和切出 App，息屏不触发惩罚
- **悬浮窗提醒**：切出 App 时显示倒计时悬浮窗，即使(引导)回到 App
- **本地持久化**：用户档案、建筑记录、设置全部存储在 SharedPreferences
- **公网服务器**：Node.js + WebSocket，支持任意网络环境下的联机

## 技术栈

| 端 | 技术 |
|------|------|
| Android | Kotlin + Jetpack Compose + Foreground Service + WindowManager |
| 服务器 | Node.js + ws (WebSocket) + better-sqlite3 |
| 通信 | OkHttp WebSocket 客户端，JSON 消息协议 |

## 项目结构

```
sleeptown-oss/
├── android/                    # Android 客户端
│   ├── app/
│   │   ├── src/main/java/com/sleeptown/demo/
│   │   │   ├── MainActivity.kt        # 入口 + 4Tab 导航 + 生命周期
│   │   │   ├── SleepService.kt        # 核心服务（计时/切出检测/悬浮窗/联机惩罚）
│   │   │   ├── SleepDataManager.kt    # 数据层（建筑/记录/用户/时间/服务器配置）
│   │   │   ├── MultiplayerManager.kt  # WebSocket 联机管理器
│   │   │   ├── RoomScreen.kt          # 联机 UI（连接/创建/加入/大厅/游戏中）
│   │   │   ├── SleepScreen.kt         # 睡眠页 5 种状态 UI
│   │   │   ├── TownScreen.kt          # 小镇统计 + 建筑图鉴
│   │   │   ├── SettingsScreen.kt      # 设置页（档案/时间/宽限）
│   │   │   └── ScrollTimePicker.kt    # 上下滚动时间选择器
│   │   ├── src/main/AndroidManifest.xml
│   │   └── build.gradle.kts
│   ├── gradle/
│   └── settings.gradle.kts
├── server/                     # WebSocket 服务器
│   ├── server.js               # 主程序（房间管理/消息路由/广播）
│   ├── db.js                   # SQLite 数据层
│   └── package.json
└── README.md
```

## 快速开始

### 1. 部署服务器

```bash
cd server
npm install
node server.js
# 或用 PM2 守护
pm2 start server.js --name sleeptown-server
```

服务器默认监听 `8080` 端口。确保防火墙/安全组放行该端口。

### 2. 配置 Android 客户端

1. 用 Android Studio 打开 `android/` 目录
2. 打开 App → 联机 Tab → 输入服务器地址 `ws://你的服务器IP:8080`
3. 连接后创建房间或加入房间

### 3. 联机流程

1. A 创建房间，获得 4 位房间号
2. B 输入房间号加入
3. A 点击「开始联机睡眠」
4. 所有人同步进入建造倒计时
5. 建造期间切出 App 会触发惩罚倒计时，超时则全员坍塌
6. 坚持到建造结束，所有人获得同一栋建筑

## 建筑类型

| 建筑 | 稀有度 | 权重 |
|------|--------|------|
| 小木屋 | 普通 | 30 |
| 砖房 | 普通 | 25 |
| 帐篷 | 普通 | 20 |
| 树屋 | 普通 | 15 |
| 灯塔 | 稀有 | 5 |
| 风车 | 稀有 | 3 |
| 城堡 | 稀有 | 2 |
| 金字塔 | 史诗 | 1 |
| 宫殿 | 史诗 | 1 |
| 世界奇观 | 传说 | 1 |

## WebSocket 消息协议

| 消息类型 | 方向 | 说明 |
|----------|------|------|
| REGISTER | C→S | 注册用户（userId/nickname/avatar） |
| CREATE_ROOM | C→S | 创建房间 |
| JOIN_ROOM | C→S | 加入房间（roomCode） |
| ROOM_CREATED | S→C | 房间创建成功（roomCode） |
| ROOM_JOINED | S→C | 加入房间成功（roomCode） |
| MEMBER_LIST | S→C | 成员列表广播 |
| START_GAME | C→S | 房主开始游戏（duration/countdown） |
| GAME_STARTED | S→C | 游戏开始广播（含共同建筑类型） |
| LEFT_APP | C→S | 离开 App 上报 |
| BACK_APP | C→S | 回到 App 上报 |
| PENALTY_REPORT | C→S | 惩罚上报（倒计时超时） |
| PENALTY | S→C | 全员惩罚广播 |
| LEAVE_ROOM | C→S | 离开房间 |
| ROOM_DISBANDED | S→C | 房间解散 |

## 系统要求

- Android 7.0+（minSdk 24）
- Android Studio (建议最新版)
- Node.js 16+
- 需要悬浮窗权限（SYSTEM_ALERT_WINDOW）和通知权限

## 许可证

MIT License - 详见 [LICENSE](LICENSE)
