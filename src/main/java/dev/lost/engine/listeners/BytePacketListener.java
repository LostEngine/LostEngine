package dev.lost.engine.listeners;

import dev.lost.annotations.NotNull;
import dev.lost.annotations.Nullable;
import dev.lost.engine.LostEngine;
import dev.lost.engine.items.customitems.CustomItem;
import dev.lost.engine.utils.BedrockUtils;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.papermc.paper.network.ChannelInitializeListenerHolder;
import net.kyori.adventure.key.Key;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.Connection;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.VarInt;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerCommonPacketListenerImpl;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.item.Items;

import java.util.function.BiFunction;

public class BytePacketListener {

    public static void inject() {
        ChannelInitializeListenerHolder.addListener(
                Key.key("lost_engine", "byte_packet_listener"),
                channel -> channel.pipeline().addBefore("decoder", "lost_engine_byte_packet_listener", new ByteChannelDupeHandler())
        );
    }

    private static class ByteChannelDupeHandler extends ChannelDuplexHandler {
        private Boolean isNotBedrockClient = null;

        private boolean isNotBedrockClient(@NotNull ChannelHandlerContext ctx) {
            if (isNotBedrockClient != null) return isNotBedrockClient;
            Channel channel = ctx.channel();
            Connection connection = (Connection) channel.pipeline().get("packet_handler");
            if (connection != null && connection.getPacketListener() instanceof ServerCommonPacketListenerImpl serverCommonPacketListener) {
                return isNotBedrockClient = !BedrockUtils.isBedrockPlayer(serverCommonPacketListener.getOwner().id());
            }
            return true;
        }

        private @Nullable PacketListener.ChannelDupeHandler getDupeHandler(@NotNull ChannelHandlerContext ctx) {
            return (PacketListener.ChannelDupeHandler) ctx.pipeline().get("lost_engine_packet_listener");
        }

        private boolean isPlay(@NotNull ChannelHandlerContext ctx) {
            Channel channel = ctx.channel();
            Connection connection = (Connection) channel.pipeline().get("packet_handler");
            return connection != null && connection.getPacketListener() instanceof ServerGamePacketListenerImpl;
        }

        @Override
        public void channelRead(@NotNull ChannelHandlerContext ctx, Object msg) throws Exception {
            /// Listen to the raw {@link net.minecraft.network.protocol.game.ServerboundSetCreativeModeSlotPacket} in order to intercept fake paintings
            if (isPlay(ctx) && isNotBedrockClient(ctx) && msg instanceof ByteBuf byteBuf) {
                byteBuf.markReaderIndex();
                try {
                    FriendlyByteBuf friendlyBuf = new FriendlyByteBuf(byteBuf);
                    int packetId = friendlyBuf.readVarInt();

                    if (packetId == 0x37) { // set_creative_mode_slot
                        short slot = friendlyBuf.readShort();
                        int itemCount = friendlyBuf.readVarInt();
                        if (itemCount > 0) {
                            int itemId = friendlyBuf.readVarInt();
                            int componentsToAdd = friendlyBuf.readVarInt();
                            int componentsToRemove = friendlyBuf.readVarInt();
                            if (itemId == BuiltInRegistries.ITEM.getId(Items.PAINTING) && componentsToAdd == 1 && componentsToRemove == 0) {
                                int componentTypeId = friendlyBuf.readVarInt();
                                if (componentTypeId == BuiltInRegistries.DATA_COMPONENT_TYPE.getId(DataComponents.PAINTING_VARIANT)) {
                                    /// {@link net.minecraft.network.codec.ByteBufCodecs#lengthPrefixed(int, BiFunction)
                                    int i = friendlyBuf.readVarInt();
                                    int i1 = friendlyBuf.readerIndex();
                                    int paintingId = VarInt.read(friendlyBuf.slice(i1, i));
                                    PacketListener.ChannelDupeHandler channelDupeHandler = getDupeHandler(ctx);
                                    if (channelDupeHandler != null) {
                                        CustomItem customItem = channelDupeHandler.paintingItems.get(paintingId);
                                        if (customItem != null) {
                                            int newItemId = BuiltInRegistries.ITEM.getId(customItem.asItem());
                                            ByteBuf newPacket = ctx.alloc().buffer();
                                            FriendlyByteBuf out = new FriendlyByteBuf(newPacket);
                                            out.writeVarInt(0x37);
                                            out.writeShort(slot);
                                            out.writeVarInt(itemCount);
                                            out.writeVarInt(newItemId);
                                            out.writeVarInt(0);
                                            out.writeVarInt(0);

                                            ctx.fireChannelRead(newPacket);
                                            byteBuf.release();

                                            ServerPlayer player = channelDupeHandler.getPlayer(ctx);
                                            if (player != null) {
                                                player.getBukkitEntity().getScheduler().runDelayed(
                                                        LostEngine.getInstance(),
                                                        scheduledTask -> player.inventoryMenu.sendAllDataToRemote(),
                                                        null,
                                                        1
                                                );
                                            }

                                            return;
                                        }
                                    }
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    LostEngine.logger().error("Failed to parse raw packet set_creative_mode_slot (0x37)", e);
                } finally {
                    byteBuf.resetReaderIndex();
                }
            }
            super.channelRead(ctx, msg);
        }
    }

}
