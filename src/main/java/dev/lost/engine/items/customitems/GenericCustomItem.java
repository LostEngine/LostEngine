package dev.lost.engine.items.customitems;

import lombok.Getter;
import net.minecraft.core.component.TypedDataComponent;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class GenericCustomItem extends Item implements CustomItem {

    @Getter
    private final String id;
    private final String toolType;

    public GenericCustomItem(Properties properties, String id, String toolType) {
        super(properties);
        this.toolType = toolType;
        this.id = id;
    }

    public GenericCustomItem(Properties properties, String id) {
        this(properties, id, null);
    }

    @Override
    public ItemStack getDynamicMaterial() {
        ItemStack itemStack = Items.RECOVERY_COMPASS.getDefaultInstance();
        for (TypedDataComponent<?> component : itemStack.getComponents()) itemStack.remove(component.type());
        return itemStack;
    }

    @Override
    public String toolType() {
        return toolType;
    }
}
