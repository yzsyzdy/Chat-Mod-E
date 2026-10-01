package com.niuqu.chatbubble.render;

/**
 * 面板背景模糊。
 *
 * 【26.2：暂时停用，不是删除】
 * 旧实现是「拿主帧缓冲 → 多级下采样/上采样 blit」的手写 OpenGL 后处理：
 * GlStateManager._genTexture/_texImage2D、GL30.glGenFramebuffers/glBlitFramebuffer，
 * 以及 Minecraft.getMainRenderTarget().frameBufferId 直接读 FBO 句柄。
 * 26.2 把这层换成了跨后端（OpenGL / Vulkan）的 GPU 抽象：
 *   · com.mojang.blaze3d.platform.GlStateManager 已不存在；
 *   · Minecraft.getMainRenderTarget() 也没有了；
 *   · 直接玩 FBO 句柄的字面 GL 调用会绕过抽象层，在 Vulkan 后端上必然出错。
 * 也就是说这不是「改个名字」能修的，要么改用 26.2 的后处理/渲染管线 API 重写，
 * 要么放弃这个效果。本轮先让它变成空操作：调用点保留，模糊暂时不出现，
 * 而不是留一段在 26.2 上会崩的代码。
 *
 * TODO(26.2 收尾)：用 26.2 的后处理管线重做面板模糊，或明确改成半透明遮罩。
 */
public final class BlurRenderer {

    private BlurRenderer() {}

    /** 目前是空操作：面板不会模糊（见类注释）。 */
    public static void blurPanel(int x, int y, int w, int h) {
        // intentionally empty
    }
}
