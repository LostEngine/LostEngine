package dev.lost.engine.bootstrap.components;

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
import org.jetbrains.annotations.NotNull;

import java.util.Map;

@SuppressWarnings({"UnstableApiUsage", "FieldMayBeFinal", "FieldCanBeLocal"})
@Property(key = "food")
public class FoodProperty implements ComponentProperty {
    @Parameter(key = "nutrition", type = Integer.class, required = true)
    private int nutrition = 6;

    @Parameter(key = "saturation_modifier", type = Float.class)
    private float saturationModifier = 0.6F;

    @Parameter(key = "can_always_eat", type = Boolean.class)
    private boolean canAlwaysEat = false;

    @Parameter(key = "consume_seconds", type = Float.class)
    private float consumeSeconds = 1.6F;

    @Override
    public void applyComponent(@NotNull BootstrapContext context, @NotNull ConfigurationSection itemSection, @NotNull String itemID, @NotNull Map<DataComponentType<?>, Object> components) {
        FoodProperties foodProperties = new FoodProperties(nutrition, saturationModifier, canAlwaysEat);

        components.put(DataComponents.FOOD, foodProperties);
        components.put(
                DataComponents.CONSUMABLE,
                Consumable.builder()
                        .consumeSeconds(consumeSeconds)
                        .animation(ItemUseAnimation.EAT)
                        .sound(SoundEvents.GENERIC_EAT)
                        .hasConsumeParticles(true)
                        .build()
        );
    }
}
