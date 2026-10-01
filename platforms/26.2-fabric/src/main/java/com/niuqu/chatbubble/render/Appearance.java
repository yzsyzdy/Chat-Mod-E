package com.niuqu.chatbubble.render;

import com.niuqu.chatbubble.ChatBubbleClientSetup;
import com.niuqu.chatbubble.config.ChatBubbleConfig;

/**
 * 外观快照：当前主题的颜色快照（取色统一入口）+ 布局参数宿主。
 *
 * 【为什么这里比官方映射层那份多几个方法】缩到单目标时平台侧抄的是
 * layers/mapping/official 里的 Appearance，而那份缺了 Fabric 侧本来有的布局参数
 * （bubbleSizePx / bubbleScale / scaledWrapWidth / bubbleWrapWidth）—— 那批方法只在
 * 旧 Fabric 平台目录里。这里按旧 Fabric 版补回来，并按 26.2 的 record 访问器取值。
 */
public final class Appearance {
    private Appearance() {}

    /** 当前主题的颜色快照。 */
    public static ChatBubbleTheme.Colors snapshot() {
        ChatBubbleConfig cfg = ChatBubbleClientSetup.config();
        ChatBubbleTheme theme = "light".equalsIgnoreCase(cfg.theme())
            ? ChatBubbleTheme.LIGHT : ChatBubbleTheme.DARK;
        return theme.colors();
    }

    /** 消息之间的垂直间距（手改配置文件也可能越界，钳制到 UI 范围）。 */
    public static int messageGap() {
        Integer g = ChatBubbleClientSetup.config().messageGap();
        return g == null ? 6 : Math.max(0, Math.min(12, g));
    }

    /** 消息气泡头像尺寸（像素，钳制到 UI 范围）。 */
    public static int avatarSize() {
        Integer a = ChatBubbleClientSetup.config().avatarSize();
        return a == null ? 20 : Math.max(12, Math.min(32, a));
    }

    /** 气泡文字目标高度 px（钳制到 5-14，默认 9 = 原版字高）。 */
    public static int bubbleSizePx() {
        Integer s = ChatBubbleClientSetup.config().bubbleSize();
        return s == null ? 9 : Math.max(5, Math.min(14, s));
    }

    /** 指定字高下气泡缩放系数（px / 字高）。 */
    public static float bubbleScale(int fontHeight) {
        return Math.max(5, Math.min(14, bubbleSizePx())) / (float) fontHeight;
    }

    /** 指定缩放下气泡文字换行宽度（设计单位；钳制保证可读）。 */
    public static int scaledWrapWidth(int bubbleMaxW, float scale) {
        return Math.max(16, (int) (bubbleMaxW / scale));
    }

    /** 当前缩放下气泡文字换行宽度。 */
    public static int bubbleWrapWidth(int bubbleMaxW, int fontHeight) {
        return scaledWrapWidth(bubbleMaxW, bubbleScale(fontHeight));
    }
}
