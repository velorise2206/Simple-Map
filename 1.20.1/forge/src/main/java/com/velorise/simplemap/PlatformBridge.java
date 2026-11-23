package com.velorise.simplemap;

import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.loading.FMLPaths;
import java.nio.file.Path;

public final class PlatformBridge {
    private PlatformBridge() {}

    public static Path configDirectory() {
        return FMLPaths.CONFIGDIR.get();
    }

    public static boolean areMapBooksReady() {
        return ModItems.MAP_BOOK.isPresent() && ModItems.EMPTY_MAP_BOOK.isPresent();
    }

    public static boolean isModLoaded(String modId) {
        return ModList.get().isLoaded(modId);
    }
}
