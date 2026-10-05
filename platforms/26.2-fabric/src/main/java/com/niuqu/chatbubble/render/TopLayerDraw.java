package com.niuqu.chatbubble.render;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;

/**
 * 一帧之内从 {@code TopLayerScreenMixin} 的 {@code @WrapOperation} 传到 TAIL 注入的暂存区。
 *
 * <p>为什么不直接在 mixin 里放静态字段：mixin 类的静态字段会被合并进目标类
 * （这里是原版的 {@code Gui}），等于往原版类里塞我们自己的状态。放到这个普通类里更干净，
 * 也不受 mixin 重载时的限制。
 *
 * <p>线程：只在渲染线程上读写，不需要同步。
 */
public final class TopLayerDraw {

    private static Screen screen;
    private static GuiGraphicsExtractor graphics;
    private static int mouseX;
    private static int mouseY;
    private static float delta;

    /**
     * 补画钩子（{@code TopLayerRedrawMixin}）至少成功跑过一帧。
     *
     * <p>【为什么必须有这个】推迟绘制的意思是"这一帧先不画，等会儿补"。万一补画那一钩子
     * 因为任何原因没装上/没跑到，推迟下去就没人补 —— 表现是**打开聊天面板整个界面不见**，
     * 比"面板被 MiniHUD 压住"严重得多。所以推迟之前先确认补画确实在工作：
     * 钩子每跑一帧就置位，只有置位之后才允许推迟。第一帧先按原样画，之后就都走新路径。
     */
    private static boolean redrawHookRan;

    private TopLayerDraw() {}

    /** 由补画钩子每帧调用。 */
    public static void noteRedrawHookRan() {
        redrawHookRan = true;
    }

    /** 补画钩子是否已经在工作；没确认之前不要推迟任何绘制。 */
    public static boolean redrawHookHealthy() {
        return redrawHookRan;
    }

    /**
     * 这一帧是否登记了「待补画」的界面（还没补画）。
     *
     * <p>给 Fabric 的 {@code ScreenEvents.afterExtract} 用。那个钩子是挂在
     * {@code Screen.extractRenderStateWithTooltipAndSubtitles} 的**调用点**上的，
     * 在本帧界面真正画出来之前就触发。界面被推迟之后，那一刻画上去的东西都会被随后
     * 补画的面板盖住 —— 所以 {@code renderBannerForScreen} 要先问一句轮没轮到自己。
     */
    public static boolean isPending() {
        return screen != null && graphics != null;
    }

    /**
     * 登记「这一帧这个界面推迟到最上层画」。
     *
     * <p>调用点每帧最多命中一次（原版一帧只抽取一个 Screen），所以顺便当重置点用。
     */
    public static void defer(Screen s, GuiGraphicsExtractor g, int mx, int my, float d) {
        screen = s;
        graphics = g;
        mouseX = mx;
        mouseY = my;
        delta = d;
    }

    /**
     * 清掉暂存。
     *
     * <p>【不能省】每次 {@code @WrapOperation} 一进来就得调一次：如果这一帧的界面不是
     * {@link TopLayerScreen}（比如玩家刚把聊天面板关掉），不重置的话 TAIL 会拿上一帧
     * 的引用把**已经关掉的界面**再画一遍。
     */
    public static void clear() {
        screen = null;
        graphics = null;
    }

    /** 把推迟的界面画出来。没有就什么都不做。 */
    public static void drawPending() {
        Screen s = screen;
        GuiGraphicsExtractor g = graphics;
        if (s == null || g == null) {
            return;
        }
        clear();
        // 走原版这一整条：nextStratum + 背景 + 内容 + tooltip 冲刷。
        // 只搬位置、不拆开，tooltip（setTooltipForNextFrame）才不用自己补冲刷。
        s.extractRenderStateWithTooltipAndSubtitles(g, mouseX, mouseY, delta);
        // 界面之上那一层也要跟着搬过来。Fabric 的 ScreenEvents.afterExtract 是在**调用点**
        // 触发的、早于这次补画，所以那一刻 renderBannerForScreen 会主动跳过（见 isPending）；
        // 真正该画它的位置是这里 —— 界面之后，才不会被面板盖住。
        // 不是 ChatBubbleScreen 时它自己会 return，这里不用判断。
        ChatBubbleHudOverlay.renderBannerForScreen(g);
    }
}
