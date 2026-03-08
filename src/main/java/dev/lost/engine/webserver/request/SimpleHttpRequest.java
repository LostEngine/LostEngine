package dev.lost.engine.webserver.request;

import java.io.InputStream;

public interface SimpleHttpRequest {

    String method();

    String path();

    String query();

    InputStream body();

    String getHeader(String name);
}