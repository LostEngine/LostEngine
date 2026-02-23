/* eslint-disable @typescript-eslint/no-explicit-any */
import type {
    NewBlockResult,
    NewGlyphResult,
    NewItemResult,
    NewMaterialResult
} from "@/fileviewer/configeditor/formresults.ts";
import * as yaml from "yaml";
import type {Block, Glyph, Item, Material} from "@/config.ts";

export function handleNewItem(
    result: NewItemResult,
    doc: yaml.Document,
    onEditConfig: () => void
) {
    let node = doc.get("items");

    if (!yaml.isMap(node)) {
        node = doc.createNode({});
        doc.set("items", node);
    }

    const newItemData: Item = {
        type: result.itemIDAndType?.type,
        name: result.itemName?.name
    };

    (node as yaml.YAMLMap).set(
        result.itemIDAndType.id?.trim()
            .toLowerCase()
            .replace(/[^a-z0-9\s]/g, "")
            .replace(/\s+/g, "_"),
        doc.createNode(newItemData)
    );
    onEditConfig();
}

export function handleNewBlock(
    result: NewBlockResult,
    doc: yaml.Document,
    onEditConfig: () => void
) {
    let node = doc.get("blocks");

    if (!yaml.isMap(node)) {
        node = doc.createNode({});
        doc.set("blocks", node);
    }

    const newData: Block = {
        type: result.blockIDAndType?.type,
        name: result.blockName?.name
    };

    (node as yaml.YAMLMap).set(
        result.blockIDAndType.id?.trim()
            .toLowerCase()
            .replace(/[^a-z0-9\s]/g, "")
            .replace(/\s+/g, "_"),
        doc.createNode(newData)
    );
    onEditConfig();
}

export function handleNewMaterial(
    result: NewMaterialResult,
    doc: yaml.Document,
    onEditConfig: () => void
) {
    let node = doc.get("materials");

    if (!yaml.isMap(node)) {
        node = doc.createNode({});
        doc.set("materials", node);
    }

    const toNum = (val: any) => (isNaN(val) ? val : Number(val));

    const newData: Material = {
        enchantment_value: toNum(result.materialIDAndRepairItem?.enchantment_value),
        repair_item: result.materialIDAndRepairItem?.repair_item,
        armor: {
            defense: {
                boots: toNum(result.armor?.defense_boots),
                chestplate: toNum(result.armor?.defense_chestplate),
                helmet: toNum(result.armor?.defense_helmet),
                leggings: toNum(result.armor?.defense_leggings)
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
            speed: toNum(result.tool?.speed)
        }
    };
    if (!result.armor) delete newData.armor;
    if (!result.tool) delete newData.tool;

    (node as yaml.YAMLMap).set(
        result.materialIDAndRepairItem.id?.trim()
            .toUpperCase()
            .replace(/[^A-Z0-9\s]/g, "")
            .replace(/\s+/g, "_"),
        doc.createNode(newData)
    );
    onEditConfig();
}

export function handleNewGlyph(
    result: NewGlyphResult,
    doc: yaml.Document,
    onEditConfig: () => void
) {
    let node = doc.get("glyphs");

    if (!yaml.isMap(node)) {
        node = doc.createNode({});
        doc.set("glyphs", node);
    }

    const newData: Glyph = {
        image_path: result.generic.image_path,
        ascent: result.generic.ascent,
        height: result.generic.height,
    };

    (node as yaml.YAMLMap).set(
        result.generic.id?.trim()
            .toLowerCase()
            .replace(/[^a-z0-9\s]/g, "")
            .replace(/\s+/g, "_"),
        doc.createNode(newData)
    );
    onEditConfig();
}