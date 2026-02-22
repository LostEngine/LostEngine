package dev.lost.furnace.resourcepack;

import com.google.gson.JsonElement;
import dev.lost.annotations.NotNull;
import dev.lost.furnace.files.manifest.Manifest;
import dev.lost.furnace.files.texture.Texture;
import dev.lost.furnace.files.unknown.UnknownFile;
import dev.lost.furnace.resourcepackbuilder.ResourcePackBuilder;
import dev.lost.furnace.resourcepackbuilder.ResourcePackBuilderImpl;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;

import java.io.File;
import java.util.Map;

public class BedrockResourcePackImpl implements BedrockResourcePack {

    private Manifest packManifest;
    private final Object2ObjectOpenHashMap<String, JsonElement> jsonFiles = new Object2ObjectOpenHashMap<>();
    private final Object2ObjectOpenHashMap<String, Texture> textures = new Object2ObjectOpenHashMap<>();
    private final Object2ObjectOpenHashMap<String, UnknownFile> unknownFiles = new Object2ObjectOpenHashMap<>();

    @Override
    public Manifest manifest() {
        return packManifest;
    }

    @Override
    public void manifest(@NotNull Manifest manifest) {
        packManifest = manifest;
    }

    @Override
    public void manifest(String name, String description) {
        packManifest = Manifest.manifest(name, description);
    }

    @Override
    public Map<String, JsonElement> jsonFiles() {
        return jsonFiles;
    }

    @Override
    public BedrockResourcePackImpl jsonFile(String path, JsonElement element) {
        jsonFiles.put(path, element);
        return this;
    }

    @Override
    public Map<String, Texture> textures() {
        return textures;
    }

    @Override
    public BedrockResourcePackImpl texture(Texture texture) {
        textures.put(texture.path(), texture);
        return this;
    }

    @Override
    public Map<String, UnknownFile> unknownFiles() {
        return unknownFiles;
    }

    @Override
    public BedrockResourcePackImpl unknownFile(UnknownFile unknownFile) {
        unknownFiles.put(unknownFile.path(), unknownFile);
        return this;
    }

    @Override
    public void build(File outputFile, ResourcePackBuilder.BuildOptions options) {
        new ResourcePackBuilderImpl().build(this, outputFile, options);
    }
}
