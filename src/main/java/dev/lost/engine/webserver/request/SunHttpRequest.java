package dev.lost.engine.webserver.request;

import com.sun.net.httpserver.HttpExchange;

import java.io.InputStream;

public record SunHttpRequest(HttpExchange exchange) implements SimpleHttpRequest {
    public String method() {
        return exchange.getRequestMethod();
    }

    public String path() {
        return exchange.getRequestURI().getPath();
    }

    public String query() {
        return exchange.getRequestURI().getQuery();
    }

    public InputStream body() {
        return exchange.getRequestBody();
    }

    public String getHeader(String name) {
        return exchange.getRequestHeaders().getFirst(name);
    }
}