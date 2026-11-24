package com.velorise.simplemap.compat;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

/**
 * Minecraft 1.21.11 GuiGraphicsExtractor (Matrix3x2fStack + RenderPipelines) implementation of {@link MapGuiAdapter}.
 */
public final class LegacyGuiGraphicsAdapter implements MapGuiAdapter {
    private final GuiGraphicsExtractor graphics;
    private final Font font;

    public LegacyGuiGraphicsAdapter(GuiGraphicsExtractor graphics) {
        this.graphics = graphics;
        this.font = Minecraft.getInstance().font;
    }

    public GuiGraphicsExtractor unwrap() {
        return graphics;
    }

    @Override public void pushTransform() { graphics.pose().pushMatrix(); }
    @Override public void popTransform() { graphics.pose().popMatrix(); }
    @Override public void translate(float x, float y) { graphics.pose().translate(x, y); }
    @Override public void scale(float sx, float sy) { graphics.pose().scale(sx, sy); }
    @Override public void rotateZ(float radians) { graphics.pose().rotate(radians); }

    @Override public void enableScissor(int x0, int y0, int x1, int y1) { graphics.enableScissor(x0, y0, x1, y1); }
    @Override public void disableScissor() { graphics.disableScissor(); }

    @Override public void fill(int x0, int y0, int x1, int y1, int argb) { graphics.fill(x0, y0, x1, y1, argb); }
    @Override public void fillOutline(int x0, int y0, int x1, int y1, int argb) {
        graphics.outline(x0, y0, x1 - x0, y1 - y0, argb);
    }

    @Override
    public void blitTexture(String textureId, int x, int y, int width, int height,
                            float u, float v, int regionW, int regionH, int texW, int texH, int tintArgb) {
        Identifier location = Identifier.parse(textureId);
        graphics.blit(RenderPipelines.GUI_TEXTURED, location, x, y, u, v, width, height, regionW, regionH, texW, texH, tintArgb);
    }

    @Override public void drawString(String text, int x, int y, int argb, boolean shadow) {
        graphics.text(font, text, x, y, argb, shadow);
    }

    @Override public void drawComponent(Component text, int x, int y, int argb, boolean shadow) {
        graphics.text(font, text, x, y, argb, shadow);
    }

    @Override public int textWidth(String text) { return font.width(text); }
    @Override public int textWidth(Component text) { return font.width(text); }

    @Override
    public void flushIfImmediate() {
        // 1.21.11 GuiGraphicsExtractor uses deferred GuiRenderState; explicit flush is no longer needed.
    }
}
