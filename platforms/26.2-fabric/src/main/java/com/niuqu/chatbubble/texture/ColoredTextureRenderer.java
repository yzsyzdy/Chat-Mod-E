package com.niuqu.chatbubble.texture;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;

/**
 * 带整体透明度 / tint 的纹理渲染。
 *
 * 【26.2 的现实：这一层做不到了】
 * 旧版自己开 BufferBuilder + POSITION_TEX_COLOR 顶点、再 BufferUploader 上传，
 * 于是能在顶点色里带「纹理 × 运行时 alpha / tint」。
 * 26.2 把 GUI 渲染换成「收集渲染状态 → 统一提交」，对外只剩
 * GuiGraphicsExtractor.blit(...)，而**它的所有重载都没有颜色参数**
 * （已逐个核对描述符：只有 (Identifier,x,y,w,h,u,v,uw,uh) 与带 RenderPipeline 的变体，
 * 以及 (GpuTextureView,GpuSampler,...)，没有一处吃颜色）。
 * 想恢复 tint / 淡入，只能自己注册带颜色属性的 RenderPipeline，再构造自定义
 * GuiElementRenderState 塞进 GuiRenderState —— 那是独立的渲染管线工作，本轮没做。
 *
 * 所以这里退化成普通 blit：**纹理照画，但整体 alpha / tint 不生效**。
 * 调用点一个没动（方法签名保持不变）；受影响的观感是面板/弹层的淡入淡出与
 * 白纹理着色。已记在 docs/port-26.2-notes.md 的待收尾项里。
 */
public final class ColoredTextureRenderer {

    private ColoredTextureRenderer() {}

    /** 整张纹理铺满 (x,y,w,h)。alpha 目前不生效（见类注释）。 */
    public static void drawWithAlpha(GuiGraphicsExtractor g, Identifier tex,
                                     int x, int y, int w, int h, float alpha) {
        if (w <= 0 || h <= 0) return;
        if (alpha <= 0.003f) return;
        g.blit(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED, tex,
            x, y, 0f, 0f, w, h, w, h, w, h);
    }

    /** 整张纹理铺满 (x,y,w,h)。tint 目前不生效（见类注释）。 */
    public static void drawTinted(GuiGraphicsExtractor g, Identifier tex,
                                  int x, int y, int w, int h, int argb) {
        if (w <= 0 || h <= 0) return;
        if ((argb >>> 24) <= 0) return;
        g.blit(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED, tex,
            x, y, 0f, 0f, w, h, w, h, w, h);
    }

    /** 带 UV 采样的纹理渲染（区域语义与旧版一致）。alpha / tint 目前不生效。 */
    public static void drawWithAlpha(GuiGraphicsExtractor g, Identifier tex,
                                     int x, int y, int w, int h,
                                     float u, float v, int regionW, int regionH,
                                     int texW, int texH, float alpha) {
        if (w <= 0 || h <= 0) return;
        if (alpha <= 0.003f) return;
        if (texW <= 0 || texH <= 0) return;
        g.blit(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED, tex,
            x, y, u, v, w, h, regionW, regionH, texW, texH);
    }
}
