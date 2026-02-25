export type Item = {
    type?: ItemType;

    // Only for sword, shovel, pickaxe, axe, hoe, armor
    material?: string; // Ask API for enum

    // Only for sword, shovel, pickaxe, axe, hoe
    attack_damage?: number;
    attack_speed?: number;

    // Only for armor
    armor_type?: ArmorType;

    // Only for elytra
    elytra?: {
        durability?: number;
        texture?: string;
        use_player_skin?: boolean;
        repair_item?: string;
    };

    // Only for trident
    trident?: {
        durability?: number;
        attack_damage?: number;
    };

    // Components
    components?: {
        enchantment_glint_override?: boolean;
        fire_resistant?: boolean;
        food?: {
            nutrition?: number;
            saturation_modifier?: number;
            can_always_eat?: boolean;
            consume_seconds?: number;
        };
        max_damage?: number;
        max_stack_size?: number; // (1-99)
        rarity?: Rarity;
        tooltip_display?: {
            hide_tooltip?: boolean;
            hidden_components?: string[]; // Minecraft components (ex: enchantments, attribute_modifiers)
        };
        unbreakable?: boolean;
        use_cooldown?: {
            cooldown_seconds?: number;
            group?: string; // You most likely don't need this, it will be generated automatically
        };
    };

    // Resource pack generation
    model?: string; // Either model or texture
    texture?: string; // Only for trident
    icon?: string;
    name?: Record<string, string>; // locale: display name (ex: en_US:Iron Sword)
};

export const ITEM_TYPES = ["generic", "sword", "shovel", "pickaxe", "axe", "hoe", "armor", "elytra", "trident"] as const;
export type ItemType = (typeof ITEM_TYPES)[number];

export const ARMOR_TYPES = ["helmet", "chestplate", "leggings", "boots"] as const;
export type ArmorType = (typeof ARMOR_TYPES)[number];

export const RARITIES = ["COMMON", "UNCOMMON", "RARE", "EPIC"] as const;
export type Rarity = (typeof RARITIES)[number];

export type Block = {
    type?: BlockType;
    drops?: {
        type?: BlockDropType;
        // Only for ore
        item?: string; // Ask API for enum
        min?: number;
        max?: number;
    };
    registry?: BlockRegistry;

    // Only for regular
    required_material?: BlockRequiredMaterial;
    destroy_time?: number;
    explosion_resistance?: number;
    tool_type?: BlockToolType;

    // Only for tnt
    explosion_power?: number;

    // Resource pack generation
    texture?: string;
    name?: Record<string, string>; // locale: display name (ex: en_US:Note Block)
};

export const BLOCK_TYPES = ["regular", "tnt"] as const;
export type BlockType = (typeof BLOCK_TYPES)[number];

export const BLOCK_DROP_TYPES = ["self", "ore"] as const;
export type BlockDropType = (typeof BLOCK_DROP_TYPES)[number];

export const BLOCK_REGISTRIES = ["stone", "wood", "grass"] as const;
export type BlockRegistry = (typeof BLOCK_REGISTRIES)[number];

export const BLOCK_REQUIRED_MATERIALS = ["WOOD", "STONE", "IRON", "DIAMOND", "NETHERITE", "NONE"];
export type BlockRequiredMaterial = (typeof BLOCK_REQUIRED_MATERIALS)[number];

export const BLOCK_TOOL_TYPES = ["AXE", "HOE", "PICKAXE", "SHOVEL", "NONE"];
export type BlockToolType = (typeof BLOCK_TOOL_TYPES)[number];

export type Material = {
    enchantment_value?: number;
    repair_item?: string;
    tool?: {
        base?: BaseMaterial;
        durability?: number;
        speed?: number;
        attack_damage_bonus?: number;
    };
    armor?: {
        durability?: number;
        defense?: {
            boots?: number;
            leggings?: number;
            chestplate?: number;
            helmet?: number;
        };
        equip_sound?: string;
        toughness?: number;
        knockback_resistance?: number;
        texture?: string;
    };
};

export const BASE_MATERIALS = ["WOOD", "STONE", "IRON", "DIAMOND", "GOLD", "NETHERITE"] as const;
export type BaseMaterial = (typeof BASE_MATERIALS)[number];

export type Glyph = {
    image_path?: string;
    height?: number; // at most 64 for bedrock support
    ascent?: number; // smaller than height, between (height / 2 + 3 - (64 - height) / 2) and (height / 2 + 3 + (64 - height) / 2) (inclusive) for bedrock support
};

export type Config = {
    items?: Record<string, Item>;
    blocks?: Record<string, Block>;
    materials?: Record<string, Material>;
    glyphs?: Record<string, Glyph>;
};
