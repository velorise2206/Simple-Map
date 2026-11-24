package com.velorise.simplemap.compat;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;

/**
 * Minecraft 26.1 BlockStateModel implementation of {@link BlockSpriteSampler}.
 */
public final class LegacyBakedModelSpriteSampler implements BlockSpriteSampler {
    private static final Direction[] QUAD_DIRECTIONS = {
            Direction.UP, Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST
    };

    @Override
    public SpriteInfo samplePrimarySprite(BlockState state) {
        if (state == null) return null;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null || minecraft.getModelManager() == null) return null;

        BlockStateModel model = minecraft.getModelManager().getBlockStateModelSet().get(state);
        if (model == null) return null;

        RandomSource random = RandomSource.create(42L);
        List<BlockStateModelPart> parts = new ArrayList<>();
        try {
            model.collectParts(random, parts);
        } catch (Throwable ignored) {
        }

        boolean upOrNullTinted = false;
        boolean anyQuadTinted = false;
        boolean hasUpQuads = false;
        TextureAtlasSprite upTintedSprite = null;
        TextureAtlasSprite upSprite = null;
        TextureAtlasSprite nullTintedSprite = null;
        TextureAtlasSprite nullSprite = null;
        TextureAtlasSprite sideTintedSprite = null;
        TextureAtlasSprite sideSprite = null;

        for (BlockStateModelPart part : parts) {
            if (part == null) continue;
            List<BakedQuad> upQuads = part.getQuads(Direction.UP);
            if (upQuads != null && !upQuads.isEmpty()) {
                hasUpQuads = true;
                for (BakedQuad quad : upQuads) {
                    if (quad == null) continue;
                    TextureAtlasSprite sprite = quad.materialInfo().sprite();
                    if (upSprite == null && isUsableSprite(sprite)) upSprite = sprite;
                    if (quad.materialInfo().tintIndex() != -1) {
                        upOrNullTinted = true;
                        anyQuadTinted = true;
                        if (upTintedSprite == null && isUsableSprite(sprite)) upTintedSprite = sprite;
                    }
                }
            }
            List<BakedQuad> generalQuads = part.getQuads(null);
            if (generalQuads != null) {
                for (BakedQuad quad : generalQuads) {
                    if (quad == null) continue;
                    TextureAtlasSprite sprite = quad.materialInfo().sprite();
                    if (nullSprite == null && isUsableSprite(sprite)) nullSprite = sprite;
                    if (quad.materialInfo().tintIndex() != -1) {
                        upOrNullTinted = true;
                        anyQuadTinted = true;
                        if (nullTintedSprite == null && isUsableSprite(sprite)) nullTintedSprite = sprite;
                    }
                }
            }
            for (int i = 1; i < QUAD_DIRECTIONS.length; i++) {
                List<BakedQuad> dirQuads = part.getQuads(QUAD_DIRECTIONS[i]);
                if (dirQuads == null) continue;
                for (BakedQuad quad : dirQuads) {
                    if (quad == null) continue;
                    TextureAtlasSprite sprite = quad.materialInfo().sprite();
                    if (sideSprite == null && isUsableSprite(sprite)) sideSprite = sprite;
                    if (quad.materialInfo().tintIndex() != -1) {
                        anyQuadTinted = true;
                        if (sideTintedSprite == null && isUsableSprite(sprite)) sideTintedSprite = sprite;
                    }
                }
            }
        }

        TextureAtlasSprite chosen = upTintedSprite != null ? upTintedSprite
                : (upSprite != null ? upSprite
                : (nullTintedSprite != null ? nullTintedSprite
                : (nullSprite != null ? nullSprite
                : (sideTintedSprite != null ? sideTintedSprite : sideSprite))));
        if (!isUsableSprite(chosen)) {
            try {
                TextureAtlasSprite particle = model.particleMaterial().sprite();
                if (isUsableSprite(particle)) chosen = particle;
            } catch (Throwable ignored) {
            }
        }
        if (!isUsableSprite(chosen)) return null;

        Identifier spriteName = chosen.contents().name();
        boolean usesTint = hasUpQuads ? upOrNullTinted : anyQuadTinted;
        return new SpriteInfo(spriteName.getNamespace(), spriteName.getPath(), usesTint);
    }

    private static boolean isUsableSprite(TextureAtlasSprite sprite) {
        if (sprite == null || sprite.contents() == null || sprite.contents().name() == null) return false;
        String path = sprite.contents().name().getPath();
        return path != null && !path.isEmpty() && !path.contains("missingno");
    }
}
