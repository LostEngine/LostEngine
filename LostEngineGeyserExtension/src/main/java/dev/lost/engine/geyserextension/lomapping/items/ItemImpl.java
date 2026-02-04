package dev.lost.engine.geyserextension.lomapping.items;

import dev.lost.engine.geyserextension.lomapping.items.toolproperties.ToolPropertiesImpl;

public record ItemImpl(
        int javaId,
        String identifier,
        String icon,
        int stackSize,
        int maxDamage,
        ToolPropertiesImpl toolProperties,
        int enchantable,
        boolean isEatable,
        boolean isAlwaysEatable,
        float consumeSeconds,
        boolean isTool,
        String creativeCategory,
        String creativeGroup,
        String equitableSlot
) implements Item {
}

