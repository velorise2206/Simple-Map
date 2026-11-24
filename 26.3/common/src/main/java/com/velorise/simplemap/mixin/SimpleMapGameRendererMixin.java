package com.velorise.simplemap.mixin;

import com.velorise.simplemap.client.gui.MapScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * The fullscreen map is completely opaque. On 26.3, GameRenderer.extract() sets
 * gameRenderState.shouldRenderLevel, which GameRenderer.render() reads without
 * parameters. Clearing renderLevel at extract() skips both level extraction and
 * 3D level rendering while MapScreen is open.
 */
@Mixin(GameRenderer.class)
public abstract class SimpleMapGameRendererMixin {
    @ModifyVariable(method = "extract", at = @At("HEAD"),
            argsOnly = true, ordinal = 0, require = 0)
    private boolean simplemap$skipExtractLevelBehindFullscreenMap(boolean renderLevel) {
        Minecraft minecraft = Minecraft.getInstance();
        boolean skip = renderLevel && (minecraft.gui != null && minecraft.gui.screen() instanceof MapScreen);
        return renderLevel && !skip;
    }
}
