package dev.lost.engine.items.customitems;

import lombok.Getter;
import net.minecraft.core.component.TypedDataComponent;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ToolMaterial;

public class CustomAxeItem extends AxeItem implements CustomItem {

    @Getter
    private final String id;

    public CustomAxeItem(ToolMaterial material, float attackDamage, float attackSpeed, Properties properties, String id) {
        super(material, attackDamage, attackSpeed, properties);
        this.id = id;
    }

    @Override
    public ItemStack getDynamicMaterial() {
        ItemStack itemStack = Items.WOODEN_AXE.getDefaultInstance();
        for (TypedDataComponent<?> component : itemStack.getComponents()) itemStack.remove(component.type());
        return itemStack;
    }

    @Override
    public String toolType() {
        return "axe";
    }
}