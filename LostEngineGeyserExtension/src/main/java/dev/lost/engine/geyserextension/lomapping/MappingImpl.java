package dev.lost.engine.geyserextension.lomapping;

import com.google.gson.JsonElement;
import dev.lost.engine.geyserextension.lomapping.blocks.BlockImpl;
import dev.lost.engine.geyserextension.lomapping.items.ItemImpl;

import java.util.List;
import java.util.Map;

public record MappingImpl(
        List<ItemImpl> items,
        List<BlockImpl> blocks,
        Map<String, JsonElement> locales
) implements Mapping {
}