package dev.lost.engine.blocks;

import dev.lost.annotations.NotNull;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import org.jetbrains.annotations.Contract;

import java.util.EnumMap;
import java.util.Map;
import java.util.function.BiFunction;

public class BlockStateProvider {

    private static final Object2IntOpenHashMap<BlockStateType> BLOCK_STATES = new Object2IntOpenHashMap<>();

    private record BlockGroup(Block[] blocks, int statesPerBlock, BiFunction<BlockState, Integer, BlockState> getter) {
    }

    private static final Map<BlockStateType, BlockGroup> REGISTRY = new EnumMap<>(BlockStateType.class);

    static {
        REGISTRY.put(BlockStateProvider.BlockStateType.WOOD, new BlockGroup(new Block[]{
                Blocks.RED_MUSHROOM_BLOCK,
                Blocks.BROWN_MUSHROOM_BLOCK,
                Blocks.MUSHROOM_STEM,
        }, 63, BlockStateProvider::getMushroomBlockState));
        REGISTRY.put(BlockStateType.STONE, new BlockGroup(new Block[]{
                Blocks.DROPPER,
                Blocks.DISPENSER,
        }, 6, BlockStateProvider::getDispenserBlockState));
        REGISTRY.put(BlockStateProvider.BlockStateType.GRASS, new BlockGroup(new Block[]{
                Blocks.TARGET,
                Blocks.PALE_OAK_LEAVES,
        }, 13, (blockState, id) -> {
            Block block = blockState.getBlock();
            if (block == Blocks.TARGET) {
                return getTargetBlockState(blockState, id + 1);
            } else {
                return getLeavesBlockState(blockState, id);
            }
        }));
    }

    private static BlockState getDispenserBlockState(@NotNull BlockState blockState, int id) {
        return blockState
                .setValue(BlockStateProperties.FACING, Direction.from3DDataValue(id))
                .setValue(BlockStateProperties.TRIGGERED, true);
    }

    public static @NotNull BlockState getMushroomBlockState(@NotNull BlockState blockState, int id) {
        return blockState
                .setValue(BlockStateProperties.NORTH, getBit(id, 0))
                .setValue(BlockStateProperties.EAST, getBit(id, 1))
                .setValue(BlockStateProperties.SOUTH, getBit(id, 2))
                .setValue(BlockStateProperties.WEST, getBit(id, 3))
                .setValue(BlockStateProperties.UP, getBit(id, 4))
                .setValue(BlockStateProperties.DOWN, getBit(id, 5));
    }

    public static @NotNull BlockState getLeavesBlockState(@NotNull BlockState blockState, int id) {
        return blockState
                .setValue(BlockStateProperties.DISTANCE, id % 7 + 1)
                .setValue(BlockStateProperties.PERSISTENT, id / 7 >= 1);
    }

    public static @NotNull BlockState getTargetBlockState(@NotNull BlockState blockState, int id) {
        return blockState.setValue(BlockStateProperties.POWER, id);
    }

    public static @NotNull BlockState getNextBlockState(BlockStateType type) {
        BlockGroup group = REGISTRY.get(type);
        int id = BLOCK_STATES.getOrDefault(type, 0);
        if (group == null) throw new IllegalStateException("No registration for type: " + type);

        int blockIndex = id / group.statesPerBlock();
        if (blockIndex >= group.blocks().length) throw new IllegalStateException("Exceeded capacity for " + type);
        int localId = id % group.statesPerBlock();

        Block targetBlock = group.blocks()[blockIndex];
        BLOCK_STATES.put(type, id + 1);
        return group.getter().apply(targetBlock.defaultBlockState(), localId);
    }

    public enum BlockStateType {
        STONE,
        WOOD,
        GRASS
    }

    @Contract(pure = true)
    private static boolean getBit(int value, int bit) {
        return (value & (1 << bit)) != 0;
    }
}
