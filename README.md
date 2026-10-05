[简体中文](README.md) | [English](README_EN.md)

<h1 align="center">E33Chat</h1>

<p align="center">
  <em>以聊天 APP 风格重铸原版聊天框</em>
</p>

<p align="center">
  <img alt="MC" src="https://img.shields.io/badge/MC-26.2-green">
  <img alt="Loader" src="https://img.shields.io/badge/Loader-Fabric-orange">
  <img alt="Side" src="https://img.shields.io/badge/Side-Client%20required,%20server%20optional-blue">
  <img alt="Java" src="https://img.shields.io/badge/Java-25%2B-yellow">
  <img alt="Version" src="https://img.shields.io/badge/Version-2.4.15-informational">
  <img alt="License" src="https://img.shields.io/badge/License-MIT-brightgreen">
</p>

E33Chat 是一款聊天增强模组，把原版聊天 HUD 重做成聊天 APP 风格：气泡与头像、@ 提及、私聊侧边栏、搜索、表情与常用语、图片消息、引用回复、通知横幅、本地聊天记录，并自带一套重做的设置界面。

> [!IMPORTANT]
> **本仓库是 Minecraft 26.2 / Fabric 单目标。** 上游「1.20.1 Forge + 1.21.1 NeoForge + 1.21.1 Fabric」三端并行的结构已经废弃，只保留 `platforms/26.2-fabric`，旧平台目录与旧分支不再维护。
>
> 另外：**26.1 起 Minecraft 不再混淆**，所以这个仓库里没有 Yarn 映射，也没有 remap 步骤 —— 这不是漏写，见 [开发与构建](#开发与构建)。

---

## 目录

- [安装](#安装)
- [快速开始](#快速开始)
- [功能](#功能)
- [使用说明](#使用说明)
- [服务端加成](#服务端加成)
- [兼容性](#兼容性)
- [已知限制](#已知限制)
- [隐私与数据](#隐私与数据)
- [常见问题](#常见问题)
- [故障排查](#故障排查)
- [开发与构建](#开发与构建)
- [更新日志](#更新日志)
- [问题反馈](#问题反馈)
- [许可证](#许可证)

---

## 安装

| 依赖 | 类型 | 说明 |
|---|---|---|
| Minecraft | 必需 | **26.2** |
| Fabric Loader | 必需 | 0.19.5+ |
| Fabric API | 必需 | 0.161.0+26.2（`fabric-api`，任意兼容版本） |
| Java | 必需 | **25+**（26.2 要求，`javaVersion.majorVersion=25`） |
| ModMenu | 可选 | 20.0.0+，装了才能在模组列表里点进设置 |
| CustomSkinLoader | 可选 | 显示离线玩家头像 |

1. 下载 `e33chat-Fabric-26.2-2.4.15.jar`
2. 放入 `.minecraft/mods/`
3. 启动游戏（客户端必装；服务端可选，装了会多出一批功能，见[服务端加成](#服务端加成)）

---

## 快速开始

1. 按 **T / 回车** 打开聊天框即可看到 E33Chat 面板（**没有自定义按键**，走的就是原版聊天键；左下角图标上的 `[T]` 只是提示当前绑的是哪个键）
2. 面板左下角 **齿轮** → 菜单 → 设置 进入配置界面
3. 「聊天框」分类调整面板宽度（400–1600 物理像素，窗口缩放不影响实际宽度；可开「全屏面板」直接铺满屏幕）、气泡颜色、圆角、消息间距与头像大小
4. 「通知」分类配置 @ 提示音、横幅、私聊音效与总音量；横幅位置可调（`banner_offset_x/y`）避开其他 HUD
5. 发图片：左侧**上传按钮** / **Ctrl+V 粘贴** / **拖图片进窗口**，上传完成后自动发送

---

## 功能

- 💬 **聊天气泡** — 带头像和名字，颜色 / 文字色 / 圆角 / 主题可调；头像顶对齐，同人连发默认只首条显示（QQ 式）；消息间距组内 4px、组间 12px
- 🖼️ **图片消息** — 气泡内原生渲染 `[[CICode]]` / `[[ChatUpgrade]]` 协议图片（与 ChatImage 互通），点击开原图；防刷屏限流 + 接收开关
- ☁️ **服务端媒体托管** — 服务器也装 E33Chat 时图片直接存服务器（永久），否则自动回退第三方图床
- 😀 **自定义表情包** — `config/e33chat/emotes/` 放图即用（最多 32 个），Ctrl+V 加剪贴板图片，点击即发
- @ **提及补全** — 输入 `@` 弹玩家列表，左键头像 @ta；被 @ / 引用时提示音 + 横幅
- 👥 **私聊侧边栏** — 在线玩家列表、未读红点、公屏 / 私聊分栏、NPC 隐藏名单
- 🔍 **搜索 & 表情 & 常用语** — 实时搜索（支持中文）、emoji / 颜文字面板、常用语一键填充
- 📋 **复制 & 引用回复** — 右键消息复制 / 引用；右键头像私聊 / 传送 / 屏蔽
- 🚫 **屏蔽玩家** — 消息完全消失（原版聊天框 / 气泡 / 横幅 / 音效），即刻生效，重进不恢复
- 🔔 **通知横幅** — 覆盖 @ / 引用 / 私聊 / 系统消息，总音量滑条 + 分类型开关 + 位置偏移
- 🎬 **动画风格** — 面板 / 横幅 / 弹层 / 消息四类独立配置（SLIDE / FADE / ZOOM / NONE），弹层带开合动画
- 🗨️ **原版聊天框保留** — 正常渲染（上移避开 HUD 图标），ChatHeads / ChatAnimation 等直接生效
- 💾 **聊天历史** — 按存档 / 服务器分别保存，完整保留颜色与点击事件（默认关）；每 30 秒自动保存
- 🛠️ **配置界面** — 5 标签页 + 可折叠子分类 + 实时预览 + 快照式保存 / 退出；颜色 / 数值 / 开关全 GUI 可调
- ✅ 防刷屏合并计数 · 📝 关闭保留输入 · 🌈 `&` 颜色码本地渲染 · 🧩 服务端消息格式模板

---

## 使用说明

### 聊天显示

- 自己的气泡靠右、他人靠左，颜色各自可调；气泡圆角 0–10（默认 4）
- 私聊显示 `<玩家名>[私聊] 内容`，引用回复 `<玩家名>[引用] 内容`（黄色标签），保留服务器前缀装饰与团队颜色
- 消息间距：同发送者 5 分钟内连续消息 = `message_gap` × 2/3（默认 4px）；换人 / 超时 / 系统消息 / 时间分隔 = ×2（默认 12px）

### 图片与媒体

- **发送**：上传按钮 / Ctrl+V / 拖拽；自动缩放到 ≤2048px 并重编码；上传串行排队（最多 8 个），按一次回车即可，失败恢复输入框
- **图床**：默认 uguu.se（约 3 小时过期）；服务端托管开启时改存服务器（`e33chat://media/<id>` 永久）；自定义图床配置 `upload_url` 等四键（multipart POST，响应取整段文本或 `json:字段路径`）
- **接收**：原生渲染图片代码，历史旧图片自动兼容重载；防滥用 = 滑动窗口限流 + 64 条 LRU 纹理缓存 + 解码前缩放；「接收图片」开关关闭后显示纯文本 `[图片]` 且不下载

### 侧边栏与通知

- 侧边栏：点名字开私聊、未读跳动红点、搜索过滤、公屏 / 私聊分栏、通配符隐藏名单（如 `*[NPC]*`）
- 横幅：@ / 引用 / 私聊 / 系统消息四类，系统横幅默认开；「@ 我」快捷跳转按钮；位置偏移 ±1000px 避 HUD 重叠
- 可选「@ 必须带 @ 前缀」；自我通知开关（默认关，调试用）；总音量滑条统一调节

### 动画与外观

- 四类动画独立取值 SLIDE / FADE / ZOOM / NONE：面板（默认 SLIDE）、横幅（SLIDE）、弹层（FADE）、消息（FADE）；`animation=false` 全关
- 弹层打开 200ms 淡入、关闭 150ms 缓出（ESC / 图标切换 / 点击外部全路径生效）；横幅进入 250ms、退出 150ms
- 弹层族（设置 / 表情 / 常用语 / 搜索 / @ 弹层 / 右键菜单）圆角 + 阴影 + 1px 描边；引用块圆角 8
- 面板背景模糊 `blurEnabled` 默认关（26.2 上它是空操作，见[已知限制](#已知限制)）

### 设置与纹理

- 深色 / 浅色主题；设置界面 5 标签页（聊天框 / HUD / 通知 / 侧边栏 / 高级），快照式保存 / 退出，ESC 确认放弃
- **资源包覆盖**：界面元素与图标走纹理渲染，路径 `assets/e33chat/textures/gui/{dark|light}/<元素名>.png`，F3+T 即时生效
- ⚠️ **弹层背景自 2.3.16 起不再走纹理**（语义色驱动）；聊天气泡 / 引用块 / @ 横幅本就是程序化渲染，两者均不可被资源包覆盖

---

## 服务端加成

服务端可不装。装上后额外激活：

- 引用回复同步、跨客户端 @ 提及同步（含中文名）
- 新玩家进服收到近期聊天历史（`history_enabled`，默认关）
- 头像传送改用 `/tpa`（`use_tpa`，默认关）
- **服务端图片托管**（`media_enabled`，默认开）：图片存服务器永久（8MB/文件、512MB 总配额、随机 UUID 防遍历、每玩家限速）
- **消息格式模板**：服务端声明聊天格式并同步全服——插件 / NCR 改过格式的消息也能正确解析（`/e33chat gui` 配置，「从消息生成」或预设一键加，占位符 `{display_name}` `{prefix}` `{external}` `{content}` `{sender}` `{target}` `{sep}`）
- **EasyBot 群消息兼容**（`easybot_compat`，默认开）：把 EasyBot 转发进游戏的 QQ 群消息解析成玩家气泡，并支持气泡内显示 EasyBot / ChatImage 的 CICode 图片。常见格式自动识别（2.4.8 起群名前缀与 QQ 号均可省略）；EasyBot 模板被改过时，可用 `{external}` 聊天模板覆盖（服务端配置预设已带 EasyBot 格式，详见 [EasyBot 模板说明](#easybot-模板说明)）

**服务端配置**：`<世界目录>/serverconfig/e33chat-server.json`（首次进服时生成）

| 命令 | 权限 | 说明 |
|---|---|---|
| `/e33chat gui` | OP | 图形化配置服务端项 |
| `/e33chat template list` | OP | 列出当前模板 |
| `/e33chat template set <chat\|whisper> <模板>` | OP | 设置模板 |
| `/e33chat template remove <chat\|whisper>` | OP | 删除一条 |
| `/e33chat template clear <chat\|whisper>` | OP | 清空 |
| `/e33chat template test <chat\|whisper>` | OP | 用模板试解析一条消息 |
| `/e33chat group list \| create \| join \| leave \| delete \| msg` | — | 聊天群组（服务端开启时可用） |

### EasyBot 模板说明

- E33Chat 内置识别常见 EasyBot 格式：`[群名] <昵称(QQ号)> 内容`、`[群名] <昵称> 内容`、`<昵称> 内容`、`<昵称（群名片）> 内容`（2.4.8 起群名前缀与 QQ 号均非必需），`easybot_compat` 默认开启即可直接用
- 如果你在 EasyBot 主程序里改过「同步模板(到服务器)」，请到 `/e33chat gui` → 聊天模板，加一条 `{external}` 模板覆盖
- 常用示例：

| EasyBot 同步模板效果 | E33Chat 聊天模板 |
|---|---|
| `[群名] <昵称(QQ号)> 内容` | `[{prefix}] <{external}> {content}` |
| `[群名] 昵称: 内容` | `[{prefix}] {external}{sep}{content}` |
| `昵称 >> 内容` | `{external}{sep}{content}` |
| `<昵称> 内容` | `<{external}> {content}` |

- 也可以直接命令添加：`/e33chat template set chat "[{prefix}] <{external}> {content}"`

---

## 兼容性

| 模组 / 插件 | 状态 |
|---|---|
| No Chat Reports 等禁用举报插件 | 自 2.1.0 起自动兼容，无需配置 |
| CustomSkinLoader | 安装后显示离线玩家头像 |
| ChatImage / ChatUpgrade（图片协议） | 原生互通 |
| EasyBot（QQ 群服互通） | `easybot_compat` 默认开启，群消息自动解析为玩家气泡；CICode 图片在气泡内显示 |
| IMBlocker | 自动适配（命令输入自动切英文） |
| ModernUI | 可点击文本下划线 / 点击区域边界兼容 |
| Quark 等物品分享 | 系统消息物品图标正常渲染 |
| ChatHeads, ChatAnimation | 默认生效 |
| **MiniHUD / malilib** | 聊天面板会画在 malilib 的覆盖层（MiniHUD 信息行等）**之上** —— 它是在 `Gui.extractRenderState` 的 TAIL 追加的，位置晚于所有界面，所以 E33Chat 专门把自己那一次界面绘制推迟到它之后。详见 [docs/port-26.2-notes.md](docs/port-26.2-notes.md) |
| 昵称插件 | 部分支持，见 [常见问题](#常见问题) |
| 改聊天格式插件（EssentialsChat / CMI / DeluxeChat 等） | 服务端配置模板适配（含常见格式预设） |

---

## 已知限制

1. **仅支持 Minecraft 26.2 + Fabric，需要 Java 25**；1.20.1 Forge / 1.21.1 NeoForge / Fabric 三端已废弃
2. 昵称与真名毫无关联、且插件没挂「点击私聊」也没同步 Tab 名时，消息显示为系统灰字（模板也救不了）
3. 名字与内容纯空格无分隔符的格式无法识别；NCR 加密聊天显示密文
4. 默认图床文件约 3 小时过期（服务端托管开启后永久，但总配额 512MB，需留意清理）
5. **26.2 上的两处渲染降级**（详见 [docs/port-26.2-notes.md](docs/port-26.2-notes.md) 第三节）：
   - **圆角**：旧版是自绘 SDF shader，现在退化成「主体矩形 + 四角逐行内缩」的近似。半径 6–8px 时观感接近，但不是抗锯齿 SDF
   - **面板背景模糊**（`blurEnabled`）：26.2 换掉了 FBO 那套 API，目前是**空操作** —— 面板不模糊，但也不会崩。26.2 有原生整屏模糊，但它一帧只能调一次且语义是「模糊本层之前的所有内容」，做不了局部模糊
   - ~~纹理 alpha / tint 失效~~ —— 这条**已经修好了**，颜色参数一直都在（排在 `blit` 参数表最后）

---

## 隐私与数据

> [!WARNING]
> 聊天记录以**明文**保存在本机，开启后请勿在公共电脑或不受信任的环境中使用。

- **聊天记录**：只存本机、默认关闭、不上传。路径 `<游戏目录>/e33chat/history/<世界名>_<短哈希>.json`（每行一条 JSON，完整保留颜色与点击事件）。含凭据的敏感命令（`/login` `/register` 等）自动跳过
- **图片**：你发送的图片会上传到第三方图床（默认 uguu.se，约 3 小时过期）或（服务端托管开启时）服务器存储；客户端不会主动上传任何数据
- **服务端模组**仅转发（@ / 引用 / 历史 / 媒体），不收集客户端数据；保存历史、同步、托管均可在配置中关闭

---

## 常见问题

**服务器需要装吗？** 不必须。装上额外解锁引用同步、@ 同步、历史同步、`/tpa` 传送、图片托管。

**怎么打开配置？** 面板左下角齿轮 → 菜单 → 设置。也可以装 ModMenu 从模组列表进入。客户端配置 `config/e33chat/e33chat-client.json`，所有项均可 GUI 调整。

**有快捷键吗？** 没有自定义按键。面板走的是原版聊天键（默认 T），左下角 HUD 图标上的 `[T]` 就是提示你当前绑的哪个键。

**聊天历史默认保存吗？** 不保存（`chatHistoryEnabled: false`），在 设置 → 聊天框 → 聊天历史 开启；每 30 秒自动保存，正常退出也保存，崩溃最多丢 30 秒。

**怎么开背景模糊？** 可以开，但**在 26.2 上没有效果**（`BlurRenderer` 是空操作，见[已知限制](#已知限制)）。

**面板宽度和全屏怎么调？** `panelWidth`（默认 1000）按屏幕物理像素计：改成 800，面板在任意窗口 / 缩放档下都稳定占 800 物理像素宽，不会随窗口变宽变窄或错位（窗口比面板窄时会夹紧到窗宽）。范围 400–1600。想让它直接铺满屏幕，开 `panelFullscreen`（默认关）即可，此时忽略 `panelWidth`，侧边栏保留可点。

**我发的图片去哪了？** 服务端托管开启（默认）→ 存服务器永久；否则 → 第三方图床（uguu.se，约 3 小时过期）。均可配置更换。

**为什么某条消息是灰字？** 客户端没把握它是玩家说的就保守归灰字（见[已知限制](#已知限制)），昵称插件 / 特殊广播格式是常见原因。

**支持昵称插件吗？** 部分支持：昵称挂「点击私聊」事件或同步 Tab 名时可正确归属，否则灰字。

**服务器改了聊天格式，消息对不上？** 用消息格式模板：OP 输入 `/e33chat gui`，「从消息生成」粘贴一条真实聊天行或预设一键加，保存即同步全服；模板留空恢复守卫识别。

**消息里的链接 / 玩家名点了没反应？** 自 2.4.15 起已修复：26.2 把原版 `handleTextClick` 拆成了两个方法，早先版本只接上了处理 URL 的那一个，导致 `run_command` 类点击（`/msg`、`/warp`、`/tpa`、可点菜单、QQ 转发链接）静默失效。升级即可。

**怎么恢复原版聊天？** 设置 → 聊天框 → 关闭「启用 E33Chat」（`enabled: false`）；移除 mod 完全还原。

**可以放进整合包吗？** 可以，无需额外授权。

---

## 故障排查

1. 确认是 **Minecraft 26.2 + Fabric + Java 25**，并且用的是本仓库构建的 26.2 JAR（旧平台的 JAR 装不上）
2. 备份后删除 `config/e33chat/e33chat-client.json` 测试配置损坏；只保留 E33Chat 排查冲突
3. 图片上传失败：检查 `upload_url` 与网络；默认图床 uguu.se 部分网络不可达，可换自定义图床或开服务端托管
4. 查看 `.minecraft/logs/latest.log` 中 `[e33chat]` 相关错误；崩溃时看 `crash-reports/` 里最上面那条的 `Description` 和第一段堆栈
5. 提交 issue 时附上版本号、模组列表、`latest.log`、截图和复现步骤

---

## 开发与构建

### 环境

- **JDK 25**（26.2 的硬要求），`JAVA_HOME` 指向它
- Gradle 用仓库自带的 wrapper，无需另装

### 构建

```bash
cd platforms/26.2-fabric
./gradlew build          # Windows: gradlew.bat build
```

产物在 `platforms/26.2-fabric/build/libs/e33chat-Fabric-26.2-2.4.15.jar`。

跑测试：

```bash
./gradlew test
```

### 仓库结构

```
gradle.properties              仓库级身份（mod_version 等）
gradle/e33chat-layers.gradle   共享层装配脚本
versions/*.json                层 / 目标 / 依赖的定义（脚本据此装配，平台里不硬编码目标名）
shared/src/main/java/...       平台无关部分（chat / compat / image / mixin / render / server）
platforms/26.2-fabric/         唯一目标：Fabric 26.2
  src/main/java/...            Fabric 侧实现（config / network / store / texture / ui / command …）
  src/main/resources/          fabric.mod.json、mixin 配置、纹理与语言文件
  src/test/java/...            JUnit 5
docs/port-26.2-notes.md        26.2 移植的 API 变更、渲染降级与坑（改渲染前建议先读）
ARCHITECTURE.md                整体架构
CHANGELOG.md                   变更记录
```

`platforms/26.2-fabric/build.gradle` 通过 `apply from: '../../gradle/e33chat-layers.gradle'` 把 `shared/` 和本目标挂载的层接进源集，判定依据全在 `versions/*.json`。

### 26.2 上改代码要注意的几件事

这些是移植过程中真实踩过的，改之前值得看一眼：

- **没有 Yarn 映射，也不需要 remap。** 26.1 起 Minecraft 不再混淆，`com.mojang:minecraft` 出来的就是带官方名的可读代码；依赖用 `implementation` 而不是 `modImplementation`（Loom 1.18 也去掉了 `mod*` 那一族配置）
- **`Gui.extractRenderState(DeltaTracker, boolean renderHud, boolean renderScreen)` 一次调用同时负责 HUD 和当前界面**，内部顺序是 `Hud` → `overlay` → `Screen`。所以在它 HEAD 处 `cancel` 会把**界面一起取消**；想只跳 HUD 就拦 `Hud.extractRenderState` 那一次调用
- **不要手动调 `extractBackground`**：`Screen.extractRenderStateWithTooltipAndSubtitles` 已经在调用 `extractRenderState` 之前调过一次，再调会抛 `IllegalStateException: Can only blur once per frame`。界面里**覆写** `extractBackground` 是对的，在 `extractRenderState` 里**调用**它是错的
- **`blit` 有带颜色的重载**，颜色排在参数表最后（10/11 参与 12/13 参两族）。判断「新 API 有没有某个能力」之前，先把整族重载按参数个数排开看一遍
- **`Screen.handleTextClick` 没了**，拆成互补的两个静态方法：`defaultHandleClickEvent`（URL / 打开文件 / 命令补全 / 剪贴板）与 `defaultHandleGameClickEvent`（`run_command` / `show_dialog` / `custom`）。只接前者会让「点击执行命令」静默失效
- **pose 是 2D 的**（`Matrix3x2fStack`），`translate` 没有 z 分量 —— 旧代码靠 z 分层的地方现在只能靠绘制顺序
- **mixin 目标是字符串，只在运行期解析**：编译通过不代表装得上。所有 mixin 都没写 `require = 0`，所以目标写错会响亮报错而不是静默失效
- **MixinExtras 由 Fabric Loader 内嵌**（0.19.5 里是 `META-INF/jars/mixinextras-fabric-0.5.5.jar`），`@WrapOperation` / `@Local` 可以直接用，不需要额外依赖
- **`HudElementRegistry` 注册的元素在 HUD 图层内部**，比 malilib 等 mod 在 `extractRenderState` TAIL 追加的覆盖层早 —— 想让自己的东西盖住它们，得挂在更晚的位置

### 本机（这台 Windows）的两个额外变通

与仓库无关，换台能直连 Maven 的机器就不需要：

1. JDK 自带 cacerts 缺 Let's Encrypt 2026 新根 `Root YR`，Gradle 下载会握手失败 → 复制了一份 JDK 并导入该根
2. `repo.maven.apache.org` 被本地中间盒劫持（返回自签证书）→ 用一份 `init.gradle` 把仓库换成镜像

具体命令见 [docs/port-26.2-notes.md](docs/port-26.2-notes.md) 第四节。

---

## 更新日志

完整变更记录见 [CHANGELOG.md](CHANGELOG.md)，26.2 移植的细节见 [docs/port-26.2-notes.md](docs/port-26.2-notes.md)。

---

## 问题反馈

到上游仓库 [Issues](https://github.com/E33EPUS/E33Chat/issues) 提交，附上版本号、加载器、模组列表、`latest.log`、截图或视频与复现步骤。

> 如果是 26.2 端口特有的问题（例如只有 26.2 才出现的渲染异常），请额外说明，并尽量附上 `crash-reports/` 里对应那份的堆栈。

---

## 许可证

[MIT License](LICENSE)

Copyright &copy; 2026 E33EPUS
