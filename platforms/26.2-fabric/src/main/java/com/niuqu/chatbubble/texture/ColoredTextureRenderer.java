package com.niuqu.chatbubble.texture;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;

/**
 * 带整体透明度 / tint 的纹理渲染。
 *
 * <p>【26.2 修正】移植时这里曾判定"26.2 的 blit 没有颜色参数，所以 alpha/tint 做不到了"，
 * 并把 alpha 直接丢掉（只照原样 blit）。<b>那个判定是错的</b>，颜色参数一直都在：
 *
 * <pre>
 *   blit(pipeline, id, x, y, u, v, w, h, texW, texH)                              // 10
 *   blit(pipeline, id, x, y, u, v, w, h, texW, texH, color)                       // 11
 *   blit(pipeline, id, x, y, u, v, w, h, regionW, regionH, texW, texH)            // 12
 *   blit(pipeline, id, x, y, u, v, w, h, regionW, regionH, texW, texH, color)     // 13
 * </pre>
 *
 * <p>怎么确认的（原版字节码）：11 参重载的方法体把自己的第 11 个参数原样转发给 13 参重载的
 * <b>最后一个</b>参数；而 {@code BlitRenderState} 的字段里，除 x0/y0/x1/y1 之外唯一的 int
 * 就是 {@code color}。所以那个多出来的 int 就是 ARGB 颜色，且排在最后。
 *
 * <p>于是恢复成"原调用 + 追加一个颜色参数"，几何与 UV 完全不变，只是补上顶点色。
 * 颜色按通道相乘：{@code 0xFFFFFFFF} = 原样，alpha 小于 255 就是半透明。
 */
public final class ColoredTextureRenderer {

    private ColoredTextureRenderer() {}

    /** 把 [0,1] 的 alpha 变成 ARGB 里的白色带 alpha；白色不改变纹理色相。 */
    private static int whiteWithAlpha(float alpha) {
        int a = Math.round(Math.max(0f, Math.min(1f, alpha)) * 255f);
        return (a << 24) | 0x00FFFFFF;
    }

    /** 整张纹理铺满 (x,y,w,h)，alpha 生效。 */
    public static void drawWithAlpha(GuiGraphicsExtractor g, Identifier tex,
                                     int x, int y, int w, int h, float alpha) {
        if (w <= 0 || h <= 0) return;
        if (alpha <= 0.003f) return;
        g.blit(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED, tex,
            x, y, 0f, 0f, w, h, w, h, w, h, whiteWithAlpha(alpha));
    }

    /** 整张纹理铺满 (x,y,w,h)，tint（含其 alpha 通道）生效。 */
    public static void drawTinted(GuiGraphicsExtractor g, Identifier tex,
                                  int x, int y, int w, int h, int argb) {
        if (w <= 0 || h <= 0) return;
        if ((argb >>> 24) <= 0) return;
        g.blit(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED, tex,
            x, y, 0f, 0f, w, h, w, h, w, h, argb);
    }

    /** 带 UV 采样的纹理渲染（区域语义与旧版一致），alpha 生效。 */
    public static void drawWithAlpha(GuiGraphicsExtractor g, Identifier tex,
                                     int x, int y, int w, int h,
                                     float u, float v, int regionW, int regionH,
                                     int texW, int texH, float alpha) {
        if (w <= 0 || h <= 0) return;
        if (alpha <= 0.003f) return;
        if (texW <= 0 || texH <= 0) return;
        g.blit(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED, tex,
            x, y, u, v, w, h, regionW, regionH, texW, texH, whiteWithAlpha(alpha));
    }
}
