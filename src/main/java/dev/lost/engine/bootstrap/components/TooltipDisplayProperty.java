package dev.lost.engine.bootstrap.components;

import io.papermc.paper.plugin.bootstrap.BootstrapContext;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.component.TooltipDisplay;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.SequencedSet;

@SuppressWarnings("UnstableApiUsage")
public class TooltipDisplayProperty implements ComponentProperty {
    @Override
    public void applyComponent(@NotNull BootstrapContext context, @NotNull ConfigurationSection itemSection, @NotNull Map<DataComponentType<?>, Object> components) {
        if (itemSection.getBoolean("hide_tooltip", false) || !itemSection.contains("tooltip_display")) return;

        List<String> tooltipList = itemSection.getStringList("tooltip_display");
        SequencedSet<DataComponentType<?>> tooltipTypes = new LinkedHashSet<>();

        for (String s : tooltipList) {
            if (s == null || s.isBlank())
                continue;

            try {
                Holder.Reference<DataComponentType<?>> ref = BuiltInRegistries.DATA_COMPONENT_TYPE.get(ResourceLocation.parse(s)).orElse(null);
                if (ref == null) {
                    context.getLogger().warn("Unknown data component: {} for item", s);
                    continue;
                }

                tooltipTypes.add(ref.value());
            } catch (Exception e) {
                context.getLogger().warn("Invalid data component id: {} for item", s);
            }
        }

        if (!tooltipTypes.isEmpty()) {
            components.put(DataComponents.TOOLTIP_DISPLAY, new TooltipDisplay(false, tooltipTypes));
        }
    }
}
