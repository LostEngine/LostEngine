package dev.lost.engine.webserver.request;

import io.netty.buffer.ByteBufInputStream;
import io.netty.handler.codec.http.FullHttpRequest;

import java.io.InputStream;
import java.net.URI;

public record NettyHttpRequest(FullHttpRequest request, URI uri) implements SimpleHttpRequest {

    public NettyHttpRequest(FullHttpRequest request) {
        this(request, URI.create(request.uri()));
    }

    public String method() {
        return request.method().name();
    }

    public String path() {
        return uri.getPath();
    }

    @Override
    public String query() {
        return uri.getQuery();
    }

    public InputStream body() {
        return new ByteBufInputStream(request.content());
    }

    @Override
    public String getHeader(String name) {
        return request.headers().get(name);
    }
}