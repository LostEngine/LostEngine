package dev.lost.engine.items;

import dev.lost.engine.annotations.CanBreakOnUpdates;
import dev.lost.engine.assetsgenerators.DataPackGenerator;
import dev.lost.engine.customblocks.customblocks.CustomBlock;
import dev.lost.engine.items.customitems.*;
import dev.lost.engine.utils.ReflectionUtils;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Unit;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.BlockItemStateProperties;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.component.Weapon;
import net.minecraft.world.item.equipment.*;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import org.bukkit.Material;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.function.Function;

@SuppressWarnings("UnusedReturnValue")
@CanBreakOnUpdates(lastCheckedVersion = "1.21.11")
public class ItemInjector {

    @Contract("_, _, _, _, _, _ -> new")
    public static @NotNull ToolMaterial createToolMaterial(@NotNull ToolMaterial baseMaterial, int durability, float speed, float attackDamageBonus, int enchantmentValue, TagKey<Item> repairItems) {
        return new ToolMaterial(baseMaterial.incorrectBlocksForDrops(), durability, speed, attackDamageBonus, enchantmentValue, repairItems);
    }

    @Contract("_, _, _, _, _, _, _, _ -> new")
    public static @NotNull ArmorMaterial createArmorMaterial(int durability, Map<net.minecraft.world.item.equipment.ArmorType, Integer> defense, int enchantmentValue, String equipSound, float toughness, float knockbackResistance, TagKey<Item> repairItems, String assetId) {
        return new ArmorMaterial(
                durability,
                defense,
                enchantmentValue,
                Holder.direct(SoundEvent.createVariableRangeEvent(Identifier.parse(equipSound))),
                toughness,
                knockbackResistance,
                repairItems,
                ReflectionUtils.createEquipmentAssetId(assetId)
        );
    }

    @SuppressWarnings("unchecked")
    public static @NotNull Item injectSword(
            String name,
            float attackDamage,
            float attackSpeed,
            ToolMaterial material,
            DataPackGenerator dataPackGenerator,
            @Nullable Map<DataComponentType<?>, ?> components
    ) throws Exception {
        String fullName = "lost_engine:" + name;
        Item.Properties properties = new Item.Properties();
        if (components != null) {
            for (Map.Entry<DataComponentType<?>, ?> component : components.entrySet()) {
                properties.component((DataComponentType<Object>) component.getKey(), component.getValue());
            }
        }

        Item item = registerItem(
                fullName,
                properties.sword(material, attackDamage, attackSpeed),
                "sword"
        );
        ReflectionUtils.setItemMaterial(item.getDefaultInstance(), Material.WOODEN_SWORD);
        dataPackGenerator.addSword(fullName);
        return item;
    }

    @SuppressWarnings("unchecked")
    public static @NotNull Item injectShovel(
            String name,
            float attackDamage,
            float attackSpeed,
            ToolMaterial material,
            DataPackGenerator dataPackGenerator,
            @Nullable Map<DataComponentType<?>, ?> components
    ) throws Exception {
        String fullName = "lost_engine:" + name;
        Item.Properties properties = new Item.Properties();
        if (components != null) {
            for (Map.Entry<DataComponentType<?>, ?> component : components.entrySet()) {
                properties.component((DataComponentType<Object>) component.getKey(), component.getValue());
            }
        }

        Item item = registerItem(
                fullName,
                pr -> new CustomShovelItem(material, attackDamage, attackSpeed, pr, fullName),
                properties
        );
        ReflectionUtils.setItemMaterial(item.getDefaultInstance(), Material.WOODEN_SHOVEL);
        dataPackGenerator.addShovel(fullName);
        return item;
    }

    @SuppressWarnings("unchecked")
    public static @NotNull Item injectPickaxe(
            String name,
            float attackDamage,
            float attackSpeed,
            ToolMaterial material,
            DataPackGenerator dataPackGenerator,
            @Nullable Map<DataComponentType<?>, ?> components
    ) throws Exception {
        String fullName = "lost_engine:" + name;
        Item.Properties properties = new Item.Properties();
        if (components != null) {
            for (Map.Entry<DataComponentType<?>, ?> component : components.entrySet()) {
                properties.component((DataComponentType<Object>) component.getKey(), component.getValue());
            }
        }

        Item item = registerItem(
                fullName,
                properties.pickaxe(material, attackDamage, attackSpeed),
                "pickaxe"
        );
        ReflectionUtils.setItemMaterial(item.getDefaultInstance(), Material.WOODEN_PICKAXE);
        dataPackGenerator.addPickaxe(fullName);
        return item;
    }

    @SuppressWarnings("unchecked")
    public static @NotNull Item injectAxe(
            String name,
            float attackDamage,
            float attackSpeed,
            ToolMaterial material,
            DataPackGenerator dataPackGenerator,
            @Nullable Map<DataComponentType<?>, ?> components
    ) throws Exception {
        String fullName = "lost_engine:" + name;
        Item.Properties properties = new Item.Properties();
        if (components != null) {
            for (Map.Entry<DataComponentType<?>, ?> component : components.entrySet()) {
                properties.component((DataComponentType<Object>) component.getKey(), component.getValue());
            }
        }

        Item item = registerItem(
                fullName,
                pr -> new CustomAxeItem(material, attackDamage, attackSpeed, pr, fullName),
                properties
        );
        ReflectionUtils.setItemMaterial(item.getDefaultInstance(), Material.WOODEN_AXE);
        dataPackGenerator.addAxe(fullName);
        return item;
    }

    @SuppressWarnings("unchecked")
    public static @NotNull Item injectHoe(
            String name,
            float attackSpeed,
            ToolMaterial material,
            DataPackGenerator dataPackGenerator,
            @Nullable Map<DataComponentType<?>, ?> components
    ) throws Exception {
        String fullName = "lost_engine:" + name;
        Item.Properties properties = new Item.Properties();
        if (components != null) {
            for (Map.Entry<DataComponentType<?>, ?> component : components.entrySet()) {
                properties.component((DataComponentType<Object>) component.getKey(), component.getValue());
            }
        }

        Item item = registerItem(
                fullName,
                pr -> new CustomHoeItem(material, -material.attackDamageBonus(), attackSpeed, pr, fullName),
                properties
        );
        ReflectionUtils.setItemMaterial(item.getDefaultInstance(), Material.WOODEN_HOE);
        dataPackGenerator.addHoe(fullName);
        return item;
    }

    @SuppressWarnings("unchecked")
    public static @NotNull Item injectItem(
            String name,
            @Nullable Map<DataComponentType<?>, ?> components
    ) throws Exception {
        Item.Properties properties = new Item.Properties();
        if (components != null) {
            for (Map.Entry<DataComponentType<?>, ?> component : components.entrySet()) {
                properties.component((DataComponentType<Object>) component.getKey(), component.getValue());
            }
        }

        Item item = registerItem(
                "lost_engine:" + name,
                properties
        );
        ReflectionUtils.setItemMaterial(item.getDefaultInstance(), Material.FILLED_MAP);
        return item;
    }

    @SuppressWarnings("unchecked")
    public static @NotNull Item injectArmor(
            String name,
            ArmorMaterial armorMaterial,
            ArmorType armorType,
            DataPackGenerator dataPackGenerator,
            @Nullable Map<DataComponentType<?>, ?> components
    ) throws Exception {
        String fullName = "lost_engine:" + name;
        Item.Properties properties = new Item.Properties();
        if (components != null) {
            for (Map.Entry<DataComponentType<?>, ?> component : components.entrySet()) {
                properties.component((DataComponentType<Object>) component.getKey(), component.getValue());
            }
        }

        return switch (armorType) {
            case HELMET -> {
                dataPackGenerator.addHelmet(fullName);
                Item item = registerItem(fullName, properties.humanoidArmor(armorMaterial, net.minecraft.world.item.equipment.ArmorType.HELMET));
                ReflectionUtils.setItemMaterial(item.getDefaultInstance(), Material.IRON_HELMET);
                yield item;
            }
            case CHESTPLATE -> {
                dataPackGenerator.addChestplate(fullName);
                Item item = registerItem(fullName, properties.humanoidArmor(armorMaterial, net.minecraft.world.item.equipment.ArmorType.CHESTPLATE));
                ReflectionUtils.setItemMaterial(item.getDefaultInstance(), Material.IRON_CHESTPLATE);
                yield item;
            }
            case LEGGINGS -> {
                dataPackGenerator.addLeggings(fullName);
                Item item = registerItem(fullName, properties.humanoidArmor(armorMaterial, net.minecraft.world.item.equipment.ArmorType.LEGGINGS));
                ReflectionUtils.setItemMaterial(item.getDefaultInstance(), Material.IRON_LEGGINGS);
                yield item;
            }
            case BOOTS -> {
                dataPackGenerator.addBoots(fullName);
                Item item = registerItem(fullName, properties.humanoidArmor(armorMaterial, net.minecraft.world.item.equipment.ArmorType.BOOTS));
                ReflectionUtils.setItemMaterial(item.getDefaultInstance(), Material.IRON_BOOTS);
                yield item;
            }
        };
    }

    @SuppressWarnings("unchecked")
    public static @NotNull Item injectElytra(
            String name,
            @Nullable String repairItem,
            int durability,
            Map<DataComponentType<?>, Object> components
    ) throws Exception {
        String fullName = "lost_engine:" + name;
        Item.Properties properties = new Item.Properties();
        if (components != null) {
            for (Map.Entry<DataComponentType<?>, ?> component : components.entrySet()) {
                properties.component((DataComponentType<Object>) component.getKey(), component.getValue());
            }
        }
        if (repairItem != null) properties.repairable(BuiltInRegistries.ITEM.getValue(Identifier.parse(repairItem)));

        Item item = registerItem(
                fullName,
                properties.component(DataComponents.GLIDER, Unit.INSTANCE)
                        .component(
                                DataComponents.EQUIPPABLE,
                                Equippable.builder(EquipmentSlot.CHEST)
                                        .setEquipSound(SoundEvents.ARMOR_EQUIP_ELYTRA)
                                        .setAsset(ReflectionUtils.createEquipmentAssetId(name))
                                        .setDamageOnHurt(false)
                                        .build()
                        )
                        .durability(durability)
        );
        ReflectionUtils.setItemMaterial(item.getDefaultInstance(), Material.ELYTRA);
        return item;
    }

    @SuppressWarnings("unchecked")
    public static @NotNull Item injectTrident(
            String name,
            int durability,
            float attackDamage,
            DataPackGenerator dataPackGenerator,
            @Nullable Map<DataComponentType<?>, ?> components
    ) throws Exception {
        String fullName = "lost_engine:" + name;
        Item.Properties properties = new Item.Properties();
        if (components != null) {
            for (Map.Entry<DataComponentType<?>, ?> component : components.entrySet()) {
                properties.component((DataComponentType<Object>) component.getKey(), component.getValue());
            }
        }

        Item item = registerItem(
                fullName,
                pr -> new CustomTridentItem(pr, fullName),
                new Item.Properties()
                        .durability(durability)
                        .attributes(
                                ItemAttributeModifiers.builder()
                                        .add(
                                                Attributes.ATTACK_DAMAGE,
                                                new AttributeModifier(
                                                        Item.BASE_ATTACK_DAMAGE_ID,
                                                        attackDamage,
                                                        AttributeModifier.Operation.ADD_VALUE
                                                ),
                                                EquipmentSlotGroup.MAINHAND
                                        )
                                        .add(
                                                Attributes.ATTACK_SPEED,
                                                new AttributeModifier(
                                                        Item.BASE_ATTACK_SPEED_ID,
                                                        -2.9F,
                                                        AttributeModifier.Operation.ADD_VALUE
                                                ),
                                                EquipmentSlotGroup.MAINHAND
                                        )
                                        .build()
                        )
                        .component(DataComponents.TOOL, TridentItem.createToolProperties())
                        .enchantable(1)
                        .component(DataComponents.WEAPON, new Weapon(1))
        );

        ReflectionUtils.setItemMaterial(item.getDefaultInstance(), Material.TRIDENT);
        dataPackGenerator.addTrident(fullName);
        return item;
    }

    public enum ArmorType {
        HELMET,
        CHESTPLATE,
        LEGGINGS,
        BOOTS
    }

    public static @NotNull Map<String, String> blockStateToPropertyMap(@NotNull BlockState blockState) {
        Map<String, String> map = new Object2ObjectOpenHashMap<>();
        for (Property<?> property : blockState.getProperties()) {
            Object value = blockState.getValue(property);
            map.put(property.getName(), value.toString());
        }
        return map;
    }

    public static @NotNull Item injectBlockItem(String name, Block customBlock) {
        return injectBlockItem(name, customBlock, null);
    }

    @SuppressWarnings("unchecked")
    public static @NotNull Item injectBlockItem(String name, Block customBlock, @Nullable Map<DataComponentType<?>, ?> components) {
        ItemStack dynamicMaterial;
        if (customBlock instanceof CustomBlock) {
            BlockState clientBlockState = ((CustomBlock) customBlock).getClientBlockState();
            ItemStack itemStack = clientBlockState.getBlock().asItem().getDefaultInstance().copy();
            Map<String, String> properties = blockStateToPropertyMap(clientBlockState);
            itemStack.set(DataComponents.BLOCK_STATE, new BlockItemStateProperties(properties));
            dynamicMaterial = itemStack;
        } else {
            dynamicMaterial = Items.BARRIER.getDefaultInstance();
        }
        String fullName = "lost_engine:" + name;
        Item.Properties properties = new Item.Properties();
        if (components != null) {
            for (Map.Entry<DataComponentType<?>, ?> component : components.entrySet()) {
                properties.component((DataComponentType<Object>) component.getKey(), component.getValue());
            }
        }

        return registerItem(
                vanillaItemId(fullName),
                pr -> new CustomBlockItem(customBlock, pr, dynamicMaterial, fullName),
                properties
        );
    }

    private static @NotNull ResourceKey<Item> vanillaItemId(String id) {
        return ResourceKey.create(Registries.ITEM, Identifier.parse(id));
    }

    public static @NotNull Item registerItem(String id, Item.Properties properties) {
        return registerItem(vanillaItemId(id), props -> new GenericCustomItem(props, id), properties);
    }

    public static @NotNull Item registerItem(String id, Item.Properties properties, String toolType) {
        return registerItem(vanillaItemId(id), props -> new GenericCustomItem(props, id, toolType), properties);
    }

    public static @NotNull Item registerItem(String id, Function<Item.Properties, Item> factory, Item.Properties properties) {
        return registerItem(vanillaItemId(id), factory, properties);
    }

    public static @NotNull Item registerItem(ResourceKey<Item> key, @NotNull Function<Item.Properties, Item> factory, Item.@NotNull Properties properties) {
        Item item = factory.apply(properties.setId(key));
        if (item instanceof BlockItem blockItem) {
            blockItem.registerBlocks(Item.BY_BLOCK, item);
        }

        return Registry.register(BuiltInRegistries.ITEM, key, item);
    }

}
