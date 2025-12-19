package dev.lost.engine.bootstrap.components;

import com.google.common.collect.ImmutableSortedSet;
import io.papermc.paper.plugin.bootstrap.BootstrapContext;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.component.TooltipDisplay;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;

import java.util.*;

@SuppressWarnings("UnstableApiUsage")
public class TooltipDisplayProperty implements ComponentProperty {
    @Override
    public void applyComponent(@NotNull BootstrapContext context, @NotNull ConfigurationSection itemSection, @NotNull Map<DataComponentType<?>, Object> components) {
        if (!itemSection.contains("tooltip_display")) return;

        if (itemSection.getBoolean("tooltip_display.hide_tooltip", false)) {
            components.put(DataComponents.TOOLTIP_DISPLAY, new TooltipDisplay(true, ImmutableSortedSet.of()));
            return;
        }

        List<String> tooltipList = itemSection.getStringList("tooltip_display.hidden_components");
        SequencedSet<DataComponentType<?>> tooltipTypes = new LinkedHashSet<>();

        for (String s : tooltipList) {
            if (s == null || s.isBlank())
                continue;

            try {
                Holder.Reference<DataComponentType<?>> ref = BuiltInRegistries.DATA_COMPONENT_TYPE.get(ResourceLocation.parse(s)).orElse(null);
                if (ref == null) {
                    context.getLogger().warn("Unknown component to hide: {} for item {}", s, itemSection.getName());
                    continue;
                }

                tooltipTypes.add(ref.value());
            } catch (Exception e) {
                context.getLogger().warn("Invalid component to hide: {} for item {}", s, itemSection.getName());
            }
        }

        if (!tooltipTypes.isEmpty()) {
            components.put(DataComponents.TOOLTIP_DISPLAY, new TooltipDisplay(false, tooltipTypes));
        }
    }
}
