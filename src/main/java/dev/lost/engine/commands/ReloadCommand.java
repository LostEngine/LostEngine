package dev.lost.engine.commands;

import dev.lost.engine.LostEngine;
import dev.lost.engine.ResourcePackBuilder;
import dev.lost.engine.WebServer;
import dev.lost.engine.utils.HashUtils;
import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.Objects;
import java.util.UUID;

import static dev.lost.engine.utils.HashUtils.getFileHash;

public class ReloadCommand implements BasicCommand {

    @Override
    public void execute(CommandSourceStack commandSourceStack, String @NotNull [] args) {
        CommandSender sender = commandSourceStack.getSender();
        LostEngine plugin = LostEngine.getInstance();
        WebServer.stop();
        plugin.reloadConfig();
        if (sender instanceof Player) sender.sendMessage("Reloading LostEngine configuration and resource pack...");
        plugin.getSLF4JLogger().info("Reloading LostEngine configuration and resource pack...");

        byte[] resourcePackHash;
        try {
            ResourcePackBuilder.buildResourcePack(plugin, LostEngine.getResourcePackFile(), null);
            resourcePackHash = getFileHash(LostEngine.getResourcePackFile());
        } catch (IOException | NoSuchAlgorithmException e) {
            if (sender instanceof Player) {
                sender.sendMessage("Failed to build resource pack: " + e.getMessage());
            }
            plugin.getSLF4JLogger().error("Failed to build resource pack", e);
            return;
        }

        if (plugin.getConfig().getBoolean("pack_hosting.self_hosted.enabled")) {
            String resourcePackUrl = "http://" + plugin.getConfig().getString("pack_hosting.self_hosted.hostname", "127.0.0.1") + ":" + plugin.getConfig().getInt("self_hosted.port", 7270);
            LostEngine.setResourcePackUrl(resourcePackUrl);
            try {
                String resourcePackHashString = HashUtils.getFileHashString(LostEngine.getResourcePackFile());
                LostEngine.setResourcePackHash(resourcePackHashString);
                LostEngine.setResourcePackUUID(UUID.nameUUIDFromBytes(resourcePackHashString.getBytes()));

                WebServer.start(plugin.getConfig().getInt("self_hosted.port", 7270));
                Bukkit.getOnlinePlayers().forEach(player -> {
                    player.removeResourcePacks();
                    player.addResourcePack(
                            UUID.nameUUIDFromBytes(resourcePackHash),
                            resourcePackUrl,
                            resourcePackHash,
                            plugin.getConfig().getString("pack_hosting.resource_pack_prompt", "Prompt"),
                            true
                    );
                });
            } catch (IOException | NoSuchAlgorithmException e) {
                if (sender instanceof Player) {
                    sender.sendMessage("Failed to start resource pack server: " + e.getMessage());
                }
                plugin.getSLF4JLogger().error("Failed to start resource pack server", e);
            }
        } else if (plugin.getConfig().getBoolean("pack_hosting.external_host.enabled")) {
            String resourcePackUrl = plugin.getConfig().getString("pack_hosting.external_host.url");
            Bukkit.getOnlinePlayers().forEach(player -> {
                player.removeResourcePacks();
                player.addResourcePack(
                        UUID.nameUUIDFromBytes(resourcePackHash),
                        Objects.requireNonNull(resourcePackUrl, "Resource pack URL is not set but external hosting is enabled in the config!"),
                        resourcePackHash,
                        plugin.getConfig().getString("pack_hosting.resource_pack_prompt", "Prompt"),
                        true
                );
            });
        }

        if (sender instanceof Player) sender.sendMessage("Resource pack built successfully.");
        plugin.getSLF4JLogger().info("Resource pack built successfully.");
    }

    @Override
    public String permission() {
        return "op";
    }
}
