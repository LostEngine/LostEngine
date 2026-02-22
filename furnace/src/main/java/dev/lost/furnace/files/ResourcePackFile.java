package dev.lost.furnace.files;

import dev.lost.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static java.nio.file.Files.readAllBytes;

public class ResourcePackFile {
    private final String path;
    private final byte[] content;

    public ResourcePackFile(String path, byte[] content) {
        this.path = path;
        this.content = content;
    }

    public ResourcePackFile(String path, @NotNull String utf8Text) {
        this(path, utf8Text.getBytes(StandardCharsets.UTF_8));
    }

    public ResourcePackFile(String path, @NotNull File file) throws IOException {
        this(path, readAllBytes(file.toPath()));
    }

    public String getPath() {
        return path;
    }

    public byte[] getBytes() {
        return content;
    }

    public String getText() {
        return new String(content, StandardCharsets.UTF_8);
    }
}
