package com.velorise.simplemap;

import com.velorise.simplemap.client.MapManager.MapConfig;
import com.velorise.simplemap.client.MapManager;
import com.velorise.simplemap.client.gui.MapScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.Locale;

/**
 * Shared loader-independent gameplay and client-command helpers extracted from SimpleMap.java.
 */
public final class SimpleMapCommon {
    private SimpleMapCommon() {}

    public static boolean isMapUnlocked(Player player) {
        if (player == null) return false;
        if (!MapConfig.serverRequireMapBook) {
            return true;
        }
        return MapManager.getInstance().hasLearnedMap() && hasMapBookInInventory(player);
    }

    public static boolean hasMapBookInInventory(Player player) {
        if (player == null) return false;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.is(ModItems.MAP_BOOK.get())) {
                CompoundTag tag = stack.getTag();
                if (tag != null && tag.contains("MapBookID")) {
                    return true;
                }
            }
        }
        return false;
    }

    public static boolean handleSimpleMapViewCommand(String command) {
        if (command == null) return false;
        String trimmed = command.trim();
        if (trimmed.startsWith("/")) trimmed = trimmed.substring(1);
        String[] parts = trimmed.split("\\s+");
        if (parts.length < 4 || !"simplemap_view".equals(parts[0])) return false;
        try {
            int x = Integer.parseInt(parts[1]);
            int z = Integer.parseInt(parts[3]);
            String dim = parts.length >= 5 ? parts[4] : "";
            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null || !SimpleMap.isMapUnlocked(mc.player)) return false;
            mc.execute(() -> {
                MapScreen mapScreen = mc.screen instanceof MapScreen existing
                        ? existing : new MapScreen();
                mc.setScreen(mapScreen);
                mapScreen.focusOnCoordinates(x + 0.5, z + 0.5, dim);
            });
            return true;
        } catch (NumberFormatException ignored) {
            return false;
        }
    }

    public static String inferDimensionFromChatMessage(String fullText) {
        if (fullText == null || fullText.isEmpty()) return "";
        String lower = fullText.toLowerCase(java.util.Locale.ROOT);
        if (lower.contains("minecraft:the_nether")
                || lower.contains("(the nether)")
                || lower.contains("(nether)")
                || lower.contains("(địa ngục)")
                || lower.contains("(下界)")
                || lower.contains("(地獄)")
                || lower.contains("(ネザー)")
                || lower.contains("(네더)")) {
            return "minecraft:the_nether";
        }
        if (lower.contains("minecraft:the_end")
                || lower.contains("(the end)")
                || lower.contains("(end)")
                || lower.contains("(kết thúc)")
                || lower.contains("(末地)")
                || lower.contains("(終界)")
                || lower.contains("(ジ・エンド)")
                || lower.contains("(엔드)")) {
            return "minecraft:the_end";
        }
        if (lower.contains("minecraft:overworld")
                || lower.contains("(overworld)")
                || lower.contains("(thế giới thực)")
                || lower.contains("(主世界)")
                || lower.contains("(オーバーワールド)")
                || lower.contains("(오버월드)")) {
            return "minecraft:overworld";
        }
        String overworldLocal = MapManager.displayDimensionName("minecraft:overworld")
                .toLowerCase(java.util.Locale.ROOT);
        String netherLocal = MapManager.displayDimensionName("minecraft:the_nether")
                .toLowerCase(java.util.Locale.ROOT);
        String endLocal = MapManager.displayDimensionName("minecraft:the_end")
                .toLowerCase(java.util.Locale.ROOT);
        if (!netherLocal.isEmpty() && lower.contains("(" + netherLocal + ")")) {
            return "minecraft:the_nether";
        }
        if (!endLocal.isEmpty() && lower.contains("(" + endLocal + ")")) {
            return "minecraft:the_end";
        }
        if (!overworldLocal.isEmpty() && lower.contains("(" + overworldLocal + ")")) {
            return "minecraft:overworld";
        }
        return "";
    }
}
