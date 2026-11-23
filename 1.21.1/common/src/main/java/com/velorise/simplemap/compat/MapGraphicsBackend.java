package com.velorise.simplemap.compat;

import java.util.List;
import java.util.function.Consumer;

/**
 * Low-level GPU/Texture/Framebuffer abstraction.
 * Isolates direct LWJGL OpenGL / TextureTarget / PBO calls (1.20.1-1.21.1)
 * from Blaze3D GpuDevice / GpuTextureView / RenderPipeline (1.21.5-26.3).
 */
public interface MapGraphicsBackend {
    interface AtlasHandle {
        String resourceKey();
        int width();
        int height();
        boolean isValid();
    }

    interface OffscreenTarget {
        int width();
        int height();
        boolean isValid();
    }

    record TexturedQuad(
            float x0, float y0, float x1, float y1,
            float u0, float v0, float u1, float v1,
            int argbTint) {}

    int maxTextureSize();

    AtlasHandle createAtlas(String resourceKey, int width, int height, boolean linearMinFilter);
    void uploadSubImage(AtlasHandle atlas, int dstX, int dstY, int width, int height,
                        int[] abgrSource, int sourceStride, int sourceX, int sourceY);
    void destroyAtlas(AtlasHandle atlas);
    void drainPendingDeletions();

    OffscreenTarget createOffscreenTarget(int width, int height, boolean linearFilter);
    void renderIntoTarget(OffscreenTarget target, int clearArgb, Consumer<MapGuiAdapter> pass);
    void destroyOffscreenTarget(OffscreenTarget target);

    void drawQuads(MapGuiAdapter gui, AtlasHandle atlas, List<TexturedQuad> quads);

    void presentRect(MapGuiAdapter gui, OffscreenTarget target,
                     float dstX, float dstY, float dstW, float dstH,
                     float u0, float v0, float u1, float v1);
}
