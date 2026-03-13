package dev.lost.engine.webserver.response;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

public interface SimpleHttpResponse {

    void setHeader(String name, String value);

    default void send(int status, String body, String contentType) throws IOException {
        send(status, body != null ? body.getBytes(StandardCharsets.UTF_8) : null, contentType);
    }

    void send(int status, byte[] body, String contentType) throws IOException;

    void send(File file, String contentType) throws IOException;

    void sendOptions() throws IOException;
}