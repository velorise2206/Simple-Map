package com.velorise.simplemap.paper;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRegisterChannelEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Minimal Paper / Spigot / Purpur / Folia companion plugin for Velorise Simple Map.
 * Sends the 2-byte {@code simplemap:sync_config} payload so server owners can enforce
 * {@code caveMapMode} without installing the client/server mod.
 */
public final class SimpleMapPaperPlugin extends JavaPlugin implements Listener {
    public static final String SYNC_CONFIG_CHANNEL = "simplemap:sync_config";
    private static final String BYPASS_PERMISSION = "simplemap.cave.bypass";

    private static final int MODE_OFF = 0;
    private static final int MODE_AUTO = 1;
    private static final int MODE_ON = 2;

    private volatile int caveMapMode = MODE_OFF;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        loadPolicyFromConfig();

        getServer().getMessenger().registerOutgoingPluginChannel(this, SYNC_CONFIG_CHANNEL);
        getServer().getPluginManager().registerEvents(this, this);

        PluginCommand cmd = getCommand("simplemap");
        if (cmd != null) {
            cmd.setExecutor(this);
            cmd.setTabCompleter(this);
        }

        syncAllOnlinePlayers();
        getLogger().info("SimpleMapPaper enabled (cave-map-mode=" + modeToName(caveMapMode) + ").");
    }

    @Override
    public void onDisable() {
        getServer().getMessenger().unregisterOutgoingPluginChannel(this, SYNC_CONFIG_CHANNEL);
    }

    @EventHandler
    public void onChannelRegister(PlayerRegisterChannelEvent event) {
        if (SYNC_CONFIG_CHANNEL.equals(event.getChannel())) {
            sendPolicy(event.getPlayer());
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (player.getListeningPluginChannels().contains(SYNC_CONFIG_CHANNEL)) {
            sendPolicy(player);
        }
    }

    @EventHandler
    public void onWorldChange(PlayerChangedWorldEvent event) {
        Player player = event.getPlayer();
        if (player.getListeningPluginChannels().contains(SYNC_CONFIG_CHANNEL)) {
            sendPolicy(player);
        }
    }

    private void loadPolicyFromConfig() {
        reloadConfig();
        String raw = getConfig().getString("cave-map-mode", "off");
        int parsed = parseMode(raw);
        if (parsed < 0) {
            getLogger().warning("Invalid cave-map-mode '" + raw + "' in config.yml; falling back to 'off'.");
            parsed = MODE_OFF;
        }
        this.caveMapMode = parsed;
    }

    private void sendPolicy(Player player) {
        int effectiveMode = player.hasPermission(BYPASS_PERMISSION) ? MODE_ON : caveMapMode;
        // Wire format matches SyncConfigPayload.STREAM_CODEC:
        // byte[0] = requireMapBook (always false on Paper since Map Book is a mod item)
        // byte[1] = caveMapMode (0 = OFF, 1 = AUTO, 2 = ON)
        byte[] payload = new byte[] { 0, (byte) effectiveMode };
        player.sendPluginMessage(this, SYNC_CONFIG_CHANNEL, payload);
    }

    private int syncAllOnlinePlayers() {
        int synced = 0;
        for (Player player : getServer().getOnlinePlayers()) {
            if (player.getListeningPluginChannels().contains(SYNC_CONFIG_CHANNEL)) {
                sendPolicy(player);
                synced++;
            }
        }
        return synced;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0 || "status".equalsIgnoreCase(args[0])) {
            int listeners = 0;
            for (Player player : getServer().getOnlinePlayers()) {
                if (player.getListeningPluginChannels().contains(SYNC_CONFIG_CHANNEL)) {
                    listeners++;
                }
            }
            sender.sendMessage("§a[SimpleMap] §fServer cave-map-mode: §e" + modeToName(caveMapMode)
                    + " §7(" + listeners + " online Simple Map client(s) synced)");
            return true;
        }

        if ("reload".equalsIgnoreCase(args[0])) {
            loadPolicyFromConfig();
            int synced = syncAllOnlinePlayers();
            sender.sendMessage("§a[SimpleMap] §fReloaded config (cave-map-mode=§e"
                    + modeToName(caveMapMode) + "§f), synced §e" + synced + "§f client(s).");
            return true;
        }

        if ("cave".equalsIgnoreCase(args[0])) {
            if (args.length < 2) {
                sender.sendMessage("§cUsage: /" + label + " cave <off|auto|on>");
                return true;
            }
            int parsed = parseMode(args[1]);
            if (parsed < 0) {
                sender.sendMessage("§cInvalid mode '" + args[1] + "'. Use: off, auto, or on.");
                return true;
            }
            this.caveMapMode = parsed;
            getConfig().set("cave-map-mode", modeToName(parsed));
            saveConfig();
            int synced = syncAllOnlinePlayers();
            sender.sendMessage("§a[SimpleMap] §fSet cave-map-mode to §e" + modeToName(parsed)
                    + "§f and synced §e" + synced + "§f client(s).");
            return true;
        }

        sender.sendMessage("§cUsage: /" + label + " <status|cave <off|auto|on>|reload>");
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return filterPrefix(Arrays.asList("status", "cave", "reload"), args[0]);
        }
        if (args.length == 2 && "cave".equalsIgnoreCase(args[0])) {
            return filterPrefix(Arrays.asList("off", "auto", "on"), args[1]);
        }
        return Collections.emptyList();
    }

    private static List<String> filterPrefix(List<String> options, String prefix) {
        String lower = prefix == null ? "" : prefix.toLowerCase(Locale.ROOT);
        return options.stream().filter(opt -> opt.startsWith(lower)).toList();
    }

    private static int parseMode(String raw) {
        if (raw == null) return -1;
        return switch (raw.trim().toLowerCase(Locale.ROOT)) {
            case "0", "off", "false", "disabled" -> MODE_OFF;
            case "1", "auto" -> MODE_AUTO;
            case "2", "on", "true", "full" -> MODE_ON;
            default -> -1;
        };
    }

    private static String modeToName(int mode) {
        return switch (mode) {
            case MODE_AUTO -> "auto";
            case MODE_ON -> "on";
            default -> "off";
        };
    }
}
