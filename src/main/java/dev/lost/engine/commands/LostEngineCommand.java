package dev.lost.engine.commands;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.tree.LiteralCommandNode;
import dev.lost.engine.LostEngine;
import dev.lost.engine.ResourcePackBuilder;
import dev.lost.engine.WebServer;
import dev.lost.engine.utils.FileUtils;
import dev.lost.engine.utils.HashUtils;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.registry.data.dialog.DialogBase;
import io.papermc.paper.registry.data.dialog.body.DialogBody;
import io.papermc.paper.registry.data.dialog.type.DialogType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.minecraft.world.item.Item;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.craftbukkit.inventory.CraftItemStack;
import org.bukkit.craftbukkit.util.CraftMagicNumbers;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import static dev.lost.engine.utils.HashUtils.getFileHash;

public class LostEngineCommand {

    public static LiteralCommandNode<CommandSourceStack> getCommand() {
        return Commands.literal("lostengine")
                .then(
                        Commands.literal("reload")
                                .requires(commandSourceStack -> commandSourceStack.getSender().hasPermission("op"))
                                .executes(LostEngineCommand::reload)
                )
                .then(
                        Commands.literal("info")
                                .executes(LostEngineCommand::info)
                )
                .then(
                        Commands.literal("editor")
                                .requires(commandSourceStack -> commandSourceStack.getSender().hasPermission("op"))
                                .executes(LostEngineCommand::editor)
                )
                .then(
                        Commands.literal("debug")
                                .requires(commandSourceStack -> commandSourceStack.getSender().hasPermission("op"))
                                .executes(LostEngineCommand::debug)
                )
                .build();
    }

    private static int debug(@NotNull CommandContext<CommandSourceStack> context) {
        if (context.getSource().getExecutor() instanceof Player player) {
            Item item = ((CraftItemStack) player.getInventory().getItemInMainHand()).handle.getItem();
            Material material = CraftMagicNumbers.getMaterial(item);
            player.sendMessage(Component.text(String.valueOf(material)));
        }
        return 0;
    }

    @SuppressWarnings("UnstableApiUsage") // Dialog API
    private static int editor(@NotNull CommandContext<CommandSourceStack> context) {
        Player player = Optional.ofNullable(context.getSource().getExecutor())
                .filter(Player.class::isInstance)
                .map(Player.class::cast)
                .orElse(null);
        if (player != null) {
            player.showDialog(
                    Dialog.create(dialogRegistryBuilderFactory -> dialogRegistryBuilderFactory.empty()
                            .base(DialogBase.builder(Component.text("LostEngine Web Editor"))
                                    .body(
                                            List.of(
                                                    DialogBody.plainMessage(
                                                            Component.text("Open link\uD83E\uDC55")
                                                                    .clickEvent(
                                                                            ClickEvent.openUrl(LostEngine.getResourcePackUrl() +
                                                                                    "?token=" +
                                                                                    WebServer.getToken()
                                                                            )
                                                                    )
                                                    ),
                                                    DialogBody.plainMessage(
                                                            Component.text("Read-only link\uD83E\uDC55")
                                                                    .clickEvent(
                                                                            ClickEvent.openUrl(LostEngine.getResourcePackUrl() +
                                                                                    "?token=" +
                                                                                    WebServer.getReadOnlyToken()
                                                                            )
                                                                    )
                                                    )
                                            )
                                    )
                                    .build()
                            )
                            .type(DialogType.notice())
                    )
            );
        } else {
            context.getSource().getSender().sendPlainMessage(
                    "Web editor link: " +
                            LostEngine.getResourcePackUrl() +
                            "?token=" +
                            WebServer.getToken() +
                            "\n" +
                            "Read-only link: " +
                            LostEngine.getResourcePackUrl() +
                            "?token=" +
                            WebServer.getReadOnlyToken()
            );
        }
        return 1;
    }

    private static int info(@NotNull CommandContext<CommandSourceStack> context) {
        CommandSender sender = context.getSource().getSender();
        LostEngine plugin = LostEngine.getInstance();
        sender.sendMessage(Component.text("LostEngine version: " + plugin.getPluginMeta().getVersion() + " by " + plugin.getPluginMeta().getAuthors()));
        return 1;
    }

    private static int reload(@NotNull CommandContext<CommandSourceStack> context) {
        CommandSender sender = context.getSource().getSender();
        LostEngine plugin = LostEngine.getInstance();
        WebServer.stop();
        plugin.reloadConfig();
        if (sender instanceof Player) sender.sendMessage("Reloading LostEngine configuration and resource pack...");
        plugin.getSLF4JLogger().info("Reloading LostEngine configuration and resource pack...");

        byte[] resourcePackHash;
        try {
            ResourcePackBuilder.buildResourcePack(plugin, LostEngine.getResourcePackFile(), FileUtils.withExtension(LostEngine.getResourcePackFile(), "mcpack"), null);
            resourcePackHash = getFileHash(LostEngine.getResourcePackFile());
        } catch (IOException | NoSuchAlgorithmException e) {
            if (sender instanceof Player) {
                sender.sendMessage("Failed to build resource pack: " + e.getMessage());
            }
            plugin.getSLF4JLogger().error("Failed to build resource pack", e);
            return 0;
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
        return 1;
    }

}
