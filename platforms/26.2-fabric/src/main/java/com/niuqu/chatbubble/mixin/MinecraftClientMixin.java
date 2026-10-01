package com.niuqu.chatbubble.mixin;

import com.niuqu.chatbubble.ui.BedScreen;
import com.niuqu.chatbubble.ChatBubbleClientSetup;
import com.niuqu.chatbubble.ChatBubbleScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.InBedChatScreen;
import net.minecraft.client.gui.screens.Screen;

/**
 * 把原版聊天界面换成气泡界面。
 *
 * 【26.2 为什么从 Minecraft 挪到 Gui】旧版拦的是 {@code Minecraft.setScreen(Screen)}；
 * 26.2 把「当前界面」这个状态搬进了 {@code Minecraft.gui}，设屏入口变成
 * {@code Gui.setScreen(Screen)}，Minecraft 上已经没有 setScreen 了。
 * 继续挂在 Minecraft 上的后果不是"不生效"，而是 mixin 找不到注入目标：
 * 要么加载即抛 InjectionError 把游戏带崩，要么注入被静默跳过 —— 前者更糟。
 *
 * 这里换到 Gui 上，并且取「当前界面」改用 {@code mc.gui.screen()}。
 * 由于 cancel 之后立刻设的是 ChatBubbleScreen / BedScreen（都不是 ChatScreen /
 * InBedChatScreen），重入时会直接 return，不会自锁。
 */
@Mixin(Gui.class)
public class MinecraftClientMixin {

    @Inject(method = "setScreen", at = @At("HEAD"), cancellable = true)
    private void onSetScreen(Screen screen, CallbackInfo ci) {
        var cfg = ChatBubbleClientSetup.config();
        if (cfg == null || !cfg.enabled()) return;

        Minecraft mc = Minecraft.getInstance();

        if (screen instanceof InBedChatScreen) {
            ci.cancel();
            BedScreen.setScreenBeforeSleep(mc.gui.screen());
            mc.gui.setScreen(new BedScreen());
        } else if (screen instanceof ChatScreen chatScreen
                && !(chatScreen instanceof ChatBubbleScreen)) {
            ci.cancel();
            // 26.2 的 ChatScreen.initial 仍是 protected 字段、没有公开 getter，
            // 所以还是反射读；字段名对不上时退化成空串（不会崩）。
            mc.gui.setScreen(new ChatBubbleScreen(getChatInitialText(chatScreen)));
        }
    }

    private static String getChatInitialText(ChatScreen chatScreen) {
        return readInitialField(chatScreen);
    }

    /** ChatScreen.initial 是 protected 字段，没有公开 getter，只能反射读。 */
    private static String readInitialField(ChatScreen chatScreen) {
        try {
            java.lang.reflect.Field f = ChatScreen.class.getDeclaredField("initial");
            f.setAccessible(true);
            Object val = f.get(chatScreen);
            return val instanceof String s ? s : "";
        } catch (Throwable ignored) {
            return "";
        }
    }
}
