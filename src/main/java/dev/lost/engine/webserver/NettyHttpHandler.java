package dev.lost.engine.webserver;

import dev.lost.engine.webserver.request.NettyHttpRequest;
import dev.lost.engine.webserver.response.NettyHttpResponse;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.timeout.ReadTimeoutException;

public class NettyHttpHandler extends SimpleChannelInboundHandler<FullHttpRequest> {

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest req) throws Exception {
        WebRequestHandler.handle(new NettyHttpRequest(req), new NettyHttpResponse(ctx));
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        if (cause instanceof ReadTimeoutException) {
            ctx.close();
            return;
        }

        cause.printStackTrace();
        ctx.close();
    }
}