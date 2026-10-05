package com.niuqu.chatbubble.compat;

import com.niuqu.chatbubble.render.TopLayerScreen;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.screens.Screen;

/**
 * malilib（MiniHUD / Tweakeroo / Litematica 等的公共前置）共存处理。
 *
 * <p>【问题】malilib 是在 {@code Gui.extractRenderState(DeltaTracker, boolean, boolean)}
 * 的 TAIL 处追加自己的覆盖层的（{@code @Inject(method = "extractRenderState", at = @At("TAIL"))}，
 * 且 malilib 的 {@code mixinPriority} 是 900，比默认的 1000 低 = 比其他 mod 后应用、后执行）。
 * 而这个方法是「一次调用同时抽取 HUD 和 Screen」，内部顺序是：
 *
 * <pre>
 *   Hud.extractRenderState(...)                                     // HUD 各图层
 *   overlay.extractRenderState(...)                                 // 加载界面之类
 *   screen.extractRenderStateWithTooltipAndSubtitles(...)           // 当前打开的界面
 *   ── TAIL ──  malilib 在这里追加 MiniHUD 的信息行
 * </pre>
 *
 * <p>结论：<b>MiniHUD 的信息行永远画在任何 Screen 之上</b>，E33Chat 的面板会被压住。
 * 这一点在 Fabric 的 HUD 图层系统（{@code HudElementRegistry}）里怎么调顺序都没用 ——
 * 那些元素在 {@code Hud.extractRenderState} 里面，位置比 TAIL 早得多。
 *
 * <p>【对策】把 E33Chat 界面的那一次 {@code Screen.extractRenderStateWithTooltipAndSubtitles}
 * 调用整体推迟到 TAIL 之后执行（见 {@code TopLayerScreenMixin}，它的 mixin priority 低于
 * malilib 的 900，因此在同一个 TAIL 插入点上排得更靠后、执行得更晚）。
 *
 * <p>注意是「整体搬家」而不是「再画一遍」：面板是半透明的（{@code panelOpacity}），
 * 画两遍会叠加成更不透明，看起来就跟玩家配的不一样了。
 */
public final class MalilibCompat {

    private static final String MOD_ID = "malilib";

    private static Boolean loaded;

    private MalilibCompat() {}

    /**
     * malilib 是否在场（也就是有没有 mod 把覆盖层画在 Screen 之上）。
     *
     * <p>结果缓存：mod 列表在运行期不会变。
     */
    public static boolean overlaysAboveScreens() {
        if (loaded == null) {
            boolean detected;
            try {
                detected = FabricLoader.getInstance().isModLoaded(MOD_ID);
            } catch (Throwable t) {
                // loader 不可用（被别的工具加载进来之类）时当成"没有"：
                // 宁可继续用原版层序，也不要因为探测失败把界面搞没
                detected = false;
            }
            loaded = detected;
        }
        return loaded;
    }

    /**
     * 这一帧的这个界面要不要推迟到最上层画。
     *
     * <p>三个条件缺一不可：界面自己要求上浮、malilib 在场（否则没人会压它，没必要动层序）、
     * 且补画钩子已经确认在工作（见 {@link com.niuqu.chatbubble.render.TopLayerDraw#redrawHookHealthy()}）。
     * 最后一条是防止"推迟了但没人补画"把界面弄丢。
     */
    public static boolean shouldDefer(Screen screen) {
        return screen instanceof TopLayerScreen
            && overlaysAboveScreens()
            && com.niuqu.chatbubble.render.TopLayerDraw.redrawHookHealthy();
    }
}
