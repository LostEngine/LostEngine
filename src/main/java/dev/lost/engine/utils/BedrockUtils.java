package dev.lost.engine.utils;

import org.bukkit.Bukkit;

import java.util.UUID;

public class BedrockUtils {

    private static Boolean isFloodgateEnabled = null;
    private static Boolean isGeyserEnabled = null;

    public static boolean isBedrockPlayer(UUID uuid) {
        return isFloodgateEnabled() && org.geysermc.floodgate.api.FloodgateApi.getInstance().isFloodgatePlayer(uuid) ||
                isGeyserEnabled() && org.geysermc.api.Geyser.api().isBedrockPlayer(uuid);
    }

    public static boolean isFloodgateEnabled() {
        return isFloodgateEnabled != null ? isFloodgateEnabled : (isFloodgateEnabled = Bukkit.getPluginManager().isPluginEnabled("floodgate"));
    }

    public static boolean isGeyserEnabled() {
        return isGeyserEnabled != null ? isGeyserEnabled : (isGeyserEnabled = Bukkit.getPluginManager().isPluginEnabled("Geyser-Spigot"));
    }

}
