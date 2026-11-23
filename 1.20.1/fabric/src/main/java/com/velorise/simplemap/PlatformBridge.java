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
}
