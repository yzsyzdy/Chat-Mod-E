package com.niuqu.chatbubble.mixin;

import com.niuqu.chatbubble.render.HudVisibility;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Hides the vanilla HUD while a translucent E33Chat screen is open (2.4.11).
 *
 * Previously the screens set {@code options.hudHidden}, which is the F1 flag —
 * {@code GameRenderer} also gates first-person hands/held items on it, so
 * opening the chat panel made the hand disappear. Cancelling the HUD render
 * here skips only the HUD layer.
 *
 * 【26.2 注】Gui.render(GuiGraphicsExtractor, DeltaTracker) 没了，HUD 的入口改成
 * {@code extractRenderState(DeltaTracker, boolean, boolean)} —— 同样是「收集本帧要画的
 * HUD 状态」，在这里 cancel 就等于整层 HUD 不画，语义与旧版一致。
 */
@Mixin(Gui.class)
public class InGameHudMixin {

    @Inject(method = "extractRenderState", at = @At("HEAD"), cancellable = true)
    private void e33chat$hideHudForTranslucentScreens(DeltaTracker tickCounter, boolean renderHud,
                                                      boolean renderScreen, CallbackInfo ci) {
        if (HudVisibility.shouldHideHud()) ci.cancel();
    }
}
