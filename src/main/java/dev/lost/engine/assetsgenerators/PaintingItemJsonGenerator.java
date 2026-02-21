package dev.lost.engine.assetsgenerators;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import dev.lost.furnace.resourcepack.JavaResourcePack;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;

public class PaintingItemJsonGenerator {

    private final Object2ObjectOpenHashMap<String, JsonObject> items = new Object2ObjectOpenHashMap<>();

    public void addItem(String id, JsonObject model) {
        items.put(id, model);
    }

    public void build(JavaResourcePack pack) {
        JsonObject json = new JsonObject();
        JsonObject model = new JsonObject();
        model.addProperty("type", "minecraft:select");
        model.addProperty("property", "minecraft:component");
        model.addProperty("component", "minecraft:painting/variant");
        JsonArray cases = new JsonArray();
        for (Object2ObjectOpenHashMap.Entry<String, JsonObject> entry : items.object2ObjectEntrySet()) {
            JsonObject caseObject = new JsonObject();
            caseObject.addProperty("when", entry.getKey());
            caseObject.add("model", entry.getValue());
            cases.add(caseObject);
        }
        model.add("cases", cases);
        JsonObject fallback = new JsonObject();
        fallback.addProperty("type", "minecraft:model");
        fallback.addProperty("model", "minecraft:item/painting");
        model.add("fallback", fallback);
        json.add("model", model);
        pack.jsonFile("assets/minecraft/items/painting.json", json);
    }

}
