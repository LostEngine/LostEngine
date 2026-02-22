package dev.lost.engine.bootstrap.components;

import dev.lost.annotations.NotNull;
import dev.lost.annotations.Nullable;
import dev.lost.engine.bootstrap.components.annotations.Parameter;
import dev.lost.engine.bootstrap.components.annotations.Property;
import io.papermc.paper.plugin.bootstrap.BootstrapContext;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.component.Consumable;
import org.bukkit.configuration.ConfigurationSection;

import java.util.List;
import java.util.Map;

@SuppressWarnings({"UnstableApiUsage", "FieldMayBeFinal", "FieldCanBeLocal"})
@Property(key = "food")
public class FoodProperty implements ComponentProperty {
    @Parameter(key = "nutrition", type = Integer.class, required = true)
    private Integer nutrition = 6;

    @Nullable
    @Parameter(key = "saturation_modifier", type = Float.class)
    private Float saturationModifier = 0.6F;

    @Nullable
    @Parameter(key = "can_always_eat", type = Boolean.class)
    private Boolean canAlwaysEat = false;

    @Nullable
    @Parameter(key = "consume_seconds", type = Float.class)
    private Float consumeSeconds = 1.6F;

    @Override
    public void applyComponent(@NotNull BootstrapContext context, @NotNull ConfigurationSection itemSection, @NotNull String itemID, @NotNull Map<DataComponentType<?>, Object> components) {
        FoodProperties foodProperties = new FoodProperties(nutrition, saturationModifier != null ? saturationModifier : 0.6F, canAlwaysEat != null ? canAlwaysEat : false);

        components.put(DataComponents.FOOD, foodProperties);
        components.put(DataComponents.CONSUMABLE, new Consumable(consumeSeconds != null ? consumeSeconds : 1.6F, ItemUseAnimation.EAT, SoundEvents.GENERIC_EAT, true, List.of()));
    }
}
