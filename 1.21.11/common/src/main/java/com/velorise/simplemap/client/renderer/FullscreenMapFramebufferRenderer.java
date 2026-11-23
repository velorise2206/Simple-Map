package com.velorise.simplemap.client.renderer;

import com.velorise.simplemap.client.MapConfig;
import com.velorise.simplemap.client.MapManager;
import com.velorise.simplemap.client.MapRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

public final class FullscreenMapFramebufferRenderer {
    private static final FullscreenMapFramebufferRenderer INSTANCE = new FullscreenMapFramebufferRenderer();
    public static FullscreenMapFramebufferRenderer getInstance() { return INSTANCE; }

    public static boolean shouldUse(Minecraft minecraft, float pixelsPerBlock) {
        return false;
    }

    public static int demandOverscanPixels() {
        return 0;
    }

    public boolean render(GuiGraphics guiGraphics, int viewportX, int viewportY,
            int width, int height, double centerX, double centerZ,
            float renderPixelsPerBlock, float displayPixelsPerBlock,
            float partialTick, boolean viewportInteracting) {
        return false;
    }

    public void invalidate() {}
    public void resetFailureState() {}
    public void destroy() {}

    public Snapshot snapshot() {
        return new Snapshot(0L, 0L, 0L, 0L, 0L, 0L, 0, 0, true);
    }

    public record Snapshot(long renderedFrames, long redrawFrames, long reuseFrames,
            long coalescedFrames, long fallbackFrames, long reallocations,
            int width, int height, boolean disabled) {
    }
}
