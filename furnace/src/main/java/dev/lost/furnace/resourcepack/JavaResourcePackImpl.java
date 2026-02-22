package dev.lost.furnace.resourcepack;

import com.google.gson.JsonElement;
import dev.lost.annotations.NotNull;
import dev.lost.furnace.files.model.Model;
import dev.lost.furnace.files.packmcmeta.MCMeta;
import dev.lost.furnace.files.texture.Texture;
import dev.lost.furnace.files.unknown.UnknownFile;
import dev.lost.furnace.resourcepackbuilder.ResourcePackBuilder;
import dev.lost.furnace.resourcepackbuilder.ResourcePackBuilderImpl;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;

import java.io.File;
import java.util.Map;

public class JavaResourcePackImpl implements JavaResourcePack {

    private MCMeta mcmeta;
    private final Object2ObjectOpenHashMap<String, JsonElement> jsonFiles = new Object2ObjectOpenHashMap<>();
    private final Object2ObjectOpenHashMap<String, Texture> textures = new Object2ObjectOpenHashMap<>();
    private final Object2ObjectOpenHashMap<String, UnknownFile> unknownFiles = new Object2ObjectOpenHashMap<>();
    private final Object2ObjectOpenHashMap<String, Model> models = new Object2ObjectOpenHashMap<>();

    @Override
    public MCMeta mcmeta() {
        return mcmeta;
    }

    @Override
    public JavaResourcePack mcmeta(@NotNull MCMeta mcmeta) {
        this.mcmeta = mcmeta;
        return this;
    }

    @Override
    public JavaResourcePack mcmeta(int packFormat, String description) {
        this.mcmeta = MCMeta.meta(packFormat, description);
        return this;
    }

    @Override
    public JavaResourcePack model(Model model) {
        models.put(model.path(), model);
        return this;
    }

    @Override
    public Map<String, Model> models() {
        return models;
    }

    @Override
    public Map<String, JsonElement> jsonFiles() {
        return jsonFiles;
    }

    @Override
    public JavaResourcePack jsonFile(String path, JsonElement element) {
        jsonFiles.put(path, element);
        return this;
    }

    @Override
    public Map<String, Texture> textures() {
        return textures;
    }

    @Override
    public JavaResourcePack texture(Texture texture) {
        textures.put(texture.path(), texture);
        return this;
    }

    @Override
    public Map<String, UnknownFile> unknownFiles() {
        return unknownFiles;
    }

    @Override
    public JavaResourcePack unknownFile(UnknownFile unknownFile) {
        unknownFiles.put(unknownFile.path(), unknownFile);
        return this;
    }

    @Override
    public void build(File outputFile, ResourcePackBuilder.BuildOptions options) {
        new ResourcePackBuilderImpl().build(this, outputFile, options);
    }

}
