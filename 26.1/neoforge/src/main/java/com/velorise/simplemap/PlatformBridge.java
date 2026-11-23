package com.velorise.simplemap;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.neoforged.fml.ModList;
import net.neoforged.fml.loading.FMLPaths;
import java.nio.file.Path;

public final class PlatformBridge {
    private PlatformBridge() {}

    public static Path configDirectory() {
        return FMLPaths.CONFIGDIR.get();
    }

    public static boolean areMapBooksReady() {
        return ModItems.MAP_BOOK.isBound() && ModItems.EMPTY_MAP_BOOK.isBound();
    }

    public static boolean isModLoaded(String modId) {
        return ModList.get().isLoaded(modId);
    }

    public static float biomeDownfall(Biome biome) {
        return (float) biome.getModifiedClimateSettings().downfall();
    }

    public static void onMapBookCrafted(ItemStack stack, Level level, Player player) {
        // NeoForge dispatches ItemCraftedEvent automatically on the game event bus.
    }
}
