package com.niuqu.chatbubble.render;

/**
 * 标记接口：这个界面要画在 malilib 的覆盖层（MiniHUD 的信息行、Tweakeroo / Litematica
 * 的 HUD 之类）**之上**。
 *
 * <p>为什么需要单独标记而不是"所有 E33Chat 界面都算"：这套机制会把界面的**整个**
 * {@code extractRenderStateWithTooltipAndSubtitles} 调用从原来的位置搬到
 * {@code Gui.extractRenderState} 的 TAIL 之后，连背景和 tooltip 的层序一起变。
 * 对面板类界面这是想要的；但将来说不定有哪个界面反而需要待在 HUD 底下，
 * 所以做成显式选择，加一个界面就多写一个 {@code implements}。
 *
 * <p>实际搬运在 {@code com.niuqu.chatbubble.mixin.TopLayerScreenMixin}，
 * 触发条件见 {@code com.niuqu.chatbubble.compat.MalilibCompat#shouldDefer}。
 */
public interface TopLayerScreen {
}
