package dev.lost.engine.items.customitems;

import lombok.Getter;
import net.minecraft.core.component.TypedDataComponent;
import net.minecraft.world.item.HoeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ToolMaterial;

public class CustomHoeItem extends HoeItem implements CustomItem {

    @Getter
    private final String id;

    public CustomHoeItem(ToolMaterial material, float attackDamage, float attackSpeed, Properties properties, String id) {
        super(material, attackDamage, attackSpeed, properties);
        this.id = id;
    }

    @Override
    public ItemStack getDynamicMaterial() {
        ItemStack itemStack = Items.WOODEN_HOE.getDefaultInstance();
        for (TypedDataComponent<?> component : itemStack.getComponents()) itemStack.remove(component.type());
        return itemStack;
    }

    @Override
    public String toolType() {
        return "hoe";
    }
}
