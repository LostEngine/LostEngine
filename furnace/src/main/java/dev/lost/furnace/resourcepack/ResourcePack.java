package dev.lost.furnace.resourcepack;

import com.google.gson.JsonElement;
import dev.lost.annotations.NotNull;
import dev.lost.furnace.files.texture.Texture;
import dev.lost.furnace.files.unknown.UnknownFile;
import dev.lost.furnace.resourcepackbuilder.ResourcePackBuilder;

import java.io.File;
import java.util.Map;

public interface ResourcePack {

    static @NotNull JavaResourcePack java() {
        return JavaResourcePack.resourcePack();
    }

    static @NotNull BedrockResourcePack bedrock() {
        return BedrockResourcePack.resourcePack();
    }

    Map<String, JsonElement> jsonFiles();

    ResourcePack jsonFile(String path, JsonElement element);

    Map<String, Texture> textures();

    ResourcePack texture(Texture texture);

    Map<String, UnknownFile> unknownFiles();

    ResourcePack unknownFile(UnknownFile unknownFile);

    void build(File outputFile, ResourcePackBuilder.BuildOptions options);

}
