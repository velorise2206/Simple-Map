package com.velorise.simplemap.compat;

import net.minecraft.world.level.biome.Biome;

import java.nio.file.Path;
import java.util.List;

/**
 * Unified platform & optional Map Book networking facade.
 * Isolates loader-specific PlatformBridge, ClientNetworkHandler, and CustomPacketPayload types.
 */
public interface MapPlatformNet {
    Path configDirectory();
    boolean isModLoaded(String modId);
    boolean isServerExtensionAvailable();
    boolean areMapBooksReady();
    float biomeDownfall(Biome biome);

    boolean sendInitSaveBook(List<String> regionNames, boolean mainHand, String bookId);
    boolean sendRegionFile(String bookId, String regionName, byte[] data,
                           int currentIdx, int totalCount, boolean mainHand);
    boolean sendRequestLearnBook(String bookId);
    boolean sendAckLearnRegion(String bookId, int nextIdx, boolean accepted);
}
