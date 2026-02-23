package dev.lost.engine;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import dev.lost.engine.bootstrap.ResourceInjector;
import dev.lost.annotations.NotNull;
import dev.lost.annotations.Nullable;
import lombok.Getter;
import net.minecraft.core.registries.BuiltInRegistries;
import org.apache.commons.lang3.exception.ExceptionUtils;

import java.io.*;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadLocalRandom;

public class WebServer {

    private static HttpServer server;
    @Getter
    private static final String token;
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

    public static void start(int port) throws IOException {
        server = HttpServer.create(new InetSocketAddress(port), 0);

        server.createContext("/", exchange -> {
            String path = exchange.getRequestURI().getPath();
            String method = exchange.getRequestMethod();

            if (method.equalsIgnoreCase("OPTIONS")) {
                exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
                exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, DELETE, OPTIONS");
                exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type, Authorization");
                exchange.sendResponseHeaders(204, -1);
                return;
            }

            try {
                if (path.startsWith("/api/")) {
                    handleApiRoutes(exchange, path, method);
                } else {
                    handleStaticFiles(exchange, path);
                }
            } catch (IOException e) {
                LostEngine.logger().error("Error handling http request", e);
                sendResponse(exchange, 500, "Internal Server Error", "text/plain");
            }
        });

        server.setExecutor(Executors.newFixedThreadPool(10, runnable -> {
            Thread t = new Thread(runnable);
            t.setName("web-server-worker");
            t.setDaemon(true);
            return t;
        }));
        server.start();
    }

    public static void stop() {
        if (server != null) {
            server.stop(0);
            server = null;
        }
    }

    private static void handleApiRoutes(@NotNull HttpExchange exchange, @NotNull String path, String method) throws IOException {

        String query = exchange.getRequestURI().getQuery();
        String token = getToken(query);

        boolean isReadOnlyToken;
        if (token != null && token.equals(readOnlyToken)) {
            isReadOnlyToken = true;
        } else {
            isReadOnlyToken = false;
            if (token == null || !token.equals(WebServer.token)) {
                sendResponse(exchange, 403, "Forbidden", "text/html");
                return;
            }
        }

        if (path.equals("/api/status") && method.equals("GET")) {
            String json = "{\"status\": \"online\"}";
            sendResponse(exchange, 200, json, "application/json");
            return;
        }

        if (path.equals("/api/data") && method.equals("GET")) {
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
            sendResponse(exchange, 200, json.toString(), "application/json");
            return;
        }

        if (path.equals("/api/download_resource") && method.equals("GET")) {
            String rawPath = extractQueryParam(query, "path");

            if (rawPath == null || rawPath.isEmpty()) {
                sendResponse(exchange, 400, "{\"error\": \"Missing 'path' parameter\"}", "application/json");
                return;
            }

            String cleanPath = sanitizeResourcePath(rawPath);
            if (cleanPath == null) {
                sendResponse(exchange, 403, "{\"error\": \"Invalid or unsafe path\"}", "application/json");
                return;
            }

            File baseDir = new File(LostEngine.getInstance().getDataFolder(), "resources");
            File target = new File(baseDir, cleanPath);

            if (!target.exists() || !target.isFile()) {
                sendResponse(exchange, 404, "{\"error\": \"File not found\"}", "application/json");
                return;
            }

            String mime = getMimeType(target.getName());
            try (FileInputStream fis = new FileInputStream(target)) {
                byte[] bytes = fis.readAllBytes();
                exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
                exchange.getResponseHeaders().set("Content-Type", mime);
                exchange.sendResponseHeaders(200, bytes.length);

                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(bytes);
                }
            } catch (IOException e) {
                sendResponse(exchange, 500, "{\"error\": \"Failed to read file\"}", "application/json");
            }

            return;
        }

        if (path.equals("/api/upload_resource") && method.equals("POST")) {
            if (isReadOnlyToken) {
                sendResponse(exchange, 403, "Forbidden", "text/html");
                return;
            }
            String contentType = exchange.getRequestHeaders().getFirst("Content-Type");
            if (contentType == null || !contentType.startsWith("multipart/form-data")) {
                sendResponse(exchange, 400, "{\"error\": \"Content-Type must be multipart/form-data\"}", "application/json");
                return;
            }

            String boundary = getBoundary(contentType);
            if (boundary == null) {
                sendResponse(exchange, 400, "{\"error\": \"Boundary missing in Content-Type\"}", "application/json");
                return;
            }

            MultipartParts parts;
            try {
                parts = parseMultipart(exchange.getRequestBody(), boundary);
            } catch (Exception e) {
                sendResponse(exchange, 400, "{\"error\": \"Invalid multipart data\"}", "application/json");
                return;
            }

            if (parts.path == null || parts.fileBytes == null) {
                sendResponse(exchange, 400, "{\"error\": \"Missing 'path' or 'file' part\"}", "application/json");
                return;
            }

            String cleanPath = sanitizeResourcePath(parts.path);
            if (cleanPath == null) {
                sendResponse(exchange, 403, "{\"error\": \"Invalid or unsafe path\"}", "application/json");
                return;
            }

            File baseDir = new File(LostEngine.getInstance().getDataFolder(), "resources");
            File target = new File(baseDir, cleanPath);

            try {
                File parent = target.getParentFile();
                if (!parent.exists() && !parent.mkdirs()) {
                    sendResponse(exchange, 500, "{\"error\": \"Failed to create directories\"}", "application/json");
                    return;
                }

                try (FileOutputStream fos = new FileOutputStream(target)) {
                    fos.write(parts.fileBytes);
                }
            } catch (IOException e) {
                sendResponse(exchange, 500, "{\"error\": \"Failed to save file\"}", "application/json");
                return;
            }

            LostEngine.logger().info("(Web editor) Uploaded file: {}", cleanPath);
            sendResponse(exchange, 200,
                    "{\"message\":\"File uploaded successfully\",\"path\":\"" + cleanPath + "\"}",
                    "application/json");
            return;
        }

        if (path.equals("/api/delete_resource") && method.equals("DELETE")) {
            if (isReadOnlyToken) {
                sendResponse(exchange, 403, "Forbidden", "text/html");
                return;
            }
            String rawPath = extractQueryParam(query, "path");

            if (rawPath == null || rawPath.isEmpty()) {
                sendResponse(exchange, 400, "{\"error\": \"Missing 'path' parameter\"}", "application/json");
                return;
            }

            String cleanPath = sanitizeResourcePath(rawPath);
            if (cleanPath == null) {
                sendResponse(exchange, 403, "{\"error\": \"Invalid or unsafe path\"}", "application/json");
                return;
            }

            File baseDir = new File(LostEngine.getInstance().getDataFolder(), "resources");
            File target = new File(baseDir, cleanPath);
            boolean isDir = target.isDirectory();

            if (!target.exists()) {
                sendResponse(exchange, 404, "{\"error\": \"File or directory not found\"}", "application/json");
                return;
            }

            if (!deleteRecursive(target)) {
                sendResponse(exchange, 500, "{\"error\": \"Failed to delete resource\"}", "application/json");
                return;
            }

            LostEngine.logger().info("(Web editor) Deleted {}: {}", isDir ? "folder" : "file", cleanPath);
            sendResponse(exchange, 200,
                    "{\"message\":\"Resource deleted successfully\",\"path\":\"" + cleanPath + "\"}",
                    "application/json");
            return;
        }

        if (path.equals("/api/move_resource") && method.equals("POST")) {
            if (isReadOnlyToken) {
                sendResponse(exchange, 403, "Forbidden", "text/html");
                return;
            }
            String rawPath = extractQueryParam(query, "path");
            String rawDestination = extractQueryParam(query, "destination");

            if (rawPath == null || rawPath.isEmpty()) {
                sendResponse(exchange, 400, "{\"error\": \"Missing 'path' parameter\"}", "application/json");
                return;
            }

            if (rawDestination == null || rawDestination.isEmpty()) {
                sendResponse(exchange, 400, "{\"error\": \"Missing 'destination' parameter\"}", "application/json");
                return;
            }

            String cleanPath = sanitizeResourcePath(rawPath);
            String cleanDestination = sanitizeResourcePath(rawDestination);
            if (cleanPath == null || cleanDestination == null) {
                sendResponse(exchange, 403, "{\"error\": \"Invalid or unsafe path/destination\"}", "application/json");
                return;
            }

            File baseDir = new File(LostEngine.getInstance().getDataFolder(), "resources");
            File file = new File(baseDir, cleanPath);
            File targetFile = new File(baseDir, cleanDestination);

            if (!file.exists()) {
                sendResponse(exchange, 404, "{\"error\": \"File or directory not found\"}", "application/json");
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
                    sendResponse(exchange, 500, "{\"error\": \"Failed to move resource (source -> tempDir)\"}", "application/json");
                    return;
                }

                if (!moveRecursive(tempDir, targetFile)) {
                    sendResponse(exchange, 500, "{\"error\": \"Failed to move resource (tempDir -> destination)\"}", "application/json");
                    return;
                }
            } else {
                if (!moveRecursive(file, targetFile)) {
                    sendResponse(exchange, 500, "{\"error\": \"Failed to move resource (source -> destination)\"}", "application/json");
                    return;
                }
            }

            boolean isDir = file.isDirectory();
            LostEngine.logger().info("(Web editor) Moved {}: '{}' to '{}'", isDir ? "folder" : "file", cleanPath, cleanDestination);
            sendResponse(exchange, 200,
                    "{\"message\":\"Resource moved successfully\",\"destination\":\"" + cleanDestination + "\"}",
                    "application/json");
            return;
        }

        sendResponse(exchange, 404, "{\"error\": \"API Endpoint Not Found\"}", "application/json");
    }

    private static void handleStaticFiles(@NotNull HttpExchange exchange, @NotNull String path) throws IOException {
        List<String> userAgents = exchange.getRequestHeaders().get("User-Agent");

        if (userAgents == null || userAgents.isEmpty()) {
            sendResponse(exchange, 400, "Bad Request", "text/html");
            return;
        }

        if (userAgents.getFirst().toLowerCase().contains("minecraft java")) {
            File file = LostEngine.getResourcePackFile();
            serveFile(exchange, file, "application/zip");
            return;
        }

        if (path.equals("/favicon.ico")) {
            sendResponse(exchange, 404, "Not Found", "text/html");
            return;
        }

        String query = exchange.getRequestURI().getQuery();
        String token = getToken(query);

        if (token == null || (!token.equals(WebServer.token) && !token.equals(readOnlyToken))) {
            sendResponse(exchange, 403, "Forbidden", "text/html");
            return;
        }

        serveResource(exchange, "generated/index.html", "text/html");
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

    private static void serveFile(@NotNull HttpExchange exchange, @NotNull File file, String mimeType) throws IOException {
        if (!file.exists()) {
            sendResponse(exchange, 404, "File Not Found", "text/html");
            return;
        }

        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().set("Content-Type", mimeType);
        exchange.sendResponseHeaders(200, file.length());

        try (OutputStream os = exchange.getResponseBody();
             FileInputStream fs = new FileInputStream(file)) {
            fs.transferTo(os);
        }
    }

    private static void serveResource(@NotNull HttpExchange exchange, String resourcePath, String mimeType) throws IOException {
        try (InputStream is = WebServer.class.getResourceAsStream("/" + resourcePath)) {
            if (is == null) {
                sendResponse(exchange, 404, "Not Found", "text/html");
                return;
            }

            byte[] content = is.readAllBytes();
            exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
            exchange.getResponseHeaders().set("Content-Type", mimeType);
            exchange.sendResponseHeaders(200, content.length);

            try (OutputStream os = exchange.getResponseBody()) {
                os.write(content);
            }
        }
    }


    private static void sendResponse(@NotNull HttpExchange exchange, int statusCode, @NotNull String response, String contentType) throws IOException {
        byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().set("Content-Type", contentType);
        exchange.sendResponseHeaders(statusCode, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
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

    private static class MultipartParts {
        String path;
        byte[] fileBytes;
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

}