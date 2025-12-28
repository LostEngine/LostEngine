package dev.lost.engine.utils;

import com.mojang.datafixers.util.Pair;
import dev.lost.engine.annotations.CanBreakOnUpdates;
import dev.lost.engine.bootstrap.components.SimpleComponentProperty;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.network.protocol.game.ClientboundLevelChunkPacketData;
import net.minecraft.network.protocol.game.ClientboundSectionBlocksUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundSetEquipmentPacket;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.bukkit.Material;
import org.bukkit.craftbukkit.util.CraftMagicNumbers;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;

@CanBreakOnUpdates(lastCheckedVersion = "1.21.10") // Make sure the field names are still correct on new Minecraft versions
public class ReflectionUtils {

    private static final Field STATES_FIELD;
    private static final Field BUFFER_FIELD;
    private static final Field ITEMSTACK_FIELD;
    private static final Field EQUIPMENT_SLOTS_FIELD;
    private static final Field BLOCK_MATERIAL_FIELD;
    private static final Field ITEM_MATERIAL_FIELD;
    private static final Field MATERIAL_ITEM_FIELD;
    private static final Field MATERIAL_BLOCK_FIELD;

    static {
        try {
            STATES_FIELD = ClientboundSectionBlocksUpdatePacket.class.getDeclaredField("states");
            STATES_FIELD.setAccessible(true);
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize STATES_FIELD", e);
        }
        try {
            BUFFER_FIELD = ClientboundLevelChunkPacketData.class.getDeclaredField("buffer");
            BUFFER_FIELD.setAccessible(true);
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize BUFFER_FIELD", e);
        }
        try {
            ITEMSTACK_FIELD = ClientboundContainerSetSlotPacket.class.getDeclaredField("itemStack");
            ITEMSTACK_FIELD.setAccessible(true);
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize ITEMSTACK_FIELD", e);
        }
        try {
            EQUIPMENT_SLOTS_FIELD = ClientboundSetEquipmentPacket.class.getDeclaredField("slots");
            EQUIPMENT_SLOTS_FIELD.setAccessible(true);
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize EQUIPMENT_SLOTS_FIELD", e);
        }
        try {
            BLOCK_MATERIAL_FIELD = CraftMagicNumbers.class.getDeclaredField("BLOCK_MATERIAL");
            BLOCK_MATERIAL_FIELD.setAccessible(true);
            ITEM_MATERIAL_FIELD = CraftMagicNumbers.class.getDeclaredField("ITEM_MATERIAL");
            ITEM_MATERIAL_FIELD.setAccessible(true);
            MATERIAL_ITEM_FIELD = CraftMagicNumbers.class.getDeclaredField("MATERIAL_ITEM");
            MATERIAL_ITEM_FIELD.setAccessible(true);
            MATERIAL_BLOCK_FIELD = CraftMagicNumbers.class.getDeclaredField("MATERIAL_BLOCK");
            MATERIAL_BLOCK_FIELD.setAccessible(true);
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize Bukkit material fields", e);
        }
    }

    public static BlockState[] getBlockStates(ClientboundSectionBlocksUpdatePacket packet) throws Exception {
        return (BlockState[]) STATES_FIELD.get(packet);
    }

    public static void setBlockStates(ClientboundSectionBlocksUpdatePacket packet, BlockState[] states) throws Exception {
        STATES_FIELD.set(packet, states);
    }

    public static byte[] getBuffer(ClientboundLevelChunkPacketData packet) throws Exception {
        return (byte[]) BUFFER_FIELD.get(packet);
    }

    public static void setBuffer(ClientboundLevelChunkPacketData packet, byte[] buffer) throws Exception {
        BUFFER_FIELD.set(packet, buffer);
    }

    public static void setItemStack(ClientboundContainerSetSlotPacket packet, ItemStack item) throws Exception {
        ITEMSTACK_FIELD.set(packet, item);
    }

    public static void setEquipmentSlots(ClientboundSetEquipmentPacket packet, List<Pair<EquipmentSlot, ItemStack>> slots) throws Exception {
        EQUIPMENT_SLOTS_FIELD.set(packet, slots);
    }

    @SuppressWarnings("unchecked")
    public static void setBlockMaterial(Block block, Material material) throws Exception {
        ((Map<Block, Material>) BLOCK_MATERIAL_FIELD.get(CraftMagicNumbers.INSTANCE)).put(block, material);
        ((Map<Material, Block>) MATERIAL_BLOCK_FIELD.get(CraftMagicNumbers.INSTANCE)).put(material, block);
    }

    @SuppressWarnings("unchecked")
    public static void setItemMaterial(ItemStack itemStack, Material material) throws Exception {
        ((Map<net.minecraft.world.item.Item, Material>) ITEM_MATERIAL_FIELD.get(CraftMagicNumbers.INSTANCE)).put(itemStack.getItem(), material);
        ((Map<Material, net.minecraft.world.item.Item>) MATERIAL_ITEM_FIELD.get(CraftMagicNumbers.INSTANCE)).put(material, itemStack.getItem());
    }

    public static @Nullable Class<?> getTypeArgument(@NonNull Class<?> clazz) {
        for (Type type : clazz.getGenericInterfaces()) {
            if (type instanceof ParameterizedType parameterizedType &&
                    parameterizedType.getRawType().equals(SimpleComponentProperty.class)) {
                Type actualType = parameterizedType.getActualTypeArguments()[0];
                if (actualType instanceof Class<?> actualClass) {
                    return actualClass;
                }
            }
        }
        return null;
    }
}
