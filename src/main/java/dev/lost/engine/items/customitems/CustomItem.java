package dev.lost.engine.items.customitems;

import net.minecraft.core.component.TypedDataComponent;
import net.minecraft.util.Util;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.Nullable;

public interface CustomItem {

    ItemStack DEFAULT_ITEM = Util.make(() -> {
        ItemStack itemStack = Items.FILLED_MAP.getDefaultInstance();
        for (TypedDataComponent<?> component : itemStack.getComponents()) itemStack.remove(component.type());
        return itemStack;
    });

    ItemStack getDynamicMaterial();

    String getId();

    default @Nullable String toolType() {
        return null;
    }

    default Item asItem() {
        return (Item) this;
    }

    default ItemStack getDefaultMaterial() {
        return DEFAULT_ITEM.copy();
    }
}
