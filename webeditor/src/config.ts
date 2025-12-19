export type Item = {
    name?: Record<string, string>;
    type?: string;
    texture?: string;
    material?: string;
    tool_type?: string;
    destroy_time?: number;
    explosion_resistance?: number;
    drops?: {
        type?: string;
        item?: string;
        min?: number;
        max?: number;
    }
    required_material?: string;
}

export type Material = {
    base?: string;
    durability?: number;
    speed?: number;
    attack_damage_bonus?: number;
    enchantment_value?: number;
    repair_item?: string;
}

export type Config = {
    items?: Record<string, Item>;
    tool_materials?: Record<string, Material>;
}