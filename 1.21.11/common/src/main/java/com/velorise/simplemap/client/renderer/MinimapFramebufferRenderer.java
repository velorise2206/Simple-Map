package com.velorise.simplemap.client.renderer;

import com.velorise.simplemap.client.MapConfig;
import com.velorise.simplemap.client.MapManager;
import com.velorise.simplemap.client.MapRenderer;
import net.minecraft.client.gui.GuiGraphics;

public final class MinimapFramebufferRenderer {
    private static final MinimapFramebufferRenderer INSTANCE = new MinimapFramebufferRenderer();
    public static MinimapFramebufferRenderer getInstance() { return INSTANCE; }
    public HudPoint projectWorldToHud(double worldX, double worldZ) { return null; }

    public record HudPoint(float x, float y) { }

    public void invalidate() {}

    public static float demandCoverageFactor(boolean rotateWithPlayer) {
        return rotateWithPlayer ? 1.45f : 1.0f;
    }

    public boolean render(GuiGraphics guiGraphics, int x, int y, int size,
            double centerX, double centerZ, float pixelsPerBlock,
            boolean rotateWithPlayer, float partialTick) {
        return false;
    }

    public void destroy() {}
    public void resetFailureState() {}

    public Snapshot snapshot() {
        return new Snapshot(0L, 0L, 0L, 0L, 0L, 0, 0, true);
    }

    public record Snapshot(long redrawFrames, long reuseFrames,
            long coalescedFrames, long fallbackFrames, long reallocations,
            int width, int height, boolean disabled) { }
}
