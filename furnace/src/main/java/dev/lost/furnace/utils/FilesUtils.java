package dev.lost.furnace.utils;

import org.jetbrains.annotations.NotNull;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Stream;

public class FilesUtils {

    /**
     * Deletes the specified folder and its contents asynchronously.
     *
     * @param folder the path to the folder to delete
     */
    public static void deleteFolder(@NotNull Path folder) {
        Thread.startVirtualThread(() -> {
            try {
                deleteFolderSync(folder);
            } catch (Exception ignored) {
            }
        });
    }

    /**
     * Deletes the specified folder and its contents synchronously.
     *
     * @param folder the path to the folder to delete
     * @throws IOException if an I/O error occurs while deleting files or directories
     */
    public static void deleteFolderSync(@NotNull Path folder) throws IOException {
        Files.walkFileTree(folder, new SimpleFileVisitor<>() {
            @Override
            public @NotNull FileVisitResult visitFile(@NotNull Path file, @NotNull BasicFileAttributes attrs)
                    throws IOException {
                Files.deleteIfExists(file);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public @NotNull FileVisitResult postVisitDirectory(@NotNull Path dir, IOException exc)
                    throws IOException {
                Files.deleteIfExists(dir);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    /**
     * Extracts a file from the classpath and writes it to the specified target directory.
     *
     * @param resourcePath the classpath location of the resource to extract
     * @param targetDir    the directory where the extracted file will be placed
     *
     * @return the path to the extracted file in the target directory
     * @throws IOException if an I/O error occurs during extraction
     */
    public static @NotNull Path extractFile(@NotNull String resourcePath, @NotNull Path targetDir) throws IOException {
        Files.createDirectories(targetDir);

        String fileName = Paths.get(resourcePath).getFileName().toString();
        Path targetFile = targetDir.resolve(fileName);

        if (Files.exists(targetFile)) return targetFile;

        try (InputStream in = StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE)
                .getCallerClass()
                .getResourceAsStream(resourcePath)) {

            if (in == null) {
                throw new FileNotFoundException(resourcePath);
            }

            Files.copy(in, targetFile);
        }

        return targetFile;
    }

    public static byte @NotNull [] extractFileBytes(@NotNull String resourcePath) throws IOException {
        try (InputStream in = StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE)
                .getCallerClass()
                .getResourceAsStream(resourcePath)) {

            if (in == null) {
                throw new FileNotFoundException("Resource not found: " + resourcePath);
            }

            return in.readAllBytes();
        }
    }

    /**
     * Extracts a folder from the JAR and copies its contents to the specified target directory.
     *
     * @param folder    the classpath location of the folder to extract
     * @param targetDir the directory where the extracted folder will be placed
     *
     * @throws IOException        if an I/O error occurs during extraction
     * @throws URISyntaxException if the JAR location URI is malformed
     */
    public static void extractFolderFromJar(String folder, Path targetDir) throws IOException, URISyntaxException {
        Files.createDirectories(targetDir);

        URL location = StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE)
                .getCallerClass()
                .getProtectionDomain()
                .getCodeSource()
                .getLocation();

        Path path = Paths.get(location.toURI());

        if (Files.isDirectory(path)) {
            URL folderURL = StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE)
                    .getCallerClass()
                    .getClassLoader()
                    .getResource(folder);
            if (folderURL == null)
                throw new IOException("Resource folder not found: " + folder);
            Path folderPath = Paths.get(folderURL.toURI());
            if (!Files.exists(folderPath)) {
                throw new IllegalStateException("Resource folder not found: " + folder);
            }
            try (Stream<Path> stream = Files.walk(folderPath)) {
                stream.forEach(p -> {
                    try {
                        Path dest = targetDir.resolve(folderPath.relativize(p).toString());
                        if (Files.isDirectory(p))
                            Files.createDirectories(dest);
                        else {
                            Files.createDirectories(dest.getParent());
                            Files.copy(p, dest, StandardCopyOption.REPLACE_EXISTING);
                        }
                    } catch (IOException e) {
                        throw new UncheckedIOException(e);
                    }
                });
            }
        } else {
            try (JarFile jar = new JarFile(path.toFile())) {
                boolean fileFound = false;
                Enumeration<JarEntry> entries = jar.entries();
                while (entries.hasMoreElements()) {
                    JarEntry entry = entries.nextElement();
                    String name = entry.getName();
                    if (name.startsWith(folder + "/")) {
                        fileFound = true;
                        Path dest = targetDir.resolve(name.substring(folder.length() + 1));
                        if (entry.isDirectory()) {
                            Files.createDirectories(dest);
                        } else {
                            Files.createDirectories(dest.getParent());
                            try (InputStream in = jar.getInputStream(entry)) {
                                Files.copy(in, dest, StandardCopyOption.REPLACE_EXISTING);
                            }
                        }
                    }
                }
                if (!fileFound) {
                    throw new IllegalStateException("Resource folder not found: " + folder);
                }
            }
        }
    }


}
