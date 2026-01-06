package dev.lost.engine.items.customitems;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.Nullable;

public interface CustomItem {

    ItemStack getDynamicMaterial();

    String getId();

    default @Nullable String toolType() {
        return null;
    }

    default ItemStack getDefaultMaterial() {
        return Items.FILLED_MAP.getDefaultInstance();
    }
}
