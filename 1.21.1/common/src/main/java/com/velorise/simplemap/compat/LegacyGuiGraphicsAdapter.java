package com.velorise.simplemap.compat;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/**
 * Minecraft 1.21.1 GuiGraphics implementation of {@link MapGuiAdapter}.
 */
public final class LegacyGuiGraphicsAdapter implements MapGuiAdapter {
    private final GuiGraphics graphics;
    private final Font font;

    public LegacyGuiGraphicsAdapter(GuiGraphics graphics) {
        this.graphics = graphics;
        this.font = Minecraft.getInstance().font;
    }

    public GuiGraphics unwrap() {
        return graphics;
    }

    @Override
    public void pushTransform() {
        graphics.pose().pushPose();
    }

    @Override
    public void popTransform() {
        graphics.pose().popPose();
    }

    @Override
    public void translate(float x, float y) {
        graphics.pose().translate(x, y, 0.0f);
    }

    @Override
    public void scale(float sx, float sy) {
        graphics.pose().scale(sx, sy, 1.0f);
    }

    @Override
    public void rotateZ(float radians) {
        graphics.pose().mulPose(Axis.ZP.rotation(radians));
    }

    @Override
    public void enableScissor(int x0, int y0, int x1, int y1) {
        graphics.enableScissor(x0, y0, x1, y1);
    }

    @Override
    public void disableScissor() {
        graphics.disableScissor();
    }

    @Override
    public void fill(int x0, int y0, int x1, int y1, int argb) {
        graphics.fill(x0, y0, x1, y1, argb);
    }

    @Override
    public void fillOutline(int x0, int y0, int x1, int y1, int argb) {
        graphics.renderOutline(x0, y0, x1 - x0, y1 - y0, argb);
    }

    @Override
    public void blitTexture(String textureId, int x, int y, int width, int height,
                            float u, float v, int regionW, int regionH, int texW, int texH, int tintArgb) {
        ResourceLocation location = ResourceLocation.parse(textureId);
        float a = ((tintArgb >>> 24) & 0xFF) / 255.0f;
        float r = ((tintArgb >>> 16) & 0xFF) / 255.0f;
        float g = ((tintArgb >>> 8) & 0xFF) / 255.0f;
        float b = (tintArgb & 0xFF) / 255.0f;
        RenderSystem.setShaderColor(r, g, b, a);
        graphics.blit(location, x, y, width, height, u, v, regionW, regionH, texW, texH);
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
    }

    @Override
    public void drawString(String text, int x, int y, int argb, boolean shadow) {
        graphics.drawString(font, text, x, y, argb, shadow);
    }

    @Override
    public void drawComponent(Component text, int x, int y, int argb, boolean shadow) {
        graphics.drawString(font, text, x, y, argb, shadow);
    }

    @Override
    public int textWidth(String text) {
        return font.width(text);
    }

    @Override
    public int textWidth(Component text) {
        return font.width(text);
    }

    @Override
    public void flushIfImmediate() {
        graphics.flush();
    }
}
