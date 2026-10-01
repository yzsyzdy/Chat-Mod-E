package com.niuqu.chatbubble.mixin;

import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * 让 AWT 文件对话框在接管输入期间清掉 MC 的「鼠标键按下」状态。
 *
 * 【26.2 为什么改成三个布尔而不是一个 activeButton】
 * 1.21.1 里 MouseHandler.activeButton 是个 int，把它设成 0 就等于「没有键按下」，
 * 所以旧实现只写一个 {@code @Accessor("activeButton") void setActiveButton(int)}。
 * 26.2 把它换成了 {@code private MouseButtonInfo activeButton}（记录类型），
 * 而且真正决定「哪个键被认为按着」的是三个独立的 boolean 字段。
 * 于是那个 int 访问器在运行期找不到目标字段，直接抛
 *   InvalidAccessorException: No candidates were found matching activeButton:I
 * 导致 MouseHandler 类变换失败、游戏在初始化阶段就崩。
 *
 * 现在直接对三个状态字段做 setter —— 语义更直白：清的就是「按着没松开」这件事。
 */
@Mixin(MouseHandler.class)
public interface MouseHandlerAccessor {
    @Accessor("isLeftPressed")
    void e33chat$setLeftPressed(boolean pressed);

    @Accessor("isMiddlePressed")
    void e33chat$setMiddlePressed(boolean pressed);

    @Accessor("isRightPressed")
    void e33chat$setRightPressed(boolean pressed);
}
