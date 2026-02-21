package dev.lost.engine.geyserextension.lomapping.items;


import dev.lost.engine.geyserextension.lomapping.items.toolproperties.ToolProperties;

public interface Item {

    int javaId();

    String identifier();

    String icon();

    int stackSize();

    int maxDamage();

    ToolProperties toolProperties();

    int enchantable();

    boolean isEatable();

    boolean isAlwaysEatable();

    float consumeSeconds();

    boolean isTool();

    String creativeCategory();

    String creativeGroup();

    String equitableSlot();

}
