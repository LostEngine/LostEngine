package dev.lost.engine.bootstrap;

import dev.lost.engine.LostEngine;
import dev.lost.engine.annotations.CanBreakOnUpdates;
import dev.lost.engine.assetsgenerators.DataPackGenerator;
import dev.lost.engine.utils.TimeUtils;
import io.papermc.paper.plugin.bootstrap.BootstrapContext;
import io.papermc.paper.plugin.bootstrap.PluginBootstrap;
import io.papermc.paper.plugin.bootstrap.PluginProviderContext;
import net.minecraft.SharedConstants;
import org.bukkit.craftbukkit.util.CraftMagicNumbers;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Objects;
import java.util.Properties;

import static dev.lost.engine.bootstrap.ResourceInjector.injectResources;

@CanBreakOnUpdates(lastCheckedVersion = "1.21.11") // Have to update this class every new Minecraft version
@SuppressWarnings("UnstableApiUsage")
public class LostEngineBootstrap implements PluginBootstrap {
    @Override
    public void bootstrap(@NotNull BootstrapContext context) {
        try {
            String versionId = SharedConstants.getCurrentVersion().id();
            if (!versionId.equals("1.21.11")) {
                context.getLogger().error("This version of LostEngine only supports Minecraft/Paper 1.21.11, detected version: {}", versionId);
                stopServer(context);
            }
            context.getLogger().info("Loading Minecraft classes...");
            long startTime = System.nanoTime();
            Objects.requireNonNull(net.minecraft.world.level.block.Blocks.AIR);
            Objects.requireNonNull(net.minecraft.world.item.Items.AIR);
            Objects.requireNonNull(CraftMagicNumbers.INSTANCE);
            long elapsedNanos = System.nanoTime() - startTime;

            // This is a personalized version of Nancyj-Improved
            context.getLogger().info("""
                    Finished loading Minecraft classes! ({})
                    dP                                    88888888b
                    88                            88       88                            oo
                    88        .d8888b. .d8888b. d8888P    a88aaaa    88d888b. .d8888b. d888   88d888b. .d8888b.
                    88        88'  `88 Y8ooooo.   88       88        88'  `88 88'  `88   88   88'  `88 88ooood8
                    88        88.  .88       88   88       88        88    88 88.  .88   88   88    88 88.  ...
                    88888888P `88888P' `88888P'   `8b.    88888888P  dP    dP `8888P88 d8888P dP    dP `88888P'
                                                                                   .88
                                                                               d8888P""", TimeUtils.formatNanos(elapsedNanos));
            context.getLogger().info("Start injecting custom items...");
            startTime = System.nanoTime();
            DataPackGenerator dataPackGenerator = new DataPackGenerator();
            injectResources(context, dataPackGenerator);
            elapsedNanos = System.nanoTime() - startTime;
            context.getLogger().info("\u001b[1A\u001b[2KFinished injecting custom items! ({})", TimeUtils.formatNanos(elapsedNanos));
            context.getLogger().info("Start building the data pack...");
            startTime = System.nanoTime();

            // Load server properties to get the world name
            Properties props = new Properties();
            String levelName;
            try (FileInputStream fis = new FileInputStream("server.properties")) {
                props.load(fis);
                levelName = props.getProperty("level-name");
            } catch (IOException e) {
                context.getLogger().warn("Could not load server.properties, it will use the folder 'world' for the data pack.");
                levelName = "world";
            }
            dataPackGenerator.build(new File(levelName + File.separator + "datapacks" + File.separator + "lost_engine_generated"));
            elapsedNanos = System.nanoTime() - startTime;
            context.getLogger().info("\u001b[1A\u001b[2KFinished building the data pack! ({})", TimeUtils.formatNanos(elapsedNanos));
        } catch (Exception | LinkageError e) {
            context.getLogger().error("Failed to inject custom resources are you using Minecraft/Paper 1.21.11?", e);
            stopServer(context);
        }

    }

    static void stopServer(@NotNull BootstrapContext context) {
        context.getLogger().info("Stopping the server...");
        System.exit(1);
    }

    @Override
    public @NotNull JavaPlugin createPlugin(@NotNull PluginProviderContext context) {
        return new LostEngine();
    }
}
