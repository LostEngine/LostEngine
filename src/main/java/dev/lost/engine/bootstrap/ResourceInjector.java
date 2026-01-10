package dev.lost.engine.bootstrap;

import com.google.common.collect.Maps;
import dev.lost.engine.annotations.CanBreakOnUpdates;
import dev.lost.engine.assetsgenerators.DataPackGenerator;
import dev.lost.engine.bootstrap.components.*;
import dev.lost.engine.bootstrap.components.annotations.Parameter;
import dev.lost.engine.bootstrap.components.annotations.Property;
import dev.lost.engine.customblocks.BlockInjector;
import dev.lost.engine.customblocks.BlockStateProvider;
import dev.lost.engine.items.ItemInjector;
import dev.lost.engine.utils.FileUtils;
import dev.lost.engine.utils.ReflectionUtils;
import io.papermc.paper.plugin.bootstrap.BootstrapContext;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ToolMaterial;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.equipment.ArmorMaterials;
import net.minecraft.world.item.equipment.ArmorType;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@SuppressWarnings("UnstableApiUsage")
public class ResourceInjector {

    @CanBreakOnUpdates(lastCheckedVersion = "1.21.11") // If there is a new Material
    static Map<String, ToolMaterial> toolMaterials = new Object2ObjectOpenHashMap<>(Map.of(
            "WOOD", ToolMaterial.WOOD,
            "STONE", ToolMaterial.STONE,
            "COPPER", ToolMaterial.COPPER,
            "IRON", ToolMaterial.IRON,
            "DIAMOND", ToolMaterial.DIAMOND,
            "GOLD", ToolMaterial.GOLD,
            "NETHERITE", ToolMaterial.NETHERITE
    ));
    static Map<String, ArmorMaterial> armorMaterials = new Object2ObjectOpenHashMap<>(Map.of(
            "LEATHER", ArmorMaterials.LEATHER,
            "COPPER", ArmorMaterials.COPPER,
            "CHAINMAIL", ArmorMaterials.CHAINMAIL,
            "IRON", ArmorMaterials.IRON,
            "GOLD", ArmorMaterials.GOLD,
            "DIAMOND", ArmorMaterials.DIAMOND,
            "TURTLE_SCUTE", ArmorMaterials.TURTLE_SCUTE,
            "NETHERITE", ArmorMaterials.NETHERITE,
            "ARMADILLO_SCUTE", ArmorMaterials.ARMADILLO_SCUTE
    ));

    static List<ComponentProperty> propertyClassInstances = List.of(
            new EnchantmentGlintOverrideProperty(),
            new FireResistantProperty(),
            new FoodProperty(),
            new MaxDamageProperty(),
            new MaxStackSizeProperty(),
            new RarityProperty(),
            new TooltipDisplayProperty(),
            new UnbreakableProperty(),
            new UseCooldownProperty()
    );

    public static void injectResources(@NotNull BootstrapContext context, DataPackGenerator dataPackGenerator) throws Exception {
        File resourceFolder = new File(context.getDataDirectory().toFile(), "resources");
        if (!resourceFolder.exists())
            FileUtils.extractDirectoryFromJar(context.getPluginSource(), "resources", resourceFolder.toPath());

        List<FileUtils.ItemConfig> configs = FileUtils.yamlFiles(resourceFolder);
        for (FileUtils.ItemConfig config : configs) {
            injectMaterials(dataPackGenerator, config.config());
            injectItems(context, dataPackGenerator, config.config());
            injectBlocks(context, dataPackGenerator, config.config());
        }
    }

    private static void injectMaterials(DataPackGenerator dataPackGenerator, @NotNull YamlConfiguration config) {
        ConfigurationSection materialsSection = config.getConfigurationSection("materials");
        if (materialsSection == null)
            return;

        for (String key : materialsSection.getKeys(false)) {
            ConfigurationSection materialSection = materialsSection.getConfigurationSection(key);
            if (materialSection == null)
                continue;

            int enchantmentValue = materialSection.getInt("enchantment_value", 15);
            String repairItem = materialSection.getString("repair_item", null);
            TagKey<Item> repairItems = TagKey.create(Registries.ITEM, Identifier.parse(key.toLowerCase() + "_repair_items"));
            if (repairItem != null)
                dataPackGenerator.addRepairItems(repairItems.location().getPath(), repairItem);

            ConfigurationSection toolSection = materialSection.getConfigurationSection("tool");
            if (toolSection != null) {
                String base = toolSection.getString("base", "NETHERITE").toUpperCase(Locale.ROOT);
                int durability = toolSection.getInt("durability", 59);
                float speed = (float) toolSection.getDouble("speed", 2.0F);
                float attackDamageBonus = (float) toolSection.getDouble("attack_damage_bonus", 0.0);

                ToolMaterial baseMaterial = getOrThrow(toolMaterials, base, "Invalid base material: " + base);

                toolMaterials.put(key.toUpperCase(Locale.ROOT), ItemInjector.createToolMaterial(baseMaterial, durability, speed, attackDamageBonus, enchantmentValue, repairItems));
            }
            ConfigurationSection armorSection = materialSection.getConfigurationSection("armor");
            if (armorSection != null) {
                int durability = armorSection.getInt("durability", 5);
                Map<ArmorType, Integer> defense = Maps.newEnumMap(Map.of(
                        ArmorType.BOOTS, armorSection.getInt("defense.boots", 1),
                        ArmorType.LEGGINGS, armorSection.getInt("defense.leggings", 2),
                        ArmorType.CHESTPLATE, armorSection.getInt("defense.chestplate", 3),
                        ArmorType.HELMET, armorSection.getInt("defense.helmet", 1),
                        ArmorType.BODY, 0
                ));
                String equipSound = armorSection.getString("equip_sound", "item.armor.equip_leather");
                float toughness = (float) armorSection.getDouble("toughness", 0.0F);
                float knockbackResistance = (float) armorSection.getDouble("knockback_resistance", 0.0F);
                armorMaterials.put(key.toUpperCase(Locale.ROOT), ItemInjector.createArmorMaterial(durability, defense, enchantmentValue, equipSound, toughness, knockbackResistance, repairItems, key.toLowerCase(Locale.ROOT)));
            }
        }
    }

    private static void injectItems(@NotNull BootstrapContext context, DataPackGenerator dataPackGenerator, @NotNull YamlConfiguration config) {
        ConfigurationSection itemsSection = config.getConfigurationSection("items");
        if (itemsSection == null)
            return;

        for (String key : itemsSection.getKeys(false)) {
            ConfigurationSection itemSection = itemsSection.getConfigurationSection(key);
            if (itemSection == null)
                continue;

            String type = itemSection.getString("type", "item").toLowerCase();
            Map<DataComponentType<?>, Object> components = new Object2ObjectOpenHashMap<>();

            applyComponents(context, itemSection, components);

            try {
                switch (type) {
                    case "generic" -> ItemInjector.injectItem(key, components);

                    case "sword" -> {
                        float attackDamage = (float) itemSection.getDouble("attack_damage", 3.0F);
                        float attackSpeed = (float) itemSection.getDouble("attack_speed", -2.4F);
                        String materialName = itemSection.getString("material", "NETHERITE").toUpperCase(Locale.ROOT);
                        ToolMaterial material = getOrThrow(toolMaterials, materialName, "Invalid tool material: " + materialName + " for item " + key);

                        ItemInjector.injectSword(key, attackDamage, attackSpeed, material, dataPackGenerator, components);
                    }

                    case "shovel" -> {
                        float attackDamage = (float) itemSection.getDouble("attack_damage", 1.5F);
                        float attackSpeed = (float) itemSection.getDouble("attack_speed", -3.0F);
                        String materialName = itemSection.getString("material", "NETHERITE").toUpperCase(Locale.ROOT);
                        ToolMaterial material = getOrThrow(toolMaterials, materialName, "Invalid tool material: " + materialName + " for item " + key);

                        ItemInjector.injectShovel(key, attackDamage, attackSpeed, material, dataPackGenerator, components);
                    }

                    case "pickaxe" -> {
                        float attackDamage = (float) itemSection.getDouble("attack_damage", 1.0F);
                        float attackSpeed = (float) itemSection.getDouble("attack_speed", -2.8F);
                        String materialName = itemSection.getString("material", "NETHERITE").toUpperCase(Locale.ROOT);
                        ToolMaterial material = getOrThrow(toolMaterials, materialName, "Invalid tool material: " + materialName + " for item " + key);

                        ItemInjector.injectPickaxe(key, attackDamage, attackSpeed, material, dataPackGenerator, components);
                    }

                    case "axe" -> {
                        float attackDamage = (float) itemSection.getDouble("attack_damage", 5.0F);
                        float attackSpeed = (float) itemSection.getDouble("attack_speed", -3.0F);
                        String materialName = itemSection.getString("material", "NETHERITE").toUpperCase(Locale.ROOT);
                        ToolMaterial material = getOrThrow(toolMaterials, materialName, "Invalid tool material: " + materialName + " for item " + key);

                        ItemInjector.injectAxe(key, attackDamage, attackSpeed, material, dataPackGenerator, components);
                    }

                    case "hoe" -> {
                        float attackSpeed = (float) itemSection.getDouble("attack_speed", 0.0F);
                        String materialName = itemSection.getString("material", "NETHERITE").toUpperCase(Locale.ROOT);
                        ToolMaterial material = getOrThrow(toolMaterials, materialName, "Invalid tool material: " + materialName + " for item " + key);

                        ItemInjector.injectHoe(key, attackSpeed, material, dataPackGenerator, components);
                    }

                    case "armor" -> {
                        String materialName = itemSection.getString("material", "IRON").toUpperCase(Locale.ROOT);
                        ItemInjector.ArmorType armorType = switch (itemSection.getString("armor_type", "CHESTPLATE").toUpperCase(Locale.ROOT)) {
                            case "HELMET" -> ItemInjector.ArmorType.HELMET;
                            case "CHESTPLATE" -> ItemInjector.ArmorType.CHESTPLATE;
                            case "LEGGINGS" -> ItemInjector.ArmorType.LEGGINGS;
                            case "BOOTS" -> ItemInjector.ArmorType.BOOTS;
                            default ->
                                    throw new IllegalStateException("Invalide armor type: " + itemSection.getString("armor_type", "CHESTPLATE").toUpperCase(Locale.ROOT) + " for item " + key + " (HELMET, CHESTPLATE, LEGGINGS, or BOOTS)");
                        };
                        ArmorMaterial material = getOrThrow(armorMaterials, materialName, "Invalid armor material: " + materialName + " for item " + key);
                        ItemInjector.injectArmor(key, material, armorType, dataPackGenerator, components);
                    }

                    case "elytra" -> {
                        int durability = itemSection.getInt("elytra.durability", 432);
                        String repairItem = itemSection.getString("elytra.repair_item", null);
                        ItemInjector.injectElytra(key, repairItem, durability, components);
                    }

                    case "trident" -> {
                        int durability = itemSection.getInt("trident.durability", 250);
                        float attackDamage = (float) itemSection.getDouble("trident.attack_damage", 8.0F);
                        ItemInjector.injectTrident(key, durability, attackDamage, dataPackGenerator, components);
                    }

                    default -> context.getLogger().warn("Unknown item type: {} for item: {}", type, key);
                }
            } catch (Exception e) {
                throw new RuntimeException("Failed to inject item: " + key, e);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private static void applyComponents(@NotNull BootstrapContext context, @NotNull ConfigurationSection itemSection, Map<DataComponentType<?>, Object> components) {
        ConfigurationSection componentsSection = itemSection.getConfigurationSection("components");
        if (componentsSection == null)
            return;

        for (ComponentProperty componentProperty : propertyClassInstances) {
            Property property = componentProperty.getClass().getAnnotation(Property.class);
            if (property == null) {
                context.getLogger().warn("Missing @Property annotation on ComponentProperty class: {}", componentProperty.getClass().getName());
                continue;
            }

            String key = property.key();
            if (!componentsSection.contains(key))
                continue;

            if (componentProperty instanceof SimpleComponentProperty<?> simpleComponentProperty) {
                Object value = componentsSection.get(key);
                Class<?> expectedType = ReflectionUtils.getTypeArgument(simpleComponentProperty.getClass());

                if (expectedType != null) {
                    value = convertNumberType(value, expectedType);
                }

                if (expectedType != null && expectedType.isInstance(value)) {
                    ((SimpleComponentProperty<Object>) simpleComponentProperty).applyComponent(context, value, key, components);
                } else if (value != null && expectedType != null) {
                    context.getLogger().warn("Invalid type for component property '{}'. Expected {}, got {} for item {}", key, expectedType.getSimpleName(), value.getClass().getSimpleName(), itemSection.getName());
                }
                continue;
            }

            ConfigurationSection componentPropertySection = componentsSection.getConfigurationSection(key);

            if (componentPropertySection == null) continue;
            if (fillParameters(context, componentProperty, componentPropertySection, itemSection.getName()))
                componentProperty.applyComponent(context, componentPropertySection, itemSection.getName(), components);
        }
    }

    private static boolean fillParameters(@NotNull BootstrapContext context, @NotNull ComponentProperty componentProperty, @NotNull ConfigurationSection section, @NotNull String name) throws RuntimeException {
        for (Field field : componentProperty.getClass().getDeclaredFields()) {
            Parameter parameter = field.getAnnotation(Parameter.class);
            if (parameter == null)
                continue;

            Object value = section.get(parameter.key());
            value = convertNumberType(value, parameter.type());

            try {
                // First set the field to accessible
                field.setAccessible(true);
                if (parameter.type().isInstance(value)) {
                    // if the type is the right one, we can set it
                    field.set(componentProperty, value);
                } else {
                    // if not, reset the field's value
                    field.set(componentProperty, null);
                    if (value != null) {
                        // if the value is the wrong type, tell the user and if it is required, return false
                        context.getLogger().warn("Invalid type for parameter '{}'. Expected {}, got {} for item {}", parameter.key(), parameter.type().getSimpleName(), value.getClass().getSimpleName(), name);
                        if (parameter.required()) {
                            context.getLogger().warn("This parameter is required, can't create component");
                            return false;
                        }
                    } else {
                        // if the value is null and required, return false
                        if (parameter.required()) {
                            context.getLogger().warn("Missing required parameters for field {} on ComponentProperty: {}", field.getName(), parameter.key());
                            return false;
                        }
                    }

                }
            } catch (Exception e) {
                context.getLogger().error("Failed to inject value for field: {}", field.getName(), e);
                // We can stop the server if this happens it would generate too many exceptions
                LostEngineBootstrap.stopServer(context);
                return false; // return false will never be executed ;(
            }
        }
        return true;
    }

    private static @Nullable Object convertNumberType(@Nullable Object value, @Nullable Class<?> targetType) {
        if (!(value instanceof Number number) || targetType == null) {
            return value;
        }

        if (targetType == Float.class || targetType == float.class) return number.floatValue();
        if (targetType == Double.class || targetType == double.class) return number.doubleValue();
        if (targetType == Integer.class || targetType == int.class) return number.intValue();
        if (targetType == Long.class || targetType == long.class) return number.longValue();
        if (targetType == Short.class || targetType == short.class) return number.shortValue();
        if (targetType == Byte.class || targetType == byte.class) return number.byteValue();

        return value;
    }

    private static void injectBlocks(@NotNull BootstrapContext context, @NotNull DataPackGenerator dataPackGenerator, @NotNull YamlConfiguration config) {
        ConfigurationSection blocksSection = config.getConfigurationSection("blocks");
        if (blocksSection == null) return;

        for (String key : blocksSection.getKeys(false)) {
            ConfigurationSection blockSection = blocksSection.getConfigurationSection(key);
            if (blockSection == null) continue;
            String requiredMaterial = blockSection.getString("required_material", "NONE").toUpperCase(Locale.ROOT);
            switch (requiredMaterial) {
                case "WOOD" -> dataPackGenerator.needsWoodenTool("lost_engine:" + key);
                case "STONE" -> dataPackGenerator.needsStoneTool("lost_engine:" + key);
                case "IRON" -> dataPackGenerator.needsIronTool("lost_engine:" + key);
                case "DIAMOND" -> dataPackGenerator.needsDiamondTool("lost_engine:" + key);
                case "NETHERITE" -> dataPackGenerator.needsNetheriteTool("lost_engine:" + key);
                case "NONE" -> {
                    // Nothing to do
                }
                default ->
                        context.getLogger().error("Unknown required material: {} for block: {} (WOOD, STONE, IRON, DIAMOND, or NETHERITE)", requiredMaterial, key);
            }
            ConfigurationSection dropsSection = blockSection.getConfigurationSection("drops");
            if (dropsSection != null) {
                String dropType = dropsSection.getString("type", null);
                if (dropType != null) {
                    switch (dropType.toLowerCase()) {
                        case "self" -> dataPackGenerator.simpleLootTable(key, "lost_engine:" + key);
                        case "ore" -> dataPackGenerator.oreLootTable(
                                key,
                                dropsSection.getString("item", "minecraft:stick"),
                                "lost_engine:" + key,
                                dropsSection.getInt("max", 1),
                                dropsSection.getInt("min", 1)
                        );
                        default -> context.getLogger().error("Unknown drop type: {} for block: {}", dropType, key);
                    }
                }
            }
            String type = blockSection.getString("type", "regular").toLowerCase();
            try {
                switch (type) {
                    case "regular" -> BlockInjector.injectRegularBlock(
                            key,
                            BlockStateProvider.getNextBlockState(BlockStateProvider.BlockStateType.REGULAR),
                            dataPackGenerator,
                            (float) blockSection.getDouble("destroy_time", 0F),
                            (float) blockSection.getDouble("explosion_resistance", 0F),
                            BlockInjector.Minable.valueOf(blockSection.getString("tool_type", "none").toUpperCase(Locale.ROOT))
                    );
                    case "tnt" -> BlockInjector.injectTNTBlock(
                            key,
                            BlockStateProvider.getNextBlockState(BlockStateProvider.BlockStateType.REGULAR),
                            blockSection.getInt("explosion_power", 4)
                    );
                }
            } catch (Exception e) {
                throw new RuntimeException("Failed to inject block: " + key, e);
            }
        }
    }

    public static <K> K getOrThrow(Map<?, K> map, Object key, String message) {
        K obj = map.get(key);
        if (obj == null)
            throw new IllegalArgumentException(message);

        return obj;
    }

}
