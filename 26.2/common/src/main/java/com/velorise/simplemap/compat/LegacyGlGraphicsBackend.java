package com.velorise.simplemap.compat;

import com.mojang.blaze3d.systems.RenderSystem;
import org.lwjgl.opengl.GL11;

import java.util.List;
import java.util.function.Consumer;

/**
 * Placeholder 1.21.11 backend stub during Phase 1 compilation bring-up.
 * Will be implemented with Blaze3D GpuDevice / GpuTextureView / RenderPipeline
 * once core compilation passes.
 */
public final class LegacyGlGraphicsBackend implements MapGraphicsBackend {
    @Override
    public int maxTextureSize() {
        RenderSystem.assertOnRenderThread();
        return GL11.glGetInteger(GL11.GL_MAX_TEXTURE_SIZE);
    }

    @Override public AtlasHandle createAtlas(String resourceKey, int width, int height, boolean linearMinFilter) { return null; }
    @Override public void uploadSubImage(AtlasHandle atlas, int dstX, int dstY, int width, int height, int[] abgrSource, int sourceStride, int sourceX, int sourceY) {}
    @Override public void destroyAtlas(AtlasHandle atlas) {}
    @Override public void drainPendingDeletions() {}
    @Override public OffscreenTarget createOffscreenTarget(int width, int height, boolean linearFilter) { return null; }
    @Override public void renderIntoTarget(OffscreenTarget target, int clearArgb, Consumer<MapGuiAdapter> pass) {}
    @Override public void destroyOffscreenTarget(OffscreenTarget target) {}
    @Override public void drawQuads(MapGuiAdapter gui, AtlasHandle atlas, List<TexturedQuad> quads) {}
    @Override public void presentRect(MapGuiAdapter gui, OffscreenTarget target, float dstX, float dstY, float dstW, float dstH, float u0, float v0, float u1, float v1) {}
}
