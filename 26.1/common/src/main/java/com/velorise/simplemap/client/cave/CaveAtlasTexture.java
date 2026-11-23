package com.velorise.simplemap.client.cave;

import com.mojang.blaze3d.opengl.GlStateManager;
import com.mojang.blaze3d.opengl.GlTexture;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.TextureFormat;
import net.minecraft.client.renderer.texture.AbstractTexture;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

public final class CaveAtlasTexture extends AbstractTexture {
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
                () -> "SimpleMap Atlas " + size,
                GpuTexture.USAGE_COPY_DST | GpuTexture.USAGE_TEXTURE_BINDING,
                TextureFormat.RGBA8,
                size, size, 1, 1);
        this.textureView = RenderSystem.getDevice().createTextureView(this.texture);
        int glId = getId();
        if (glId > 0) {
            GlStateManager._bindTexture(glId);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER,
                    linearMinification ? GL11.GL_LINEAR : GL11.GL_NEAREST);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL12.GL_CLAMP_TO_EDGE);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL12.GL_CLAMP_TO_EDGE);
        }
        if (storageAllocatedCallback != null) storageAllocatedCallback.run();
    }

    public int getId() {
        if (this.texture instanceof GlTexture glTexture) {
            return glTexture.glId();
        }
        return 0;
    }
}
