package com.velorise.simplemap.compat;

import net.minecraft.world.level.block.state.BlockState;

/**
 * Version-agnostic block model sprite sampler.
 * Isolates BakedModel/BakedQuad (1.20.1-1.21.4) from BlockStateModel/BlockModelPart (1.21.5+/26.x).
 */
public interface BlockSpriteSampler {
    record SpriteInfo(String namespace, String path, boolean usesTint) {}

    /**
     * Samples the primary representative sprite for a block state, prioritizing:
     * UP tinted -> UP -> general/null tinted -> general/null -> side tinted -> side -> particle.
     *
     * @return SpriteInfo or null if no usable sprite is found.
     */
    SpriteInfo samplePrimarySprite(BlockState state);
}
