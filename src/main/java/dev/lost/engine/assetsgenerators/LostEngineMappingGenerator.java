package dev.lost.engine.assetsgenerators;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import dev.lost.engine.items.customitems.CustomItem;
import dev.lost.engine.utils.FileUtils;
import dev.lost.engine.utils.ItemUtils;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.component.Consumable;
import net.minecraft.world.item.component.Tool;
import net.minecraft.world.item.enchantment.Enchantable;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;

public class LostEngineMappingGenerator {

    private final JsonArray items = new JsonArray();
    private final JsonArray blocks = new JsonArray();
    private final JsonObject locales = new JsonObject();

    public void addItem(JsonObject item) {
        items.add(item);
    }

    public void addLocale(String langCode, JsonObject locale) {
        locales.add(langCode, locale);
    }

    @SuppressWarnings("deprecation")
    public void addItem(@NotNull Item item, @Nullable String icon) {
        JsonObject itemJson = new JsonObject();
        itemJson.addProperty("javaId", ItemUtils.packetIdFromItem(item));
        itemJson.addProperty("identifier", item.builtInRegistryHolder().key().identifier().toString());
        itemJson.addProperty("icon", icon);
        itemJson.addProperty("stackSize", item.getDefaultMaxStackSize());
        itemJson.addProperty("maxDamage", item.components().getOrDefault(DataComponents.MAX_DAMAGE, 0));
        Enchantable enchantable = item.components().get(DataComponents.ENCHANTABLE);
        if (enchantable != null) itemJson.addProperty("enchantable", enchantable.value());
        itemJson.addProperty("block", item instanceof BlockItem blockItem ? blockItem.getBlock().builtInRegistryHolder().key().identifier().toString() : null);
        FoodProperties foodProperties = item.components().get(DataComponents.FOOD);
        if (foodProperties != null) {
            itemJson.addProperty("isEatable", true);
            itemJson.addProperty("isAlwaysEatable", foodProperties.canAlwaysEat());
            Consumable consumable = item.components().get(DataComponents.CONSUMABLE);
            if (consumable != null) itemJson.addProperty("consumeSeconds", consumable.consumeSeconds());
        }
        Tool tool = item.components().get(DataComponents.TOOL);
        if (tool != null) {
            itemJson.addProperty("isTool", true);
            JsonObject toolPropertiesJson = new JsonObject();
            toolPropertiesJson.addProperty("defaultMiningSpeed", tool.defaultMiningSpeed());
            toolPropertiesJson.addProperty("canDestroyBlocksInCreative", tool.canDestroyBlocksInCreative());
            JsonArray rulesArray = new JsonArray();
            for (Tool.Rule rule : tool.rules()) {
                if (rule.speed().isEmpty()) continue;
                JsonObject ruleJson = new JsonObject();
                JsonArray blocksArray = new JsonArray();
                for (Holder<Block> holder : rule.blocks()) {
                    blocksArray.add(holder.value().builtInRegistryHolder().key().identifier().toString());
                }
                ruleJson.add("blocks", blocksArray);
                ruleJson.addProperty("speed", rule.speed().get());
                rulesArray.add(ruleJson);
            }
            toolPropertiesJson.add("rules", rulesArray);
            itemJson.add("toolProperties", toolPropertiesJson);
        }
        itemJson.addProperty("creativeCategory", switch (((CustomItem) item).toolType()) {
            case "sword" -> {
                itemJson.addProperty("creativeGroup", "minecraft:itemGroup.name.sword");
                yield "equipment";
            }
            case "hoe" -> {
                itemJson.addProperty("creativeGroup", "minecraft:itemGroup.name.hoe");
                yield "equipment";
            }
            case "shovel" -> {
                itemJson.addProperty("creativeGroup", "minecraft:itemGroup.name.shovel");
                yield "equipment";
            }
            case "pickaxe" -> {
                itemJson.addProperty("creativeGroup", "minecraft:itemGroup.name.pickaxe");
                yield "equipment";
            }
            case "axe" -> {
                itemJson.addProperty("creativeGroup", "minecraft:itemGroup.name.axe");
                yield "equipment";
            }
            case null, default -> {
                if (item instanceof BlockItem) {
                    yield "construction";
                } else if (itemJson.has("isEatable")) {
                    itemJson.addProperty("creativeGroup", "minecraft:itemGroup.name.miscFood");
                    yield "equipment";
                } else {
                    yield "items";
                }
            }
        });
        items.add(itemJson);
    }

    public void addBlock(JsonObject block) {
        this.blocks.add(block);
    }

    private @NotNull JsonObject generateMapping() {
        JsonObject mapping = new JsonObject();
        mapping.add("items", items);
        mapping.add("blocks", blocks);
        mapping.add("locales", locales);
        return mapping;
    }

    public void build(File dataFolder) throws IOException {
        File mappingFile = new File(dataFolder, "mappings.lomapping");
        JsonObject mapping = generateMapping();
        FileUtils.saveJsonToFile(mapping, mappingFile);
    }

}
