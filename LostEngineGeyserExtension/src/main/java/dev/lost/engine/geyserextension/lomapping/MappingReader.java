package dev.lost.engine.geyserextension.lomapping;

import com.google.gson.Gson;
import dev.lost.annotations.NotNull;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;

public class MappingReader {

    private static final Gson GSON = new Gson();

    public static @NotNull Mapping read(@NotNull InputStream input) {
        try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(input))) {
            return GSON.fromJson(bufferedReader, MappingImpl.class);
        } catch (Exception e) {
            throw new RuntimeException("Failed to read Lost Engine mapping from input stream", e);
        }
    }

    public static @NotNull Mapping read(@NotNull Path path) {
        try (InputStream input = Files.newInputStream(path)) {
            return read(input);
        } catch (Exception e) {
            throw new RuntimeException("Failed to read Lost Engine mapping from path: " + path, e);
        }
    }

}
