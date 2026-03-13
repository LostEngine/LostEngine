package dev.lost.engine.webserver.response;

import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.DefaultFileRegion;
import io.netty.handler.codec.http.*;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;

public record NettyHttpResponse(ChannelHandlerContext ctx, HttpHeaders headers) implements SimpleHttpResponse {

    public NettyHttpResponse(ChannelHandlerContext ctx) {
        this(ctx, new DefaultHttpHeaders());
    }

    @Override
    public void setHeader(String name, String value) {
        headers.set(name, value);
    }

    @Override
    public void send(int status, byte[] body, String contentType) {
        FullHttpResponse response = new DefaultFullHttpResponse(
                HttpVersion.HTTP_1_1,
                HttpResponseStatus.valueOf(status),
                Unpooled.wrappedBuffer(body)
        );
        response.headers().set(headers);
        response.headers().set(HttpHeaderNames.ACCESS_CONTROL_ALLOW_ORIGIN, "*");
        response.headers().set(HttpHeaderNames.CONTENT_TYPE, contentType);
        response.headers().set(HttpHeaderNames.CONTENT_LENGTH, body.length);

        ctx.writeAndFlush(response);
    }

    @Override
    public void send(File file, String contentType) throws IOException {
        if (!file.exists()) {
            send(404, "File Not Found", "text/html");
            return;
        }

        @SuppressWarnings("resource") // closed with future
        RandomAccessFile raf = new RandomAccessFile(file, "r");
        HttpResponse response = new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK);
        response.headers().set(headers);
        response.headers().set(HttpHeaderNames.ACCESS_CONTROL_ALLOW_ORIGIN, "*");
        response.headers().set(HttpHeaderNames.CONTENT_TYPE, contentType);
        response.headers().set(HttpHeaderNames.CONTENT_LENGTH, file.length());

        ctx.write(response);
        ChannelFuture future = ctx.write(new DefaultFileRegion(raf.getChannel(), 0, raf.length()));
        ctx.writeAndFlush(LastHttpContent.EMPTY_LAST_CONTENT);
        future.addListener(f -> {
            try {
                raf.close();
            } catch (IOException ignored) {}
        });
    }

    @Override
    public void sendOptions() {
        FullHttpResponse response = new DefaultFullHttpResponse(
                HttpVersion.HTTP_1_1,
                HttpResponseStatus.NO_CONTENT
        );
        response.headers().set(headers);
        response.headers().set(HttpHeaderNames.ACCESS_CONTROL_ALLOW_ORIGIN, "*");
        response.headers().set(HttpHeaderNames.ACCESS_CONTROL_ALLOW_METHODS, "GET, POST, DELETE, OPTIONS");
        response.headers().set(HttpHeaderNames.ACCESS_CONTROL_ALLOW_HEADERS, "Content-Type, Authorization");
        response.headers().set(HttpHeaderNames.CONTENT_LENGTH, -1);

        ctx.writeAndFlush(response);
    }
}