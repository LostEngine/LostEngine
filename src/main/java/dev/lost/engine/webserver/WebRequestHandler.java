package dev.lost.engine.webserver;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.lost.annotations.NotNull;
import dev.lost.annotations.Nullable;
import dev.lost.engine.LostEngine;
import dev.lost.engine.bootstrap.ResourceInjector;
import dev.lost.engine.webserver.request.SimpleHttpRequest;
import dev.lost.engine.webserver.response.SimpleHttpResponse;
import dev.misieur.fast.FastFiles;
import lombok.Getter;
import net.minecraft.core.registries.BuiltInRegistries;

import java.io.*;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.concurrent.ThreadLocalRandom;

public class WebRequestHandler {

    @Getter
    static final String token;
    @Getter
    static final String readOnlyToken;

    static {
        // 32 alphanumeric (A-Z+a-z) characters
        String charset = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
        SecureRandom random = new SecureRandom();
        StringBuilder sb = new StringBuilder(32);
        for (int i = 0; i < 32; i++) {
            sb.append(charset.charAt(random.nextInt(charset.length())));
        }
        token = sb.toString();

        sb = new StringBuilder(32);
        for (int i = 0; i < 32; i++) {
            sb.append(charset.charAt(random.nextInt(charset.length())));
        }
        sb.append("_readonly");
        readOnlyToken = sb.toString();
    }

    public static void handle(@NotNull SimpleHttpRequest req, @NotNull SimpleHttpResponse res) throws IOException {
        String path = req.path();
        String method = req.method();

        if (method.equalsIgnoreCase("OPTIONS")) {
            res.sendOptions();
            return;
        }

        try {
            if (path.startsWith("/api/")) {
                handleApiRoutes(req, res);
            } else {
                handleStaticFiles(req, res);
            }
        } catch (Exception e) {
            LostEngine.logger().error("Error handling http request", e);
            res.send(500, "Internal Server Error", "text/plain");
        }
    }

    static void handleApiRoutes(@NotNull SimpleHttpRequest req, @NotNull SimpleHttpResponse res) throws IOException {
        String query = req.query();
        String token = getToken(query);

        boolean isReadOnlyToken;
        if (token != null && token.equals(readOnlyToken)) {
            isReadOnlyToken = true;
        } else {
            isReadOnlyToken = false;
            if (token == null || !token.equals(WebRequestHandler.token)) {
                res.send(403, "Forbidden", "text/html");
                return;
            }
        }

        if (req.path().equals("/api/status") && req.method().equals("GET")) {
            String json = "{\"status\": \"online\"}";
            res.send(200, json, "application/json");
            return;
        }

        if (req.path().equals("/api/data") && req.method().equals("GET")) {
            JsonObject json = new JsonObject();
            JsonArray items = new JsonArray();
            BuiltInRegistries.ITEM.forEach(item -> items.add(BuiltInRegistries.ITEM.getKey(item).toString()));
            json.add("items", items);
            json.add("files", buildFileTree(new File(LostEngine.getInstance().getDataFolder(), "resources")));
            JsonArray toolMaterials = new JsonArray();
            ResourceInjector.getToolMaterials().forEach((id, toolMaterial) -> toolMaterials.add(id));
            json.add("tool_materials", toolMaterials);
            JsonArray armorMaterials = new JsonArray();
            ResourceInjector.getArmorMaterials().forEach((id, toolMaterial) -> armorMaterials.add(id));
            json.add("armor_materials", toolMaterials);
            res.send(200, json.toString(), "application/json");
            return;
        }

        if (req.path().equals("/api/download_resource") && req.method().equals("GET")) {
            String rawPath = extractQueryParam(query, "path");

            if (rawPath == null || rawPath.isEmpty()) {
                res.send(400, "{\"error\": \"Missing 'path' parameter\"}", "application/json");
                return;
            }

            String cleanPath = sanitizeResourcePath(rawPath);
            if (cleanPath == null) {
                res.send(403, "{\"error\": \"Invalid or unsafe path\"}", "application/json");
                return;
            }

            File baseDir = new File(LostEngine.getInstance().getDataFolder(), "resources");
            File target = new File(baseDir, cleanPath);

            if (!target.exists() || !target.isFile()) {
                res.send(404, "{\"error\": \"File not found\"}", "application/json");
                return;
            }

            String mime = getMimeType(target.getName());
            try (FileInputStream fis = new FileInputStream(target)) {
                byte[] bytes = fis.readAllBytes();
                res.send(200, bytes, mime);
            } catch (IOException e) {
                res.send(500, "{\"error\": \"Failed to read file\"}", "application/json");
            }

            return;
        }

        if (req.path().equals("/api/upload_resource") && req.method().equals("POST")) {
            if (isReadOnlyToken) {
                res.send(403, "Forbidden", "text/html");
                return;
            }
            String contentType = req.getHeader("Content-Type");
            if (contentType == null || !contentType.startsWith("multipart/form-data")) {
                res.send(400, "{\"error\": \"Content-Type must be multipart/form-data\"}", "application/json");
                return;
            }

            String boundary = getBoundary(contentType);
            if (boundary == null) {
                res.send( 400, "{\"error\": \"Boundary missing in Content-Type\"}", "application/json");
                return;
            }

            MultipartParts parts;
            try {
                parts = parseMultipart(req.body(), boundary);
            } catch (Exception e) {
                res.send(400, "{\"error\": \"Invalid multipart data\"}", "application/json");
                return;
            }

            if (parts.path == null || parts.fileBytes == null) {
                res.send(400, "{\"error\": \"Missing 'path' or 'file' part\"}", "application/json");
                return;
            }

            String cleanPath = sanitizeResourcePath(parts.path);
            if (cleanPath == null) {
                res.send(403, "{\"error\": \"Invalid or unsafe path\"}", "application/json");
                return;
            }

            File baseDir = new File(LostEngine.getInstance().getDataFolder(), "resources");
            File target = new File(baseDir, cleanPath);

            try {
                File parent = target.getParentFile();
                if (!parent.exists() && !parent.mkdirs()) {
                    res.send(500, "{\"error\": \"Failed to create directories\"}", "application/json");
                    return;
                }

                try (FileOutputStream fos = new FileOutputStream(target)) {
                    fos.write(parts.fileBytes);
                }
            } catch (IOException e) {
                res.send(500, "{\"error\": \"Failed to save file\"}", "application/json");
                return;
            }

            LostEngine.logger().info("(Web editor) Uploaded file: {}", cleanPath);
            res.send(200,
                    "{\"message\":\"File uploaded successfully\",\"path\":\"" + cleanPath + "\"}",
                    "application/json");
            return;
        }

        if (req.path().equals("/api/delete_resource") && req.method().equals("DELETE")) {
            if (isReadOnlyToken) {
                res.send(403, "Forbidden", "text/html");
                return;
            }
            String rawPath = extractQueryParam(query, "path");

            if (rawPath == null || rawPath.isEmpty()) {
                res.send(400, "{\"error\": \"Missing 'path' parameter\"}", "application/json");
                return;
            }

            String cleanPath = sanitizeResourcePath(rawPath);
            if (cleanPath == null) {
                res.send(403, "{\"error\": \"Invalid or unsafe path\"}", "application/json");
                return;
            }

            File baseDir = new File(LostEngine.getInstance().getDataFolder(), "resources");
            File target = new File(baseDir, cleanPath);
            boolean isDir = target.isDirectory();

            if (!target.exists()) {
                res.send(404, "{\"error\": \"File or directory not found\"}", "application/json");
                return;
            }

            if (!deleteRecursive(target)) {
                res.send(500, "{\"error\": \"Failed to delete resource\"}", "application/json");
                return;
            }

            LostEngine.logger().info("(Web editor) Deleted {}: {}", isDir ? "folder" : "file", cleanPath);
            res.send(200,
                    "{\"message\":\"Resource deleted successfully\",\"path\":\"" + cleanPath + "\"}",
                    "application/json");
            return;
        }

        if (req.path().equals("/api/move_resource") && req.method().equals("POST")) {
            if (isReadOnlyToken) {
                res.send(403, "Forbidden", "text/html");
                return;
            }
            String rawPath = extractQueryParam(query, "path");
            String rawDestination = extractQueryParam(query, "destination");

            if (rawPath == null || rawPath.isEmpty()) {
                res.send(400, "{\"error\": \"Missing 'path' parameter\"}", "application/json");
                return;
            }

            if (rawDestination == null || rawDestination.isEmpty()) {
                res.send(400, "{\"error\": \"Missing 'destination' parameter\"}", "application/json");
                return;
            }

            String cleanPath = sanitizeResourcePath(rawPath);
            String cleanDestination = sanitizeResourcePath(rawDestination);
            if (cleanPath == null || cleanDestination == null) {
                res.send(403, "{\"error\": \"Invalid or unsafe path/destination\"}", "application/json");
                return;
            }

            File baseDir = new File(LostEngine.getInstance().getDataFolder(), "resources");
            File file = new File(baseDir, cleanPath);
            File targetFile = new File(baseDir, cleanDestination);

            if (!file.exists()) {
                res.send(404, "{\"error\": \"File or directory not found\"}", "application/json");
                return;
            }

            if (cleanDestination.startsWith(cleanPath)) {
                // if we move `parent/folder` to `parent/folder/child`, we need to first move it to a temp dir and then to the destination dir
                File tempDir = LostEngine.getInstance()
                        .getDataPath()
                        .resolve(".lost_engine/tmp/")
                        .resolve(cleanPath.replaceAll("/", "_" + ThreadLocalRandom.current().nextInt()))
                        .toFile();
                if (!moveRecursive(file, tempDir)) {
                    res.send(500, "{\"error\": \"Failed to move resource (source -> tempDir)\"}", "application/json");
                    return;
                }

                if (!moveRecursive(tempDir, targetFile)) {
                    res.send(500, "{\"error\": \"Failed to move resource (tempDir -> destination)\"}", "application/json");
                    return;
                }
            } else {
                if (!moveRecursive(file, targetFile)) {
                    res.send(500, "{\"error\": \"Failed to move resource (source -> destination)\"}", "application/json");
                    return;
                }
            }

            boolean isDir = file.isDirectory();
            LostEngine.logger().info("(Web editor) Moved {}: '{}' to '{}'", isDir ? "folder" : "file", cleanPath, cleanDestination);
            res.send(200,
                    "{\"message\":\"Resource moved successfully\",\"destination\":\"" + cleanDestination + "\"}",
                    "application/json");
            return;
        }

        res.send(404, "{\"error\": \"API Endpoint Not Found\"}", "application/json");
    }

    static void handleStaticFiles(@NotNull SimpleHttpRequest req, @NotNull SimpleHttpResponse res) throws IOException, URISyntaxException {
        String userAgent = req.getHeader("User-Agent");

        if (userAgent == null || userAgent.isBlank()) {
            res.send(400, "Bad Request", "text/html");
            return;
        }

        if (userAgent.toLowerCase().contains("minecraft java")) {
            File file = LostEngine.getResourcePackFile();
            res.send(file, "application/zip");
            return;
        }

        if (req.path().equals("/favicon.ico")) {
            res.send(404, "Not Found", "text/html");
            return;
        }

        String query = req.query();
        String token = getToken(query);

        if (token == null || (!token.equals(WebRequestHandler.token) && !token.equals(readOnlyToken))) {
            res.send(403, "Forbidden", "text/html");
            return;
        }

        Path tempHtmlIndex = LostEngine.getInstance()
                .getDataPath()
                .resolve(".lost_engine/cache/generated/index.html");
        if (!Files.exists(tempHtmlIndex)) FastFiles.extractFolderFromJar("generated", tempHtmlIndex.getParent());
        res.send(tempHtmlIndex.toFile(), "text/html");
    }

    private static String getToken(String query) {
        String token = null;

        if (query != null && !query.isEmpty()) {
            String[] pairs = query.split("&");
            for (String pair : pairs) {
                String[] keyValue = pair.split("=", 2);
                if (keyValue.length == 2 && "token".equals(keyValue[0])) {
                    token = keyValue[1];
                    break;
                }
            }
        }
        return token;
    }

    private static @NotNull JsonArray buildFileTree(@NotNull File directory) {
        JsonArray result = new JsonArray();
        if (!directory.exists()) return result;
        if (!directory.isDirectory()) return result;

        File[] files = directory.listFiles();
        if (files == null) return result;

        // sort files alphabetically with directories first
        Arrays.sort(files, (a, b) -> {
            if (a.isDirectory() != b.isDirectory()) return a.isDirectory() ? -1 : 1;
            return a.getName().compareTo(b.getName());
        });

        for (File file : files) {
            if (file.isDirectory()) {
                JsonArray children = buildFileTree(file);
                JsonArray dirArray = new JsonArray();
                dirArray.add(file.getName());
                for (JsonElement child : children) dirArray.add(child);
                result.add(dirArray);
            } else {
                result.add(file.getName());
            }
        }

        return result;
    }

    private static @Nullable String getBoundary(@NotNull String contentType) {
        int idx = contentType.indexOf("boundary=");
        return idx == -1 ? null : contentType.substring(idx + 9).trim();
    }

    private static @NotNull MultipartParts parseMultipart(@NotNull InputStream is, String boundary) throws IOException {
        byte[] data = is.readAllBytes();
        String delimiter = "--" + boundary;
        String endDelimiter = delimiter + "--";

        String text = new String(data, StandardCharsets.ISO_8859_1);
        String[] rawParts = text.split(delimiter);

        MultipartParts parts = new MultipartParts();

        for (String part : rawParts) {
            if (part.isBlank() || part.equals("--") || part.equals(endDelimiter)) continue;

            int headerEnd = part.indexOf("\r\n\r\n");
            if (headerEnd == -1) continue;

            String header = part.substring(0, headerEnd);
            String body = part.substring(headerEnd + 4);

            if (!header.contains("name=")) continue;

            String name = extractName(header);
            if ("path".equals(name)) {
                parts.path = body.trim();
            } else if ("file".equals(name)) {
                int end = body.lastIndexOf("\r\n");
                if (end > 0) body = body.substring(0, end);
                parts.fileBytes = body.getBytes(StandardCharsets.ISO_8859_1);
            }
        }

        return parts;
    }

    private static @Nullable String extractName(@NotNull String header) {
        int idx = header.indexOf("name=\"");
        if (idx == -1) return null;
        int end = header.indexOf("\"", idx + 6);
        return end == -1 ? null : header.substring(idx + 6, end);
    }

    private static String sanitizeResourcePath(String path) {
        if (path == null) return null;
        if (path.contains("..")) return null;
        path = path.replace("\\", "/");
        if (path.startsWith("/") || new File(path).isAbsolute()) return null;
        return path;
    }

    private static String extractQueryParam(String query, String key) {
        if (query == null) return null;
        String[] pairs = query.split("&");
        for (String pair : pairs) {
            String[] kv = pair.split("=", 2);
            if (kv.length == 2 && kv[0].equals(key)) return kv[1];
        }
        return null;
    }

    private static @NotNull String getMimeType(@NotNull String name) {
        String lower = name.toLowerCase();
        if (lower.endsWith(".png")) return "image/png";
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) return "image/jpeg";
        if (lower.endsWith(".gif")) return "image/gif";
        if (lower.endsWith(".json")) return "application/json";
        if (lower.endsWith(".txt")) return "text/plain";
        if (lower.endsWith(".zip")) return "application/zip";
        if (lower.endsWith(".html") || lower.endsWith(".htm")) return "text/html";
        if (lower.endsWith(".css")) return "text/css";
        if (lower.endsWith(".js")) return "application/javascript";
        return "application/octet-stream";
    }

    private static boolean deleteRecursive(@NotNull File file) {
        if (!file.exists()) return true;

        if (file.isDirectory()) {
            File[] children = file.listFiles();
            if (children != null) {
                for (File child : children) {
                    if (!deleteRecursive(child)) return false;
                }
            }
        }

        return file.delete();
    }

    private static boolean moveRecursive(@NotNull File source, @NotNull File target) {
        if (source.isDirectory()) {
            if (!target.exists() && !target.mkdirs()) return false;
            File[] children = source.listFiles();
            if (children != null) {
                for (File child : children) {
                    File targetChild = new File(target, child.getName());
                    if (!moveRecursive(child, targetChild)) return false;
                }
            }
            return source.delete();
        } else {
            if (target.exists() && !target.delete()) return false;

            File parent = target.getParentFile();
            if (parent != null && !parent.exists()) parent.mkdirs();

            return source.renameTo(target);
        }
    }

    private static class MultipartParts {
        String path;
        byte[] fileBytes;
    }
}