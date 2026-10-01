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
这批管线没重写，于是三处降级，代码里都留了 TODO：

1. **`RoundRectRenderer`（圆角）**
   旧版是自绘 SDF shader（`rendertype_round_rect` + `u_Rect`/`u_Radius` uniform）。
   现在退化成「主体矩形 + 四角逐行内缩的细条」近似：半径 6~8 px 时观感接近，
   但不是抗锯齿 SDF。
   *收尾*：把 `assets/minecraft/shaders/core/rendertype_round_rect.{json,vsh,fsh}`
   改成 26.2 的 UBO 形式，用 `RenderPipeline.builder()` 注册后换回 SDF。

2. **`ColoredTextureRenderer`（纹理 alpha / tint）**
   26.2 的 `GuiGraphicsExtractor.blit` **所有重载都没有颜色参数**（已逐个核对描述符），
   所以运行时 alpha 与 tint 不生效 —— 面板/弹层的淡入淡出、白纹理着色会失效。
   *收尾*：注册一个带颜色顶点属性的 `RenderPipeline`，再构造自定义
   `GuiElementRenderState` 塞进 `GuiRenderState`。

3. **`BlurRenderer`（面板背景模糊）**
   旧实现直接玩 FBO 句柄（`GL30.glGenFramebuffers` / `glBlitFramebuffer` /
   `Minecraft.getMainRenderTarget().frameBufferId`）。26.2 换成了跨后端
   （OpenGL / Vulkan）的 GPU 抽象，这条路连同 `GlStateManager` 一起没了。
   现在是空操作：面板不模糊，但也不会崩。
   *收尾*：用 26.2 的后处理管线重做，或改成半透明遮罩。

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
