package dev.lost.engine.items.customitems;

import dev.lost.annotations.NotNull;
import dev.lost.engine.lua.LuaScripts;
import lombok.Getter;
import net.minecraft.core.component.TypedDataComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ToolMaterial;
import net.minecraft.world.level.Level;
import org.luaj.vm2.LuaValue;

public class CustomAxeItem extends AxeItem implements CustomItem {

    @Getter
    private final String id;
    private final LuaValue luaValue;

    public CustomAxeItem(ToolMaterial material, float attackDamage, float attackSpeed, Properties properties, String id, LuaValue luaValue) {
        super(material, attackDamage, attackSpeed, properties);
        this.id = id;
        this.luaValue = luaValue;
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

    @Override
    public @NotNull InteractionResult use(@NotNull Level level, @NotNull Player player, @NotNull InteractionHand hand) {
        if (player instanceof ServerPlayer sp)
            LuaScripts.onClick(luaValue, sp);
        return super.use(level, player, hand);
    }
}