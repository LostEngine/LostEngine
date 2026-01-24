package dev.lost.engine.assetsgenerators;

import dev.lost.furnace.resourcepack.BedrockResourcePack;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;

import java.awt.image.BufferedImage;

public class BedrockFontGenerator {

    private final Object2ObjectOpenHashMap<String, BufferedImage> glyphs = new Object2ObjectOpenHashMap<>();

    public void addGlyph(String name, BufferedImage texture) {
        glyphs.put(name, texture);
    }

    public void build(BedrockResourcePack pack, LostEngineMappingGenerator mappingGenerator) {
        // TODO: W.I.P.
        BufferedImage fontAtlas = new BufferedImage(256, 256, BufferedImage.TYPE_INT_ARGB);
        int i = 0;
        for (Object2ObjectMap.Entry<String, BufferedImage> entry : glyphs.object2ObjectEntrySet()) {
            String name = entry.getKey();
            BufferedImage image = entry.getValue();

        }
    }

}
