package dev.lost.engine.geyserextension.lomapping;

import com.google.gson.JsonElement;
import dev.lost.engine.geyserextension.lomapping.blocks.Block;
import dev.lost.engine.geyserextension.lomapping.items.Item;

import java.util.List;
import java.util.Map;

public interface Mapping {

    List<? extends Item> items();

    List<? extends Block> blocks();

    Map<String, JsonElement> locales();

}
