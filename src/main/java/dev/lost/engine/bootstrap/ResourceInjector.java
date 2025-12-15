package dev.lost.engine.bootstrap;

import dev.lost.engine.assetsgenerators.DataPackGenerator;
import dev.lost.engine.bootstrap.components.ComponentProperty;
import dev.lost.engine.bootstrap.components.EnchantableProperty;
import dev.lost.engine.bootstrap.components.GlowingProperty;
import dev.lost.engine.customblocks.BlockInjector;
import dev.lost.engine.customblocks.BlockStateProvider;
import dev.lost.engine.items.ItemInjector;
import dev.lost.engine.utils.EnumUtils;
import dev.lost.engine.utils.FileUtils;
import io.papermc.paper.plugin.bootstrap.BootstrapContext;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectList;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Unit;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.ToolMaterial;
import net.minecraft.world.item.component.Consumables;
import net.minecraft.world.item.component.DamageResistant;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.component.UseCooldown;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.stream.Stream;

@SuppressWarnings("UnstableApiUsage")
public class ResourceInjector {

    static Map<String, ToolMaterial> toolMaterials = new Object2ObjectOpenHashMap<>();
    static ObjectList<ComponentProperty> propertyClassInstances = new ObjectArrayList<>();

    static {
        toolMaterials.putAll(Map.of(
                "WOOD", ToolMaterial.WOOD,
                "STONE", ToolMaterial.STONE,
                "IRON", ToolMaterial.IRON,
                "DIAMOND", ToolMaterial.DIAMOND,
                "GOLD", ToolMaterial.GOLD,
                "NETHERITE", ToolMaterial.NETHERITE
        ));

        Stream<Class<? extends ComponentProperty>> propertyClasses = Stream.of(
                EnchantableProperty.class,
                GlowingProperty.class
        );

        List<ComponentProperty> instances = propertyClasses
                .map(clazz -> {
                    try {
                        return (ComponentProperty) clazz.getDeclaredConstructor().newInstance();
                    } catch (InstantiationException | IllegalAccessException | InvocationTargetException |
                             NoSuchMethodException e) {
                        throw new RuntimeException("Failed to instantiate component property: " + clazz.getSimpleName(), e);
                    }
                })
                .toList();

        propertyClassInstances.addAll(instances);
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

    private static void applyComponents(@NotNull BootstrapContext context, ConfigurationSection itemSection, Map<DataComponentType<?>, Object> components) {
        for (ComponentProperty property : propertyClassInstances) {
            property.applyComponent(context, itemSection, components);
        }

        if (itemSection.contains("food")) {
            int nutrition = itemSection.getInt("food.nutrition", 6);
            float saturationModifier = (float) itemSection.getDouble("food.saturation_modifier", 0.6F);
            boolean canAlwaysEat = itemSection.getBoolean("food.can_always_eat", false);
            FoodProperties foodProperties = new FoodProperties(nutrition, saturationModifier, canAlwaysEat);
            components.put(DataComponents.FOOD, foodProperties);
            components.put(DataComponents.CONSUMABLE, Consumables.DEFAULT_FOOD);
        }

        if (itemSection.getBoolean("fire_resistant", false)) {
            components.put(DataComponents.DAMAGE_RESISTANT, new DamageResistant(DamageTypeTags.IS_FIRE));
        }

        if (itemSection.contains("max_durability")) {
            int maxDurability = itemSection.getInt("max_durability");
            components.put(DataComponents.MAX_DAMAGE, maxDurability);
        }

        if (itemSection.contains("max_stack_size")) {
            int maxStackSize = itemSection.getInt("max_stack_size");
            components.put(DataComponents.MAX_STACK_SIZE, maxStackSize);
        }

        if (itemSection.getBoolean("unbreakable", false)) {
            components.put(DataComponents.UNBREAKABLE, Unit.INSTANCE);
        }

        if (itemSection.contains("rarity")) {
            String rarityString = itemSection.getString("rarity");
            Optional<Rarity> rarity = EnumUtils.match(rarityString, Rarity.class);
            if (rarity.isPresent()) {
                components.put(DataComponents.RARITY, rarity.get());
            } else {
                context.getLogger().warn("Message TODO");
            }
        }

        if (itemSection.contains("use_cooldown")) {
            float useCooldown = (float) itemSection.getDouble("use_cooldown.use_cooldown", 0.0F);

            String groupString = itemSection.getString("use_cooldown.group");
            Optional<ResourceLocation> group = groupString == null ? Optional.empty() : Optional.of(ResourceLocation.parse(groupString));

            components.put(DataComponents.USE_COOLDOWN, new UseCooldown(useCooldown, group));
        }

        boolean hideTooltip = itemSection.getBoolean("hide_tooltip", false);
        if (hideTooltip) {
            components.put(DataComponents.TOOLTIP_DISPLAY, new TooltipDisplay(hideTooltip, LinkedHashSet.newLinkedHashSet(0)));
        }

        if (itemSection.contains("tooltip_display") && !hideTooltip) {
            List<String> tooltipList = itemSection.getStringList("tooltip_display");
            SequencedSet<DataComponentType<?>> tooltipTypes = new LinkedHashSet<>();
            for (String s : tooltipList) {
                try {
                    ResourceLocation id = ResourceLocation.parse(s);
                    Holder.Reference<DataComponentType<?>> typeReference = BuiltInRegistries.DATA_COMPONENT_TYPE.get(id).orElse(null);
                    if (typeReference == null) {
                        context.getLogger().warn("Unknown data component: {} for item", s);
                        continue;
                    }

                    tooltipTypes.add(typeReference.value());
                } catch (Exception e) {
                    context.getLogger().warn("Invalid data component id: {} for item", s);
                }
            }

            components.put(DataComponents.TOOLTIP_DISPLAY, new TooltipDisplay(false, tooltipTypes));
        }
    }

    private static void injectBlocks(@NotNull BootstrapContext context, DataPackGenerator dataPackGenerator, @NotNull YamlConfiguration config) {
        ConfigurationSection blocksSection = config.getConfigurationSection("blocks");
        if (blocksSection == null) return;

        for (String key : blocksSection.getKeys(false)) {
            ConfigurationSection blockSection = blocksSection.getConfigurationSection(key);
            if (blockSection == null) continue;
            String requiredMaterial = blockSection.getString("required_material", "NONE").toUpperCase();
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
                            BlockInjector.Minable.valueOf(blockSection.getString("tool_type", "none").toUpperCase())
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
