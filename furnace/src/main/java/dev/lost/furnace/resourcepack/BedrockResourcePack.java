package dev.lost.furnace.resourcepack;

import dev.lost.annotations.NotNull;
import dev.lost.furnace.files.manifest.Manifest;

public interface BedrockResourcePack extends ResourcePack {

    static @NotNull BedrockResourcePack resourcePack() {
        return new BedrockResourcePackImpl();
    }

    Manifest manifest();

    void manifest(@NotNull Manifest manifest);

    void manifest(String name, String description);
}
