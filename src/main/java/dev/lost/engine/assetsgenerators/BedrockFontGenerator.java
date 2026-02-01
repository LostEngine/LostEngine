package dev.lost.engine.assetsgenerators;

import dev.lost.engine.LostEngine;
import dev.lost.furnace.files.texture.Texture;
import dev.lost.furnace.resourcepack.BedrockResourcePack;
import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Comparator;
import java.util.Map;

public class BedrockFontGenerator {

    private final Object2ObjectOpenHashMap<String, BufferedImage> glyphs = new Object2ObjectOpenHashMap<>();

    public void addGlyph(@NotNull String name, @NotNull BufferedImage texture) {
        if (texture.getWidth() > 64 || texture.getHeight() > 64)
            throw new IllegalArgumentException("Glyphs textures must be at most 64x64");

        glyphs.put(name, texture);
    }

    public void build(BedrockResourcePack pack, LangFileGenerator langFileGenerator) {
        // from E2 to F8
        Object2ObjectOpenHashMap<String, Atlas> atlasGlyphs = new Object2ObjectOpenHashMap<>();
        int i = 0;
        for (Map.Entry<String, BufferedImage> entry : glyphs.object2ObjectEntrySet()
                .stream()
                .sorted(
                        Comparator.comparingInt((Map.Entry<String, BufferedImage> o) -> Math.max(o.getValue().getWidth(), o.getValue().getHeight()))
                                .reversed()
                )
                .toList()
        ) {
            int atlasIndex = i++ / 255;
            if (atlasIndex < 0 || atlasIndex > 22) throw new IllegalStateException("Too many glyphs");
            String atlasName = Integer.toHexString(0xE2 + atlasIndex).toUpperCase();
            atlasGlyphs.compute(atlasName, (s, atlas) -> {
                if (atlas == null) atlas = new Atlas();
                atlas.glyphs.put(entry.getKey(), entry.getValue());
                atlas.size = Math.max(atlas.size, Math.max(entry.getValue().getWidth(), entry.getValue().getHeight()) * 16);
                return atlas;
            });
        }

        for (Object2ObjectMap.Entry<String, Atlas> entry : atlasGlyphs.object2ObjectEntrySet()) {
            Atlas atlas = entry.getValue();
            String atlasName = entry.getKey();
            BufferedImage atlasImage = new BufferedImage(atlas.size, atlas.size, BufferedImage.TYPE_INT_ARGB);
            i = 0;
            for (Object2ObjectMap.Entry<String, BufferedImage> glyphEntry : atlas.glyphs.object2ObjectEntrySet()) {
                String glyphName = glyphEntry.getKey();
                BufferedImage glyphImage = glyphEntry.getValue();
                String characterHex = atlasName + String.format("%02x", i);
                char character = (char) Integer.parseInt(characterHex, 16);
                langFileGenerator.addTranslation("en_us", "glyph." + glyphName, String.valueOf(character), LangFileGenerator.Edition.BEDROCK);
                int gridSize = atlas.size / 16;
                int x = i % 16 * gridSize;
                int y = i / 16 * gridSize;
                int offsetX = (gridSize - glyphImage.getWidth()) / 2;
                int offsetY = (gridSize - glyphImage.getHeight()) / 2;
                Graphics2D graphics = atlasImage.createGraphics();
                graphics.drawImage(glyphImage, x + offsetX, y + offsetY, null);
                graphics.dispose();
                i++;
            }
            try {
                pack.texture(Texture.image("font/glyph_%s.png".formatted(atlasName), atlasImage));
            } catch (IOException e) {
                LostEngine.logger().error("Failed to add bedrock texture font/glyph_{}.png to the resource pack", atlasName, e);
            }
        }
    }

    static class Atlas {
        private final Object2ObjectLinkedOpenHashMap<String, BufferedImage> glyphs = new Object2ObjectLinkedOpenHashMap<>();
        int size = 128;
    }

}