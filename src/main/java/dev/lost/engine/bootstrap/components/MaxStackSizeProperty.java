package dev.lost.engine.bootstrap.components;

import io.papermc.paper.plugin.bootstrap.BootstrapContext;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

@SuppressWarnings("UnstableApiUsage")
public class MaxStackSizeProperty implements ComponentProperty {
    @Override
    public void applyComponent(@NotNull BootstrapContext context, @NotNull ConfigurationSection itemSection, @NotNull Map<DataComponentType<?>, Object> components) {
        if (!itemSection.contains("max_stack_size"))
            return;

        int maxStackSize = itemSection.getInt("max_stack_size");
        if (maxStackSize < 1 || maxStackSize > 99) {
            context.getLogger().warn("max_stack_size must be within 1 and 99 for item: {}", itemSection.getName());
            return;
        }
        components.put(DataComponents.MAX_STACK_SIZE, maxStackSize);
    }
}
