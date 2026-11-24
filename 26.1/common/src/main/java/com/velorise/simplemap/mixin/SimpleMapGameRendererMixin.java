package com.velorise.simplemap.mixin;

import com.velorise.simplemap.client.gui.MapScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * The fullscreen map is completely opaque. On 26.1, GameRenderer splits each
 * frame into extract() and render(), and LevelRenderer resets LevelRenderState
 * and ParticlesRenderState only inside renderLevel(). Both extract and render
 * must skip the 3D level together so extracted entity, block-entity, and
 * particle render states do not accumulate across frames while MapScreen is open.
 */
@Mixin(GameRenderer.class)
public abstract class SimpleMapGameRendererMixin {
    @Unique
    private boolean simplemap$skippedLevelExtract;

    @ModifyVariable(method = "extract", at = @At("HEAD"),
            argsOnly = true, ordinal = 0, require = 0)
    private boolean simplemap$skipExtractLevelBehindFullscreenMap(boolean renderLevel) {
        Minecraft minecraft = Minecraft.getInstance();
        boolean skip = renderLevel && (minecraft.screen instanceof MapScreen);
        this.simplemap$skippedLevelExtract = skip;
        return renderLevel && !skip;
    }

    @ModifyVariable(method = "render", at = @At("HEAD"),
            argsOnly = true, ordinal = 0, require = 0)
    private boolean simplemap$skipLevelBehindFullscreenMap(boolean renderLevel) {
        boolean skip = this.simplemap$skippedLevelExtract;
        this.simplemap$skippedLevelExtract = false;
        return renderLevel && !skip;
    }
}
