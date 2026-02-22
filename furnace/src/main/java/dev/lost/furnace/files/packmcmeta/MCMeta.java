package dev.lost.furnace.files.packmcmeta;

import com.google.gson.JsonElement;
import dev.lost.annotations.NotNull;
import org.jetbrains.annotations.Contract;

public interface MCMeta {

    @NotNull
    @Contract(value = "_, _ -> new", pure = true)
    static MCMeta meta(int packFormat, String description) {
        return new MCMetaImpl(packFormat, description);
    }

    int packFormat();

    String description();

    JsonElement json();

}
