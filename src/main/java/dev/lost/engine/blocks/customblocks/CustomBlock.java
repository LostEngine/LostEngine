package dev.lost.engine.blocks.customblocks;

import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public interface CustomBlock {

    BlockState getClientBlockState();

    default @Nullable BlockState getNotClickableBlockState() {
        return null;
    }

}
