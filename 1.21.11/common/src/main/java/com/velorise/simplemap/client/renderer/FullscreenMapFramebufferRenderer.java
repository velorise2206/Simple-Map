package com.velorise.simplemap.client.renderer;

import net.minecraft.client.Minecraft;

/**
 * Fullscreen map rendering draws directly to the main framebuffer via MapRenderer.
 * Kept as a minimal no-op compatibility shim for debug telemetry snapshots and legacy hooks.
 */
public final class FullscreenMapFramebufferRenderer {
    private static final FullscreenMapFramebufferRenderer INSTANCE = new FullscreenMapFramebufferRenderer();
    private static final Snapshot DISABLED_SNAPSHOT =
            new Snapshot(0L, 0L, 0L, 0L, 0L, 0L, 0, 0, true);

    private FullscreenMapFramebufferRenderer() {
    }

    public static FullscreenMapFramebufferRenderer getInstance() {
        return INSTANCE;
    }

    public static boolean shouldUse(Minecraft minecraft, float scale) {
        return false;
    }

    public static double demandOverscanPixels() {
        return 64.0;
    }

    public void invalidate() {
    }

    public void resetFailureState() {
    }

    public void destroy() {
    }

    public Snapshot snapshot() {
        return DISABLED_SNAPSHOT;
    }

    public record Snapshot(long renderedFrames, long redrawFrames, long reuseFrames,
            long coalescedFrames, long fallbackFrames, long reallocations,
            int width, int height, boolean disabled) {
    }
}
