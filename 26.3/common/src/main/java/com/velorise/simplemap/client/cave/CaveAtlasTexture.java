package com.velorise.simplemap.client.cave;

import com.mojang.renderpearl.backend.opengl.GlStateManager;
import com.mojang.renderpearl.backend.opengl.GlTexture;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.renderpearl.api.textures.AddressMode;
import com.mojang.renderpearl.api.textures.FilterMode;
import java.util.OptionalDouble;
import com.mojang.renderpearl.api.textures.GpuSampler;
import com.mojang.renderpearl.api.textures.GpuTexture;
import com.mojang.renderpearl.api.GpuFormat;
import net.minecraft.client.renderer.texture.AbstractTexture;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL13;

public final class CaveAtlasTexture extends AbstractTexture {
    private static GpuSampler sharedLinearMinNearestMagSampler;
    private static GpuSampler sharedNearestSampler;

    private static GpuSampler getOrCreateSharedSampler(boolean linearMin) {
        if (linearMin) {
            if (sharedLinearMinNearestMagSampler == null) {
                sharedLinearMinNearestMagSampler = RenderSystem.getDevice().createSampler(
                        AddressMode.CLAMP_TO_EDGE,
                        AddressMode.CLAMP_TO_EDGE,
                        FilterMode.LINEAR,
                        FilterMode.NEAREST,
                        1,
                        OptionalDouble.empty());
            }
            return sharedLinearMinNearestMagSampler;
        }
        if (sharedNearestSampler == null) {
            sharedNearestSampler = RenderSystem.getDevice().createSampler(
                    AddressMode.CLAMP_TO_EDGE,
                    AddressMode.CLAMP_TO_EDGE,
                    FilterMode.NEAREST,
                    FilterMode.NEAREST,
                    1,
                    OptionalDouble.empty());
        }
        return sharedNearestSampler;
    }
    private final int size;
    private final boolean linearMinification;
    private final Runnable storageAllocatedCallback;

    public CaveAtlasTexture(int size, Runnable storageAllocatedCallback) {
        this(size, false, storageAllocatedCallback);
    }

    public CaveAtlasTexture(int size, boolean linearMinification,
            Runnable storageAllocatedCallback) {
        if (size <= 0) throw new IllegalArgumentException("Atlas size must be positive");
        this.size = size;
        this.linearMinification = linearMinification;
        this.storageAllocatedCallback = storageAllocatedCallback;
    }

    public void allocateStorage() {
        RenderSystem.assertOnRenderThread();
        if (this.textureView != null) {
            this.textureView.close();
            this.textureView = null;
        }
        if (this.texture != null) {
            this.texture.close();
            this.texture = null;
        }
        this.texture = RenderSystem.getDevice().createTexture(
                "SimpleMap Atlas " + size,
                GpuTexture.USAGE_COPY_DST | GpuTexture.USAGE_TEXTURE_BINDING,
                GpuFormat.RGBA8_UNORM,
                size, size, 1, 1);
        this.textureView = RenderSystem.getDevice().createTextureView(this.texture);
        this.sampler = getOrCreateSharedSampler(linearMinification);
        // Blaze3D GpuSampler handles min/mag filtering and edge clamping cleanly
        // across both direct GlTexture and wrapped ValidationGpuTexture backends.
        if (storageAllocatedCallback != null) storageAllocatedCallback.run();
    }

    public int getId() {
        if (this.texture instanceof GlTexture glTexture) {
            return glTexture.glId();
        }
        return 0;
    }
}
