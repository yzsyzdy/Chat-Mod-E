package com.niuqu.chatbubble.mixin;

import com.niuqu.chatbubble.render.HudVisibility;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Hud;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Hides the vanilla HUD while a translucent E33Chat screen is open (2.4.11).
 *
 * <p>History: this used to set {@code options.hudHidden} (the F1 flag), but
 * {@code GameRenderer} also gates first-person hands/held items on it, so opening
 * the chat panel made the hand disappear. The lever has to skip only the HUD layer.
 *
 * <p>【26.2 修正】原先这里是在 {@code Gui#extractRenderState} 的 HEAD 处
 * {@code ci.cancel()}。反汇编 26.2 的原版实现后确认这是**错的**：
 * {@code Gui#extractRenderState(DeltaTracker, boolean renderHud, boolean renderScreen)}
 * 是**一次调用同时负责 HUD 和当前界面**的，
 *
 * <pre>
 *   iload_2 ifeq → 跳过 Hud.extractRenderState(...)                            // renderHud
 *   ...
 *   iload_3 ifeq → 跳过 Screen.extractRenderStateWithTooltipAndSubtitles(...)  // renderScreen
 *   ── TAIL ──
 * </pre>
 *
 * <p>所以整方法 cancel 会把当前界面一并跳过（连 {@code GuiRenderState.reset()} 都不执行），
 * 结果就是打开 E33Chat 的配置界面时**界面根本不画**。正确做法是只拦
 * {@code Hud.extractRenderState} 这一次调用，这也正是 {@link HudVisibility} 注释里
 * 一直说的"只跳 HUD 层"。
 */
@Mixin(Gui.class)
public class InGameHudMixin {

    /** 需要藏 HUD 时跳过 HUD 图层本身；界面（screen）、覆盖层、状态重置都保持原样。 */
    @WrapOperation(method = "extractRenderState",
        at = @At(value = "INVOKE",
                 target = "Lnet/minecraft/client/gui/Hud;extractRenderState"
                     + "(Lnet/minecraft/client/gui/GuiGraphicsExtractor;Lnet/minecraft/client/DeltaTracker;)V"))
    private void e33chat$skipHudLayerOnly(Hud hud, GuiGraphicsExtractor g, DeltaTracker tick,
                                         Operation<Void> original) {
        if (HudVisibility.shouldHideHud()) {
            return;
        }
        original.call(hud, g, tick);
    }
}
