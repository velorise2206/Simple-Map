package com.velorise.simplemap.compat;

import com.velorise.simplemap.PlatformBridge;
import com.velorise.simplemap.network.ClientNetworkHandler;
import com.velorise.simplemap.network.payload.AckLearnRegionPayload;
import com.velorise.simplemap.network.payload.InitSaveBookPayload;
import com.velorise.simplemap.network.payload.RequestLearnBookPayload;
import com.velorise.simplemap.network.payload.SendRegionFilePayload;
import net.minecraft.world.level.biome.Biome;

import java.nio.file.Path;
import java.util.List;

/**
 * Minecraft 1.21.1 Fabric/NeoForge implementation of {@link MapPlatformNet}.
 */
public final class LegacyPlatformNetBridge implements MapPlatformNet {
    @Override
    public Path configDirectory() {
        return PlatformBridge.configDirectory();
    }

    @Override
    public boolean isModLoaded(String modId) {
        return PlatformBridge.isModLoaded(modId);
    }

    @Override
    public boolean isServerExtensionAvailable() {
        return ClientNetworkHandler.isServerExtensionAvailable();
    }

    @Override
    public boolean areMapBooksReady() {
        return PlatformBridge.areMapBooksReady();
    }

    @Override
    public float biomeDownfall(Biome biome) {
        return PlatformBridge.biomeDownfall(biome);
    }

    @Override
    public boolean sendInitSaveBook(List<String> regionNames, boolean mainHand, String bookId) {
        return ClientNetworkHandler.sendToServer(new InitSaveBookPayload(regionNames, mainHand, bookId));
    }

    @Override
    public boolean sendRegionFile(String bookId, String regionName, byte[] data,
                                  int currentIdx, int totalCount, boolean mainHand) {
        return ClientNetworkHandler.sendToServer(
                new SendRegionFilePayload(bookId, regionName, data, currentIdx, totalCount, mainHand));
    }

    @Override
    public boolean sendRequestLearnBook(String bookId) {
        return ClientNetworkHandler.sendToServer(new RequestLearnBookPayload(bookId));
    }

    @Override
    public boolean sendAckLearnRegion(String bookId, int nextIdx, boolean accepted) {
        return ClientNetworkHandler.sendToServer(new AckLearnRegionPayload(bookId, nextIdx, accepted));
    }
}
