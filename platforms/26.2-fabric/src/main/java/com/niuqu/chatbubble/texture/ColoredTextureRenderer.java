package com.niuqu.chatbubble.texture;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;

/**
 * 带整体透明度 / tint 的纹理渲染。
 *
 * 【26.2 的重写说明】旧版是自己开 BufferBuilder + POSITION_TEX_COLOR 顶点、再
 * BufferUploader.drawWithShader 手动上传。26.2 把 GUI 渲染换成了「收集渲染状态 →
 * 统一提交」的模型：Tesselator / BufferUploader / RenderSystem 这一套在 GUI 路径上
 * 已经不可用。等价物是 GuiGraphicsExtractor 的 blit 重载，它带一个颜色参数，
 * 正好覆盖「纹理 × 运行时 alpha / tint」这个需求。
 *
 * 【颜色为什么这样算】blit 的颜色参数按预乘 alpha 解释（RGB 已经乘过 A），
 * 所以这里用 premultiplied(r,g,b,a) 而不是直接塞 argb。
 */
public final class ColoredTextureRenderer {

    private ColoredTextureRenderer() {}

    /** 预乘 alpha：blit 的颜色参数是 RGB 已乘 A 的形式。 */
    private static int premultiplied(int r, int g, int b, float alpha) {
        int a = Math.max(0, Math.min(255, Math.round(alpha * 255f)));
        int pr = Math.round(r * alpha);
        int pg = Math.round(g * alpha);
        int pb = Math.round(b * alpha);
        return (a << 24) | (pr << 16) | (pg << 8) | pb;
    }

    /** 整张纹理铺满 (x,y,w,h)，整体乘 alpha（面板淡入、滚动条渐显）。 */
    public static void drawWithAlpha(GuiGraphicsExtractor g, Identifier tex,
                                     int x, int y, int w, int h, float alpha) {
        if (w <= 0 || h <= 0 || alpha <= 0.003f) return;
        g.blit(tex, x, y, x + w, y + h, 0f, 0f, 1f, 1f,
            premultiplied(255, 255, 255, alpha));
    }

    /** 整张纹理铺满 (x,y,w,h)，乘 tint 色（白纹理 × 主题色着色）。 */
    public static void drawTinted(GuiGraphicsExtractor g, Identifier tex,
                                  int x, int y, int w, int h, int argb) {
        if (w <= 0 || h <= 0) return;
        float a = (argb >>> 24) / 255f;
        if (a <= 0.003f) return;
        int r = (argb >> 16) & 0xFF, gr = (argb >> 8) & 0xFF, b = argb & 0xFF;
        g.blit(tex, x, y, x + w, y + h, 0f, 0f, 1f, 1f, premultiplied(r, gr, b, a));
    }

    /**
     * 带 UV 采样的纹理渲染：等价旧版 drawTexture 的
     * (u, v, regionW, regionH, texW, texH) 语义，外加整体 alpha。
     * 图标与带采样区域的元素淡入走这里。
     */
    public static void drawWithAlpha(GuiGraphicsExtractor g, Identifier tex,
                                     int x, int y, int w, int h,
                                     float u, float v, int regionW, int regionH,
                                     int texW, int texH, float alpha) {
        if (w <= 0 || h <= 0 || alpha <= 0.003f) return;
        if (texW <= 0 || texH <= 0) return;
        // 26.2 的 blit 直接吃归一化 UV，不再需要调用方自己除 texW/texH 之外再做别的处理。
        float u1 = u / texW, u2 = (u + regionW) / texW;
        float v1 = v / texH, v2 = (v + regionH) / texH;
        g.blit(tex, x, y, x + w, y + h, u1, v1, u2, v2,
            premultiplied(255, 255, 255, alpha));
    }
}
