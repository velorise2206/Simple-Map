package com.velorise.simplemap.compat;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

/**
 * Minecraft 1.21.1 BakedModel implementation of {@link BlockSpriteSampler}.
 */
public final class LegacyBakedModelSpriteSampler implements BlockSpriteSampler {
    private static final Direction[] QUAD_DIRECTIONS = {
            Direction.UP, Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST
    };

    @Override
    public SpriteInfo samplePrimarySprite(BlockState state) {
        if (state == null) return null;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null || minecraft.getBlockRenderer() == null) return null;

        BakedModel bakedModel = minecraft.getBlockRenderer().getBlockModel(state);
        if (bakedModel == null) return null;

        RandomSource random = RandomSource.create(42L);
        boolean upOrNullTinted = false;
        boolean anyQuadTinted = false;
        boolean hasUpQuads = false;
        TextureAtlasSprite upTintedSprite = null;
        TextureAtlasSprite upSprite = null;
        TextureAtlasSprite nullTintedSprite = null;
        TextureAtlasSprite nullSprite = null;
        TextureAtlasSprite sideTintedSprite = null;
        TextureAtlasSprite sideSprite = null;

        try {
            List<BakedQuad> upQuads = bakedModel.getQuads(state, Direction.UP, random);
            if (upQuads != null && !upQuads.isEmpty()) {
                hasUpQuads = true;
                for (BakedQuad quad : upQuads) {
                    if (quad == null) continue;
                    TextureAtlasSprite sprite = quad.getSprite();
                    if (upSprite == null && isUsableSprite(sprite)) {
                        upSprite = sprite;
                    }
                    if (quad.isTinted()) {
                        upOrNullTinted = true;
                        anyQuadTinted = true;
                        if (upTintedSprite == null && isUsableSprite(sprite)) {
                            upTintedSprite = sprite;
                        }
                    }
                }
            }
        } catch (Throwable ignored) {
        }

        try {
            random.setSeed(42L);
            List<BakedQuad> generalQuads = bakedModel.getQuads(state, null, random);
            if (generalQuads != null && !generalQuads.isEmpty()) {
                for (BakedQuad quad : generalQuads) {
                    if (quad == null) continue;
                    TextureAtlasSprite sprite = quad.getSprite();
                    if (nullSprite == null && isUsableSprite(sprite)) {
                        nullSprite = sprite;
                    }
                    if (quad.isTinted()) {
                        upOrNullTinted = true;
                        anyQuadTinted = true;
                        if (nullTintedSprite == null && isUsableSprite(sprite)) {
                            nullTintedSprite = sprite;
                        }
                    }
                }
            }
        } catch (Throwable ignored) {
        }

        for (int i = 1; i < QUAD_DIRECTIONS.length; i++) {
            Direction dir = QUAD_DIRECTIONS[i];
            try {
                random.setSeed(42L);
                List<BakedQuad> dirQuads = bakedModel.getQuads(state, dir, random);
                if (dirQuads == null) continue;
                for (BakedQuad quad : dirQuads) {
                    if (quad == null) continue;
                    TextureAtlasSprite sprite = quad.getSprite();
                    if (sideSprite == null && isUsableSprite(sprite)) {
                        sideSprite = sprite;
                    }
                    if (quad.isTinted()) {
                        anyQuadTinted = true;
                        if (sideTintedSprite == null && isUsableSprite(sprite)) {
                            sideTintedSprite = sprite;
                        }
                    }
                }
            } catch (Throwable ignored) {
            }
        }

        TextureAtlasSprite chosen = upTintedSprite != null ? upTintedSprite
                : (upSprite != null ? upSprite
                : (nullTintedSprite != null ? nullTintedSprite
                : (nullSprite != null ? nullSprite
                : (sideTintedSprite != null ? sideTintedSprite : sideSprite))));
        if (!isUsableSprite(chosen)) {
            try {
                TextureAtlasSprite particle = bakedModel.getParticleIcon();
                if (isUsableSprite(particle)) {
                    chosen = particle;
                }
            } catch (Throwable ignored) {
            }
        }
        if (!isUsableSprite(chosen)) return null;

        ResourceLocation spriteName = chosen.contents().name();
        boolean usesTint = hasUpQuads ? upOrNullTinted : anyQuadTinted;
        return new SpriteInfo(spriteName.getNamespace(), spriteName.getPath(), usesTint);
    }

    private static boolean isUsableSprite(TextureAtlasSprite sprite) {
        if (sprite == null || sprite.contents() == null || sprite.contents().name() == null) {
            return false;
        }
        String path = sprite.contents().name().getPath();
        return path != null && !path.isEmpty() && !path.contains("missingno");
    }
}
