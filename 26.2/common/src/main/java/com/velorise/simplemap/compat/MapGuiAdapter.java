package com.velorise.simplemap.compat;

import net.minecraft.network.chat.Component;

/**
 * Version-agnostic 2D GUI rendering surface.
 * Hides PoseStack (1.20.1-1.21.5) vs Matrix3x2fStack (1.21.6+) and
 * GuiGraphicsExtractor (1.20.1-1.21.11) vs GuiGraphicsExtractor (26.1+).
 */
public interface MapGuiAdapter {
    void pushTransform();
    void popTransform();
    void translate(float x, float y);
    void scale(float sx, float sy);
    void rotateZ(float radians);

    void enableScissor(int x0, int y0, int x1, int y1);
    void disableScissor();

    void fill(int x0, int y0, int x1, int y1, int argb);
    void fillOutline(int x0, int y0, int x1, int y1, int argb);

    void blitTexture(String textureId, int x, int y, int width, int height,
                     float u, float v, int regionW, int regionH, int texW, int texH, int tintArgb);

    void drawString(String text, int x, int y, int argb, boolean shadow);
    void drawComponent(Component text, int x, int y, int argb, boolean shadow);
    int textWidth(String text);
    int textWidth(Component text);

    /** Flushes immediate GuiGraphicsExtractor batches on 1.20.1/1.21.1; no-op on deferred 1.21.6+/26.x. */
    void flushIfImmediate();
}
