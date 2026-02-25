import {
    ARMOR_TYPES,
    BASE_MATERIALS,
    BLOCK_DROP_TYPES,
    BLOCK_REGISTRIES,
    BLOCK_REQUIRED_MATERIALS,
    BLOCK_TOOL_TYPES,
    BLOCK_TYPES,
    ITEM_TYPES,
    RARITIES,
} from "@/config.ts";
import type {FormTab} from "@/fileviewer/configeditor/configeditor.tsx";

export const getNewItemForm = (toolMaterials: string[], armorMaterials: string[], items: string[]) =>
    [
        {
            id: "itemIDAndType",
            tabLabel: "Item ID and Type",
            title: "New Item",
            fields: [
                {
                    name: "id",
                    label: "Item ID",
                    type: "text",
                    required: true,
                    placeholder: "my_custom_item",
                },
                {
                    name: "type",
                    label: "Type",
                    type: "combobox",
                    required: true,
                    options: [...ITEM_TYPES],
                },
            ],
        },
        {
            id: "tool",
            tabLabel: "Tool",
            title: "Tool Properties",
            condition:
                'itemIDAndType?.type === "sword" || itemIDAndType?.type === "shovel" || itemIDAndType?.type === "pickaxe" || itemIDAndType?.type === "axe" || itemIDAndType?.type === "hoe"',
            fields: [
                {
                    name: "material",
                    label: "Material",
                    type: "text",
                    placeholder: "NETHERITE",
                    required: true,
                    options: toolMaterials,
                },
                {
                    name: "attack_damage",
                    label: "Attack Damage",
                    type: "float",
                    placeholder: "sword: 3.0, shovel: 1.5, pickaxe: 1.0, axe: 5.0, hoe: 0.0",
                },
                {
                    name: "attack_speed",
                    label: "Attack Speed",
                    type: "float",
                    placeholder: "sword: -2.4, shovel: -3.0, pickaxe: -2.8, axe: -3.0, hoe: 0.0",
                },
            ],
        },
        {
            id: "armor",
            tabLabel: "Armor",
            title: "Armor Properties",
            condition: 'itemIDAndType?.type === "armor"',
            fields: [
                {
                    name: "material",
                    label: "Material",
                    type: "text",
                    placeholder: "NETHERITE",
                    required: true,
                    options: armorMaterials,
                },
                {
                    name: "armor_type",
                    label: "Armor Type",
                    type: "combobox",
                    options: [...ARMOR_TYPES],
                },
            ],
        },
        {
            id: "elytra",
            tabLabel: "Elytra",
            title: "Elytra Properties",
            condition: 'itemIDAndType?.type === "elytra"',
            fields: [
                {
                    name: "durability",
                    label: "Durability",
                    type: "int",
                    placeholder: "432",
                },
                {
                    name: "texture",
                    label: "Elytra Texture",
                    type: "texture",
                    textureFolders: ["entity/equipment/wings/"],
                },
                {
                    name: "use_player_skin",
                    label: "Use Player Skin",
                    type: "boolean",
                },
                {
                    name: "repair_item",
                    label: "Repair Item",
                    type: "text",
                    options: items,
                },
            ],
        },
        {
            id: "trident",
            tabLabel: "Trident",
            title: "Trident Properties",
            condition: 'itemIDAndType?.type === "trident"',
            fields: [
                {
                    name: "durability",
                    label: "Durability",
                    type: "int",
                    placeholder: "250",
                },
                {
                    name: "texture",
                    label: "Elytra Texture",
                    type: "texture",
                    textureFolders: ["item/"],
                },
                {
                    name: "attack_damage",
                    label: "Attack Damage",
                    type: "float",
                    placeholder: "8.0",
                },
            ],
        },
        {
            id: "components",
            tabLabel: "Components",
            title: "Custom Components",
            fields: [
                {
                    name: "enchantment_glint_override",
                    label: "Enchantment Glint Override",
                    type: "boolean",
                },
                {
                    name: "fire_resistant",
                    label: "Fire Resistant",
                    type: "boolean",
                },
                {name: "food", label: "Food", type: "boolean"},
                {
                    name: "max_damage",
                    label: "Maximum Damage/Durability",
                    type: "int",
                },
                {
                    name: "max_stack_size",
                    label: "Max Stack Size",
                    type: "int",
                    placeholder: "64 (1-99)",
                },
                {
                    name: "rarity",
                    label: "Rarity",
                    type: "combobox",
                    options: [...RARITIES],
                },
                {
                    name: "hide_tooltip",
                    label: "Hide Tooltip",
                    type: "boolean",
                },
                {name: "unbreakable", label: "Unbreakable", type: "boolean"},
                {
                    name: "use_cooldown",
                    label: "Use Cooldown",
                    type: "boolean",
                },
            ],
        },
        {
            id: "food",
            tabLabel: "Food",
            title: "Food Properties",
            condition: "components?.food === true",
            fields: [
                {
                    name: "nutrition",
                    label: "Nutrition",
                    type: "int",
                    placeholder: "6",
                },
                {
                    name: "saturation_modifier",
                    label: "Saturation Modifier",
                    type: "float",
                    placeholder: "0.6",
                },
                {
                    name: "can_always_eat",
                    label: "Can Always Eat",
                    type: "boolean",
                },
                {
                    name: "consume_seconds",
                    label: "Consume Seconds",
                    type: "float",
                    placeholder: "1.6",
                },
            ],
        },
        {
            id: "use_cooldown",
            tabLabel: "Use Cooldown",
            title: "Use Cooldown Properties",
            condition: "components?.use_cooldown === true",
            fields: [
                {
                    name: "cooldown_seconds",
                    label: "Cooldown Seconds",
                    type: "float",
                    placeholder: "4.0",
                },
                {
                    name: "group",
                    label: "Group",
                    type: "text",
                    placeholder: "You most likely don't need to edit this, it will be generated automatically.",
                },
            ],
        },
        {
            id: "resourcePack",
            tabLabel: "Resource Pack",
            title: "Resource Pack Properties",
            fields: [
                {
                    name: "icon",
                    label: "Icon",
                    type: "texture",
                    required: true,
                    textureFolders: ["item/"],
                },
                {
                    name: "name",
                    label: "Name by language",
                    type: "name",
                    required: true,
                },
            ],
        },
    ] as const satisfies FormTab[];

export const getNewBlockForm = (items: string[]) =>
    [
        {
            id: "blockIDAndType",
            tabLabel: "Block ID and Type",
            title: "New Block",
            fields: [
                {
                    name: "id",
                    label: "Block ID",
                    type: "text",
                    required: true,
                    placeholder: "my_custom_block",
                },
                {
                    name: "type",
                    label: "Type",
                    type: "combobox",
                    required: true,
                    options: [...BLOCK_TYPES],
                },
                {
                    name: "registry",
                    label: "Block Registry",
                    type: "combobox",
                    required: true,
                    options: [...BLOCK_REGISTRIES],
                },
                {
                    name: "drop_type",
                    label: "Drop Type",
                    type: "combobox",
                    options: [...BLOCK_DROP_TYPES],
                },
            ],
        },
        {
            id: "regular",
            tabLabel: "Regular",
            title: "Regular Block Properties",
            condition: 'blockIDAndType?.type === "regular"',
            fields: [
                {
                    name: "required_material",
                    label: "Required Material",
                    type: "combobox",
                    options: [...BLOCK_REQUIRED_MATERIALS],
                },
                {
                    name: "destroy_time",
                    label: "Destroy Time",
                    type: "float",
                    placeholder: "1.0",
                },
                {
                    name: "explosion_resistance",
                    label: "Explosion Resistance",
                    type: "float",
                    placeholder: "1.0",
                },
                {
                    name: "tool_type",
                    label: "Tool Type",
                    type: "combobox",
                    options: [...BLOCK_TOOL_TYPES],
                },
            ],
        },
        {
            id: "tnt",
            tabLabel: "Tnt",
            title: "Tnt Block Properties",
            condition: 'blockIDAndType?.type === "tnt"',
            fields: [
                {
                    name: "explosion_power",
                    label: "Explosion Power",
                    type: "int",
                    placeholder: "4",
                },
            ],
        },
        {
            id: "ore_drops",
            tabLabel: "Drops",
            title: "Block Drops Properties",
            condition: 'blockIDAndType?.drop_type === "ore"',
            fields: [
                {
                    name: "item",
                    label: "Drop Item",
                    type: "text",
                    options: items,
                },
                {
                    name: "max",
                    label: "Maximum",
                    type: "int",
                    placeholder: "1",
                },
                {
                    name: "min",
                    label: "Minimum",
                    type: "int",
                    placeholder: "1",
                },
            ],
        },
        {
            id: "resourcePack",
            tabLabel: "Resource Pack",
            title: "Resource Pack Properties",
            fields: [
                {
                    name: "texture",
                    label: "Block Texture",
                    type: "texture",
                    textureFolders: ["block/"],
                    required: true,
                },
                {
                    name: "name",
                    label: "Name by language",
                    type: "name",
                    required: true,
                },
            ],
        },
    ] as const satisfies FormTab[];

export const getNewMaterialForm = (items: string[], sounds?: string[]) =>
    [
        {
            id: "materialIDAndRepairItem",
            tabLabel: "Generic",
            title: "New Material",
            fields: [
                {
                    name: "id",
                    label: "Material ID",
                    type: "text",
                    required: true,
                    placeholder: "MY_CUSTOM_MATERIAL",
                },
                {
                    name: "repair_item",
                    label: "Repair Item",
                    type: "text",
                    options: items,
                },
                {
                    name: "enchantment_value",
                    label: "Enchantement Value",
                    type: "int",
                    placeholder: "15",
                },
            ],
        },
        {
            id: "tool",
            tabLabel: "Tool Properties",
            title: "Material Tool Properties",
            fields: [
                {
                    name: "base",
                    label: "Base Material",
                    type: "combobox",
                    options: [...BASE_MATERIALS],
                },
                {
                    name: "durability",
                    label: "Durability",
                    type: "int",
                    placeholder: "2031",
                },
                {
                    name: "speed",
                    label: "Block Break Speed",
                    type: "float",
                    placeholder: "9.0",
                },
                {
                    name: "attack_damage_bonus",
                    label: "Attack Damage Bonus",
                    type: "float",
                    placeholder: "4.0",
                },
            ],
        },
        {
            id: "armor",
            tabLabel: "Armor Properties",
            title: "Material Armor Properties",
            fields: [
                {
                    name: "durability",
                    label: "Durability",
                    type: "int",
                    placeholder: "37",
                },
                {
                    name: "defense_boots",
                    label: "Boots Defense",
                    type: "int",
                    placeholder: "3",
                },
                {
                    name: "defense_leggings",
                    label: "Leggings Defense",
                    type: "int",
                    placeholder: "6",
                },
                {
                    name: "defense_chestplate",
                    label: "Chestplate Defense",
                    type: "int",
                    placeholder: "8",
                },
                {
                    name: "defense_helmet",
                    label: "Helmet Defense",
                    type: "int",
                    placeholder: "3",
                },
                {
                    name: "equip_sound",
                    label: "Equip Sound",
                    type: "text",
                    placeholder: "item.armor.equip_netherite",
                    options: sounds,
                },
                {
                    name: "toughness",
                    label: "Toughness",
                    type: "float",
                    placeholder: "3.0",
                },
                {
                    name: "knockback_resistance",
                    label: "Knockback Resistance",
                    type: "float",
                    placeholder: "0.1",
                },
                {
                    name: "texture",
                    label: "Texture",
                    type: "texture",
                    textureFolders: ["entity/equipment/humanoid/", "entity/equipment/humanoid_leggings/"],
                },
            ],
        },
    ] as const satisfies FormTab[];

export const NEW_GLYPH_FORM = [
    {
        id: "generic",
        tabLabel: "Generic",
        title: "New Glyph",
        fields: [
            {
                name: "id",
                label: "Glyph ID",
                type: "text",
                required: true,
                placeholder: "my_custom_glyph",
            },
            {
                name: "image_path",
                label: "Image Path",
                type: "texture",
                textureFolders: ["font/"],
            },
            {
                name: "height",
                label: "Glyph Height",
                type: "int",
                placeholder: "8",
            },
            {
                name: "ascent",
                label: "Glyph Ascent",
                type: "int",
                placeholder: "7",
            },
        ],
    },
] as const satisfies FormTab[];
