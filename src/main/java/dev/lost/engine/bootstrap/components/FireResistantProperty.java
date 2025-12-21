package dev.lost.engine.bootstrap.components;

import io.papermc.paper.plugin.bootstrap.BootstrapContext;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.item.component.DamageResistant;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

@SuppressWarnings("UnstableApiUsage")
public class FireResistantProperty implements ComponentProperty {
    @Override
    public void applyComponent(@NotNull BootstrapContext context, @NotNull ConfigurationSection itemSection, @NotNull Map<DataComponentType<?>, Object> components) {
        if (!itemSection.getBoolean("fire_resistant", false))
            return;

        components.put(DataComponents.DAMAGE_RESISTANT, new DamageResistant(DamageTypeTags.IS_FIRE));
    }
}
