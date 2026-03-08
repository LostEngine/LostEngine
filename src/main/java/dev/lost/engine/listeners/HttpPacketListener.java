package dev.lost.engine.listeners;

import dev.lost.annotations.NotNull;
import dev.lost.engine.webserver.NettyHttpHandler;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.papermc.paper.network.ChannelInitializeListenerHolder;
import net.kyori.adventure.key.Key;
import net.minecraft.network.Connection;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.network.ServerHandshakePacketListenerImpl;
import org.jetbrains.annotations.Contract;

import java.nio.charset.StandardCharsets;

public class HttpPacketListener {

    public static void inject() {
        ChannelInitializeListenerHolder.addListener(
                Key.key("lost_engine", "http_packet_listener"),
                channel -> channel.pipeline().addAfter("timeout","lost_engine_http_packet_listener", new HttpChannelDupeHandler())
        );
    }

    private static class HttpChannelDupeHandler extends ChannelDuplexHandler {

        private boolean isHandshake(@NotNull ChannelHandlerContext ctx) {
            Channel channel = ctx.channel();
            Connection connection = (Connection) channel.pipeline().get("packet_handler");
            return connection != null && connection.getPacketListener() instanceof ServerHandshakePacketListenerImpl;
        }

        @Contract(pure = true)
        private boolean isHttp(@NotNull String prefix) {
            return prefix.startsWith("GET")
                    || prefix.startsWith("POST")
                    || prefix.startsWith("PUT")
                    || prefix.startsWith("HEAD")
                    || prefix.startsWith("OPTI") // OPTIONS
                    || prefix.startsWith("DELE"); // DELETE
        }

        private void switchToHttp(@NotNull ChannelHandlerContext ctx) {
            ChannelPipeline p = ctx.pipeline();

            // We remove all handlers starting from that one
            String thisName = ctx.name();
            boolean remove = false;
            for (String name : p.names()) {
                if (name.equals(thisName)) remove = true;
                if (remove && p.get(name) != null) p.remove(name);
            }

            // And we put our HTTP handlers
            p.addLast("http_server_codec", new HttpServerCodec());
            p.addLast("http_object_aggregator", new HttpObjectAggregator(25 * 1024 * 1024)); // Same size as in app.tsx
            p.addLast("netty_http_handler", new NettyHttpHandler());
        }

        @Override
        public void channelRead(@NotNull ChannelHandlerContext ctx, Object msg) throws Exception {
            // Both Minecraft Java and HTTP use TCP,
            // so we listen to any packet and if it is an HTTP packet we use it for the Web server
            if (isHandshake(ctx) && msg instanceof ByteBuf byteBuf) {
                FriendlyByteBuf friendlyBuf = new FriendlyByteBuf(byteBuf);
                if (friendlyBuf.readableBytes() >= 4) {
                    String prefix = friendlyBuf.toString(friendlyBuf.readerIndex(), 4, StandardCharsets.US_ASCII);

                    if (isHttp(prefix)) {
                        switchToHttp(ctx);
                        ctx.pipeline().fireChannelRead(byteBuf.retain());
                        return;
                    }
                }
            }
            super.channelRead(ctx, msg);
        }
    }

}
