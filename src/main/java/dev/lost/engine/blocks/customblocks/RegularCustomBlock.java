package dev.lost.engine.blocks.customblocks;

import lombok.Getter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public class RegularCustomBlock extends Block implements CustomBlock {

    @Getter private final BlockState clientBlockState;

    public RegularCustomBlock(Properties properties, BlockState clientBlockState) {
        super(properties);
        this.clientBlockState = clientBlockState;
    }
}
