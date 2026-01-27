package dev.lost.engine.utils;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ItemUtils {
    @SuppressWarnings("deprecation")
    public static void addCustomStringData(@NotNull ItemStack itemStack, String key, String value) {
        CustomData customData = itemStack.get(DataComponents.CUSTOM_DATA);
        if (customData == null) {
            customData = CustomData.of(new CompoundTag());
        }
        customData.getUnsafe().putString(key, value);
        itemStack.set(DataComponents.CUSTOM_DATA, customData);
    }

    @SuppressWarnings("deprecation")
    public static void removeCustomStringData(@NotNull ItemStack itemStack, String key) {
        CustomData customData = itemStack.get(DataComponents.CUSTOM_DATA);
        if (customData != null && customData.contains(key)) {
            customData.getUnsafe().remove(key);
            if (customData.isEmpty()) itemStack.remove(DataComponents.CUSTOM_DATA);
            else itemStack.set(DataComponents.CUSTOM_DATA, customData);
        }
    }

    @SuppressWarnings("deprecation")
    public static @Nullable String getCustomStringData(@NotNull ItemStack itemStack, String key) {
        CustomData customData = itemStack.get(DataComponents.CUSTOM_DATA);
        if (customData != null && customData.contains(key)) {
            return customData.getUnsafe().getString(key).orElse(null);
        }
        return null;
    }

    public static int packetIdFromItem(Item item) {
        int id = BuiltInRegistries.ITEM.getId(item);
        if (id == -1) {
            throw new IllegalStateException("Item is not registered: " + item);
        }
        return id;
    }
}
