# 26.2 端口待办与已查明的新 API 差异

本文件是升级到 Minecraft 26.2 过程中查到的**事实清单**，不是计划书：每条都来自
26.2 的 jar（`~/.gradle/caches/fabric-loom/26.2/minecraft-merged.jar`）或迁移编译器的
真实报错，查完即记，免得下次重新踩。

## 已经改完的（机械部分，47 个文件）

| 旧（1.21.1 / Mojang 名） | 新（26.2） | 说明 |
|---|---|---|
| `net.minecraft.resources.ResourceLocation` | `net.minecraft.resources.Identifier` | **类名改回 Identifier**，工厂方法 `fromNamespaceAndPath` / `parse` / `tryParse` 不变 |
| `net.minecraft.client.gui.GuiGraphics` | `net.minecraft.client.gui.GuiGraphicsExtractor` | 名字变了，`fill` / `fillGradient` / `enableScissor` / `blit` / `text` / `blitSprite` 等主要方法同名 |
| `net.minecraft.Util` | `net.minecraft.util.Util` | 换了包 |
| `net.minecraft.client.MinecraftClient` | `net.minecraft.client.Minecraft` | 本来就已经是 `Minecraft`，迁移器漏改 |
| `modCompileOnly`（Loom） | `compileOnly` | Loom 1.18 去掉了 `mod*` 依赖族（无映射可 remap） |
| `remapJar`（Loom 任务） | 无此任务 | 26.2 不混淆，`jar` 就是成品；minotaur 由默认值取无 classifier 的 jar |

## 还没改的（按难度分组）

### A. 一行级修正

- `ChatBubbleClientSetup.java`：`HudRenderCallback`（Fabric 已删除）→
  `net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry` +
  `HudElement#extractRenderState(GuiGraphicsExtractor, DeltaTracker)`；
  定位可用 `VanillaHudElements.CHAT` 配 `attachElementAfter`。
- `mixin/MinecraftClientMixin.java`：类名被批量改成 `MinecraftMixin`，与文件名不符 ——
  改回 `MinecraftClientMixin`（mixin 配置里也按这个名字引用）。
- `render/Appearance.java`、`ui/ChatSettingsMenu.java`：`import com.niuqu.chatbubble.render.ChatBubbleScreen`
  是**本来就错的**路径（该类在根包 `com.niuqu.chatbubble`，另 7 个文件都 import 对了）。
- `ChatBubbleScreen.java:56`：还 import 着 `net.minecraft.text.*`（Yarn 包名，26.2 无此包）。
- `ChatBubbleScreen.java`：`net.minecraft.client.render` 包不存在（旧 `ShaderInstance` 所在）。
- `com.mojang.blaze3d.platform.GlStateManager`、`com.mojang.blaze3d.shaders.Uniform`：
  26.2 已无此类。
- `com.mojang.authlib.GameProfile#getName()/#getId()`：authlib 换了 getter
  （4 个文件 + ChatMessageStore）—— 需查 authlib 版本对应的新方法名。
- `ChatMessageStore.java:813`：`Optional<TeamColor>` 不能当 `ChatFormatting` 用（26.2 新增 TeamColor）。
- `ChatClassifier.java:177`：`ChatBubbleConfig.SYSTEM_CHAT_AS_BUBBLE` 找不到（配置项被迁移/改名）。

### B. 硬骨头：4 个文件 ~3.9k 行，手工重写渲染

26.2 把底层渲染换了：`Tesselator`、`BufferUploader`、`com.mojang.blaze3d.vertex.BufferBuilder`
的旧用法、`ShaderInstance`、`GlStateManager`、`Uniform` 都已不在或改名。
受影响：

| 文件 | 用到的旧 API |
|---|---|
| `render/RoundRectRenderer.java` | `ShaderInstance` / `Tesselator` / `BufferUploader` / `Uniform` |
| `texture/ColoredTextureRenderer.java` | `Tesselator` / `BufferUploader` |
| `render/ChatBubbleHudOverlay.java` | `RenderSystem` |
| `ChatBubbleScreen.java`（3260 行） | `RenderSystem` / `BufferBuilder` / 全部 GUI 绘制 |

**方向**：26.2 的 GUI 走「渲染状态收集」模型（`net.minecraft.client.renderer.state.gui.GuiRenderState`，
`GuiGraphicsExtractor` 里 `fill(RenderPipeline, ...)` / `blit(RenderPipeline, ...)` / `blitSprite(...)`），
自定义管线用 `com.mojang.blaze3d.pipeline.RenderPipeline` 注册，而不是自己开 shader + 手动
`BufferUploader`。资源侧现有的 `assets/minecraft/shaders/core/rendertype_round_rect.{json,vsh,fsh}`
要么改成 26.2 的管线格式，要么改用原版现成管线（例如圆角可考虑 `blitSprite` + 九宫格贴图）。

## 环境坑（与代码无关，但会挡住构建）

本机 JDK 的 `cacerts`（144 条）里没有 Let's Encrypt 2026 新根 `Root YR`，而
`repo.maven.apache.org` 又被本地中间盒用自签证书 `O=redirect-cnzz` 劫持。处理办法：

1. `Root YR` 补进一份 JDK 的 `cacerts` 副本（`~/_jdk25`），`JAVA_HOME` 指它；
2. 用 `_build.init.gradle`（`-I` 注入）把 Maven Central / Gradle 插件门户换成阿里云镜像，
   并把 `repo.maven.apache.org` 从候选仓库里**移除**（只改 URL 不够：Gradle 会按缓存里的
   原 URL 去取 jar）。

两条都只在**本机**生效，仓库本身保持干净 —— CI 在能直连的机器上不需要它们。
