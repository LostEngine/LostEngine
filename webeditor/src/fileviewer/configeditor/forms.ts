import {BLOCK_TYPES, ITEM_TYPES} from "@/config.ts";

export const NEW_ITEM_FORM = [
    {
        id: "itemIDAndType",
        tabLabel: "Item ID and Type",
        title: "New Item",
        fields: [
            {name: "id", label: "Item ID", type: "text", required: true, placeholder: "my_custom_item"},
            {name: "type", label: "Type", type: "combobox", required: true, options: [...ITEM_TYPES]},
        ]
    },
    {
        id: "itemName",
        tabLabel: "Item Name",
        title: "Item Name",
        fields: [
            {name: "name", label: "Name by language", type: "name", required: true}
        ]
    }
] as const;

export const NEW_BLOCK_FORM = [
    {
        id: "blockIDAndType",
        tabLabel: "Block ID and Type",
        title: "New Block",
        fields: [
            {name: "id", label: "Block ID", type: "text", required: true, placeholder: "my_custom_block"},
            {name: "type", label: "Type", type: "combobox", required: true, options: [...BLOCK_TYPES]},
        ]
    },
    {
        id: "blockName",
        tabLabel: "Block Name",
        title: "Block Name",
        fields: [
            {name: "name", label: "Name by language", type: "name", required: true}
        ]
    }
] as const;

export const getNewMaterialForm = (items: string[], sounds?: string[]) => [
    {
        id: "materialIDAndRepairItem",
        tabLabel: "Generic",
        title: "New Material",
        fields: [
            {name: "id", label: "Material ID", type: "text", required: true, placeholder: "MY_CUSTOM_MATERIAL"},
            {name: "repair_item", label: "Repair Item", type: "text", options: items},
            {name: "enchantment_value", label: "Enchantement Value", type: "int", placeholder: "15"}
        ]
    },
    {
        id: "tool",
        tabLabel: "Tool Properties",
        title: "New Material",
        fields: [
            {
                name: "base",
                label: "Base Material",
                type: "combobox",
                options: ["WOOD", "STONE", "IRON", "DIAMOND", "GOLD", "NETHERITE"]
            },
            {name: "durability", label: "Durability", type: "int", placeholder: "2031"},
            {name: "speed", label: "Block Break Speed", type: "float", placeholder: "9.0"},
            {name: "attack_damage_bonus", label: "Attack Damage Bonus", type: "float", placeholder: "4.0"},
        ]
    },
    {
        id: "armor",
        tabLabel: "Tool Properties",
        title: "New Material",
        fields: [
            {name: "durability", label: "Durability", type: "int", placeholder: "37"},
            {name: "defense_boots", label: "Boots Defense", type: "int", placeholder: "3"},
            {name: "defense_leggings", label: "Leggings Defense", type: "int", placeholder: "6"},
            {name: "defense_chestplate", label: "Chestplate Defense", type: "int", placeholder: "8"},
            {name: "defense_helmet", label: "Helmet Defense", type: "int", placeholder: "3"},
            {
                name: "equip_sound",
                label: "Equip Sound",
                type: "text",
                placeholder: "item.armor.equip_netherite",
                options: sounds
            },
            {name: "toughness", label: "Toughness", type: "float", placeholder: "3.0"},
            {name: "knockback_resistance", label: "Knockback Resistance", type: "float", placeholder: "0.1"},
            {name: "texture", label: "Texture", type: "texture"},
        ]
    }
] as const;

export const NEW_GLYPH_FORM = [
    {
        id: "generic",
        tabLabel: "Generic",
        title: "New Glyph",
        fields: [
            {name: "id", label: "Glyph ID", type: "text", required: true, placeholder: "my_custom_glyph"},
            {name: "image_path", label: "Image Path", type: "text"},
            {name: "height", label: "Glyph Height", type: "int", placeholder: "8"},
            {name: "ascent", label: "Glyph Ascent", type: "int", placeholder: "7"}
        ]
    }
] as const;