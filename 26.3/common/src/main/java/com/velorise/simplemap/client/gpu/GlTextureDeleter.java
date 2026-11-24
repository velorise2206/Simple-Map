package com.velorise.simplemap.client.gpu;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.renderpearl.backend.opengl.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.Identifier;
import org.lwjgl.opengl.GL15;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Centralized render-thread GPU/VRAM deletion queue.
 *
 * Ensures OpenGL texture IDs, PBO buffer handles, TextureManager entries, and
 * RenderTarget framebuffers are always destroyed on the render thread without
 * leaking VRAM when caches are cleared or evicted from worker/network threads.
 */
public final class GlTextureDeleter {
    private static final ConcurrentLinkedQueue<Integer> PENDING_TEXTURE_IDS = new ConcurrentLinkedQueue<>();
    private static final ConcurrentLinkedQueue<Integer> PENDING_BUFFER_IDS = new ConcurrentLinkedQueue<>();
    private static final ConcurrentLinkedQueue<Identifier> PENDING_TEXTURES = new ConcurrentLinkedQueue<>();
    private static final ConcurrentLinkedQueue<RenderTarget> PENDING_RENDER_TARGETS = new ConcurrentLinkedQueue<>();
    private static final AtomicBoolean DRAIN_SCHEDULED = new AtomicBoolean(false);

    private GlTextureDeleter() {}

    public static void deleteTexture(int textureId) {
        if (textureId <= 0) return;
        if (RenderSystem.isOnRenderThread()) {
            GlStateManager._deleteTexture(textureId);
            return;
        }
        PENDING_TEXTURE_IDS.add(textureId);
        scheduleDrain();
    }

    public static void deleteBuffer(int bufferId) {
        if (bufferId <= 0) return;
        if (RenderSystem.isOnRenderThread()) {
            GL15.glDeleteBuffers(bufferId);
            return;
        }
        PENDING_BUFFER_IDS.add(bufferId);
        scheduleDrain();
    }

    public static void releaseTexture(Identifier location) {
        if (location == null) return;
        if (RenderSystem.isOnRenderThread()) {
            releaseTextureImmediate(location);
            return;
        }
        PENDING_TEXTURES.add(location);
        scheduleDrain();
    }

    public static void destroyRenderTarget(RenderTarget target) {
        if (target == null) return;
        if (RenderSystem.isOnRenderThread()) {
            target.destroyBuffers();
            return;
        }
        PENDING_RENDER_TARGETS.add(target);
        scheduleDrain();
    }

    public static void drain() {
        RenderSystem.assertOnRenderThread();
        DRAIN_SCHEDULED.set(false);

        Integer texId;
        while ((texId = PENDING_TEXTURE_IDS.poll()) != null) {
            if (texId > 0) GlStateManager._deleteTexture(texId);
        }

        Integer bufId;
        while ((bufId = PENDING_BUFFER_IDS.poll()) != null) {
            if (bufId > 0) GL15.glDeleteBuffers(bufId);
        }

        Identifier loc;
        while ((loc = PENDING_TEXTURES.poll()) != null) {
            releaseTextureImmediate(loc);
        }

        RenderTarget target;
        while ((target = PENDING_RENDER_TARGETS.poll()) != null) {
            target.destroyBuffers();
        }

        if (hasPending()) {
            scheduleDrain();
        }
    }

    public static int pendingCount() {
        return PENDING_TEXTURE_IDS.size()
                + PENDING_BUFFER_IDS.size()
                + PENDING_TEXTURES.size()
                + PENDING_RENDER_TARGETS.size();
    }

    private static boolean hasPending() {
        return !PENDING_TEXTURE_IDS.isEmpty()
                || !PENDING_BUFFER_IDS.isEmpty()
                || !PENDING_TEXTURES.isEmpty()
                || !PENDING_RENDER_TARGETS.isEmpty();
    }

    private static void scheduleDrain() {
        if (DRAIN_SCHEDULED.compareAndSet(false, true)) {
            Minecraft.getInstance().execute(GlTextureDeleter::drain);
        }
    }

    private static void releaseTextureImmediate(Identifier location) {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null) return;
        TextureManager manager = mc.getTextureManager();
        if (manager != null) {
            manager.release(location);
        }
    }
}
