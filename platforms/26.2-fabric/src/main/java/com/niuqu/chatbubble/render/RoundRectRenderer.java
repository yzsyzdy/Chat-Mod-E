package com.niuqu.chatbubble.render;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;

/**
 * 圆角矩形填充。
 *
 * 【26.2 的重写说明 —— 这里是「近似」，不是原来的 SDF】
 * 旧版走自绘 shader：自建 ShaderInstance("rendertype_round_rect") + Uniform(u_Rect/u_Radius)
 * + BufferBuilder/BufferUploader 手搓四边形，用有符号距离场画精确圆角（还自带抗锯齿）。
 * 26.2 把 GUI 渲染换成了「收集渲染状态 → 统一提交」的模型：
 *   · ShaderInstance / Uniform / Tesselator / BufferUploader 在 GUI 路径上已不可用；
 *   · 自定义管线要用 RenderPipeline.builder() 重建，着色器也要改成 26.2 的
 *     layout(std140) UBO 形式（原版 assets/minecraft/shaders/core/gui.{vsh,fsh} 是参照）。
 * 那一步没做，所以这里退化成「用原版 GUI 管线做圆角的牛顿式近似」：
 * 主体一条矩形 + 四个角各若干条逐行内缩的矩形。半径小（6~8 GUI px）时观感与 SDF 很接近，
 * 代价是每个圆角多十几条 fill（26.2 的 fill 只是往渲染状态里塞一条记录，不立即绘制，
 * 开销可接受）。
 *
 * TODO(26.2 收尾)：把 rendertype_round_rect 的着色器改成 26.2 管线形式并用
 * RenderPipeline.builder() 注册，然后换回真正的 SDF。资源文件仍在
 * assets/minecraft/shaders/core/rendertype_round_rect.{json,vsh,fsh}，尚未适配。
 */
public final class RoundRectRenderer {

    private RoundRectRenderer() {}

    /** 每个圆角的行内缩步进数。取 8 足够平滑，再多 fill 数会明显变多。 */
    private static final int CORNER_STEPS = 8;

    public static void resetShader() {
        // 不再持有着色器资源；保留这个方法给资源包重载回调调用（旧调用点不动）。
    }

    /**
     * 画一个圆角矩形。radius 会被夹到不超过短边一半（与旧实现一致）。
     * argb 走原版 GUI 管线的颜色语义。
     */
    public static void fill(GuiGraphicsExtractor g, int x1, int y1, int x2, int y2, float radius, int argb) {
        int w = x2 - x1, h = y2 - y1;
        if (w <= 0 || h <= 0) return;
        float r = Math.min(radius, Math.min(w, h) / 2f);
        if (r <= 0.5f) {
            g.fill(RenderPipelines.GUI, x1, y1, x2, y2, argb);
            return;
        }

        int ri = (int) Math.ceil(r);
        // 主体（中间那条不含圆角的矩形，上下各留出圆角高度）
        g.fill(RenderPipelines.GUI, x1, y1 + ri, x2, y2 - ri, argb);
        // 上下两条「去掉两个圆角」的横带
        g.fill(RenderPipelines.GUI, x1 + ri, y1, x2 - ri, y1 + ri, argb);
        g.fill(RenderPipelines.GUI, x1 + ri, y2 - ri, x2 - ri, y2, argb);

        // 四个角：逐行内缩的细条，逼近圆弧
        for (int i = 0; i < ri; i++) {
            // i=0 是最靠外的一行；用行中心的 dx 求圆弧内缩量
            double dy = ri - (i + 0.5);
            double inset = r - Math.sqrt(Math.max(0.0, r * r - dy * dy));
            int insetPx = (int) Math.round(inset);
            if (insetPx >= ri) continue;

            int yTop = y1 + i, yBot = y2 - 1 - i;
            // 上左 / 上右
            int lx = x1 + insetPx, rx = x2 - insetPx;
            if (rx > lx) {
                g.fill(RenderPipelines.GUI, lx, yTop, rx, yTop + 1, argb);
                g.fill(RenderPipelines.GUI, lx, yBot, rx, yBot + 1, argb);
            }
        }
    }
}
