# 26.2 端口：环境坑与渲染待收尾

本文记录两件事：**本机构建必须处理的环境问题**（与仓库代码无关），以及
**这次端口做不了、留待收尾的视觉效果**。API 迁移本身已完成，`gradlew build` 通过。

## 一、结果

| 项 | 值 |
|---|---|
| 目标 | 单目标：Minecraft 26.2 / Fabric |
| 工具链 | Loom 1.18.2、Java 25、Gradle 9.7.1、Fabric Loader 0.19.5、Fabric API 0.161.0+26.2 |
| 映射 | 无（26.1 起 Minecraft 不再混淆，源码即官方名） |
| 产物 | `platforms/26.2-fabric/build/libs/e33chat-Fabric-26.2-2.4.15.jar`（约 552 KB，292 条目）|
| 测试 | 445 通过 / 0 失败 / 2 跳过 |
| 源码 | 平台 87 main + 17 test，shared 18 main |

`python tools/verify_targets.py` 通过。

## 二、26.2 的主要 API 变动（已全部处理）

这些是照着 26.2 的 jar 逐个核实的，不是猜的。

**类改名 / 换包**

| 旧 | 26.2 |
|---|---|
| `net.minecraft.resources.ResourceLocation` | `net.minecraft.resources.Identifier`（**改回 Identifier**，工厂方法不变）|
| `net.minecraft.client.gui.GuiGraphics` | `net.minecraft.client.gui.GuiGraphicsExtractor` |
| `net.minecraft.Util` | `net.minecraft.util.Util` |
| `net.minecraft.client.MinecraftClient` | `net.minecraft.client.Minecraft` |
| `com.mojang.blaze3d.platform.GlStateManager` | 已删除 |
| `net.minecraft.client.renderer.ShaderInstance` / `Uniform` | 已删除（换 `RenderPipeline`）|
| `com.mojang.blaze3d.vertex.Tesselator` / `BufferUploader` | GUI 路径上不可用 |
| `net.minecraft.client.resources.PlayerSkin` | `net.minecraft.world.entity.player.PlayerSkin`（record；贴图是 `ClientAsset.Texture`，取 `body().texturePath()`）|
| `net.minecraft.util.WorldSavePath` | `net.minecraft.world.level.storage.LevelResource` |
| `net.minecraft.commands.CommandManager` | `net.minecraft.commands.Commands`（旧的 `server.command` 包没了）|
| `PlayerInfo.getSkinTextures()` | `PlayerInfo.getSkin()` |

**输入事件全面改成事件对象**

`GuiEventListener` / `Screen` 的鼠标与键盘入口变成
`MouseButtonEvent`（`x()`/`y()`/`button()`）、`KeyEvent`（`key()`/`scancode()`/`modifiers()`）、
`CharacterEvent`（`codepoint()`）。`Screen.hasShiftDown()` 也不在了，改用
`Minecraft.hasShiftDown()`。

**渲染入口改名**

| 旧 | 26.2 |
|---|---|
| `Screen.render` | `extractRenderState` |
| `Screen.renderBackground` | `extractBackground` |
| `Widget.render` / `Renderable.render` | `extractRenderState` |
| `GuiGraphics.drawString` | `GuiGraphicsExtractor.text` |
| `GuiGraphics.renderOutline` | `outline` |
| `GuiGraphics.renderTooltip` | `setTooltipForNextFrame` |
| `GuiGraphics.renderComponentHoverEffect` | 无对应（改走 `setTooltipForNextFrame`）|
| `Screen.resize(Minecraft,int,int)` | `resize(int,int)` |
| `Screen.init(Minecraft,int,int)` | `init(int,int)` |

**事件载荷变成 record（密封接口）**

`ClickEvent` / `HoverEvent` 从「action + getValue(action)」改成每种动作一个 record，
取值必须模式匹配：`ClickEvent.SuggestCommand.command()`、`OpenFile.file()`、
`OpenUrl.uri()`、`HoverEvent.ShowText.value()`、`ShowEntity.entity()`。
构造也不再是 `new ClickEvent(Action, String)`，而是 `new ClickEvent.RunCommand(s)`。

**其它**

- `ChatFormatting` 不再有 `getColor()`；颜色走 `TextColor` 常量，`Style` 用 `withColor(TextColor)`。
- `TextColor` 没有 `getRgb()` 了，改 `getValue()`；比较颜色要比数值，不能比 TextColor 对象
  （它是有身份缓存的，`§6` 建出来的实例与 `TextColor.GOLD` 不是同一个对象）。
- `Component.Serializer` 删除 → `ComponentSerialization.CODEC` + `RegistryOps.create(JsonOps, Provider)`。
- `HolderLookup.Provider` 的抽象方法从 `listRegistries()` 换成 `listRegistryKeys()`。
- `ServerPlayer` 不再有 `getServer()` → 经 `level().getServer()`；权限从
  `hasPermissionLevel(int)` 换成 `permissions()` + `LevelBasedPermissionSet` / `PermissionLevel`。
- `ServerPlayer.sendMessage` / `Entity.displayClientMessage` → `sendSystemMessage`。
- `GameProfile` 变成 record：`getName()`/`getId()` → `name()`/`id()`。
- `TextureManager.register` 返回 void；`DynamicTexture` 需要名字 Supplier。
- `ChatComponent`：`render` → `extractRenderState(GuiGraphicsExtractor, Font, int, int, int, DisplayMode, boolean)`，
  `addMessage` 三个重载拆成 `addPlayerMessage` / `addClientSystemMessage` / `addServerSystemMessage`。
- `Team.getColor()` 返回 `Optional<TeamColor>`，`TeamColor` 给的是 `TextColor`。
- 音效常量去掉 `BLOCK_` 前缀；`PositionedSoundInstance` 删除 → `SimpleSoundInstance.forUI`。
- Fabric：`HudRenderCallback` 删除 → `HudElementRegistry` / `HudElement`；
  `PayloadTypeRegistry.playC2S/playS2C` → `serverboundPlay/clientboundPlay`；
  `ScreenEvents.afterRender` → `afterExtract`；
  `ResourceReloadListener.reload` → `onResourceManagerReload`。
- Gradle 9 起测试运行时必须显式带 `junit-platform-launcher`。

## 三、渲染降级（未收尾的视觉项）

26.2 把自定义着色器的写法换成了 `RenderPipeline` 注册 + `layout(std140)` UBO。
这批管线没重写，于是有降级，代码里都留了 TODO：

1. **`RoundRectRenderer`（圆角）**
   旧版是自绘 SDF shader（`rendertype_round_rect` + `u_Rect`/`u_Radius` uniform）。
   现在退化成「主体矩形 + 四角逐行内缩的细条」近似：半径 6~8 px 时观感接近，
   但不是抗锯齿 SDF。
   *收尾*：把 `assets/minecraft/shaders/core/rendertype_round_rect.{json,vsh,fsh}`
   改成 26.2 的 UBO 形式，用 `RenderPipeline.builder()` 注册后换回 SDF。

2. ~~**`ColoredTextureRenderer`（纹理 alpha / tint）**~~ —— **已修，原判断是错的**
   原文写的是"26.2 的 `blit` 所有重载都没有颜色参数（已逐个核对描述符）"。
   颜色参数一直都在，只是排在**最后**、容易被忽略：

   ```
   blit(pipeline, id, x, y, u, v, w, h, texW, texH)                            // 10
   blit(pipeline, id, x, y, u, v, w, h, texW, texH, color)                     // 11
   blit(pipeline, id, x, y, u, v, w, h, regionW, regionH, texW, texH)          // 12
   blit(pipeline, id, x, y, u, v, w, h, regionW, regionH, texW, texH, color)   // 13
   ```

   证据（原版字节码）：11 参重载的方法体把自己的第 11 个参数原样转发给 13 参重载的
   **最后一个**参数；而 `BlitRenderState` 的字段里，除 `x0/y0/x1/y1` 外唯一的 int 就是
   `color`，`buildVertices` 会把它写进顶点色。所以「多出来的那个 int」就是 ARGB 颜色。

   现在 `ColoredTextureRenderer` 恢复成「原调用 + 追加一个颜色参数」，几何与 UV 不变。
   颜色按通道相乘：`0xFFFFFFFF` = 原样，alpha < 255 即半透明。
   **教训**：判断"新 API 没有某个能力"之前，先把整族重载按参数个数排开看一遍 ——
   颜色是最后一个参数，只核对"描述符里有没有 color 字样"是看不出来的。

3. **`BlurRenderer`（面板背景模糊）**
   旧实现直接玩 FBO 句柄（`GL30.glGenFramebuffers` / `glBlitFramebuffer` /
   `Minecraft.getMainRenderTarget().frameBufferId`）。26.2 换成了跨后端
   （OpenGL / Vulkan）的 GPU 抽象，这条路连同 `GlStateManager` 一起没了。
   现在是空操作：面板不模糊，但也不会崩。
   *收尾*：26.2 已经有原生的整屏模糊（`GuiGraphicsExtractor.blurBeforeThisStratum`，
   就是 `Screen.extractBackground` 用来做原版背景模糊的那个），但它**一帧只能调一次**，
   而且语义是"模糊这一层之前的所有内容"。想给面板加局部模糊，得走 26.2 的后处理管线，
   或者干脆改成半透明遮罩。

## 三·补、GUI 绘制顺序：为什么 MiniHUD 的信息行永远压在界面之上

反汇编 26.2 的 `Gui.extractRenderState(DeltaTracker, boolean renderHud, boolean renderScreen)`
可以确认，它是**一次调用同时负责 HUD 和当前界面**：

```
iload_2 ifeq → 跳过 Hud.extractRenderState(...)                            // renderHud
...
iload_3 ifeq → 跳过 Screen.extractRenderStateWithTooltipAndSubtitles(...)  // renderScreen
── TAIL ──
```

而 malilib（MiniHUD / Tweakeroo / Litematica 的前置）是在这个方法的 **TAIL** 处追加自己的
覆盖层：`@Inject(method = "extractRenderState", at = @At("TAIL"))` → `runExtractGuiOverlayPost`。
它没有做任何「界面是否打开」的判断（已核对字节码），所以 **MiniHUD 的信息行画在任何 Screen 之上**。

两个推论：

- **在 Fabric 的 HUD 图层系统里调顺序没用。** `HudElementRegistry` 注册的元素在
  `Hud.extractRenderState` 内部，位置比 TAIL 早得多。想让自己的东西盖住 MiniHUD，
  只能也排到它后面。
- **不要靠 mixin 优先级去抢同一个 TAIL 插入点。** 第一版实现是在 `Gui.extractRenderState`
  上再挂一个 `@At("TAIL")`、把 `priority` 设成 800（低于 malilib 的 900）指望排到它后面 ——
  **实测不生效**。多个 mixin 在同一个 TAIL 插入点上的先后取决于 Mixin 的插入实现细节
  （插入是锚在末尾那条 RETURN 的索引上，还是锚在"尾部"语义上，结果正好相反），
  不能当作可靠依据。

### 现在的做法：换成在**调用方**补画

`GameRenderer.extract(DeltaTracker, boolean)` 里只调用一次 `Gui.extractRenderState`。
把补画挂在**这次调用返回之后**（`TopLayerRedrawMixin`），malilib 的 TAIL 注入必然已经执行完
（它在被调用方法内部），先后关系是确定的，跟优先级、跟别的 mod 都无关。

时机也安全：26.2 把一帧拆成「抽取状态」和「真正绘制」两段 —— `GameRenderer.render(DeltaTracker,
boolean)` 里先 `extract(...)`、之后才 `guiRenderer.render()`，而 `extract` 的最后一步是
`sampleDuringExtract()`。所以这时候往 `GuiGraphicsExtractor` 里追加的元素一定还在本帧的绘制
清单里，而且排在 malilib 追加的那些之后。这一点其实由用户的实测反证过：malilib 追加在 TAIL、
能盖住界面，说明"抽取阶段里后追加的 = 后画的"。

整体做法是「搬家」而不是「再画一遍」：面板是半透明的，画两遍会叠加得更不透明。搬家也顺带
保住了 tooltip 冲刷（`GuiGraphicsExtractor.extractDeferredElements` 在
`Screen.extractRenderStateWithTooltipAndSubtitles` 里面）和背景层序。

用 `@WrapOperation` 而不是 `@Local` 抓 `GuiGraphicsExtractor`：可以直接从实参拿到它，
不依赖局部变量表在别的注入点是否还在作用域内。

### 防呆：推迟之前先确认补画在工作

推迟绘制的含义是"这帧先不画、等会儿补"。万一补画那一钩子没装上，推迟下去就没人补，
表现是**打开聊天面板整个界面不见**——比"被 MiniHUD 压住"严重得多。所以
`TopLayerDraw` 里有个握手：补画钩子每跑一帧置位一次，只有置位之后才允许推迟；
第一帧照原样画。这样最坏情况退化成"没有改进"，而不是"界面丢失"。

### 顺带修掉的一个端口 bug

`InGameHudMixin` 原先在 `Gui.extractRenderState` 的 **HEAD** 处 `ci.cancel()`。
由上面那段字节码可知，这会把**当前界面一起取消**（连 `GuiRenderState.reset()` 都不执行），
也就是打开 E33Chat 的配置界面时界面根本不画。现已改成只拦 `Hud.extractRenderState`
那一次调用 —— 这才是 `HudVisibility` 注释里一直说的「只跳 HUD 层」。

### 同类端口 bug：手动调 `extractBackground` → 「Can only blur once per frame」

`PanelCropScreen`（选完自定义背景图之后的裁剪界面）和 `PlayerProfileScreen` 在
`extractRenderState` 里又手动调了一次 `extractBackground`。1.21.1 时代 `Screen.render`
**不**自动画背景，所以各界面得自己调；26.2 改了：

```
Screen.extractRenderStateWithTooltipAndSubtitles
    nextStratum() -> extractBackground(...)     // 背景在这里已经画过一次
    nextStratum() -> extractRenderState(...)     // 之后才进到界面自己的实现
```

再调一次会让背景模糊重复执行，直接抛
`IllegalStateException: Can only blur once per frame`。表现就是「一选自定义背景贴图就崩」。

两处的手动调用都已删掉（背景仍由原版那一次画出来，观感不变）。另外三个界面
（`ChatBubbleConfigScreen` / `ServerConfigScreen` / `BedScreen`）是**覆写**
`extractBackground` 成 no-op，那是正常做法，不受影响。

> 判别方法：界面里**覆写** `extractBackground` 是对的；在 `extractRenderState` 里
> **调用** `extractBackground` 是错的。

## 四、本机构建环境（与仓库无关）

这台机器有两个 TLS 问题，会让 Gradle 连不上 Maven：

1. **JDK 缺 Let's Encrypt 2026 新根 `Root YR`**（`O=ISRG`）。
   JDK 自带的 cacerts（144 条）只有旧根 `ISRG Root X1`，链到 `Root YR` 就断。
   Windows 证书存储里有它，所以非 JVM 客户端（PowerShell / curl / pip）不受影响。
   处理：把 JDK 复制一份补上该根（`C:\Users\38759\Documents\My_mods\_jdk25`），
   `JAVA_HOME` 指它。**没有**改 Program Files 下的原 JDK（需要管理员，且会影响全局）。

2. **`repo.maven.apache.org` 被本地中间盒劫持**，端上给的是自签证书
   `O=redirect-cnzz`，不是 Google Trust Services 的正式链。同样只有 JVM 受影响
   （`repo1.maven.org`、`plugins.gradle.org`、`maven.aliyun.com` 都正常）。
   处理：用 `_build.init.gradle`（`-I` 注入）把 Maven Central / Gradle 插件门户
   换成阿里云镜像，并把 `repo.maven.apache.org` 从候选仓库里**移除**
   —— 只改 URL 不够，Gradle 会按缓存里的原 URL 去取 jar。
   **没有**把那个来路不明的自签证书加进信任库：信任它等于把本机 JVM 的 TLS 降级。

两条都只在**本机**生效，仓库本身保持干净；CI 在能直连的机器上不需要它们。
本机跑完整构建：

```powershell
$env:JAVA_HOME="C:\Users\38759\Documents\My_mods\_jdk25"
cd platforms/26.2-fabric
.\gradlew.bat build -I C:\Users\38759\Documents\My_mods\_build.init.gradle
```
