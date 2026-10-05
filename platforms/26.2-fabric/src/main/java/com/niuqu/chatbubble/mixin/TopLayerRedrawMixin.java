package com.niuqu.chatbubble.mixin;

import com.niuqu.chatbubble.render.TopLayerDraw;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * 在 {@code Gui.extractRenderState} **整个返回之后**把被推迟的 E33Chat 界面补画上去。
 *
 * <p>【为什么放在这里而不是放在 {@code Gui} 里】malilib（MiniHUD 等的前置）是在
 * {@code Gui.extractRenderState} 的 TAIL 处追加自己的覆盖层的，所以想盖住 MiniHUD 的信息行，
 * 就必须在那之后执行。曾经试过在同一个方法上再挂一个 {@code @At("TAIL")}、靠 {@code priority}
 * 排到 malilib 后面 —— 不成立，多个 mixin 在同一个 TAIL 插入点上的先后取决于 Mixin 的插入
 * 实现细节，实测没有生效。
 *
 * <p>现在改成挂在**调用方**这里：{@code GameRenderer.extract(DeltaTracker, boolean)} 调用完
 * {@code Gui.extractRenderState} 之后，malilib 的 TAIL 注入必然已经执行过了（它在那个方法内部），
 * 这个先后是确定的，跟优先级、跟别的 mod 都无关。
 *
 * <p>时机也是安全的：26.2 把一帧拆成「抽取状态」和「真正绘制」两段，
 * {@code GameRenderer.extract} 属于抽取段（它最后一步是 {@code sampleDuringExtract()}），
 * 所以这时候往 {@code GuiGraphicsExtractor} 里追加的元素一定还在本帧的绘制清单里，
 * 而且排在 malilib 追加的那些之后 —— 也就是画在最上面。
 */
@Mixin(GameRenderer.class)
public class TopLayerRedrawMixin {

    @WrapOperation(method = "extract(Lnet/minecraft/client/DeltaTracker;Z)V",
        at = @At(value = "INVOKE",
                 target = "Lnet/minecraft/client/gui/Gui;extractRenderState"
                     + "(Lnet/minecraft/client/DeltaTracker;ZZ)V"))
    private void e33chat$redrawDeferredScreenLast(Gui gui, DeltaTracker tick, boolean renderHud, boolean renderScreen,
                                                  Operation<Void> original) {
        // 先让原版把 HUD、覆盖层、界面、以及 malilib 的 MiniHUD 信息行全部抽完
        original.call(gui, tick, renderHud, renderScreen);
        // 告诉推迟逻辑"补画这一步确实在工作"，否则它会一直不敢推迟（见 TopLayerDraw 里的说明）
        TopLayerDraw.noteRedrawHookRan();
        // 再补画我们那一层：此刻追加 = 排在它们后面 = 画在最上面
        TopLayerDraw.drawPending();
    }
}
