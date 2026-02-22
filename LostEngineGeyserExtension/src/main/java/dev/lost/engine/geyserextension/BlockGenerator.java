package dev.lost.engine.geyserextension;

import dev.lost.annotations.NotNull;
import dev.lost.engine.geyserextension.lomapping.Mapping;
import org.geysermc.geyser.api.event.lifecycle.GeyserDefineCustomBlocksEvent;

public class BlockGenerator {
    public static void generateFromJson(Mapping mapping, @NotNull GeyserDefineCustomBlocksEvent event) {
//        NonVanillaCustomBlockData nonVanillaCustomBlockData = NonVanillaCustomBlockData.builder()
//                .namespace("lost_engine:block")
//                .name("test")
//                .creativeCategory(CreativeCategory.CONSTRUCTION)
//                .includedInCreativeInventory(true)
//                .components(CustomBlockComponents.builder().destructibleByMining(4.0f).build())
//                .build();
//        event.register(nonVanillaCustomBlockData);
    }
}