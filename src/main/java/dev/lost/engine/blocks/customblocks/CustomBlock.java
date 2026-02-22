package dev.lost.engine.blocks.customblocks;

import dev.lost.annotations.Nullable;
import net.minecraft.world.level.block.state.BlockState;

public interface CustomBlock {

    BlockState getClientBlockState();

    default @Nullable BlockState getNotClickableBlockState() {
        return null;
    }

}
