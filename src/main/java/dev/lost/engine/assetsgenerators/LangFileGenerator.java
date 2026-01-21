package dev.lost.engine.assetsgenerators;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import dev.lost.furnace.resourcepack.JavaResourcePack;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import org.jetbrains.annotations.Nullable;

public class LangFileGenerator {

    private static final Gson GSON = new Gson();
    Object2ObjectOpenHashMap<String, Lang> languages = new Object2ObjectOpenHashMap<>();

    public void addTranslation(String langCode, String key, String value) {
        languages.computeIfAbsent(langCode, k -> new Lang()).put(key, value);
    }

    public void build(JavaResourcePack resourcePack, @Nullable LostEngineMappingGenerator mappingGenerator) {
        for (Object2ObjectMap.Entry<String, Lang> entry : languages.object2ObjectEntrySet()) {
            JsonElement jsonElement = GSON.toJsonTree(entry.getValue());
            resourcePack.jsonFile("assets/minecraft/lang/%s.json".formatted(entry.getKey().toLowerCase()), jsonElement);
            if (mappingGenerator != null) mappingGenerator.addLocale(entry.getKey() ,jsonElement.getAsJsonObject());
        }
    }

    public static class Lang extends Object2ObjectOpenHashMap<String, String> {
    }
}
