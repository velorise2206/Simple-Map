package com.velorise.simplemap.compat;

import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.velorise.simplemap.client.cave.CaveAtlasTexture;
import com.velorise.simplemap.client.gpu.CaveAtlasPboUploader;
import com.velorise.simplemap.client.gpu.GlTextureDeleter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;

import java.util.List;
import java.util.function.Consumer;

/**
 * Minecraft 1.21.1 OpenGL / Blaze3D legacy implementation of {@link MapGraphicsBackend}.
 * Wraps existing CaveAtlasTexture, CaveAtlasPboUploader, GlTextureDeleter, and TextureTarget.
 */
public final class LegacyGlGraphicsBackend implements MapGraphicsBackend {
    private final CaveAtlasPboUploader pboUploader = new CaveAtlasPboUploader();

    private static final class LegacyAtlasHandle implements AtlasHandle {
        final String key;
        final ResourceLocation location;
        final CaveAtlasTexture texture;
        final int size;
        boolean disposed;

        LegacyAtlasHandle(String key, ResourceLocation location, CaveAtlasTexture texture, int size) {
            this.key = key;
            this.location = location;
            this.texture = texture;
            this.size = size;
        }

        @Override public String resourceKey() { return key; }
        @Override public int width() { return size; }
        @Override public int height() { return size; }
        @Override public boolean isValid() { return !disposed && texture != null; }
    }

    private static final class LegacyOffscreenTarget implements OffscreenTarget {
        final TextureTarget target;
        boolean disposed;

        LegacyOffscreenTarget(TextureTarget target) {
            this.target = target;
        }

        @Override public int width() { return target.width; }
        @Override public int height() { return target.height; }
        @Override public boolean isValid() { return !disposed && target.getColorTextureId() > 0; }
    }

    @Override
    public int maxTextureSize() {
        RenderSystem.assertOnRenderThreadOrInit();
        return GL11.glGetInteger(GL11.GL_MAX_TEXTURE_SIZE);
    }

    @Override
    public AtlasHandle createAtlas(String resourceKey, int width, int height, boolean linearMinFilter) {
        RenderSystem.assertOnRenderThreadOrInit();
        int size = Math.max(width, height);
        ResourceLocation loc = ResourceLocation.parse(resourceKey);
        CaveAtlasTexture tex = new CaveAtlasTexture(size, linearMinFilter, null);
        Minecraft.getInstance().getTextureManager().register(loc, tex);
        tex.allocateStorage();
        return new LegacyAtlasHandle(resourceKey, loc, tex, size);
    }

    @Override
    public void uploadSubImage(AtlasHandle atlas, int dstX, int dstY, int width, int height,
                               int[] abgrSource, int sourceStride, int sourceX, int sourceY) {
        if (!(atlas instanceof LegacyAtlasHandle handle) || !handle.isValid()) return;
        pboUploader.upload(handle.texture.getId(), dstX, dstY, width, height,
                abgrSource, sourceStride, sourceX, sourceY);
    }

    @Override
    public void destroyAtlas(AtlasHandle atlas) {
        if (!(atlas instanceof LegacyAtlasHandle handle) || handle.disposed) return;
        handle.disposed = true;
        GlTextureDeleter.releaseTexture(handle.location);
    }

    @Override
    public void drainPendingDeletions() {
        GlTextureDeleter.drain();
    }

    @Override
    public OffscreenTarget createOffscreenTarget(int width, int height, boolean linearFilter) {
        RenderSystem.assertOnRenderThreadOrInit();
        TextureTarget target = new TextureTarget(width, height, false, Minecraft.ON_OSX);
        target.setFilterMode(linearFilter ? GL11.GL_LINEAR : GL11.GL_NEAREST);
        return new LegacyOffscreenTarget(target);
    }

    @Override
    public void renderIntoTarget(OffscreenTarget target, int clearArgb, Consumer<MapGuiAdapter> pass) {
        if (!(target instanceof LegacyOffscreenTarget handle) || !handle.isValid() || pass == null) return;
        TextureTarget rt = handle.target;
        float a = ((clearArgb >>> 24) & 0xFF) / 255.0f;
        float r = ((clearArgb >>> 16) & 0xFF) / 255.0f;
        float g = ((clearArgb >>> 8) & 0xFF) / 255.0f;
        float b = (clearArgb & 0xFF) / 255.0f;
        rt.setClearColor(r, g, b, a);
        rt.clear(Minecraft.ON_OSX);
        rt.bindWrite(true);
        try {
            pass.accept(MapCompat.noopGui());
        } finally {
            Minecraft.getInstance().getMainRenderTarget().bindWrite(true);
        }
    }

    @Override
    public void destroyOffscreenTarget(OffscreenTarget target) {
        if (!(target instanceof LegacyOffscreenTarget handle) || handle.disposed) return;
        handle.disposed = true;
        GlTextureDeleter.destroyRenderTarget(handle.target);
    }

    @Override
    public void drawQuads(MapGuiAdapter gui, AtlasHandle atlas, List<TexturedQuad> quads) {
        if (!(gui instanceof LegacyGuiGraphicsAdapter legacyGui)
                || !(atlas instanceof LegacyAtlasHandle handle)
                || !handle.isValid() || quads == null || quads.isEmpty()) {
            return;
        }
        legacyGui.flushIfImmediate();
        RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
        RenderSystem.setShaderTexture(0, handle.texture.getId());
        Matrix4f matrix = legacyGui.unwrap().pose().last().pose();
        BufferBuilder buffer = Tesselator.getInstance()
                .begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
        for (TexturedQuad q : quads) {
            int c = q.argbTint();
            buffer.addVertex(matrix, q.x0(), q.y1(), 0.0f).setUv(q.u0(), q.v1()).setColor(c);
            buffer.addVertex(matrix, q.x1(), q.y1(), 0.0f).setUv(q.u1(), q.v1()).setColor(c);
            buffer.addVertex(matrix, q.x1(), q.y0(), 0.0f).setUv(q.u1(), q.v0()).setColor(c);
            buffer.addVertex(matrix, q.x0(), q.y0(), 0.0f).setUv(q.u0(), q.v0()).setColor(c);
        }
        var mesh = buffer.build();
        if (mesh != null) {
            BufferUploader.drawWithShader(mesh);
        }
    }

    @Override
    public void presentRect(MapGuiAdapter gui, OffscreenTarget target,
                            float dstX, float dstY, float dstW, float dstH,
                            float u0, float v0, float u1, float v1) {
        if (!(gui instanceof LegacyGuiGraphicsAdapter legacyGui)
                || !(target instanceof LegacyOffscreenTarget handle)
                || !handle.isValid()) {
            return;
        }
        legacyGui.flushIfImmediate();
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderTexture(0, handle.target.getColorTextureId());
        Matrix4f matrix = legacyGui.unwrap().pose().last().pose();
        BufferBuilder buffer = Tesselator.getInstance()
                .begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        buffer.addVertex(matrix, dstX, dstY + dstH, 0.0f).setUv(u0, v0);
        buffer.addVertex(matrix, dstX + dstW, dstY + dstH, 0.0f).setUv(u1, v0);
        buffer.addVertex(matrix, dstX + dstW, dstY, 0.0f).setUv(u1, v1);
        buffer.addVertex(matrix, dstX, dstY, 0.0f).setUv(u0, v1);
        var mesh = buffer.build();
        if (mesh != null) {
            BufferUploader.drawWithShader(mesh);
        }
    }
}
