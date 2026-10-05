package com.niuqu.chatbubble.render;
import com.niuqu.chatbubble.ChatBubbleClientSetup;
import com.niuqu.chatbubble.store.ChatMessageStore;
import com.niuqu.chatbubble.ChatBubbleScreen;
import com.niuqu.chatbubble.ChatBubbleMod;
import com.niuqu.chatbubble.config.ChatBubbleConfig;
import com.niuqu.chatbubble.render.ChatBubbleTheme;
import com.niuqu.chatbubble.render.Appearance;

import com.mojang.blaze3d.systems.RenderSystem;
import com.niuqu.chatbubble.chat.notification.MentionNotificationBanner;
import com.niuqu.chatbubble.config.ChatBubbleConfig;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;

public class ChatBubbleHudOverlay {

    private static final int ICON_S = 16;
    private static final int SRC_U = 6;
    private static final int SRC_V = 6;
    private static final int SRC_S = 4;
    private static final int TIP_DISP = 4;

    private static ChatBubbleConfig cfg() { return ChatBubbleClientSetup.config(); }

    private static Identifier chatIconTex() {
        String theme = cfg().theme().toLowerCase();
        return Identifier.fromNamespaceAndPath("e33chat", "textures/gui/" + theme + "/chat_icon.png");
    }

    private static ChatBubbleTheme theme() {
        return "light".equalsIgnoreCase(cfg().theme()) ? ChatBubbleTheme.LIGHT : ChatBubbleTheme.DARK;
    }

    private static ChatBubbleTheme.Colors c() { return Appearance.snapshot(); }

    public static void render(GuiGraphicsExtractor g) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options == null) return;
        // F1 hides through vanilla hudHidden (InGameHud is skipped entirely).
        // F3 does not toggle hudHidden, so mirror the same "no E33Chat HUD while
        // the debug screen is open" behavior here.
        if (mc.getDebugOverlay().showDebugScreen()) return;

        g.pose().pushMatrix();
        // 26.2：pose 是 2D 的 Matrix3x2fStack，translate 只有两个分量（没有 z 分层）。
        g.pose().translate(0f, 0f);

        MentionNotificationBanner.INSTANCE.tick();
        if (mc.gui.screen() == null) {
            MentionNotificationBanner.INSTANCE.render(g,
                mc.getWindow().getGuiScaledWidth(),
                mc.getWindow().getGuiScaledHeight());
        }

        if (mc.gui.screen() != null) { g.pose().popMatrix(); return; }

        String keyName = mc.options.keyChat.getTranslatedKeyMessage().getString();
        int screenH = mc.getWindow().getGuiScaledHeight();
        int x = cfg().hudIconX();
        int iconY = screenH - ICON_S - cfg().hudIconY();
        int textY = iconY + ICON_S + 1;

        if (!cfg().hideChatIcon()) {
            drawIcon(g, x, iconY);

            if (cfg().redDotEnabled() && ChatMessageStore.getUnreadCount() > 0) {
                double wave = Math.abs(Math.sin(System.currentTimeMillis() / 300.0)) * 3;
                int tipX = x + ICON_S - TIP_DISP / 2;
                int tipY = iconY - TIP_DISP / 2 + (int) wave;
                drawScaledTip(g, tipX, tipY, TIP_DISP);
            }

            String keyDisplay = "[" + keyName + "]";
            int keyW = mc.font.width(keyDisplay);
            int keyX = keyW > ICON_S ? x : x + (ICON_S - keyW) / 2;
            g.text(mc.font, keyDisplay, keyX, textY, 0xFFFFFFFF, false);
        }

        g.pose().popMatrix();
    }

    // Fabric's HUD layer draws behind the screen batch; screens that render over
    // it re-invoke this so the banner stays visible on top
    public static void renderBannerForScreen(GuiGraphicsExtractor g) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options == null) return;
        if (mc.getDebugOverlay().showDebugScreen()) return;
        // 界面被推迟到最上层画时（TopLayerScreen + malilib），本钩子触发的时刻早于真正的
        // 界面绘制，此刻画横幅会被随后补画的面板盖住 —— 先跳过，交给
        // TopLayerDraw.drawPending() 在界面之后补画。详见 TopLayerDraw.isPending()。
        if (TopLayerDraw.isPending()) return;
        if (mc.gui.screen() instanceof ChatBubbleScreen) {
            MentionNotificationBanner.INSTANCE.render(g,
                mc.getWindow().getGuiScaledWidth(),
                mc.getWindow().getGuiScaledHeight());
        }
    }

    public static boolean isMouseOverIcon(double mx, double my) {
        if (cfg().hideChatIcon()) return false;
        Minecraft mc = Minecraft.getInstance();
        if (mc.gui.screen() != null) return false;
        int screenH = mc.getWindow().getGuiScaledHeight();
        int iconY = screenH - ICON_S - cfg().hudIconY();
        return mx >= cfg().hudIconX() && mx <= cfg().hudIconX() + ICON_S && my >= iconY && my <= iconY + ICON_S + mc.font.lineHeight + 2;
    }


    private static void drawIcon(GuiGraphicsExtractor g, int x, int y) {
        // 26.2：blit 自行解析纹理与管线。带 UV 的这版重载参数序是
        // (pipeline, tex, x, y, u, v, w, h, regionW, regionH, texW, texH)。
        g.blit(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED, chatIconTex(), x, y, 0.0F, 0.0F, ICON_S, ICON_S, ICON_S, ICON_S, ICON_S, ICON_S);
    }

    private static void drawScaledTip(GuiGraphicsExtractor g, int x, int y, int disp) {
        Identifier tex = ChatBubbleScreen.iconTex("private_tip");
        g.blit(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED, tex, x, y, (float) SRC_U, (float) SRC_V, disp, disp, SRC_S, SRC_S, 16, 16);
    }
}
