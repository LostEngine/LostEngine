package dev.lost.furnace.resourcepackbuilder;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import dev.lost.annotations.NotNull;
import dev.lost.furnace.files.ResourcePackFile;
import dev.lost.furnace.files.model.Model;
import dev.lost.furnace.files.texture.Texture;
import dev.lost.furnace.files.unknown.UnknownFile;
import dev.lost.furnace.resourcepack.BedrockResourcePack;
import dev.lost.furnace.resourcepack.JavaResourcePack;
import dev.lost.furnace.resourcepack.ResourcePack;
import dev.misieur.packobf.PackOBF;
import dev.misieur.packobf.log.LogLevel;
import it.unimi.dsi.fastutil.io.FastByteArrayOutputStream;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class ResourcePackBuilderImpl implements ResourcePackBuilder {

    private static final Gson GSON = new Gson();

    @Override
    public void build(ResourcePack resourcePack, File outputFile, @NotNull BuildOptions options) {
        try (FastByteArrayOutputStream baos = new FastByteArrayOutputStream(); ZipOutputStream zos = new ZipOutputStream(baos)) {
            switch (resourcePack) {
                case JavaResourcePack javaPack -> {
                    writeEntry(zos, "pack.mcmeta", GSON.toJson(javaPack.mcmeta().json()));
                    for (Map.Entry<String, Model> e : javaPack.models().entrySet()) {
                        writeEntry(zos, e.getKey(), GSON.toJson(e.getValue().toJson()));
                    }
                }
                case BedrockResourcePack bedrockPack ->
                        writeEntry(zos, "manifest.json", GSON.toJson(bedrockPack.manifest().json()));
                default -> throw new IllegalStateException("Unexpected value: " + resourcePack);
            }

            for (Map.Entry<String, JsonElement> e : resourcePack.jsonFiles().entrySet()) {
                writeEntry(zos, e.getKey(), GSON.toJson(e.getValue()));
            }

            for (Map.Entry<String, Texture> e : resourcePack.textures().entrySet()) {
                writeEntry(zos, e.getKey(), e.getValue().file().getBytes());
            }

            for (Map.Entry<String, UnknownFile> e : resourcePack.unknownFiles().entrySet()) {
                ResourcePackFile rf = e.getValue().file();
                writeEntry(zos, rf.getPath(), rf.getBytes());
            }

            zos.finish();
            byte[] bytes = baos.toByteArray();
            packobf:
            {
                if (!(resourcePack instanceof JavaResourcePack) || options.packobfOptions() == null) break packobf;

                try {
                    bytes = PackOBF.optimizeZip(
                            bytes,
                            options.packobfOptions(),
                            options.logCallback() != null ? options.logCallback() : (level, message) -> {
                            },
                            options.progressCallback() != null ? options.progressCallback() : progress -> {
                            },
                            options.cacheFilePath()
                    );
                } catch (IOException e) {
                    if (options.logCallback() == null) break packobf;
                    StringWriter sw = new StringWriter();
                    e.printStackTrace(new PrintWriter(sw, true));
                    options.logCallback().onLog(
                            LogLevel.ERROR,
                            sw.toString()
                    );
                }
            }
            try (FileOutputStream fos = new FileOutputStream(outputFile)) {
                fos.write(bytes);
            }
        } catch (IOException ex) {
            throw new UncheckedIOException("Cannot build resource pack zip", ex);
        }
    }

    private void writeEntry(ZipOutputStream zos, String path, @NotNull String utf8Text) throws IOException {
        writeEntry(zos, path, utf8Text.getBytes(StandardCharsets.UTF_8));
    }

    private void writeEntry(@NotNull ZipOutputStream zos, String path, byte[] bytes) throws IOException {
        ZipEntry entry = new ZipEntry(path);
        entry.setTime(System.currentTimeMillis());
        zos.putNextEntry(entry);
        zos.write(bytes);
        zos.closeEntry();
    }
}
