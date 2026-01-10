package dev.lost.engine.assetsgenerators;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.lost.engine.annotations.CanBreakOnUpdates;
import dev.lost.engine.utils.FileUtils;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.SharedConstants;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;

import static dev.lost.engine.utils.FileUtils.saveJsonToFile;

@CanBreakOnUpdates(lastCheckedVersion = "1.21.10")
public class DataPackGenerator {

    private final JsonObject swords = new JsonObject();
    private final JsonObject pickaxes = new JsonObject();
    private final JsonObject shovels = new JsonObject();
    private final JsonObject axes = new JsonObject();
    private final JsonObject hoes = new JsonObject();
    private final JsonObject helmets = new JsonObject();
    private final JsonObject chestplates = new JsonObject();
    private final JsonObject leggings = new JsonObject();
    private final JsonObject boots = new JsonObject();
    private final JsonObject tridents = new JsonObject();
    private final JsonObject axeMinable = new JsonObject();
    private final JsonObject hoeMinable = new JsonObject();
    private final JsonObject pickaxeMinable = new JsonObject();
    private final JsonObject shovelMinable = new JsonObject();
    private final JsonObject incorrectForWoodenTools = new JsonObject();
    private final JsonObject incorrectForGoldTools = new JsonObject();
    private final JsonObject incorrectForStoneTools = new JsonObject();
    private final JsonObject incorrectForCopperTools = new JsonObject();
    private final JsonObject incorrectForIronTools = new JsonObject();
    private final JsonObject incorrectForDiamondTools = new JsonObject();
    private final Object2ObjectOpenHashMap<String, JsonObject> repairItems = new Object2ObjectOpenHashMap<>();
    private final Object2ObjectOpenHashMap<String, JsonObject> lootTables = new Object2ObjectOpenHashMap<>();
    private static final JsonObject MCMETA;

    static {
        final var major = SharedConstants.getCurrentVersion().packVersion(net.minecraft.server.packs.PackType.SERVER_DATA).major();
        final var minor = SharedConstants.getCurrentVersion().packVersion(net.minecraft.server.packs.PackType.SERVER_DATA).minor();
        MCMETA = JsonParser.parseString("""
                    {
                        "pack": {
                            "description": "LostEngine Data Pack",
                            "min_format": [%d, %d],
                            "max_format": [%d, %d]
                        }
                    }
                    """.formatted(major, minor, major, minor)).getAsJsonObject();
    }

    public DataPackGenerator() {
        swords.add("values", new JsonArray());
        pickaxes.add("values", new JsonArray());
        shovels.add("values", new JsonArray());
        axes.add("values", new JsonArray());
        hoes.add("values", new JsonArray());
        helmets.add("values", new JsonArray());
        chestplates.add("values", new JsonArray());
        leggings.add("values", new JsonArray());
        boots.add("values", new JsonArray());
        tridents.add("values", new JsonArray());
        axeMinable.add("values", new JsonArray());
        hoeMinable.add("values", new JsonArray());
        pickaxeMinable.add("values", new JsonArray());
        shovelMinable.add("values", new JsonArray());
        incorrectForWoodenTools.add("values", new JsonArray());
        incorrectForGoldTools.add("values", new JsonArray());
        incorrectForStoneTools.add("values", new JsonArray());
        incorrectForCopperTools.add("values", new JsonArray());
        incorrectForIronTools.add("values", new JsonArray());
        incorrectForDiamondTools.add("values", new JsonArray());
    }

    public void addSword(String id) {
        swords.getAsJsonArray("values").add(id);
    }

    public void addPickaxe(String id) {
        pickaxes.getAsJsonArray("values").add(id);
    }

    public void addShovel(String id) {
        shovels.getAsJsonArray("values").add(id);
    }

    public void addAxe(String id) {
        axes.getAsJsonArray("values").add(id);
    }

    public void addHoe(String id) {
        hoes.getAsJsonArray("values").add(id);
    }

    public void addHelmet(String id) {
        helmets.getAsJsonArray("values").add(id);
    }

    public void addChestplate(String id) {
        chestplates.getAsJsonArray("values").add(id);
    }

    public void addLeggings(String id) {
        leggings.getAsJsonArray("values").add(id);
    }

    public void addBoots(String id) {
        boots.getAsJsonArray("values").add(id);
    }

    public void addTrident(String id) {
        tridents.getAsJsonArray("values").add(id);
    }

    public void addAxeMinable(String id) {
        axeMinable.getAsJsonArray("values").add(id);
    }

    public void addHoeMinable(String id) {
        hoeMinable.getAsJsonArray("values").add(id);
    }

    public void addPickaxeMinable(String id) {
        pickaxeMinable.getAsJsonArray("values").add(id);
    }

    public void addShovelMinable(String id) {
        shovelMinable.getAsJsonArray("values").add(id);
    }

    public void addRepairItems(String materialName, String @NotNull ... itemIds) {
        JsonObject materialTag = new JsonObject();
        JsonArray values = new JsonArray();
        for (String itemId : itemIds) {
            values.add(itemId);
        }
        materialTag.add("values", values);
        repairItems.put(materialName, materialTag);
    }

    public void simpleLootTable(String id, String itemId) {
        lootTables.put(id, JsonParser.parseString("""
                {
                  "type": "minecraft:block",
                  "pools": [
                    {
                      "bonus_rolls": 0.0,
                      "conditions": [
                        {
                          "condition": "minecraft:survives_explosion"
                        }
                      ],
                      "entries": [
                        {
                          "type": "minecraft:item",
                          "name": "%s"
                        }
                      ],
                      "rolls": 1.0
                    }
                  ],
                  "random_sequence": "lost_engine:blocks/%s"
                }
                
                """.formatted(itemId, id)).getAsJsonObject());
    }

    public void oreLootTable(String id, String itemId, String oreBlock , int max, int min) {
        lootTables.put(id, JsonParser.parseString("""
                {
                  "type": "minecraft:block",
                  "pools": [
                    {
                      "bonus_rolls": 0,
                      "entries": [
                        {
                          "type": "minecraft:alternatives",
                          "children": [
                            {
                              "type": "minecraft:item",
                              "conditions": [
                                {
                                  "condition": "minecraft:match_tool",
                                  "predicate": {
                                    "predicates": {
                                      "minecraft:enchantments": [
                                        {
                                          "enchantments": "minecraft:silk_touch",
                                          "levels": {
                                            "min": 1
                                          }
                                        }
                                      ]
                                    }
                                  }
                                }
                              ],
                              "name": "%s"
                            },
                            {
                              "type": "minecraft:item",
                              "functions": [
                                {
                                  "add": false,
                                  "count": {
                                    "type": "minecraft:uniform",
                                    "max": %d,
                                    "min": %d
                                  },
                                  "function": "minecraft:set_count"
                                },
                                {
                                  "enchantment": "minecraft:fortune",
                                  "formula": "minecraft:ore_drops",
                                  "function": "minecraft:apply_bonus"
                                },
                                {
                                  "function": "minecraft:explosion_decay"
                                }
                              ],
                              "name": "%s"
                            }
                          ]
                        }
                      ],
                      "rolls": 1
                    }
                  ],
                  "random_sequence": "minecraft:blocks/copper_ore"
                }
                """.formatted(oreBlock, max, min, itemId)).getAsJsonObject());
    }

    public void build(@NotNull File dataPackFolder) throws IOException {
        FileUtils.deleteFolder(dataPackFolder.toPath());
        saveJsonToFile(MCMETA,                   new File(dataPackFolder + "/pack.mcmeta"));
        saveJsonToFile(swords,                   new File(dataPackFolder + "/data/minecraft/tags/item/swords.json"));
        saveJsonToFile(pickaxes,                 new File(dataPackFolder + "/data/minecraft/tags/item/pickaxes.json"));
        saveJsonToFile(shovels,                  new File(dataPackFolder + "/data/minecraft/tags/item/shovels.json"));
        saveJsonToFile(axes,                     new File(dataPackFolder + "/data/minecraft/tags/item/axes.json"));
        saveJsonToFile(hoes,                     new File(dataPackFolder + "/data/minecraft/tags/item/hoes.json"));
        saveJsonToFile(helmets,                  new File(dataPackFolder + "/data/minecraft/tags/item/head_armor.json"));
        saveJsonToFile(chestplates,              new File(dataPackFolder + "/data/minecraft/tags/item/chest_armor.json"));
        saveJsonToFile(leggings,                 new File(dataPackFolder + "/data/minecraft/tags/item/leg_armor.json"));
        saveJsonToFile(boots,                    new File(dataPackFolder + "/data/minecraft/tags/item/foot_armor.json"));
        saveJsonToFile(tridents,                 new File(dataPackFolder + "/data/minecraft/tags/item/enchantable/trident.json"));
        saveJsonToFile(axeMinable,               new File(dataPackFolder + "/data/minecraft/tags/block/mineable/axe.json"));
        saveJsonToFile(hoeMinable,               new File(dataPackFolder + "/data/minecraft/tags/block/mineable/hoe.json"));
        saveJsonToFile(pickaxeMinable,           new File(dataPackFolder + "/data/minecraft/tags/block/mineable/pickaxe.json"));
        saveJsonToFile(shovelMinable,            new File(dataPackFolder + "/data/minecraft/tags/block/mineable/shovel.json"));
        saveJsonToFile(incorrectForWoodenTools,  new File(dataPackFolder + "/data/minecraft/tags/block/incorrect_for_wooden_tool.json"));
        saveJsonToFile(incorrectForGoldTools,    new File(dataPackFolder + "/data/minecraft/tags/block/incorrect_for_gold_tool.json"));
        saveJsonToFile(incorrectForStoneTools,   new File(dataPackFolder + "/data/minecraft/tags/block/incorrect_for_stone_tool.json"));
        saveJsonToFile(incorrectForCopperTools,  new File(dataPackFolder + "/data/minecraft/tags/block/incorrect_for_copper_tool.json"));
        saveJsonToFile(incorrectForIronTools,    new File(dataPackFolder + "/data/minecraft/tags/block/incorrect_for_iron_tool.json"));
        saveJsonToFile(incorrectForDiamondTools, new File(dataPackFolder + "/data/minecraft/tags/block/incorrect_for_diamond_tool.json"));
        var it = repairItems.object2ObjectEntrySet().fastIterator();
        while (it.hasNext()) {
            var entry = it.next();
            File tagFile = new File(dataPackFolder + "/data/minecraft/tags/item/" + entry.getKey().toLowerCase() + ".json");
            saveJsonToFile(entry.getValue(), tagFile);
        }
        it = lootTables.object2ObjectEntrySet().fastIterator();
        while (it.hasNext()) {
            var entry = it.next();
            File lootTableFile = new File(dataPackFolder + "/data/lost_engine/loot_table/blocks/" + entry.getKey().toLowerCase() + ".json");
            saveJsonToFile(entry.getValue(), lootTableFile);
        }
    }

    public void needsNetheriteTool(String blockId) {
        incorrectForDiamondTools.getAsJsonArray("values").add(blockId);
        incorrectForIronTools.getAsJsonArray("values").add(blockId);
        incorrectForCopperTools.getAsJsonArray("values").add(blockId);
        incorrectForStoneTools.getAsJsonArray("values").add(blockId);
        incorrectForGoldTools.getAsJsonArray("values").add(blockId);
        incorrectForWoodenTools.getAsJsonArray("values").add(blockId);
    }

    public void needsDiamondTool(String blockId) {
        incorrectForIronTools.getAsJsonArray("values").add(blockId);
        incorrectForCopperTools.getAsJsonArray("values").add(blockId);
        incorrectForStoneTools.getAsJsonArray("values").add(blockId);
        incorrectForGoldTools.getAsJsonArray("values").add(blockId);
        incorrectForWoodenTools.getAsJsonArray("values").add(blockId);
    }

    public void needsIronTool(String blockId) {
        incorrectForCopperTools.getAsJsonArray("values").add(blockId);
        incorrectForStoneTools.getAsJsonArray("values").add(blockId);
        incorrectForGoldTools.getAsJsonArray("values").add(blockId);
        incorrectForWoodenTools.getAsJsonArray("values").add(blockId);
    }

    public void needsStoneTool(String blockId) {
        incorrectForStoneTools.getAsJsonArray("values").add(blockId);
        incorrectForGoldTools.getAsJsonArray("values").add(blockId);
        incorrectForWoodenTools.getAsJsonArray("values").add(blockId);
    }

    public void needsWoodenTool(String blockId) {
        incorrectForWoodenTools.getAsJsonArray("values").add(blockId);
    }

}
