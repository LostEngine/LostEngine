package dev.lost.engine.items;

import dev.lost.engine.annotations.CanBreakOnUpdates;
import dev.lost.engine.bootstrap.LostEngineBootstrap;
import dev.lost.engine.customblocks.customblocks.CustomBlock;
import dev.lost.engine.items.customitems.*;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
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
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.equipment.EquipmentAssets;
import net.minecraft.world.item.equipment.Equippable;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
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
                ResourceKey.create(EquipmentAssets.ROOT_ID, Identifier.fromNamespaceAndPath("lost_engine", assetId))
        );
    }

    @SuppressWarnings("unchecked")
    public static @NotNull Item injectSword(
            String name,
            float attackDamage,
            float attackSpeed,
            ToolMaterial material,
            @Nullable Map<DataComponentType<?>, ?> components
    ) {
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
        LostEngineBootstrap.materialManager.setMaterial(item, "WOODEN_SWORD");
        LostEngineBootstrap.dataPackGenerator.addSword(fullName);
        return item;
    }

    @SuppressWarnings("unchecked")
    public static @NotNull Item injectShovel(
            String name,
            float attackDamage,
            float attackSpeed,
            ToolMaterial material,
            @Nullable Map<DataComponentType<?>, ?> components
    ) {
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
        LostEngineBootstrap.materialManager.setMaterial(item, "WOODEN_SHOVEL");
        LostEngineBootstrap.dataPackGenerator.addShovel(fullName);
        return item;
    }

    @SuppressWarnings("unchecked")
    public static @NotNull Item injectPickaxe(
            String name,
            float attackDamage,
            float attackSpeed,
            ToolMaterial material,
            @Nullable Map<DataComponentType<?>, ?> components
    ) {
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
        LostEngineBootstrap.materialManager.setMaterial(item, "WOODEN_PICKAXE");
        LostEngineBootstrap.dataPackGenerator.addPickaxe(fullName);
        return item;
    }

    @SuppressWarnings("unchecked")
    public static @NotNull Item injectAxe(
            String name,
            float attackDamage,
            float attackSpeed,
            ToolMaterial material,
            @Nullable Map<DataComponentType<?>, ?> components
    ) {
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
        LostEngineBootstrap.materialManager.setMaterial(item, "WOODEN_AXE");
        LostEngineBootstrap.dataPackGenerator.addAxe(fullName);
        return item;
    }

    @SuppressWarnings("unchecked")
    public static @NotNull Item injectHoe(
            String name,
            float attackSpeed,
            ToolMaterial material,
            @Nullable Map<DataComponentType<?>, ?> components
    ) {
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
        LostEngineBootstrap.materialManager.setMaterial(item, "WOODEN_HOE");
        LostEngineBootstrap.dataPackGenerator.addHoe(fullName);
        return item;
    }

    @SuppressWarnings("unchecked")
    public static @NotNull Item injectItem(
            String name,
            @Nullable Map<DataComponentType<?>, ?> components
    ) {
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
        LostEngineBootstrap.materialManager.setMaterial(item, "FILLED_MAP");
        return item;
    }

    @SuppressWarnings("unchecked")
    public static @NotNull Item injectArmor(
            String name,
            ArmorMaterial armorMaterial,
            ArmorType armorType,
            @Nullable Map<DataComponentType<?>, ?> components
    ) {
        String fullName = "lost_engine:" + name;
        Item.Properties properties = new Item.Properties();
        if (components != null) {
            for (Map.Entry<DataComponentType<?>, ?> component : components.entrySet()) {
                properties.component((DataComponentType<Object>) component.getKey(), component.getValue());
            }
        }

        return switch (armorType) {
            case HELMET -> {
                LostEngineBootstrap.dataPackGenerator.addHelmet(fullName);
                Item item = registerItem(fullName, properties.humanoidArmor(armorMaterial, net.minecraft.world.item.equipment.ArmorType.HELMET));
                LostEngineBootstrap.materialManager.setMaterial(item, "IRON_HELMET");
                yield item;
            }
            case CHESTPLATE -> {
                LostEngineBootstrap.dataPackGenerator.addChestplate(fullName);
                Item item = registerItem(fullName, properties.humanoidArmor(armorMaterial, net.minecraft.world.item.equipment.ArmorType.CHESTPLATE));
                LostEngineBootstrap.materialManager.setMaterial(item, "IRON_CHESTPLATE");
                yield item;
            }
            case LEGGINGS -> {
                LostEngineBootstrap.dataPackGenerator.addLeggings(fullName);
                Item item = registerItem(fullName, properties.humanoidArmor(armorMaterial, net.minecraft.world.item.equipment.ArmorType.LEGGINGS));
                LostEngineBootstrap.materialManager.setMaterial(item, "IRON_LEGGINGS");
                yield item;
            }
            case BOOTS -> {
                LostEngineBootstrap.dataPackGenerator.addBoots(fullName);
                Item item = registerItem(fullName, properties.humanoidArmor(armorMaterial, net.minecraft.world.item.equipment.ArmorType.BOOTS));
                LostEngineBootstrap.materialManager.setMaterial(item, "IRON_BOOTS");
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
    ) {
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
                                        .setAsset(ResourceKey.create(EquipmentAssets.ROOT_ID, Identifier.fromNamespaceAndPath("lost_engine", name)))
                                        .setDamageOnHurt(false)
                                        .build()
                        )
                        .durability(durability)
        );
        LostEngineBootstrap.materialManager.setMaterial(item, "ELYTRA");
        return item;
    }

    @SuppressWarnings("unchecked")
    public static @NotNull Item injectTrident(
            String name,
            int durability,
            float attackDamage,
            @Nullable Map<DataComponentType<?>, ?> components
    ) {
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

        LostEngineBootstrap.materialManager.setMaterial(item, "TRIDENT");
        LostEngineBootstrap.dataPackGenerator.addTrident(fullName);
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
