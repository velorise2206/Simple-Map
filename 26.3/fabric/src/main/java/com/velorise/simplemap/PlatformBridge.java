package com.velorise.simplemap;

import net.fabricmc.loader.api.FabricLoader;
import java.nio.file.Path;

public final class PlatformBridge {
    private PlatformBridge() {}

    public static Path configDirectory() {
        return FabricLoader.getInstance().getConfigDir();
    }

    public static boolean areMapBooksReady() {
        return true;
    }

    public static boolean isModLoaded(String modId) {
        return FabricLoader.getInstance().isModLoaded(modId);
    }

    public static float biomeDownfall(net.minecraft.world.level.biome.Biome biome) {
        return biome.hasPrecipitation() ? 0.5f : 0.0f;
    }

    public static void onMapBookCrafted(net.minecraft.world.item.ItemStack stack,
                                        net.minecraft.world.level.Level level,
                                        net.minecraft.world.entity.player.Player player) {
        if (!level.isClientSide() && player != null && player.containerMenu != null) {
            for (net.minecraft.world.inventory.Slot slot : player.containerMenu.slots) {
                if (slot instanceof com.velorise.simplemap.mixin.SimpleMapResultSlotAccessor accessor) {
                    SimpleMap.CommonGameEvents.onItemCrafted(player, stack, accessor.simplemap$getCraftSlots());
                    break;
                }
            }
        }
    }
}
