package dev.lost.engine.geyserextension;

import com.google.gson.JsonElement;
import dev.lost.annotations.NotNull;
import dev.lost.engine.geyserextension.lomapping.Mapping;
import dev.lost.engine.geyserextension.lomapping.MappingReader;
import dev.lost.engine.geyserextension.utils.FileUtils;
import org.geysermc.event.subscribe.Subscribe;
import org.geysermc.geyser.api.GeyserApi;
import org.geysermc.geyser.api.event.lifecycle.GeyserDefineCustomBlocksEvent;
import org.geysermc.geyser.api.event.lifecycle.GeyserDefineCustomItemsEvent;
import org.geysermc.geyser.api.event.lifecycle.GeyserPreInitializeEvent;
import org.geysermc.geyser.api.extension.Extension;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Stream;

public class LostEngineGeyserExtension implements Extension {

    private Mapping mapping;

    @Subscribe
    public void onGeyserDefineCustomItemsEvent(@NotNull GeyserDefineCustomItemsEvent event) {
        if (mapping == null) {
            logger().severe("LostEngine mapping is not loaded, cannot define custom items.");
            return;
        }
        ItemGenerator.generateFromJson(mapping, event);
    }

    @Subscribe
    public void onGeyserDefineCustomBlocksEvent(@NotNull GeyserDefineCustomBlocksEvent event) {
        if (mapping == null) {
            logger().severe("LostEngine mapping is not loaded, cannot define custom blocks.");
            return;
        }
        BlockGenerator.generateFromJson(mapping, event);
    }

    @Subscribe
    public void onGeyserPreInitializeEvent(@NotNull GeyserPreInitializeEvent event) {
        File mappingFile = new File(dataFolder().toFile(), "mappings.lomapping");
        if (!mappingFile.exists()) {
            logger().severe("LostEngine mapping file not found in the extension data folder. (%s)".formatted(mappingFile.getAbsolutePath()));
            mappingFile.getParentFile().mkdirs();
            return;
        }
        mapping = MappingReader.read(mappingFile.toPath());
        Path localeOverridesDir = GeyserApi.api().configDirectory().resolve("locales/overrides/");
        if (!Files.exists(localeOverridesDir)) {
            try (Stream<Path> stream = Files.list(localeOverridesDir)) {
                if (stream.findAny().isPresent()) {
                    logger().info("It looks like you already have locale overrides in locales/overrides/ of your Geyser config directory, " +
                            "LostEngineGeyserExtension needs to overwrite them in order to work.");
                    FileUtils.deleteFolder(localeOverridesDir);
                }
            } catch (IOException ignored) {
            }
        }
        for (Map.Entry<String, JsonElement> entry : mapping.locales().entrySet()) {
            try {
                FileUtils.saveJsonToFile(entry.getValue(), localeOverridesDir.resolve(entry.getKey().toLowerCase(Locale.ROOT) + ".json").toFile());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

}
