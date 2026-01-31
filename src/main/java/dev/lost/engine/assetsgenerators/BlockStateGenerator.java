package dev.lost.engine.assetsgenerators;

import com.google.gson.JsonObject;
import dev.lost.engine.annotations.CanBreakOnUpdates;
import dev.lost.furnace.resourcepack.JavaResourcePack;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

@CanBreakOnUpdates(lastCheckedVersion = "1.21.10")
public class BlockStateGenerator {

    private final Object2ObjectOpenHashMap<String, JsonObject> blockStates = new Object2ObjectOpenHashMap<>();

    public void addBlockState(@NotNull BlockState blockState, @NotNull String modelPath) {
        String key = BuiltInRegistries.BLOCK.getKey(blockState.getBlock()).getPath();
        JsonObject jsonObject;
        if (blockStates.containsKey(key)) {
            jsonObject = blockStates.get(key);
        } else {
            jsonObject = new JsonObject();
            jsonObject.add("variants", new JsonObject());
        }
        JsonObject variants = jsonObject.getAsJsonObject("variants");
        StringBuilder stateKeyBuilder = new StringBuilder();
        blockState.getValues().forEach((property, comparable) -> {
            if (!stateKeyBuilder.isEmpty()) stateKeyBuilder.append(",");
            stateKeyBuilder.append(property.getName()).append("=").append(comparable);
        });
        String stateKey = stateKeyBuilder.toString();
        JsonObject stateObject = new JsonObject();
        stateObject.addProperty("model", modelPath);
        variants.add(stateKey, stateObject);
        blockStates.put(key, jsonObject);
    }

    public void build(@NotNull JavaResourcePack resourcePack) {
        var it = blockStates.object2ObjectEntrySet().fastIterator();
        while (it.hasNext()) {
            var entry = it.next();
            resourcePack.jsonFile("assets/minecraft/blockstates/" + entry.getKey() + ".json", entry.getValue());
        }
    }

}
