package dev.lost.engine.webserver;

import com.sun.net.httpserver.HttpServer;
import dev.lost.engine.webserver.request.SunHttpRequest;
import dev.lost.engine.webserver.response.SunHttpResponse;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

public class WebServer {

    private static HttpServer server;

    public static void start(int port) throws IOException {
        server = HttpServer.create(new InetSocketAddress(port), 0);

        server.createContext("/", exchange -> WebRequestHandler.handle(new SunHttpRequest(exchange), new SunHttpResponse(exchange)));

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

}