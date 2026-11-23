package com.velorise.simplemap.compat;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

/**
 * Central compatibility service locator for Phase 0 multi-version architecture.
 * Automatically provides 1.21.1 defaults and allows version modules (1.21.11 / 26.x)
 * to swap backends without modifying core map engine classes.
 */
public final class MapCompat {
    private static volatile MapGraphicsBackend graphicsBackend;
    private static volatile BlockSpriteSampler spriteSampler;
    private static volatile MapPlatformNet platformNet;

    private static final MapGuiAdapter NOOP_GUI = new MapGuiAdapter() {
        @Override public void pushTransform() {}
        @Override public void popTransform() {}
        @Override public void translate(float x, float y) {}
        @Override public void scale(float sx, float sy) {}
        @Override public void rotateZ(float radians) {}
        @Override public void enableScissor(int x0, int y0, int x1, int y1) {}
        @Override public void disableScissor() {}
        @Override public void fill(int x0, int y0, int x1, int y1, int argb) {}
        @Override public void fillOutline(int x0, int y0, int x1, int y1, int argb) {}
        @Override public void blitTexture(String textureId, int x, int y, int width, int height,
                                          float u, float v, int regionW, int regionH, int texW, int texH, int tintArgb) {}
        @Override public void drawString(String text, int x, int y, int argb, boolean shadow) {}
        @Override public void drawComponent(Component text, int x, int y, int argb, boolean shadow) {}
        @Override public int textWidth(String text) { return 0; }
        @Override public int textWidth(Component text) { return 0; }
        @Override public void flushIfImmediate() {}
    };

    private MapCompat() {}

    public static void configure(MapGraphicsBackend graphics,
                                 BlockSpriteSampler sprites,
                                 MapPlatformNet net) {
        if (graphics != null) graphicsBackend = graphics;
        if (sprites != null) spriteSampler = sprites;
        if (net != null) platformNet = net;
    }

    public static MapGraphicsBackend graphics() {
        MapGraphicsBackend current = graphicsBackend;
        if (current == null) {
            current = new LegacyGlGraphicsBackend();
            graphicsBackend = current;
        }
        return current;
    }

    public static BlockSpriteSampler sprites() {
        BlockSpriteSampler current = spriteSampler;
        if (current == null) {
            current = new LegacyBakedModelSpriteSampler();
            spriteSampler = current;
        }
        return current;
    }

    public static MapPlatformNet net() {
        MapPlatformNet current = platformNet;
        if (current == null) {
            current = new LegacyPlatformNetBridge();
            platformNet = current;
        }
        return current;
    }

    public static MapGuiAdapter gui(GuiGraphics graphics) {
        return graphics == null ? NOOP_GUI : new LegacyGuiGraphicsAdapter(graphics);
    }

    static MapGuiAdapter noopGui() {
        return NOOP_GUI;
    }
}
