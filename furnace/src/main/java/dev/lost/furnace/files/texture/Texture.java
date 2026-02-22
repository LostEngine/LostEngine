package dev.lost.furnace.files.texture;

import dev.lost.annotations.NotNull;
import dev.lost.furnace.files.ResourcePackFile;
import org.jetbrains.annotations.Contract;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;

public interface Texture {

    String path();

    ResourcePackFile file();

    @NotNull
    @Contract(value = "_, _ -> new", pure = true)
    static Texture file(String path, File file) throws IOException {
        return new TextureImpl(path, file);
    }

    @NotNull
    @Contract(value = "_, _ -> new", pure = true)
    static Texture bytes(String path, byte[] bytes) {
        return new TextureImpl(path, bytes);
    }

    @NotNull
    @Contract(value = "_ -> new", pure = true)
    static Texture rcpFile(ResourcePackFile file) {
        return new TextureImpl(file);
    }

    @NotNull
    @Contract(value = "_, _ -> new", pure = true)
    static Texture image(String path, BufferedImage image) throws IOException {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            ImageIO.write(image, "png", baos);
            return new TextureImpl(path, baos.toByteArray());
        }
    }

}
