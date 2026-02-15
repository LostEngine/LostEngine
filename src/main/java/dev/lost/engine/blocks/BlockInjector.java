package dev.lost.engine.blocks;

import dev.lost.engine.annotations.CanBreakOnUpdates;
import dev.lost.engine.blocks.customblocks.RegularCustomBlock;
import dev.lost.engine.blocks.customblocks.TNTCustomBlock;
import dev.lost.engine.bootstrap.LostEngineBootstrap;
import dev.lost.engine.items.ItemInjector;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.RegistrationInfo;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import org.jetbrains.annotations.NotNull;

@CanBreakOnUpdates(lastCheckedVersion = "1.21.10")
public class BlockInjector {

    @SuppressWarnings("unchecked")
    private static final MappedRegistry<Block> REGISTRY = (MappedRegistry<Block>) BuiltInRegistries.BLOCK;

    public static void injectRegularBlock(
            String id,
            BlockState clientBlockState,
            float destroyTime,
            float explosionResistance,
            @NotNull Minable minable
    ) {
        ResourceKey<Block> key = ResourceKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath("lost_engine", id));

        BlockBehaviour.Properties properties = BlockBehaviour.Properties.of()
                .mapColor(MapColor.COLOR_LIGHT_BLUE) // Little easter egg
                .requiresCorrectToolForDrops()
                .strength(destroyTime, explosionResistance)
                .sound(switch (minable) {
                    case AXE -> SoundType.WOOD;
                    case HOE, SHOVEL -> SoundType.GRAVEL;
                    case PICKAXE, NONE -> SoundType.STONE;
                })
                .setId(key);

        Block custom = new RegularCustomBlock(properties, clientBlockState);

        REGISTRY.createIntrusiveHolder(custom);
        REGISTRY.register(key, custom, RegistrationInfo.BUILT_IN);

        for (BlockState blockstate: custom.getStateDefinition().getPossibleStates())
            Block.BLOCK_STATE_REGISTRY.add(blockstate);

        LostEngineBootstrap.materialManager.setMaterial(custom, "COBBLESTONE");

        Item item = ItemInjector.injectBlockItem(id, custom);

        switch (minable) {
            case AXE -> LostEngineBootstrap.dataPackGenerator.addAxeMinable(key.identifier().toString());
            case HOE -> LostEngineBootstrap.dataPackGenerator.addHoeMinable(key.identifier().toString());
            case PICKAXE -> LostEngineBootstrap.dataPackGenerator.addPickaxeMinable(key.identifier().toString());
            case SHOVEL -> LostEngineBootstrap.dataPackGenerator.addShovelMinable(key.identifier().toString());
        }
    }

    public static void injectTNTBlock(
            String id,
            BlockState clientBlockState,
            float explosionPower
    ) {
        ResourceKey<Block> key = ResourceKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath("lost_engine", id));

        BlockBehaviour.Properties properties = BlockBehaviour.Properties.of()
                .mapColor(MapColor.COLOR_LIGHT_BLUE) // Little easter egg
                .instabreak()
                .ignitedByLava()
                .isRedstoneConductor((state, level, pos) -> false)
                .sound(SoundType.GRASS)
                .setId(key);

        Block custom = new TNTCustomBlock(properties, clientBlockState, explosionPower);

        REGISTRY.createIntrusiveHolder(custom);
        REGISTRY.register(key, custom, RegistrationInfo.BUILT_IN);

        for (BlockState blockstate: custom.getStateDefinition().getPossibleStates())
            Block.BLOCK_STATE_REGISTRY.add(blockstate);

        LostEngineBootstrap.materialManager.setMaterial(custom, "TNT");

        Item item = ItemInjector.injectBlockItem(id, custom);
    }

    public enum Minable {
        AXE,
        HOE,
        PICKAXE,
        SHOVEL,
        NONE
    }

}
