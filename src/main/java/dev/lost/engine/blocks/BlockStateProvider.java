package dev.lost.engine.blocks;

import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

public class BlockStateProvider {

    public static final BlockState RED_MUSHROOM_BLOCKSTATE = getRedMushroomBlockBlockState(63);
    public static final BlockState BROWN_MUSHROOM_BLOCKSTATE = getBrownMushroomBlockBlockState(63);
    public static final BlockState MUSHROOM_STEM_BLOCKSTATE = getMushroomStemBlockState(63);

    private static final Map<BlockStateType, Integer> BLOCK_STATES = new Object2IntOpenHashMap<>();

    static {
        BLOCK_STATES.put(BlockStateType.REGULAR, 0);
    }

    public static @NotNull BlockState getRedMushroomBlockBlockState(int id) {
        return Blocks.RED_MUSHROOM_BLOCK.defaultBlockState()
                .setValue(BlockStateProperties.NORTH, getBit(id, 0))
                .setValue(BlockStateProperties.EAST, getBit(id, 1))
                .setValue(BlockStateProperties.SOUTH, getBit(id, 2))
                .setValue(BlockStateProperties.WEST, getBit(id, 3))
                .setValue(BlockStateProperties.UP, getBit(id, 4))
                .setValue(BlockStateProperties.DOWN, getBit(id, 5));
    }

    public static @NotNull BlockState getBrownMushroomBlockBlockState(int id) {
        return Blocks.BROWN_MUSHROOM_BLOCK.defaultBlockState()
                .setValue(BlockStateProperties.NORTH, getBit(id, 0))
                .setValue(BlockStateProperties.EAST, getBit(id, 1))
                .setValue(BlockStateProperties.SOUTH, getBit(id, 2))
                .setValue(BlockStateProperties.WEST, getBit(id, 3))
                .setValue(BlockStateProperties.UP, getBit(id, 4))
                .setValue(BlockStateProperties.DOWN, getBit(id, 5));
    }

    public static @NotNull BlockState getMushroomStemBlockState(int id) {
        return Blocks.MUSHROOM_STEM.defaultBlockState()
                .setValue(BlockStateProperties.NORTH, getBit(id, 0))
                .setValue(BlockStateProperties.EAST, getBit(id, 1))
                .setValue(BlockStateProperties.SOUTH, getBit(id, 2))
                .setValue(BlockStateProperties.WEST, getBit(id, 3))
                .setValue(BlockStateProperties.UP, getBit(id, 4))
                .setValue(BlockStateProperties.DOWN, getBit(id, 5));
    }

    public static @NotNull BlockState getNextBlockState(BlockStateType type) {
        BlockState blockState = null;
        int id = BLOCK_STATES.get(type);
        switch (type) {
            case REGULAR -> {
                if (id > 189) {
                    throw new IllegalStateException("No more REGULAR block states available");
                }
                blockState = id < 63 ? getRedMushroomBlockBlockState(id) :
                        id < 126 ? getBrownMushroomBlockBlockState(id - 63) :
                                getMushroomStemBlockState(id - 126);
            }
        }
        if (blockState != null) {
            BLOCK_STATES.put(type, id + 1);
            return blockState;
        }
        throw new IllegalStateException("Unknown BlockStateType: " + type);
    }

    public enum BlockStateType {
        REGULAR
    }

    private static boolean getBit(int value, int bit) {
        return (value & (1 << bit)) != 0;
    }
}
