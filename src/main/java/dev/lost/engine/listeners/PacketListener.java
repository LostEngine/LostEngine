package dev.lost.engine.listeners;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.JsonOps;
import dev.lost.engine.LostEngine;
import dev.lost.engine.annotations.CanBreakOnUpdates;
import dev.lost.engine.customblocks.BlockStateProvider;
import dev.lost.engine.customblocks.customblocks.CustomBlock;
import dev.lost.engine.entities.CustomThrownTrident;
import dev.lost.engine.items.customitems.CustomItem;
import dev.lost.engine.utils.FloodgateUtils;
import dev.lost.engine.utils.ItemUtils;
import dev.lost.engine.utils.ReflectionUtils;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.papermc.paper.network.ChannelInitializeListenerHolder;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.kyori.adventure.key.Key;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.Connection;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.VarInt;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.common.ClientboundResourcePackPushPacket;
import net.minecraft.network.protocol.common.ServerboundResourcePackPacket;
import net.minecraft.network.protocol.configuration.ClientboundFinishConfigurationPacket;
import net.minecraft.network.protocol.configuration.ClientboundRegistryDataPacket;
import net.minecraft.network.protocol.game.*;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerCommonPacketListenerImpl;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.PositionMoveRotation;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.BlockItemStateProperties;
import net.minecraft.world.item.component.Tool;
import net.minecraft.world.item.crafting.RecipePropertySet;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.chunk.*;
import net.minecraft.world.level.dimension.DimensionType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;
import org.joml.Quaternionfc;
import org.joml.Vector3f;
import org.joml.Vector3fc;

import java.util.*;
import java.util.function.BiFunction;

public class PacketListener {


    @SuppressWarnings("deprecation")
    private static final Holder.Reference<Block> MUSHROOM_STEM_HOLDER = Blocks.MUSHROOM_STEM.builtInRegistryHolder();
    @SuppressWarnings("deprecation")
    private static final Holder.Reference<Block> BROWN_MUSHROOM_BLOCK_HOLDER = Blocks.BROWN_MUSHROOM_BLOCK.builtInRegistryHolder();
    @SuppressWarnings("deprecation")
    private static final Holder.Reference<Block> RED_MUSHROOM_BLOCK_HOLDER = Blocks.RED_MUSHROOM_BLOCK.builtInRegistryHolder();

    private static final BlockItemStateProperties MUSHROOM_BLOCK_ITEM_STATE_PROPERTIES = new BlockItemStateProperties(Map.of(
            "down", "true",
            "east", "true",
            "north", "true",
            "south", "true",
            "up", "true",
            "west", "true"
    ));

    public static void inject() {
        ChannelInitializeListenerHolder.addListener(
                Key.key("lost_engine", "packet_listener"),
                channel -> channel.pipeline().addBefore("packet_handler", "lost_engine_packet_listener", new ChannelDupeHandler())
        );
        ChannelInitializeListenerHolder.addListener(
                Key.key("lost_engine", "raw_packet_listener"),
                channel -> channel.pipeline().addBefore("decoder", "lost_engine_raw_packet_listener", new RawChannelDupeHandler())
        );
    }

    private static class RawChannelDupeHandler extends ChannelDuplexHandler {
        private Boolean isNotBedrockClient = null;

        private boolean isNotBedrockClient(@NotNull ChannelHandlerContext ctx) {
            if (isNotBedrockClient != null) return isNotBedrockClient;
            Channel channel = ctx.channel();
            Connection connection = (Connection) channel.pipeline().get("packet_handler");
            if (connection != null && connection.getPacketListener() instanceof ServerCommonPacketListenerImpl serverCommonPacketListener) {
                return isNotBedrockClient = !FloodgateUtils.isBedrockPlayer(serverCommonPacketListener.getOwner().id());
            }
            return true;
        }

        private @Nullable ChannelDupeHandler getDupeHandler(@NotNull ChannelHandlerContext ctx) {
            return (ChannelDupeHandler) ctx.pipeline().get("lost_engine_packet_listener");
        }

        private boolean isPlay(@NotNull ChannelHandlerContext ctx) {
            Channel channel = ctx.channel();
            Connection connection = (Connection) channel.pipeline().get("packet_handler");
            return connection != null && connection.getPacketListener() instanceof ServerGamePacketListenerImpl;
        }

        @Override
        public void channelRead(@NotNull ChannelHandlerContext ctx, Object msg) throws Exception {
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
                                    ChannelDupeHandler channelDupeHandler = getDupeHandler(ctx);
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

    private static class ChannelDupeHandler extends ChannelDuplexHandler {
        private ServerPlayer player;
        private boolean isWaitingForResourcePack = false;
        private Boolean isBedrockClient = null;
        volatile byte slot = 0;
        volatile int sectionsCount = 0;
        volatile int minY = 0;
        volatile int maxY = 0;
        private final Long2IntOpenHashMap customBlockStateCache = new Long2IntOpenHashMap();
        private final Int2ObjectOpenHashMap<CustomItem> paintingItems = new Int2ObjectOpenHashMap<>();

        /**
         * @return {@code Blocks.AIR.defaultBlockState()} if not in cache
         */
        private @NotNull BlockState getBlockState(long pos) {
            return Block.stateById(customBlockStateCache.get(pos));
        }

        private void removeCachedBlockStates(long pos) {
            customBlockStateCache.remove(pos);
        }

        private void setCustomBlockState(long pos, BlockState blockState) {
            customBlockStateCache.put(pos, Block.getId(blockState));
        }

        private boolean isBedrockClient(ChannelHandlerContext ctx) {
            if (isBedrockClient != null) return isBedrockClient;
            Channel channel = ctx.channel();
            Connection connection = (Connection) channel.pipeline().get("packet_handler");
            if (connection != null && connection.getPacketListener() instanceof ServerCommonPacketListenerImpl serverCommonPacketListener) {
                isBedrockClient = FloodgateUtils.isBedrockPlayer(serverCommonPacketListener.getOwner().id());
                if (isBedrockClient) {
                    LostEngine.logger().info("Bedrock client detected: {}", serverCommonPacketListener.getOwner().name());
                }
                return isBedrockClient;
            }
            return false;
        }

        private @Nullable ServerPlayer getPlayer(ChannelHandlerContext ctx) {
            if (player != null) return player;
            Channel channel = ctx.channel();
            Connection connection = (Connection) channel.pipeline().get("packet_handler");
            if (connection != null && connection.getPacketListener() instanceof ServerGamePacketListenerImpl serverGamePacketListener) {
                return this.player = serverGamePacketListener.player;
            }
            return null;
        }

        @Override
        public void channelRead(@NotNull ChannelHandlerContext ctx, Object msg) throws Exception {
            Object packet = isBedrockClient(ctx) ? msg : serverbound(msg, ctx, this);
            if (packet != null) {
                super.channelRead(ctx, packet);
            }
        }

        @Override
        public void write(@NotNull ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
            Object packet = isBedrockClient(ctx) ? msg : clientbound(msg, ctx, this);
            if (packet != null) {
                super.write(ctx, packet, promise);
            }
        }
    }

    private static @Nullable Object serverbound(@NotNull Object msg, ChannelHandlerContext ctx, ChannelDupeHandler handler) {
        switch (msg) {
            case ServerboundSetCreativeModeSlotPacket packet -> {
                ItemStack item = packet.itemStack();
                Optional<ItemStack> newItem = editItemBackward(item);
                if (newItem.isPresent()) {
                    return new ServerboundSetCreativeModeSlotPacket(packet.slotNum(), newItem.get());
                }
            }
            case ServerboundContainerClickPacket packet -> {
                return new ServerboundContainerClickPacket(
                        packet.containerId(),
                        packet.stateId(),
                        packet.slotNum(),
                        packet.buttonNum(),
                        packet.clickType(),
                        packet.changedSlots(),
                        (stack, hashGenerator) -> packet.carriedItem().matches(editItem(stack, false).orElse(stack), hashGenerator)
                );
            }
            case ServerboundPlayerActionPacket packet -> {
                ServerPlayer player = handler.getPlayer(ctx);
                if (player == null) break;
                if (player.gameMode.getGameModeForPlayer() != GameType.SURVIVAL) break;
                if (packet.getAction() == ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK) {
                    BlockState blockState = handler.getBlockState(packet.getPos().asLong());
                    if (blockState.getBlock() instanceof CustomBlock || blockState.getBlock() == Blocks.BROWN_MUSHROOM_BLOCK || blockState.getBlock() == Blocks.RED_MUSHROOM_BLOCK || blockState.getBlock() == Blocks.MUSHROOM_STEM) {
                        //noinspection DataFlowIssue -- never get used
                        if (blockState.getDestroyProgress(player, null, packet.getPos()) >= 1.0F) {
                            ctx.channel().writeAndFlush(new ClientboundLevelEventPacket(2001, packet.getPos(), Block.getId(blockState), false));
                            break;
                        }
                        float clientBlockDestroySpeed = getDestroySpeed(blockState.getBlock() instanceof CustomBlock customBlock ? customBlock.getClientBlockState() : blockState, editItem(player.getInventory().getSelectedItem(), false).orElse(player.getInventory().getSelectedItem()));
                        if (clientBlockDestroySpeed == 0) break;
                        float blockDestroySpeed = getDestroySpeed(blockState, player.getInventory().getSelectedItem());
                        if (blockDestroySpeed != clientBlockDestroySpeed) {
                            AttributeInstance blockBreakSpeed = new AttributeInstance(Attributes.BLOCK_BREAK_SPEED, attributeInstance -> {
                            });
                            AttributeInstance playerAttribute = player.getAttribute(Attributes.BLOCK_BREAK_SPEED);
                            if (playerAttribute != null) blockBreakSpeed.apply(playerAttribute.pack());
                            // The ratio is between what the client actually knows and what the server thinks
                            float ratio = blockDestroySpeed / clientBlockDestroySpeed;
                            blockBreakSpeed.setBaseValue(ratio * blockBreakSpeed.getBaseValue());
                            ctx.channel().writeAndFlush(new ClientboundUpdateAttributesPacket(player.getId(), List.of(blockBreakSpeed)));
                        }
                    }
                } else if (packet.getAction() == ServerboundPlayerActionPacket.Action.ABORT_DESTROY_BLOCK ||
                        packet.getAction() == ServerboundPlayerActionPacket.Action.STOP_DESTROY_BLOCK) {
                    BlockState blockState = handler.getBlockState(packet.getPos().asLong());
                    if (blockState.getBlock() instanceof CustomBlock || blockState.getBlock() == Blocks.BROWN_MUSHROOM_BLOCK || blockState.getBlock() == Blocks.RED_MUSHROOM_BLOCK || blockState.getBlock() == Blocks.MUSHROOM_STEM) {
                        AttributeInstance blockBreakSpeed = new AttributeInstance(Attributes.BLOCK_BREAK_SPEED, attributeInstance -> {
                        });
                        AttributeInstance playerAttribute = player.getAttribute(Attributes.BLOCK_BREAK_SPEED);
                        if (playerAttribute != null) {
                            blockBreakSpeed.apply(playerAttribute.pack());
                        }
                        ctx.channel().writeAndFlush(new ClientboundUpdateAttributesPacket(player.getId(), List.of(blockBreakSpeed)));
                    }
                }
            }
            case ServerboundResourcePackPacket(UUID ignored, ServerboundResourcePackPacket.Action action) -> {
                if (handler.isWaitingForResourcePack && action == ServerboundResourcePackPacket.Action.SUCCESSFULLY_LOADED) {
                    ctx.channel().writeAndFlush(ClientboundFinishConfigurationPacket.INSTANCE);
                }
            }
            case ServerboundSetCarriedItemPacket packet -> {
                if (handler.isBedrockClient) break;
                ServerPlayer player = handler.getPlayer(ctx);
                if (player == null || player.isImmobile()) break;
                byte slot = (byte) packet.getSlot();
                if (slot < 0 || slot >= player.getInventory().getContainerSize()) break;
                processNewSlot(handler.slot, slot, player);
                handler.slot = slot;
            }
            default -> {
            }
        }
        return msg;
    }

    private static @Nullable Object clientbound(@NotNull Object msg, ChannelHandlerContext ctx, ChannelDupeHandler handler) throws Exception {
        switch (msg) {
            case ClientboundLoginPacket packet -> processCommonPlayerSpawnInfo(packet.commonPlayerSpawnInfo(), handler);
            case ClientboundRespawnPacket packet ->
                    processCommonPlayerSpawnInfo(packet.commonPlayerSpawnInfo(), handler);
            case ClientboundForgetLevelChunkPacket(ChunkPos pos) -> {
                for (int x = pos.getMinBlockX(); x <= pos.getMaxBlockX(); x++) {
                    for (int y = handler.minY; y <= handler.maxY; y++) {
                        for (int z = pos.getMinBlockZ(); z <= pos.getMaxBlockZ(); z++) {
                            handler.removeCachedBlockStates(BlockPos.asLong(x, y, z));
                        }
                    }
                }
            }
            case ClientboundSetPlayerInventoryPacket(int slot, ItemStack contents) -> {
                ServerPlayer player = handler.getPlayer(ctx);
                Optional<ItemStack> newItem = editItem(contents, slot == Inventory.SLOT_OFFHAND || player != null && slot == player.getInventory().getSelectedSlot());
                if (newItem.isPresent()) {
                    return new ClientboundSetPlayerInventoryPacket(slot, newItem.get());
                }
            }
            case ClientboundBlockUpdatePacket packet -> {
                Optional<BlockState> newBlockState = getClientBlockState(packet.blockState);
                if (newBlockState.isPresent()) {
                    if (packet.blockState.getBlock() instanceof CustomBlock)
                        handler.setCustomBlockState(packet.getPos().asLong(), packet.blockState);
                    return new ClientboundBlockUpdatePacket(packet.getPos(), newBlockState.get());
                } else {
                    handler.removeCachedBlockStates(packet.getPos().asLong());
                }
            }
            case ClientboundSectionBlocksUpdatePacket packet -> {
                try {
                    BlockState[] blockStates = ReflectionUtils.getBlockStates(packet);
                    SectionPos sectionPos = ReflectionUtils.getSectionPos(packet);
                    short[] positions = ReflectionUtils.getPositions(packet);
                    if (blockStates.length != positions.length) {
                        throw new IllegalStateException("BlockStates length does not match Positions length in ClientboundSectionBlocksUpdatePacket");
                    }
                    for (int i = 0; i < blockStates.length; i++) {
                        Optional<BlockState> newBlockState = getClientBlockState(blockStates[i]);
                        if (newBlockState.isPresent()) {
                            if (blockStates[i].getBlock() instanceof CustomBlock)
                                handler.setCustomBlockState(sectionPos.relativeToBlockPos(positions[i]).asLong(), blockStates[i]);
                            blockStates[i] = newBlockState.get();
                        } else {
                            handler.removeCachedBlockStates(sectionPos.relativeToBlockPos(positions[i]).asLong());
                        }
                    }
                    ReflectionUtils.setBlockStates(packet, blockStates);
                } catch (Exception e) {
                    throw new RuntimeException("Failed to update block states via reflection in ClientboundSectionBlocksUpdatePacket", e);
                }
            }
            case ClientboundLevelChunkWithLightPacket packet -> {
                ClientboundLevelChunkPacketData chunkData = packet.getChunkData();
                if (handler.sectionsCount <= 0) break;
                processChunkPacket(packet.getX(), packet.getZ(), chunkData, handler.sectionsCount, handler.minY, handler.customBlockStateCache);
            }
            case ClientboundContainerSetContentPacket packet -> {
                ServerPlayer player = handler.getPlayer(ctx);
                List<ItemStack> items = new ObjectArrayList<>(packet.items());
                boolean requiresEdit = false;
                for (int i = 0; i < items.size(); i++) {

                    ItemStack item = items.get(i);
                    Optional<ItemStack> newItem = editItem(item, isIsDynamicMaterial(packet.containerId(), player, i));

                    if (newItem.isPresent()) {
                        requiresEdit = true;
                        items.set(i, newItem.get());
                    }
                }
                Optional<ItemStack> carriedItem = editItem(packet.carriedItem(), false);
                if (requiresEdit || carriedItem.isPresent()) {
                    return new ClientboundContainerSetContentPacket(packet.containerId(), packet.stateId(), items, carriedItem.orElseGet(packet::carriedItem));
                }
            }
            case ClientboundContainerSetSlotPacket packet -> {
                ServerPlayer player = handler.getPlayer(ctx);
                ItemStack item = packet.getItem();
                Optional<ItemStack> newItem = editItem(item, isIsDynamicMaterial(packet.getContainerId(), player, packet.getSlot()));
                newItem.ifPresent(itemStack -> {
                    try {
                        ReflectionUtils.setItemStack(packet, itemStack);
                    } catch (Exception e) {
                        throw new RuntimeException("Failed to update item via reflection in ClientboundContainerSetSlotPacket", e);
                    }
                });
            }
            case ClientboundSetEquipmentPacket packet -> {
                List<Pair<EquipmentSlot, ItemStack>> items = new ObjectArrayList<>(packet.getSlots());
                boolean requiresEdit = false;
                for (int i = 0; i < items.size(); i++) {
                    ItemStack item = items.get(i).getSecond();
                    Optional<ItemStack> newItem = editItem(item, false);
                    if (newItem.isPresent()) {
                        items.set(i, Pair.of(items.get(i).getFirst(), newItem.get()));
                        requiresEdit = true;
                    }
                }
                if (requiresEdit) {
                    try {
                        ReflectionUtils.setEquipmentSlots(packet, items);
                    } catch (Exception e) {
                        throw new RuntimeException("Failed to update equipment slots via reflection in ClientboundSetEquipmentPacket", e);
                    }
                }
            }
            case ClientboundSetCursorItemPacket(ItemStack contents) -> {
                Optional<ItemStack> newItem = editItem(contents, false);
                if (newItem.isPresent()) {
                    return new ClientboundSetCursorItemPacket(newItem.get());
                }
            }
            case ClientboundSystemChatPacket(Component content, boolean overlay) -> {
                // Using JSON is the best way I found it might not be optimized, but if we don't do anything, it would kick the player
                JsonElement component = JsonParser.parseString(componentToJson(content));
                JsonElement newComponent = replaceLostEngineHoverItems(component.deepCopy());
                if (!newComponent.equals(component)) {
                    return new ClientboundSystemChatPacket(jsonToComponent(newComponent.toString()), overlay);
                }
            }
            case ClientboundSetEntityDataPacket(int id, List<SynchedEntityData.DataValue<?>> packedItems) -> {
                ObjectArrayList<SynchedEntityData.DataValue<?>> newItems = new ObjectArrayList<>(packedItems);
                boolean requiresEdit = false;
                ItemStack itemStack = CustomThrownTrident.CUSTOM_TRIDENTS.get(id);
                if (itemStack != null) {
                    newItems.removeIf(dataValue -> {
                        if (dataValue.id() == 10 && dataValue.value() instanceof Integer) return false;
                        if (dataValue.id() == 11 && dataValue.value() instanceof Vector3fc) return false;
                        if (dataValue.id() == 12 && dataValue.value() instanceof Vector3fc) return false;
                        if (dataValue.id() == 13 && dataValue.value() instanceof Quaternionfc) return false;
                        if (dataValue.id() == 23 && dataValue.value() instanceof ItemStack) return false;
                        if (dataValue.id() == 24 && dataValue.value() instanceof Byte) return false;
                        return dataValue.id() >= 8;
                    });
                    requiresEdit = true;
                }
                for (int i = 0; i < newItems.size(); i++) {
                    SynchedEntityData.DataValue<?> dataValue = newItems.get(i);
                    if (dataValue.value() instanceof ItemStack item) {
                        Optional<ItemStack> newItem = editItem(item, false);
                        if (newItem.isPresent()) {
                            requiresEdit = true;
                            newItems.set(i, new SynchedEntityData.DataValue<>(dataValue.id(), EntityDataSerializers.ITEM_STACK, newItem.get()));
                        }
                    } else if (dataValue.value() instanceof BlockState blockState) {
                        Optional<BlockState> newBlockState = getClientBlockState(blockState);
                        if (newBlockState.isPresent()) {
                            requiresEdit = true;
                            newItems.set(i, new SynchedEntityData.DataValue<>(dataValue.id(), EntityDataSerializers.BLOCK_STATE, newBlockState.get()));
                        }
                    }
                }
                if (newItems.isEmpty()) return null;
                if (requiresEdit) {
                    return new ClientboundSetEntityDataPacket(id, newItems);
                }
            }
            case ClientboundLevelParticlesPacket packet -> {
                if (packet.getParticle() instanceof ItemParticleOption particle) {
                    Optional<ItemStack> newItem = editItem(particle.getItem(), false);
                    if (newItem.isPresent()) {
                        return new ClientboundLevelParticlesPacket(
                                new ItemParticleOption(particle.getType(), newItem.get()),
                                packet.isOverrideLimiter(),
                                packet.alwaysShow(),
                                packet.getX(),
                                packet.getY(),
                                packet.getZ(),
                                packet.getXDist(),
                                packet.getYDist(),
                                packet.getZDist(),
                                packet.getMaxSpeed(),
                                packet.getCount()
                        );
                    }
                } else if (packet.getParticle() instanceof BlockParticleOption particle) {
                    BlockState blockState = particle.getState();
                    Optional<BlockState> newBlockState = getClientBlockState(blockState);
                    if (newBlockState.isPresent()) {
                        return new ClientboundLevelParticlesPacket(
                                new BlockParticleOption(particle.getType(), newBlockState.get()),
                                packet.isOverrideLimiter(),
                                packet.alwaysShow(),
                                packet.getX(),
                                packet.getY(),
                                packet.getZ(),
                                packet.getXDist(),
                                packet.getYDist(),
                                packet.getZDist(),
                                packet.getMaxSpeed(),
                                packet.getCount()
                        );
                    }
                }
            }
            case ClientboundBundlePacket packet -> {
                List<Packet<? super ClientGamePacketListener>> packets = new ObjectArrayList<>();
                for (Packet<?> subPacket : packet.subPackets()) {
                    Object newPacket = clientbound(subPacket, ctx, handler);
                    if (newPacket instanceof Packet<?>) {
                        @SuppressWarnings("unchecked")
                        Packet<? super ClientGamePacketListener> newPacketCasted = (Packet<? super ClientGamePacketListener>) newPacket;
                        packets.add(newPacketCasted);
                    }
                }
                if (packets.isEmpty()) return null;
                return new ClientboundBundlePacket(packets);
            }
            case ClientboundLevelEventPacket packet -> {
                if (packet.getType() == 2001 || packet.getType() == 3008) { // Block break event and Block finished brushing
                    int data = packet.getData();
                    Block block = Block.stateById(data).getBlock();
                    Optional<BlockState> newBlockState = getClientBlockState(block.defaultBlockState());
                    if (newBlockState.isPresent()) {
                        return new ClientboundLevelEventPacket(
                                packet.getType(),
                                packet.getPos(),
                                Block.getId(newBlockState.get()),
                                packet.isGlobalEvent()
                        );
                    }
                }
            }
            case ClientboundFinishConfigurationPacket ignored -> {
                if (handler.isWaitingForResourcePack) {
                    handler.isWaitingForResourcePack = false;
                    break; // Avoid sending it twice
                }
                if (LostEngine.getResourcePackUrl() == null) break;
                handler.isWaitingForResourcePack = true;
                return new ClientboundResourcePackPushPacket(
                        LostEngine.getResourcePackUUID(),
                        LostEngine.getResourcePackUrl(),
                        LostEngine.getResourcePackHash(),
                        true,
                        Optional.of(Component.literal(LostEngine.getInstance().getConfig().getString("pack_hosting.resource_pack_prompt", "Prompt")))
                );
            }
            case ClientboundSetHeldSlotPacket(int slot) -> {
                if (handler.isBedrockClient) break;
                ServerPlayer player = handler.getPlayer(ctx);
                if (player == null) break;
                processNewSlot(handler.slot, (byte) slot, player);
                handler.slot = (byte) slot;
            }
            case ClientboundUpdateRecipesPacket packet -> {
                try {
                    for (Map.Entry<ResourceKey<RecipePropertySet>, RecipePropertySet> entry : packet.itemSets().entrySet()) {
                        Set<Holder<Item>> oldItems = ReflectionUtils.getItems(entry.getValue());
                        Set<Holder<Item>> newItems = new ObjectOpenHashSet<>(oldItems);
                        newItems.removeIf(item -> item.value().asItem() instanceof CustomItem);
                        if (oldItems.size() != newItems.size())
                            ReflectionUtils.setItems(entry.getValue(), newItems);
                    }
                } catch (Exception e) {
                    throw new RuntimeException("Failed to update items via reflection in ClientboundUpdateRecipesPacket", e);
                }
            }
            case ClientboundAddEntityPacket packet -> {
                if (packet.getType() != EntityType.TRIDENT) break;
                ItemStack itemStack = CustomThrownTrident.CUSTOM_TRIDENTS.get(packet.getId());
                if (itemStack != null) {
                    ObjectArrayList<SynchedEntityData.DataValue<?>> packedItems = new ObjectArrayList<>();
                    packedItems.add(new SynchedEntityData.DataValue<>(10, EntityDataSerializers.INT, 1));
                    packedItems.add(new SynchedEntityData.DataValue<>(11, EntityDataSerializers.VECTOR3, new Vector3f(0, -0.03125f, 0.6875f)));
                    packedItems.add(new SynchedEntityData.DataValue<>(12, EntityDataSerializers.VECTOR3, new Vector3f(2, 2, 1)));
                    packedItems.add(new SynchedEntityData.DataValue<>(13, EntityDataSerializers.QUATERNION, new Quaternionf().rotateX((float) Math.toRadians(-90)).rotateY((float) Math.toRadians(90))));
                    packedItems.add(new SynchedEntityData.DataValue<>(23, EntityDataSerializers.ITEM_STACK, editItem(itemStack, false).orElse(itemStack)));
                    packedItems.add(new SynchedEntityData.DataValue<>(24, EntityDataSerializers.BYTE, (byte) 8));
                    float yRot = Mth.wrapDegrees(packet.getYRot() - (packet.getYRot() - 90) * 2);
                    ctx.channel().writeAndFlush(new ClientboundBundlePacket(List.of(
                            new ClientboundAddEntityPacket(packet.getId(), packet.getUUID(), packet.getX(), packet.getY(), packet.getZ(), packet.getXRot(), yRot, EntityType.ITEM_DISPLAY, packet.getData(), packet.getMovement(), packet.getYHeadRot()),
                            new ClientboundSetEntityDataPacket(packet.getId(), packedItems)
                    )));
                    return null;
                }
            }
            case ClientboundTeleportEntityPacket(
                    int id, PositionMoveRotation change, Set<Relative> relatives, boolean onGround
            ) -> {
                if (CustomThrownTrident.CUSTOM_TRIDENTS.containsKey(id)) {
                    float yRot = Mth.wrapDegrees(change.yRot() - (change.yRot() - 90) * 2);
                    return new ClientboundTeleportEntityPacket(id, new PositionMoveRotation(change.position(), change.deltaMovement(), yRot, change.xRot()), relatives, onGround);
                }
            }
            case ClientboundEntityPositionSyncPacket(int id, PositionMoveRotation values, boolean onGround) -> {
                if (CustomThrownTrident.CUSTOM_TRIDENTS.containsKey(id)) {
                    float yRot = Mth.wrapDegrees(values.yRot() - (values.yRot() - 90) * 2);
                    return new ClientboundEntityPositionSyncPacket(id, new PositionMoveRotation(values.position(), values.deltaMovement(), yRot, values.xRot()), onGround);
                }
            }
            case ClientboundMoveEntityPacket packet -> {
                if (!packet.hasRotation()) break;
                int entityId = ReflectionUtils.getEntityId(packet);
                if (CustomThrownTrident.CUSTOM_TRIDENTS.containsKey(entityId)) {
                    byte yRot = Mth.packDegrees(Mth.wrapDegrees(packet.getYRot() - (packet.getYRot() - 90) * 2));
                    ReflectionUtils.setYRot(packet, yRot);
                }
            }
            case ClientboundSetEntityMotionPacket packet -> {
                if (CustomThrownTrident.CUSTOM_TRIDENTS.containsKey(packet.getId())) {
                    // This packet is useless since we replace tridents with item displays, and they don't support it se we send position packets every tick
                    return null;
                }
            }
            case ClientboundRegistryDataPacket(
                    ResourceKey<? extends Registry<?>> registry,
                    List<RegistrySynchronization.PackedRegistryEntry> entries
            ) -> {
                if (registry.identifier().equals(Registries.PAINTING_VARIANT.identifier())) {
                    handler.paintingItems.clear();
                    entries = new ObjectArrayList<>(entries);
                    for (CustomItem customItem : LostEngine.getCustomItems()) {
                        CompoundTag compoundTag = new CompoundTag();
                        compoundTag.putString("asset_id", "minecraft:");
                        compoundTag.putString("author", "LostEngine");
                        compoundTag.putInt("height", 1);
                        compoundTag.putInt("width", 1);
                        Tag componentTag = ComponentSerialization.CODEC.encodeStart(
                                NbtOps.INSTANCE,
                                customItem.asItem().getName()
                        ).getOrThrow();
                        compoundTag.put("title", componentTag);
                        entries.add(new RegistrySynchronization.PackedRegistryEntry(Identifier.parse(customItem.getId() + "_painting"), Optional.of(compoundTag)));
                        handler.paintingItems.put(entries.size(), customItem);
                    }
                    return new ClientboundRegistryDataPacket(registry, entries);
                }
            }
            default -> {
            }
        }
        return msg;
    }

    private static boolean isIsDynamicMaterial(int containerId, @Nullable ServerPlayer player, int slot) {
        boolean isDynamicMaterial = false;
        int hotbarSlot = -1;
        if (player != null) {
            if (containerId == 0) {
                if (slot == 45) isDynamicMaterial = true;
                else hotbarSlot = slot - 36;
            } else if (player.containerMenu.menuType == MenuType.GENERIC_9x1) hotbarSlot = (slot - 36);
            else if (player.containerMenu.menuType == MenuType.GENERIC_9x2) hotbarSlot = (slot - 45);
            else if (player.containerMenu.menuType == MenuType.GENERIC_3x3 ||
                    player.containerMenu.menuType == MenuType.SHULKER_BOX) hotbarSlot = (slot - 54);
            else if (player.containerMenu.menuType == MenuType.GENERIC_9x4) hotbarSlot = (slot - 63);
            else if (player.containerMenu.menuType == MenuType.GENERIC_9x5) hotbarSlot = (slot - 72);
            else if (player.containerMenu.menuType == MenuType.GENERIC_9x6) hotbarSlot = (slot - 81);
            else if (player.containerMenu.menuType == MenuType.BEACON) hotbarSlot = (slot - 28);
            else if (player.containerMenu.menuType == MenuType.BLAST_FURNACE ||
                    player.containerMenu.menuType == MenuType.FURNACE ||
                    player.containerMenu.menuType == MenuType.SMOKER ||
                    player.containerMenu.menuType == MenuType.ANVIL ||
                    player.containerMenu.menuType == MenuType.GRINDSTONE ||
                    player.containerMenu.menuType == MenuType.MERCHANT ||
                    player.containerMenu.menuType == MenuType.CARTOGRAPHY_TABLE) hotbarSlot = (slot - 30);
            else if (player.containerMenu.menuType == MenuType.BREWING_STAND ||
                    player.containerMenu.menuType == MenuType.HOPPER) hotbarSlot = (slot - 32);
            else if (player.containerMenu.menuType == MenuType.CRAFTING) hotbarSlot = (slot - 37);
            else if (player.containerMenu.menuType == MenuType.ENCHANTMENT ||
                    player.containerMenu.menuType == MenuType.STONECUTTER) hotbarSlot = (slot - 29);
            else if (player.containerMenu.menuType == MenuType.LOOM ||
                    player.containerMenu.menuType == MenuType.SMITHING) hotbarSlot = (slot - 31);

            if (!isDynamicMaterial) isDynamicMaterial = hotbarSlot == player.getInventory().getSelectedSlot();
        }
        return isDynamicMaterial;
    }

    private static void processChunkPacket(int chunkX, int chunkZ, @NotNull ClientboundLevelChunkPacketData packet, int sectionCount, int minY, Long2IntOpenHashMap customBlockStateCache) throws Exception {
        FriendlyByteBuf oldBuf = new FriendlyByteBuf(packet.getReadBuffer());
        LevelChunkSection[] sections = new LevelChunkSection[sectionCount];
        boolean requiresEdit = false;

        for (int i = 0; i < sectionCount; i++) {
            //noinspection DataFlowIssue -- It should work fine
            LevelChunkSection section = new LevelChunkSection(PalettedContainerFactory.create(MinecraftServer.getServer().registryAccess()), null, null, 0);
            section.read(oldBuf);

            int sectionY = (i + (minY >> 4)) << 4;

            PalettedContainer<BlockState> container = section.getStates();
            Palette<BlockState> palette = container.data.palette();
            Object[] values = palette.moonrise$getRawPalette(null);

            for (int x = 0; x < 16; x++) {
                for (int y = 0; y < 16; y++) {
                    for (int z = 0; z < 16; z++) {
                        BlockState blockState = section.getBlockState(x, y, z);
                        Optional<BlockState> clientBlockState = getClientBlockState(blockState);
                        long blockPosLong = BlockPos.asLong((chunkX << 4) + x, sectionY + y, (chunkZ << 4) + z);
                        if (clientBlockState.isPresent()) {
                            // If we used section.setBlockState(x, y, z, clientBlockState.get());
                            // it wouldn't remove custom blocks from the palette and make the client crash,
                            // we don't have the choice to also modify the palette.
                            // But if it is a global palette, we need to edit the actual blocks.
                            if (values == null) {
                                section.setBlockState(x, y, z, clientBlockState.get());
                                requiresEdit = true;
                            }
                            if (blockState.getBlock() instanceof CustomBlock)
                                customBlockStateCache.put(blockPosLong, Block.getId(blockState));
                        } else {
                            customBlockStateCache.remove(blockPosLong);
                        }
                    }
                }
            }


            if (values != null) {
                if (palette instanceof SingleValuePalette<BlockState> singleValuePalette) {
                    Optional<BlockState> blockState = getClientBlockState((BlockState) values[0]);
                    if (blockState.isPresent()) {
                        values[0] = blockState.get();
                        ReflectionUtils.setValue(singleValuePalette, blockState.get());
                        requiresEdit = true;
                    }
                } else {
                    for (int j = 0; j < values.length; j++) {
                        Object obj = values[j];
                        if (obj instanceof BlockState state) {
                            Optional<BlockState> clientBlockState = getClientBlockState(state);
                            if (clientBlockState.isPresent()) {
                                values[j] = clientBlockState.get();
                                requiresEdit = true;
                            }
                        }
                    }
                }
            }

            sections[i] = section;
        }

        if (requiresEdit) {
            FriendlyByteBuf newBuf = new FriendlyByteBuf(Unpooled.buffer());
            for (LevelChunkSection section : sections) {
                //noinspection DataFlowIssue -- actually nullable
                section.write(newBuf, null, 0);
            }
            ReflectionUtils.setBuffer(packet, newBuf.array());
        }
    }

    public static Optional<ItemStack> editItem(@NotNull ItemStack item, boolean dynamicMaterial) {
        Optional<ItemStack> optionalItemStack = Optional.empty();
        if (item.isEmpty()) return optionalItemStack;
        if (item.is(Items.RED_MUSHROOM_BLOCK) || (item.is(Items.MUSHROOM_STEM) || item.is(Items.BROWN_MUSHROOM_BLOCK))) {
            if (ItemUtils.getCustomStringData(item, "lost_engine_id") == null) { // Verify it is not already converted
                item.set(DataComponents.BLOCK_STATE, MUSHROOM_BLOCK_ITEM_STATE_PROPERTIES);
            }
        }
        if (item.getItem() instanceof CustomItem customItem) {
            ItemStack newItem = dynamicMaterial ? customItem.getDynamicMaterial() : customItem.getDefaultMaterial();
            newItem.setCount(item.getCount());
            newItem.applyComponents(item.getComponents());
            newItem.remove(DataComponents.REPAIRABLE);
            ItemUtils.addCustomStringData(newItem, "lost_engine_id", customItem.getId());
            item = newItem;
        }
        Tool tool = item.getComponents().get(DataComponents.TOOL);
        item = item.copy();
        if (tool != null) {
            List<Tool.Rule> rules = new ObjectArrayList<>(tool.rules());
            for (int i = 0, size = rules.size(); i < size; i++) {
                Tool.Rule rule = rules.get(i);
                HolderSet<Block> originalSet = rule.blocks();
                List<Holder<Block>> filtered = new ObjectArrayList<>();
                for (Holder<Block> holder : originalSet) {
                    if (holder != MUSHROOM_STEM_HOLDER && holder != BROWN_MUSHROOM_BLOCK_HOLDER && holder != RED_MUSHROOM_BLOCK_HOLDER && !(holder.value() instanceof CustomBlock)) {
                        filtered.add(holder);
                    }
                }
                HolderSet.Direct<Block> newSet = HolderSet.direct(filtered);
                rules.set(i, new Tool.Rule(newSet, rule.speed(), rule.correctForDrops()));
            }
            rules.add(new Tool.Rule(
                    HolderSet.direct(List.of(MUSHROOM_STEM_HOLDER, BROWN_MUSHROOM_BLOCK_HOLDER, RED_MUSHROOM_BLOCK_HOLDER)),
                    Optional.of(0.01F),
                    Optional.empty()
            ));
            tool = new Tool(rules, tool.defaultMiningSpeed(), tool.damagePerBlock(), tool.canDestroyBlocksInCreative());
            item.set(DataComponents.TOOL, tool);
        } else {
            item.set(
                    DataComponents.TOOL,
                    new Tool(
                            List.of(new Tool.Rule(
                                    HolderSet.direct(List.of(MUSHROOM_STEM_HOLDER, BROWN_MUSHROOM_BLOCK_HOLDER, RED_MUSHROOM_BLOCK_HOLDER)),
                                    Optional.of(0.01F),
                                    Optional.empty()
                            )),
                            1.0F,
                            1,
                            true
                    ));
        }
        optionalItemStack = Optional.of(item);
        return optionalItemStack;
    }

    public static Optional<ItemStack> editItemBackward(@NotNull ItemStack item) {
        if (!item.isEmpty()) {
            String lostEngineId = ItemUtils.getCustomStringData(item, "lost_engine_id");
            if (lostEngineId != null) {
                Identifier id = Identifier.parse(lostEngineId);
                return BuiltInRegistries.ITEM.get(id).map(builtInItem -> {
                    ItemStack newItem = new ItemStack(builtInItem, item.getCount());
                    newItem.applyComponents(item.getComponents());
                    ItemUtils.removeCustomStringData(newItem, "lost_engine_id");
                    newItem.set(DataComponents.TOOL, newItem.getItem().getDefaultInstance().get(DataComponents.TOOL));
                    newItem.set(DataComponents.BLOCK_STATE, newItem.getItem().getDefaultInstance().get(DataComponents.BLOCK_STATE));
                    newItem.set(DataComponents.REPAIRABLE, newItem.getItem().getDefaultInstance().get(DataComponents.REPAIRABLE));
                    return newItem;
                });
            } else {
                ItemStack newItem = null;
                if (item.has(DataComponents.TOOL)) {
                    // Sadly, I don't really have the choice to remove the tool component
                    // as I send to the client that it takes ages to break blocks that are
                    // used for custom blocks using the tool component
                    newItem = item.copy();
                    newItem.set(DataComponents.TOOL, newItem.getItem().getDefaultInstance().get(DataComponents.TOOL));
                }
                BlockItemStateProperties blockItemStateProperties = item.get(DataComponents.BLOCK_STATE);
                if (blockItemStateProperties != null && MUSHROOM_BLOCK_ITEM_STATE_PROPERTIES.properties().equals(blockItemStateProperties.properties())) {
                    if (newItem == null) newItem = item.copy();
                    newItem.set(DataComponents.BLOCK_STATE, newItem.getItem().getDefaultInstance().get(DataComponents.BLOCK_STATE));
                }
                return Optional.ofNullable(newItem);
            }
        }
        return Optional.empty();
    }

    private static void processNewSlot(byte oldSlot, byte newSlot, ServerPlayer player) {
        if (oldSlot != newSlot) {
            // Previous Item
            PacketListener.editItem(player.getInventory().getItem(oldSlot), false).ifPresent(itemStack ->
                    player.connection.send(new ClientboundSetPlayerInventoryPacket(oldSlot, itemStack))
            );
            // New Item
            PacketListener.editItem(player.getInventory().getItem(newSlot), true).ifPresent(itemStack ->
                    player.connection.send(new ClientboundSetPlayerInventoryPacket(newSlot, itemStack))
            );
        }
    }

    private static void processCommonPlayerSpawnInfo(@NotNull CommonPlayerSpawnInfo commonPlayerSpawnInfo, @NotNull ChannelDupeHandler handler) {
        @CanBreakOnUpdates(lastCheckedVersion = "1.21.11") /// See {@link net.minecraft.world.level.Level#Level}
                DimensionType dimType = commonPlayerSpawnInfo.dimensionType().value();
        handler.minY = dimType.minY();
        int height = dimType.height();
        handler.maxY = handler.minY + height - 1;
        int minSectionY = handler.minY >> 4;
        int maxSectionY = handler.maxY >> 4;
        handler.sectionsCount = maxSectionY - minSectionY + 1;
        handler.customBlockStateCache.clear();
    }

    public static String componentToJson(Component component) {
        JsonElement jsonElement = ComponentSerialization.CODEC.encodeStart(
                JsonOps.INSTANCE, component
        ).getOrThrow();
        return jsonElement.toString();
    }

    public static Component jsonToComponent(String json) {
        JsonElement jsonElement = JsonParser.parseString(json);
        return ComponentSerialization.CODEC.parse(
                JsonOps.INSTANCE, jsonElement
        ).getOrThrow();
    }

    public static JsonElement replaceLostEngineHoverItems(@NotNull JsonElement element) {
        if (element.isJsonObject()) {
            JsonObject obj = element.getAsJsonObject();

            if (obj.has("hover_event")) {
                JsonObject hoverEvent = obj.getAsJsonObject("hover_event");
                if (hoverEvent.has("action") && "show_item".equals(hoverEvent.get("action").getAsString()) && hoverEvent.has("id")) {
                    String id = hoverEvent.get("id").getAsString();
                    if (id.startsWith("lost_engine:")) {
                        obj.remove("hover_event");
                        // Right now I will remove it because it is a bit useless, and if we don't, it kicks the player, but I may do something else later
                    }
                }
            }

            for (String key : obj.keySet()) {
                JsonElement child = obj.get(key);
                obj.add(key, replaceLostEngineHoverItems(child));
            }
            return obj;
        } else if (element.isJsonArray()) {
            JsonArray arr = element.getAsJsonArray();
            for (int i = 0; i < arr.size(); i++) {
                arr.set(i, replaceLostEngineHoverItems(arr.get(i)));
            }
            return arr;
        }
        return element;
    }

    public static Optional<BlockState> getClientBlockState(@NotNull BlockState blockState) {
        Block block = blockState.getBlock();
        if (block instanceof CustomBlock customBlock) {
            return Optional.of(customBlock.getClientBlockState());
        } else if (block == Blocks.BROWN_MUSHROOM_BLOCK || block == Blocks.RED_MUSHROOM_BLOCK || block == Blocks.MUSHROOM_STEM) {
            return Optional.of(BlockStateProvider.getMushroomBlockState(blockState, 63));
        } else if (block == Blocks.DROPPER || block == Blocks.DISPENSER) {
            return Optional.of(blockState.setValue(BlockStateProperties.TRIGGERED, false));
        } else if (block == Blocks.PALE_OAK_LEAVES) {
            return Optional.of(BlockStateProvider.getLeavesBlockState(blockState, 13));
        } else if (block == Blocks.TARGET) {
            return Optional.of(BlockStateProvider.getTargetBlockState(blockState, 0));
        }
        return Optional.empty();
    }

    /**
     * This is a simplified version of {@link  BlockState#getDestroyProgress}
     */
    public static float getDestroySpeed(@NotNull BlockState state, ItemStack item) {
        float destroySpeed = state.destroySpeed;
        if (destroySpeed == -1.0F) {
            return 0.0F;
        } else {
            int i = !state.requiresCorrectToolForDrops() || item.isCorrectToolForDrops(state) ? 30 : 100;
            return item.getDestroySpeed(state) / destroySpeed / i;
        }
    }
}
