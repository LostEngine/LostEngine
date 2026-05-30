package dev.lost.furnace.resourcepackbuilder;

import dev.lost.annotations.NotNull;
import dev.lost.annotations.Nullable;
import dev.lost.furnace.resourcepack.ResourcePack;
import dev.misieur.packobf.log.LogCallback;
import dev.misieur.packobf.options.Options;
import dev.misieur.packobf.progress.ProgressCallback;

import java.io.File;
import java.nio.file.Path;

public interface ResourcePackBuilder {

    void build(ResourcePack resourcePack, File outputFile, @NotNull BuildOptions option);

    record BuildOptions(@Nullable Options packobfOptions, @Nullable Path cacheFilePath, @Nullable LogCallback logCallback, @Nullable
                        ProgressCallback progressCallback) {
    }

}
