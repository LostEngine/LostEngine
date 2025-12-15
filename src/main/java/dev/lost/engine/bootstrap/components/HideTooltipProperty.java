package dev.lost.engine.bootstrap.components;

import io.papermc.paper.plugin.bootstrap.BootstrapContext;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.TooltipDisplay;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedHashSet;
import java.util.Map;

@SuppressWarnings("UnstableApiUsage")
public class HideTooltipProperty implements ComponentProperty {
    @Override
    public void applyComponent(@NotNull BootstrapContext context, @NotNull ConfigurationSection itemSection, @NotNull Map<DataComponentType<?>, Object> components) {
        if (!itemSection.getBoolean("hide_tooltip", false))
            return;

        components.put(DataComponents.TOOLTIP_DISPLAY, new TooltipDisplay(true, LinkedHashSet.newLinkedHashSet(0)));
    }
}
