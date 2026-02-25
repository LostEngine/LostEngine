/* eslint-disable @typescript-eslint/no-explicit-any */
import type {
    NewBlockResult,
    NewGlyphResult,
    NewItemResult,
    NewMaterialResult
} from "@/fileviewer/configeditor/formresults.ts";
import * as yaml from "yaml";
import type {Block, Glyph, Item, Material} from "@/config.ts";

const toNum = (val: any) => (isNaN(val) ? val : Number(val));

export function handleNewItem(result: NewItemResult, doc: yaml.Document, onEditConfig: () => void) {
    let node = doc.get("items");

    if (!yaml.isMap(node)) {
        node = doc.createNode({});
        doc.set("items", node);
    }

    const newItemData: Item = {
        type: result.itemIDAndType?.type,
        material: result.itemIDAndType?.type === "armor" ? result.armor?.material : result.tool?.material,
        attack_damage: toNum(result.tool?.attack_damage),
        attack_speed: toNum(result.tool?.attack_speed),
        armor_type: result.armor?.armor_type,
        elytra: {
            durability: toNum(result.elytra?.durability),
            repair_item: result.elytra?.repair_item,
            texture: result.elytra?.texture,
            use_player_skin: result.elytra?.use_player_skin,
        },
        trident: {
            durability: toNum(result.trident?.durability),
            attack_damage: toNum(result.trident?.attack_damage),
        },
        texture: result.trident?.texture,
        components: {
            enchantment_glint_override: result.components?.enchantment_glint_override,
            fire_resistant: result.components?.fire_resistant,
            food: {
                can_always_eat: result.food?.can_always_eat,
                consume_seconds: toNum(result.food?.consume_seconds),
                nutrition: toNum(result.food?.nutrition),
                saturation_modifier: toNum(result.food?.saturation_modifier),
            },
            max_damage: toNum(result.components?.max_damage),
            max_stack_size: toNum(result.components?.max_stack_size),
            rarity: result.components?.rarity,
            tooltip_display: {
                hide_tooltip: result.components?.hide_tooltip,
            },
            unbreakable: result.components?.unbreakable,
            use_cooldown: {
                cooldown_seconds: toNum(result.use_cooldown?.cooldown_seconds),
                group: result.use_cooldown?.group,
            },
        },
        icon: result.resourcePack?.icon,
        name: result.resourcePack?.name,
    };
    if (!result.elytra) delete newItemData.elytra;
    if (!result.trident) delete newItemData.trident;
    if (!result.components) delete newItemData.components;
    if (!result.food) delete newItemData.components?.food;
    if (!result.components?.hide_tooltip) delete newItemData.components?.tooltip_display;
    if (!result.use_cooldown) delete newItemData.components?.use_cooldown;

    (node as yaml.YAMLMap).set(
        result.itemIDAndType.id
            ?.trim()
            .toLowerCase()
            .replace(/[^a-z0-9\s_]/g, "")
            .replace(/\s+/g, "_"),
        doc.createNode(newItemData),
    );
    onEditConfig();
}

export function handleNewBlock(result: NewBlockResult, doc: yaml.Document, onEditConfig: () => void) {
    let node = doc.get("blocks");

    if (!yaml.isMap(node)) {
        node = doc.createNode({});
        doc.set("blocks", node);
    }

    const newData: Block = {
        type: result.blockIDAndType?.type,
        registry: result.blockIDAndType.registry,
        drops: {
            type: result.blockIDAndType?.drop_type,
            item: result.ore_drops?.item,
            max: toNum(result.ore_drops?.max),
            min: toNum(result.ore_drops?.min),
        },
        tool_type: result.regular?.tool_type,
        destroy_time: toNum(result.regular?.destroy_time),
        explosion_resistance: toNum(result.regular?.explosion_resistance),
        required_material: result.regular.required_material,
        explosion_power: toNum(result.tnt?.explosion_power),
        name: result.resourcePack?.name,
        texture: result.resourcePack?.texture,
    };
    if (!result.blockIDAndType?.drop_type) delete newData.drops;

    (node as yaml.YAMLMap).set(
        result.blockIDAndType.id
            ?.trim()
            .toLowerCase()
            .replace(/[^a-z0-9\s_]/g, "")
            .replace(/\s+/g, "_"),
        doc.createNode(newData),
    );
    onEditConfig();
}

export function handleNewMaterial(result: NewMaterialResult, doc: yaml.Document, onEditConfig: () => void) {
    let node = doc.get("materials");

    if (!yaml.isMap(node)) {
        node = doc.createNode({});
        doc.set("materials", node);
    }

    const newData: Material = {
        enchantment_value: toNum(result.materialIDAndRepairItem?.enchantment_value),
        repair_item: result.materialIDAndRepairItem?.repair_item,
        armor: {
            defense: {
                boots: toNum(result.armor?.defense_boots),
                chestplate: toNum(result.armor?.defense_chestplate),
                helmet: toNum(result.armor?.defense_helmet),
                leggings: toNum(result.armor?.defense_leggings),
            },
            durability: toNum(result.armor?.durability),
            equip_sound: result.armor?.equip_sound,
            knockback_resistance: toNum(result.armor?.knockback_resistance),
            texture: result.armor?.texture,
            toughness: toNum(result.armor?.toughness),
        },
        tool: {
            durability: toNum(result.tool?.durability),
            base: result.tool?.base,
            attack_damage_bonus: toNum(result.tool?.attack_damage_bonus),
            speed: toNum(result.tool?.speed),
        },
    };
    if (!result.armor) delete newData.armor;
    if (!result.tool) delete newData.tool;
    if (!result.armor.defense_leggings && !result.armor.defense_boots && !result.armor.defense_helmet && !result.armor.defense_chestplate)
        delete newData.armor?.defense;

    (node as yaml.YAMLMap).set(
        result.materialIDAndRepairItem.id
            ?.trim()
            .toUpperCase()
            .replace(/[^a-z0-9\s_]/g, "")
            .replace(/\s+/g, "_"),
        doc.createNode(newData),
    );
    onEditConfig();
}

export function handleNewGlyph(result: NewGlyphResult, doc: yaml.Document, onEditConfig: () => void) {
    let node = doc.get("glyphs");

    if (!yaml.isMap(node)) {
        node = doc.createNode({});
        doc.set("glyphs", node);
    }

    const newData: Glyph = {
        image_path: result.generic?.image_path,
        ascent: toNum(result.generic?.ascent),
        height: toNum(result.generic?.height),
    };

    (node as yaml.YAMLMap).set(
        result.generic.id
            ?.trim()
            .toLowerCase()
            .replace(/[^a-z0-9\s_]/g, "")
            .replace(/\s+/g, "_"),
        doc.createNode(newData),
    );
    onEditConfig();
}
