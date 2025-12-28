package dev.lost.engine.bootstrap;

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
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ToolMaterial;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@SuppressWarnings("UnstableApiUsage")
public class ResourceInjector {

    @CanBreakOnUpdates(lastCheckedVersion = "1.21.10") // If there is a new Material
    static Map<String, ToolMaterial> toolMaterials = new Object2ObjectOpenHashMap<>();
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

    static {
        toolMaterials.putAll(Map.of(
                "WOOD", ToolMaterial.WOOD,
                "STONE", ToolMaterial.STONE,
                "COPPER", ToolMaterial.COPPER,
                "IRON", ToolMaterial.IRON,
                "DIAMOND", ToolMaterial.DIAMOND,
                "GOLD", ToolMaterial.GOLD,
                "NETHERITE", ToolMaterial.NETHERITE
        ));
    }

    public static void injectResources(@NotNull BootstrapContext context, DataPackGenerator dataPackGenerator) throws Exception {
        File resourceFolder = new File(context.getDataDirectory().toFile(), "resources");
        if (!resourceFolder.exists())
            FileUtils.extractDirectoryFromJar(context.getPluginSource(), "resources", resourceFolder.toPath());

        List<FileUtils.ItemConfig> configs = FileUtils.yamlFiles(resourceFolder);
        for (FileUtils.ItemConfig config : configs) {
            injectToolMaterials(dataPackGenerator, config.config());
            injectItems(context, dataPackGenerator, config.config());
            injectBlocks(context, dataPackGenerator, config.config());
        }
    }

    private static void injectToolMaterials(DataPackGenerator dataPackGenerator, @NotNull YamlConfiguration config) {
        ConfigurationSection toolMaterialsSection = config.getConfigurationSection("tool_materials");
        if (toolMaterialsSection == null)
            return;

        for (String key : toolMaterialsSection.getKeys(false)) {
            ConfigurationSection materialSection = toolMaterialsSection.getConfigurationSection(key);
            if (materialSection == null)
                continue;

            String base = materialSection.getString("base", "netherite").toUpperCase(Locale.ROOT);
            int durability = materialSection.getInt("durability", 59);
            float speed = (float) materialSection.getDouble("speed", 2.0F);
            float attackDamageBonus = (float) materialSection.getDouble("attack_damage_bonus", 0.0);
            int enchantmentValue = materialSection.getInt("enchantment_value", 15);
            String repairItem = materialSection.getString("repair_item", null);
            TagKey<Item> repairItems = TagKey.create(Registries.ITEM, ResourceLocation.parse(key.toLowerCase() + "_tool_materials"));
            dataPackGenerator.addToolMaterial(repairItems.location().getPath(), repairItem);
            ToolMaterial baseMaterial = getOrThrow(toolMaterials, base, "Invalid base material: " + base);

            toolMaterials.put(key.toUpperCase(Locale.ROOT), ItemInjector.createToolMaterial(baseMaterial, durability, speed, attackDamageBonus, enchantmentValue, repairItems));
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
                        String materialName = itemSection.getString("material", "netherite").toUpperCase(Locale.ROOT);
                        ToolMaterial material = getOrThrow(toolMaterials, materialName, "Invalid tool material: " + materialName);

                        ItemInjector.injectSword(key, attackDamage, attackSpeed, material, dataPackGenerator, components);
                    }

                    case "shovel" -> {
                        float attackDamage = (float) itemSection.getDouble("attack_damage", 1.5F);
                        float attackSpeed = (float) itemSection.getDouble("attack_speed", -3.0F);
                        String materialName = itemSection.getString("material", "netherite").toUpperCase(Locale.ROOT);
                        ToolMaterial material = getOrThrow(toolMaterials, materialName, "Invalid tool material: " + materialName);

                        ItemInjector.injectShovel(key, attackDamage, attackSpeed, material, dataPackGenerator, components);
                    }

                    case "pickaxe" -> {
                        float attackDamage = (float) itemSection.getDouble("attack_damage", 1.0F);
                        float attackSpeed = (float) itemSection.getDouble("attack_speed", -2.8F);
                        String materialName = itemSection.getString("material", "netherite").toUpperCase(Locale.ROOT);
                        ToolMaterial material = getOrThrow(toolMaterials, materialName, "Invalid tool material: " + materialName);

                        ItemInjector.injectPickaxe(key, attackDamage, attackSpeed, material, dataPackGenerator, components);
                    }

                    case "axe" -> {
                        float attackDamage = (float) itemSection.getDouble("attack_damage", 5.0F);
                        float attackSpeed = (float) itemSection.getDouble("attack_speed", -3.0F);
                        String materialName = itemSection.getString("material", "netherite").toUpperCase(Locale.ROOT);
                        ToolMaterial material = getOrThrow(toolMaterials, materialName, "Invalid tool material: " + materialName);

                        ItemInjector.injectAxe(key, attackDamage, attackSpeed, material, dataPackGenerator, components);
                    }

                    case "hoe" -> {
                        float attackSpeed = (float) itemSection.getDouble("attack_speed", 0.0F);
                        String materialName = itemSection.getString("material", "netherite").toUpperCase(Locale.ROOT);
                        ToolMaterial material = getOrThrow(toolMaterials, materialName, "Invalid tool material: " + materialName);

                        ItemInjector.injectHoe(key, attackSpeed, material, dataPackGenerator, components);
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

                if (value != null && expectedType != null && expectedType.isInstance(value)) {
                    ((SimpleComponentProperty<Object>) simpleComponentProperty).applyComponent(context, value, key, components);
                } else if (value != null && expectedType != null) {
                    context.getLogger().warn("Invalid type for component property '{}'. Expected {}, got {} for item {}", key, expectedType.getSimpleName(), value.getClass().getSimpleName(), itemSection.getName());
                }
                continue;
            }

            ConfigurationSection componentPropertySection = componentsSection.getConfigurationSection(key);

            if (componentPropertySection == null) continue;
            fillParameters(context, componentProperty, componentPropertySection);
            componentProperty.applyComponent(context, componentPropertySection, itemSection.getName(), components);
        }
    }

    private static void fillParameters(@NotNull BootstrapContext context, @NotNull ComponentProperty componentProperty, @NotNull ConfigurationSection section) {
        for (Field field : componentProperty.getClass().getDeclaredFields()) {
            Parameter parameter = field.getAnnotation(Parameter.class);
            if (parameter == null)
                continue;

            if (!section.contains(parameter.key())) {
                if (parameter.required()) {
                    context.getLogger().warn("Missing required parameters for field {} on ComponentProperty: {}", field.getName(), parameter.key());
                    break;
                }
                continue;
            }

            Object value = section.get(parameter.key(), parameter.type());

            try {
                field.setAccessible(true);
                field.set(componentProperty, value);
            } catch (Exception e) {
                context.getLogger().error("Failed to inject value for field: {}", field.getName(), e);
            }
        }
    }

    private static void injectBlocks(@NotNull BootstrapContext context, DataPackGenerator dataPackGenerator, @NotNull YamlConfiguration config) {
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
