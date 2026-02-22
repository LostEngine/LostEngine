package dev.lost.furnace.files.unknown;

import dev.lost.annotations.NotNull;
import dev.lost.furnace.files.ResourcePackFile;
import org.jetbrains.annotations.Contract;

import java.io.File;
import java.io.IOException;

public interface UnknownFile {

    @NotNull
    @Contract(value = "_ -> new", pure = true)
    static UnknownFile rcpFile(ResourcePackFile file) {
        return new UnknownFileImpl(file.getPath(), file);
    }

    @NotNull
    @Contract(value = "_, _ -> new", pure = true)
    static UnknownFile file(String path, File file) throws IOException {
        return new UnknownFileImpl(path, file);
    }

    @NotNull
    @Contract(value = "_, _ -> new", pure = true)
    static UnknownFile bytes(String path, byte[] bytes) {
        return new UnknownFileImpl(path, bytes);
    }

    @NotNull
    @Contract(value = "_, _ -> new", pure = true)
    static UnknownFile utf8(String path, String utf8Text) {
        return new UnknownFileImpl(path, utf8Text);
    }

    String path();

    ResourcePackFile file();

}
