package dev.lost.engine.assetsgenerators;

import com.google.gson.JsonObject;
import dev.lost.furnace.resourcepack.JavaResourcePack;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;

public class LangFileGenerator {

    private final Object2ObjectOpenHashMap<String, Lang> languages = new Object2ObjectOpenHashMap<>();

    public void addTranslation(@NotNull String langCode, @NotNull String key, String value) {
        addTranslation(langCode, key, value, Edition.BOTH);
    }

    public void addTranslation(@NotNull String langCode, @NotNull String key, String value, @NotNull Edition edition) {
        languages.computeIfAbsent(langCode.toLowerCase(Locale.ROOT), k -> new Lang()).add(new Translation(key, value, edition));
    }

    public void build(JavaResourcePack resourcePack, @Nullable LostEngineMappingGenerator mappingGenerator) {
        for (Object2ObjectMap.Entry<String, Lang> entry : languages.object2ObjectEntrySet()) {
            JsonObject javaLangJson = new JsonObject();
            entry.getValue().forEach(translation -> {
                if (translation.edition() == Edition.JAVA || translation.edition() == Edition.BOTH) {
                    javaLangJson.addProperty(translation.key(), translation.value());
                }
            });
            resourcePack.jsonFile("assets/minecraft/lang/%s.json".formatted(entry.getKey()), javaLangJson);
            if (mappingGenerator != null) {
                JsonObject bedrockLangJson = new JsonObject();
                entry.getValue().forEach((translation) -> {
                    if (translation.edition() == Edition.BEDROCK || translation.edition() == Edition.BOTH) {
                        bedrockLangJson.addProperty(translation.key(), translation.value());
                    }
                });
                mappingGenerator.addLocale(entry.getKey(), bedrockLangJson);
            }
        }
    }

    public static class Lang extends ObjectArrayList<Translation> {
    }

    private record Translation(String key ,String value, Edition edition) {
    }

    public enum Edition {
        JAVA, BEDROCK, BOTH
    }
}
