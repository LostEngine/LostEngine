package dev.lost.furnace.resourcepack;

import dev.lost.furnace.files.model.Model;
import dev.lost.furnace.files.packmcmeta.MCMeta;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

public interface JavaResourcePack extends ResourcePack {

    @Contract(" -> new")
    static @NotNull JavaResourcePack resourcePack() {
        return new JavaResourcePackImpl();
    }

    MCMeta mcmeta();

    JavaResourcePack mcmeta(@NotNull MCMeta mcmeta);

    JavaResourcePack mcmeta(int packFormat, String description);

    JavaResourcePack model(Model model);

    Map<String, Model> models();

}
