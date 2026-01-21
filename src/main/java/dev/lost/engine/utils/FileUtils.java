package dev.lost.engine.utils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Comparator;
import java.util.List;
import java.util.jar.JarFile;
import java.util.stream.Stream;

/**
 * Another file utility class.
 */
public class FileUtils {

    private static final Gson GSON = new GsonBuilder().create();

    public static void deleteFolder(@NotNull Path folder) throws IOException {
        if (Files.exists(folder)) {
            try (Stream<@NotNull Path> paths = Files.walk(folder)) {
                paths.sorted(Comparator.reverseOrder()).forEach(path -> {
                    try {
                        Files.delete(path);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                });
            }
        }
    }

    public static @NotNull List<ItemConfig> yamlFiles(@NotNull File resourceFolder) {
        List<ItemConfig> configs = new ObjectArrayList<>();
        if (resourceFolder.exists() && resourceFolder.isDirectory()) {
            try (Stream<Path> paths = Files.walk(resourceFolder.toPath())) {
                List<File> files = paths
                        .filter(p -> p.toString().endsWith(".yml") || p.toString().endsWith(".yaml"))
                        .map(Path::toFile)
                        .toList();
                for (File file : files) {
                    configs.add(new ItemConfig(file, YamlConfiguration.loadConfiguration(file)));
                }
            } catch (IOException e) {
                throw new RuntimeException("Failed to list YAML files recursively", e);
            }
        } else {
            throw new IllegalArgumentException("Resource folder does not exist or is not a directory");
        }
        return configs;
    }

    public static void extractDirectoryFromJar(@NotNull Path jarPath, String dirName, Path targetDir) throws IOException {
        try (JarFile jarFile = new JarFile(jarPath.toFile())) {
            jarFile.stream()
                    .filter(entry -> entry.getName().startsWith(dirName + "/") && !entry.isDirectory())
                    .forEach(entry -> {
                        Path outPath = targetDir.resolve(entry.getName().substring(dirName.length() + 1));
                        try {
                            Files.createDirectories(outPath.getParent());
                            try (InputStream is = jarFile.getInputStream(entry)) {
                                Files.copy(is, outPath, StandardCopyOption.REPLACE_EXISTING);
                            }
                        } catch (IOException e) {
                            throw new RuntimeException("Failed to extract " + entry.getName(), e);
                        }
                    });
        }
    }

    public static void extractFileFromJar(@NotNull Path jarPath, String fileName, Path targetFile) throws IOException {
        try (JarFile jarFile = new JarFile(jarPath.toFile())) {
            var entry = jarFile.getJarEntry(fileName);
            if (entry != null && !entry.isDirectory()) {
                Files.createDirectories(targetFile.getParent());
                try (InputStream is = jarFile.getInputStream(entry)) {
                    Files.copy(is, targetFile, StandardCopyOption.REPLACE_EXISTING);
                }
            } else {
                throw new IOException("File " + fileName + " not found in JAR " + jarPath);
            }
        }
    }

    public static void saveJsonToFile(JsonElement json, @NotNull File file) throws IOException {
        //noinspection ResultOfMethodCallIgnored -- Do I care? No.
        file.getParentFile().mkdirs();
        try (FileWriter writer = new FileWriter(file)) {
            GSON.toJson(json, writer);
        }
    }

    public static @NotNull String getFirstResourceSubfolder(@NotNull File resourceFolder, @NotNull File configFile) {
        Path resourcePath = resourceFolder.toPath();
        Path configPath = configFile.toPath();
        Path relative = resourcePath.relativize(configPath);
        if (relative.getNameCount() > 1) {
            return relative.getName(0).toString();
        }
        throw new IllegalArgumentException("Config file is not inside a subfolder of the resource folder");
    }

    public static @NotNull File detectFileExtension(@NotNull File file, @NotNull List<String> extensions) {
        if (file.exists() && file.isFile()) return file;
        for (String ext : extensions) {
            File newfile = new File(file.getPath() + ext);
            if (newfile.exists() && newfile.isFile()) {
                return newfile;
            }
        }
        return file;
    }

    public static @NotNull File withExtension(@NotNull File original, @NotNull String newExt) {
        String ext = newExt.startsWith(".") ? newExt : "." + newExt;
        String parent = original.getParent();
        String name = original.getName();
        int i = name.lastIndexOf('.');
        String base = i == -1 ? name : name.substring(0, i);
        return parent == null ? new File(base + ext) : new File(parent, base + ext);
    }

    public record ItemConfig(File file, YamlConfiguration config) {
    }
}
