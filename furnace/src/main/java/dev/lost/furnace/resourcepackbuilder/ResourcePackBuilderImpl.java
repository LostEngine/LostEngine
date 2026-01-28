package dev.lost.furnace.resourcepackbuilder;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import dev.lost.furnace.files.ResourcePackFile;
import dev.lost.furnace.files.model.Model;
import dev.lost.furnace.files.texture.Texture;
import dev.lost.furnace.files.unknown.UnknownFile;
import dev.lost.furnace.resourcepack.BedrockResourcePack;
import dev.lost.furnace.resourcepack.JavaResourcePack;
import dev.lost.furnace.resourcepack.ResourcePack;
import dev.misieur.fast.FastImage;
import dev.misieur.fast.Native;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.zip.Deflater;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class ResourcePackBuilderImpl implements ResourcePackBuilder {

    @Override
    public void build(ResourcePack resourcePack, File outputFile, @NotNull BuildOptions option) {
        try (FileOutputStream fos = new FileOutputStream(outputFile); ZipOutputStream zos = new ZipOutputStream(fos)) {
            Gson gson = option == BuildOptions.NO_COMPRESSION
                    ? new GsonBuilder().setPrettyPrinting().create()
                    : new Gson();

            zos.setMethod(ZipOutputStream.DEFLATED);
            zos.setLevel(switch (option) {
                case MAX_COMPRESSION -> Deflater.BEST_COMPRESSION;
                case COMPRESSED -> Deflater.DEFAULT_COMPRESSION;
                case NO_COMPRESSION -> Deflater.NO_COMPRESSION;
            });

            switch (resourcePack) {
                case JavaResourcePack javaPack -> {
                    writeEntry(zos, "pack.mcmeta", gson.toJson(javaPack.mcmeta().json()));
                    for (Map.Entry<String, Model> e : javaPack.models().entrySet()) {
                        writeEntry(zos, e.getKey(), gson.toJson(e.getValue().toJson()));
                    }
                }
                case BedrockResourcePack bedrockPack -> writeEntry(zos, "manifest.json", gson.toJson(bedrockPack.manifest().json()));
                default -> throw new IllegalStateException("Unexpected value: " + resourcePack);
            }

            for (Map.Entry<String, JsonElement> e : resourcePack.jsonFiles().entrySet()) {
                writeEntry(zos, e.getKey(), gson.toJson(e.getValue()));
            }

            for (Map.Entry<String, Texture> e : resourcePack.textures().entrySet()) {
                writeEntry(zos, e.getKey(), e.getValue().file().getBytes(), option == BuildOptions.MAX_COMPRESSION);
            }

            for (Map.Entry<String, UnknownFile> e : resourcePack.unknownFiles().entrySet()) {
                ResourcePackFile rf = e.getValue().file();
                writeEntry(zos, rf.getPath(), rf.getBytes(), option == BuildOptions.MAX_COMPRESSION);
            }

            zos.finish();
        } catch (IOException ex) {
            throw new UncheckedIOException("Cannot build resource-pack zip", ex);
        }
    }

    private void writeEntry(ZipOutputStream zos, String path, @NotNull String utf8Text) throws IOException {
        writeEntry(zos, path, utf8Text.getBytes(StandardCharsets.UTF_8), false);
    }

    private void writeEntry(@NotNull ZipOutputStream zos, String path, byte[] bytes, boolean compress) throws IOException {
        ZipEntry entry = new ZipEntry(path);
        entry.setTime(System.currentTimeMillis());
        zos.putNextEntry(entry);
        if (compress && path.endsWith(".png")) {
            if (!Native.enabled()) Native.load();
            bytes = FastImage.optimizePng(bytes);
        }
        zos.write(bytes);
        zos.closeEntry();
    }
}
