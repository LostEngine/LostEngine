package dev.lost.furnace.resourcepackbuilder;

import dev.lost.annotations.NotNull;
import dev.lost.furnace.resourcepack.ResourcePack;

import java.io.File;

public interface ResourcePackBuilder {

    void build(ResourcePack resourcePack, File outputFile, @NotNull BuildOptions option);

    enum BuildOptions {
        MAX_COMPRESSION, // best compression level
        COMPRESSED, // balanced compression level
        NO_COMPRESSION // fastest but also the more readable JSON files
    }

}
