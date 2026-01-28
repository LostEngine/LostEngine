package dev.lost.engine;

import dev.lost.engine.assetsgenerators.LostEngineMappingGenerator;
import dev.lost.engine.commands.EditorCommand;
import dev.lost.engine.commands.GiveCommand;
import dev.lost.engine.commands.ReloadCommand;
import dev.lost.engine.commands.SetBlockCommand;
import dev.lost.engine.items.customitems.CustomItem;
import dev.lost.engine.listeners.PacketListener;
import dev.lost.engine.utils.FloodgateUtils;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.UUID;

import static dev.lost.engine.utils.HashUtils.getFileHashString;

public final class LostEngine extends JavaPlugin {

    @Getter private static LostEngine instance;

    @Setter @Getter private static String resourcePackHash;

    @Setter @Getter private static UUID resourcePackUUID;

    @Setter @Getter private static String resourcePackUrl;

    @Getter private static File resourcePackFile;

    @Override
    public void onLoad() {
        instance = this;
        getConfig().options().copyDefaults(true);
        saveConfig();
        LostEngineMappingGenerator mappingGenerator = null;
        if (getConfig().getBoolean("geyser_compatibility", false)) {
            mappingGenerator = new LostEngineMappingGenerator();
            if (!FloodgateUtils.IS_FLOODGATE_ENABLED) {
                logger().error("Geyser compatibility is enabled but Floodgate was not detected on the server, consider installing Floodgate for it to work.");
            }
        }

        // Building the resource pack
        resourcePackFile = new File(getDataFolder(), getConfig().getString("resource_pack.file_name", "resource-pack") + ".zip");
        File bedrockResourcePack = new File(getDataFolder().getAbsolutePath(), getConfig().getString("resource_pack.file_name", "resource-pack") + ".mcpack");
        try {
            logger().info("Building resource pack...");
            ResourcePackBuilder.buildResourcePack(this, resourcePackFile, bedrockResourcePack, mappingGenerator);
            logger().info("Finished building resource pack!");

            if (getConfig().getBoolean("geyser_compatibility", false) && bedrockResourcePack.exists() && bedrockResourcePack.isFile()) {
                Plugin geyserPlugin = Bukkit.getPluginManager().getPlugin("Geyser-Spigot");
                if (geyserPlugin != null) {
                    logger().info("Detected Geyser, putting resource pack in Geyser's resource pack folder...");
                    Files.copy(
                            bedrockResourcePack.toPath(),
                            geyserPlugin.getDataPath().resolve("packs/").resolve(bedrockResourcePack.getName()),
                            StandardCopyOption.REPLACE_EXISTING
                    );
                }
            }
            resourcePackHash = getFileHashString(resourcePackFile);
            resourcePackUUID = UUID.nameUUIDFromBytes(resourcePackHash.getBytes());
        } catch (IOException | NoSuchAlgorithmException e) {
            getSLF4JLogger().error("Failed to build resource pack", e);
        }

        // Creating the resource pack server
        if (getConfig().getBoolean("pack_hosting.self_hosted.enabled")) {
            resourcePackUrl = "http://" + getConfig().getString("pack_hosting.self_hosted.hostname", "127.0.0.1") + ":" + getConfig().getInt("self_hosted.port", 7270);
            try {
                WebServer.start(getConfig().getInt("self_hosted.port", 7270));
            } catch (IOException e) {
                getSLF4JLogger().error("Failed to start http server", e);
            }
        } else if (getConfig().getBoolean("pack_hosting.external_host.enabled")) {
            resourcePackUrl = getConfig().getString("pack_hosting.external_host.url");
        }

        // Commands
        registerCommand("lostenginereload", List.of("lereload","ler") , new ReloadCommand());
        registerCommand("editor", List.of("webeditor"), new EditorCommand());
        getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, commands -> {
            commands.registrar().register(GiveCommand.getCommand());
            commands.registrar().register(SetBlockCommand.getCommand());
        });

        // Listeners
        PacketListener.inject();

        if (mappingGenerator != null) {
            try {
                logger().info("Geyser compatibility is enabled, generating mapping file...");
                for (Item item: BuiltInRegistries.ITEM.stream().toList()) {
                    if (item instanceof CustomItem customItem) {
                        mappingGenerator.addItem(item, customItem.getId().replaceAll(":", "_"));
                    }
                }
                mappingGenerator.build(getDataFolder());
                logger().info("Finished generating mapping file!");
                File mappingFile = new File(getDataFolder(), "mappings.lomapping");
                if (getConfig().getBoolean("geyser_compatibility", false) && mappingFile.exists() && mappingFile.isFile()) {
                    Plugin geyserPlugin = Bukkit.getPluginManager().getPlugin("Geyser-Spigot");
                    if (geyserPlugin != null) {
                        logger().info("Detected Geyser, putting mapping file in LostEngine Geyser Extension's config folder...");
                        Files.copy(
                                mappingFile.toPath(),
                                geyserPlugin.getDataPath().resolve("extensions/lostenginegeyserextension/").resolve(mappingFile.getName()),
                                StandardCopyOption.REPLACE_EXISTING
                        );
                    }
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    public void onDisable() {
        WebServer.stop();
    }

    public static @NotNull Logger logger() {
        return instance.getSLF4JLogger();
    }
}
