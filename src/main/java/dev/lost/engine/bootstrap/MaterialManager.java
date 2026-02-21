package dev.lost.engine.bootstrap;

import dev.lost.engine.utils.ReflectionUtils;
import dev.misieur.justamaterial.MaterialInfo;
import dev.misieur.justamaterial.MaterialInjector;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.craftbukkit.util.CraftMagicNumbers;

import java.util.List;
import java.util.Locale;
import java.util.Objects;

public class MaterialManager {

    private final List<MaterialInfo> materials;

    public MaterialManager(boolean customMaterials) {
        materials = customMaterials ? new ObjectArrayList<>() : null;
    }

    public void setMaterial(Item item, String fallbackMaterial) {
        if (materials != null) {
            Identifier key = BuiltInRegistries.ITEM.getKey(item);
            materials.add(
                    new MaterialInfo(
                            key.getNamespace().toUpperCase(Locale.ROOT) + "_" + key.getPath().toUpperCase(Locale.ROOT),
                            null,
                            null,
                            new NamespacedKey(key.getNamespace(), key.getPath())
                    )
            );
        } else {
            try {
                Objects.requireNonNull(CraftMagicNumbers.INSTANCE);
                ReflectionUtils.setItemMaterial(item, org.bukkit.Material.getMaterial(fallbackMaterial));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    public void setMaterial(Block block, String fallbackMaterial) {
        if (materials != null) {
            Identifier key = BuiltInRegistries.BLOCK.getKey(block);
            materials.add(
                    new MaterialInfo(
                            key.getNamespace().toUpperCase(Locale.ROOT) + "_" + key.getPath().toUpperCase(Locale.ROOT),
                            null,
                            null,
                            new NamespacedKey(key.getNamespace(), key.getPath())
                    )
            );
        } else {
            Objects.requireNonNull(CraftMagicNumbers.INSTANCE);
            try {
                ReflectionUtils.setBlockMaterial(block, org.bukkit.Material.getMaterial(fallbackMaterial));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    public int getSize() {
        return materials != null ? materials.size() : 0;
    }

    public void inject(String jarFilePath) {
        if (materials == null) throw new IllegalStateException("Custom materials are not enabled");

        MaterialInjector.injectMaterials(jarFilePath, materials.toArray(new MaterialInfo[0]));
        Objects.requireNonNull(CraftMagicNumbers.INSTANCE);
        try {
            for (Block block : BuiltInRegistries.BLOCK) {
                Identifier key = BuiltInRegistries.BLOCK.getKey(block);
                if (key.getNamespace().toLowerCase(Locale.ROOT).equals("lost_engine")) {
                    String name = key.toString().toUpperCase(Locale.ROOT).replaceAll(":+", "_");
                    Material material = Material.getMaterial(name);
                    if (material == null) {
                        throw new IllegalStateException("Material not found: " + name);
                    }
                    ReflectionUtils.setBlockMaterial(block, material);
                }
            }

            for (Item item : BuiltInRegistries.ITEM) {
                Identifier key = BuiltInRegistries.ITEM.getKey(item);
                if (key.getNamespace().toLowerCase(Locale.ROOT).equals("lost_engine")) {
                    String name = key.toString().toUpperCase(Locale.ROOT).replaceAll(":+", "_");
                    Material material = Material.getMaterial(name);
                    if (material == null) {
                        throw new IllegalStateException("Material not found: " + name);
                    }
                    ReflectionUtils.setItemMaterial(item, material);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
