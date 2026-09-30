package com.niuqu.chatbubble.render;
import com.niuqu.chatbubble.ChatBubbleMod;
import com.mojang.blaze3d.shaders.Uniform;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.render.*;
import net.minecraft.client.renderer.ShaderInstance;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import org.slf4j.LoggerFactory;

public class RoundRectRenderer {
    private static ShaderInstance shader;
    private static boolean loadAttempted;

    private static ShaderInstance getShader() {
        if (!loadAttempted) {
            loadAttempted = true;
            try {
                shader = new ShaderInstance(
                    Minecraft.getInstance().getResourceManager(),
                    "rendertype_round_rect",
                    DefaultVertexFormat.POSITION_COLOR);
            } catch (Exception e) {
                LoggerFactory.getLogger("e33chat")
                    .warn("[e33chat] round rect shader failed to load, falling back to square corners", e);
            }
        }
        return shader;
    }

    public static void resetShader() {
        loadAttempted = false;
        shader = null;
    }

    public static void fill(GuiGraphics g, int x1, int y1, int x2, int y2, float radius, int argb) {
        ShaderInstance sh = getShader();
        radius = Math.min(radius, Math.min(x2 - x1, y2 - y1) / 2f);
        if (sh == null || radius <= 0) {
            g.fill(x1, y1, x2, y2, argb);
            return;
        }
        g.flush();

        Matrix4f pose = g.pose().last().pose();
        // Vertices are pre-multiplied by the pose (vertex() below), so the SDF rect must
        // follow the pose's uniform scale too — message ZOOM animation and bubble_size
        // callers may scale. GUI poses never rotate, m00 is the scale.
        float poseScale = Math.abs(pose.m00());
        Vector4f center = pose.transform(new Vector4f((x1 + x2) / 2f, (y1 + y2) / 2f, 0f, 1f));

        Uniform uRect = sh.getUniform("u_Rect");
        Uniform uRadius = sh.getUniform("u_Radius");
        if (uRect == null || uRadius == null) {
            g.fill(x1, y1, x2, y2, argb);
            return;
        }
        uRect.set(0, center.x);
        uRect.set(1, center.y);
        uRect.set(2, (x2 - x1) / 2f * poseScale);
        uRect.set(3, (y2 - y1) / 2f * poseScale);
        uRadius.set(0, radius * poseScale);

        float a = (argb >>> 24) / 255f;
        float r = (argb >> 16 & 0xFF) / 255f;
        float gr = (argb >> 8 & 0xFF) / 255f;
        float b = (argb & 0xFF) / 255f;

        // Save/restore the caller's blend state (GL hygiene, same as
        // ColoredTextureRenderer): an unconditional disableBlend at the end
        // would corrupt a caller that renders inside blend-off.
        boolean blendWasEnabled = org.lwjgl.opengl.GL11.glIsEnabled(org.lwjgl.opengl.GL11.GL_BLEND);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(() -> sh);

        BufferBuilder bb = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        bb.addVertex(pose, x1, y1, 0).setColor(r, gr, b, a);
        bb.addVertex(pose, x1, y2, 0).setColor(r, gr, b, a);
        bb.addVertex(pose, x2, y2, 0).setColor(r, gr, b, a);
        bb.addVertex(pose, x2, y1, 0).setColor(r, gr, b, a);
        BufferUploader.drawWithShader(bb.buildOrThrow());

        if (!blendWasEnabled) RenderSystem.disableBlend();
    }

}
