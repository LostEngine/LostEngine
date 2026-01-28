package dev.lost.engine.geyserextension;

import dev.lost.engine.geyserextension.lomapping.Mapping;
import org.geysermc.geyser.api.event.lifecycle.GeyserDefineCustomItemsEvent;
import org.geysermc.geyser.api.item.custom.v2.CustomItemBedrockOptions;
import org.geysermc.geyser.api.item.custom.v2.NonVanillaCustomItemDefinition;
import org.geysermc.geyser.api.item.custom.v2.component.java.JavaConsumable;
import org.geysermc.geyser.api.item.custom.v2.component.java.JavaFoodProperties;
import org.geysermc.geyser.api.item.custom.v2.component.java.JavaItemDataComponents;
import org.geysermc.geyser.api.item.custom.v2.component.java.JavaToolProperties;
import org.geysermc.geyser.api.util.CreativeCategory;
import org.geysermc.geyser.api.util.Holders;
import org.geysermc.geyser.api.util.Identifier;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;
import java.util.Optional;

public class ItemGenerator {

    public static void generateFromJson(@NotNull Mapping mapping, GeyserDefineCustomItemsEvent event) {
        mapping.items().forEach(item -> {
            NonVanillaCustomItemDefinition.Builder builder = NonVanillaCustomItemDefinition.builder(Identifier.of(item.identifier()), item.javaId())
                    .translationString("item." + item.identifier().toLowerCase(Locale.ROOT).replaceAll(":", "."))
                    .component(JavaItemDataComponents.MAX_STACK_SIZE, item.stackSize())
                    .component(JavaItemDataComponents.MAX_DAMAGE, item.maxDamage())
                    .component(JavaItemDataComponents.ENCHANTABLE, item.enchantable())
                    .bedrockOptions(CustomItemBedrockOptions.builder()
                            .allowOffhand(true)
                            .creativeCategory(Optional.ofNullable(CreativeCategory.fromName(item.creativeCategory())).orElse(CreativeCategory.ITEM_COMMAND_ONLY))
                            .creativeGroup(item.creativeGroup())
                            .displayHandheld(item.isTool())
                            .icon(item.icon())
                    );
            if (item.isEatable()) builder
                    .component(JavaItemDataComponents.FOOD, JavaFoodProperties.builder().canAlwaysEat(item.isAlwaysEatable()).build())
                    .component(JavaItemDataComponents.CONSUMABLE, JavaConsumable.builder().consumeSeconds(item.consumeSeconds()).build());
            if (item.isTool()) builder
                    .component(
                            JavaItemDataComponents.TOOL, getGeyserToolProperties(item.toolProperties())
                    );
            event.register(builder.build());
        });
    }

    private static JavaToolProperties getGeyserToolProperties(dev.lost.engine.geyserextension.lomapping.items.toolproperties.@NotNull ToolProperties properties) {
        JavaToolProperties.Builder builder = JavaToolProperties.builder()
                .canDestroyBlocksInCreative(properties.canDestroyBlocksInCreative())
                .defaultMiningSpeed(properties.defaultMiningSpeed());
        for (dev.lost.engine.geyserextension.lomapping.items.toolproperties.ToolProperties.Rule rule : properties.rules()) {
            builder.rule(JavaToolProperties.Rule.builder()
                    .blocks(Holders.of(rule.blocks().stream().map(Identifier::of).toList()))
                    .speed(rule.speed())
                    .build());
        }
        return builder.build();
    }

}
