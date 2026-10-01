package com.niuqu.chatbubble.render;

/**
 * Scrollbar geometry shared by SmoothScrollPane on Fabric.
 *
 * Fabric keeps the config-screen scrollbar rendering inline (GuiGraphicsExtractor +
 * ColoredTextureRenderer), so this class carries only the pure geometry the
 * extracted pane needs; it intentionally has no render method.
 *
 * 【为什么这份是「只有几何」的版本】旧的 Forge/Neo 端另有一份带 render() 的同名类，
 * 而那个 render() 依赖 render.ChatLayout —— 那个类只在 Forge/Neo 平台存在，
 * Fabric 侧从来没有。本仓缩到单目标时曾把带 render() 的那份抄过来，于是编不过；
 * 这里换回 Fabric 原本的几何版（render() 在 Fabric 侧没有任何调用者）。
 */
public final class ChatScrollbar {
    public static final int WIDTH = 6;
    private static final int MIN_THUMB_H = 8;
    public static final int HOVER_ZONE = 20;
    private static final long FADE_MS = 1000;

    private ChatScrollbar() {}

    public static int thumbHeight(int trackH, int totalH) {
        if (totalH <= 0) return trackH;
        int h = Math.max(MIN_THUMB_H, (int) ((long) trackH * trackH / totalH));
        return Math.min(h, trackH);
    }

    public static int thumbY(int trackTop, int trackH, int thumbH, int scrollOffset, int maxScroll) {
        int travelRange = trackH - thumbH;
        if (travelRange <= 0) return trackTop;
        return trackTop + (int) ((long) scrollOffset * travelRange / maxScroll);
    }

    public static float alphaTarget(boolean inZone, boolean dragging, long lastScrollTime) {
        // lastScrollTime 由 Util.getMillis() 赋值，同钟比较
        long since = net.minecraft.util.Util.getMillis() - lastScrollTime;
        return (inZone || dragging || since < FADE_MS) ? 1f : 0f;
    }

    public static boolean isHoveringThumb(double mouseX, double mouseY,
                                          int trackX, int thumbY, int thumbH) {
        return mouseX >= trackX && mouseX < trackX + WIDTH
            && mouseY >= thumbY && mouseY < thumbY + thumbH;
    }

    public static boolean isInZone(double mouseX, int panelX, int panelW,
                                   double mouseY, int msgTop, int effectiveMsgBottom) {
        return mouseX >= panelX + panelW - HOVER_ZONE
            && mouseX <= panelX + panelW
            && mouseY >= msgTop && mouseY < effectiveMsgBottom;
    }
}
