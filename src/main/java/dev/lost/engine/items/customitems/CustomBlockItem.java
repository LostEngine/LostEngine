package dev.lost.engine.items.customitems;

import dev.lost.annotations.NotNull;
import dev.lost.engine.lua.LuaScripts;
import lombok.Getter;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import org.luaj.vm2.LuaValue;

public class CustomBlockItem extends BlockItem implements CustomItem {

    @Getter
    private final String id;
    private final ItemStack dynamicMaterial;
    private final LuaValue luaValue;

    public CustomBlockItem(Block block, Properties properties, ItemStack dynamicMaterial, String id, LuaValue luaValue) {
        super(block, properties);
        this.dynamicMaterial = dynamicMaterial;
        this.id = id;
        this.luaValue = luaValue;
    }

    @Override
    public ItemStack getDynamicMaterial() {
        return dynamicMaterial.copy();
    }

    @Override
    public @NotNull InteractionResult use(@NotNull Level level, @NotNull Player player, @NotNull InteractionHand hand) {
        if (player instanceof ServerPlayer sp)
            LuaScripts.onClick(luaValue, sp);
        return super.use(level, player, hand);
    }
}
