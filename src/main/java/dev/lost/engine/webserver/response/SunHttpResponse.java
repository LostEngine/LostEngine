package dev.lost.engine.webserver.response;

import com.sun.net.httpserver.HttpExchange;
import dev.lost.annotations.NotNull;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;

public record SunHttpResponse(HttpExchange exchange) implements SimpleHttpResponse {

    public void setHeader(String name, String value) {
        exchange.getResponseHeaders().set(name, value);
    }

    public void send(int status, byte @NotNull [] body, String contentType, String newSessionID) throws IOException {
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().set("Content-Type", contentType);
        if (newSessionID != null) exchange.getResponseHeaders().add(
                "Set-Cookie",
                "SESSIONID=%s; Path=/; HttpOnly; SameSite=Strict".formatted(newSessionID)
        );
        exchange.sendResponseHeaders(status, body.length);

        try (OutputStream os = exchange.getResponseBody()) {
            os.write(body);
        }
    }

    @Override
    public void send(@NotNull File file, String contentType, String newSessionID) throws IOException {
        if (!file.exists()) {
            send(404, "File Not Found", "text/html", newSessionID);
            return;
        }

        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().set("Content-Type", contentType);
        if (newSessionID != null) exchange.getResponseHeaders().add(
                "Set-Cookie",
                "SESSIONID=%s; Path=/; HttpOnly; SameSite=Strict".formatted(newSessionID)
        );
        exchange.sendResponseHeaders(200, file.length());

        try (OutputStream os = exchange.getResponseBody();
             FileInputStream fs = new FileInputStream(file)) {
            fs.transferTo(os);
        }
    }

    @Override
    public void sendOptions() throws IOException {
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, DELETE, OPTIONS");
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type, Authorization");
        exchange.sendResponseHeaders(204, -1);
    }
}