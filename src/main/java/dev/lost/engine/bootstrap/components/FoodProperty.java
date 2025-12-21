package dev.lost.engine.bootstrap.components;

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

@SuppressWarnings("UnstableApiUsage")
public class FoodProperty implements ComponentProperty {
    @Override
    public void applyComponent(@NotNull BootstrapContext context, @NotNull ConfigurationSection itemSection, @NotNull Map<DataComponentType<?>, Object> components) {
        if (!itemSection.contains("food"))
            return;

        int nutrition = itemSection.getInt("food.nutrition", 6);
        float saturationModifier = (float) itemSection.getDouble("food.saturation_modifier", 0.6F);
        boolean canAlwaysEat = itemSection.getBoolean("food.can_always_eat", false);
        float consumeSeconds = (float) itemSection.getDouble("food.consumeSeconds", 1.6F);
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
