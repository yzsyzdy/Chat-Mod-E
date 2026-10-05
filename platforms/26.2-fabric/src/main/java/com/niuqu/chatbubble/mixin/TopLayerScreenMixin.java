package com.niuqu.chatbubble.mixin;

import com.niuqu.chatbubble.compat.MalilibCompat;
import com.niuqu.chatbubble.render.TopLayerDraw;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 把 E33Chat 界面（{@link com.niuqu.chatbubble.render.TopLayerScreen}）**挪出**原版这次
 * Screen 抽取，改由 {@link TopLayerRedrawMixin} 在 {@code Gui.extractRenderState} 整个返回之后补画。
 *
 * <p>为什么要挪（反汇编 26.2 原版得到）：{@code Gui.extractRenderState(DeltaTracker, boolean,
 * boolean)} 一次调用同时负责 HUD 和当前界面，
 *
 * <pre>
 *   iload_2 ifeq → 跳过 Hud.extractRenderState(...)                            // renderHud
 *   ...
 *   iload_3 ifeq → 跳过 Screen.extractRenderStateWithTooltipAndSubtitles(...)   // renderScreen
 *   ── TAIL ──  malilib(MiniHUD 等)在这里追加覆盖层
 * </pre>
 *
 * <p>也就是说面板原本画在这一步，之后 malilib 才追加 MiniHUD 的信息行 —— 面板必然被压住。
 *
 * <p>【为什么补画不在本 mixin 里做】曾经试过在本方法上再挂一个 {@code @At("TAIL")} 注入，
 * 靠 {@code priority} 低于 malilib 的 900 来排到它后面。这个做法不成立：多个 mixin 在同一个
 * TAIL 插入点上谁先谁后取决于 Mixin 的插入实现细节，实测没生效。现在改成在**调用方**
 * （{@code GameRenderer.extract}）里、等 {@code Gui.extractRenderState} 整个返回之后再追加，
 * 这个先后是确定的，跟 mixin 优先级无关。
 */
@Mixin(Gui.class)
public class TopLayerScreenMixin {

    /**
     * 每帧开头先清暂存，保证「登记」和「补画」只可能发生在同一次
     * {@code extractRenderState} 调用之内。
     *
     * <p>不这么做的话有个隐蔽的兜底漏洞：万一某帧在登记之后、补画之前抛异常，
     * 那一帧就没人清暂存；而后面若出现"没有界面打开"的帧（此时原版根本不会调用
     * 被包裹的那个方法，也就不会重置），补画那一步仍会把上一帧那个已经关掉的界面又画出来。
     */
    @Inject(method = "extractRenderState", at = @At("HEAD"))
    private void e33chat$resetTopLayerSlot(DeltaTracker tick, boolean renderHud, boolean renderScreen,
                                           CallbackInfo ci) {
        TopLayerDraw.clear();
    }

    /**
     * 拦下原版对当前界面的这一整次抽取（含 nextStratum / 背景 / 内容 / tooltip 冲刷）。
     *
     * <p>属于 {@link com.niuqu.chatbubble.render.TopLayerScreen} 且装了 malilib 时不在这里画，
     * 只登记下来，等 {@link TopLayerRedrawMixin} 在更晚的位置补画。
     *
     * <p>用 {@link WrapOperation} 而不是 {@code @Local} 抓 {@link GuiGraphicsExtractor}：
     * 可以直接从实参拿到它，不依赖局部变量表在别的注入点是否还在作用域内。
     */
    @WrapOperation(method = "extractRenderState",
        at = @At(value = "INVOKE",
                 target = "Lnet/minecraft/client/gui/screens/Screen;extractRenderStateWithTooltipAndSubtitles"
                     + "(Lnet/minecraft/client/gui/GuiGraphicsExtractor;IIF)V"))
    private void e33chat$deferScreenToTopLayer(Screen screen, GuiGraphicsExtractor g, int mouseX, int mouseY,
                                               float delta, Operation<Void> original) {
        if (!MalilibCompat.shouldDefer(screen)) {
            original.call(screen, g, mouseX, mouseY, delta);
            return;
        }

        TopLayerDraw.defer(screen, g, mouseX, mouseY, delta);
    }
}
