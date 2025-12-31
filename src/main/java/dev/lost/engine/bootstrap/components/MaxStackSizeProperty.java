package dev.lost.engine.bootstrap.components;

import dev.lost.engine.bootstrap.components.annotations.Property;
import io.papermc.paper.plugin.bootstrap.BootstrapContext;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

@SuppressWarnings("UnstableApiUsage")
@Property(key = "max_stack_size")
public class MaxStackSizeProperty implements SimpleComponentProperty<Integer> {
    @Override
    public void applyComponent(@NotNull BootstrapContext context, @Nullable Integer maxStackSize, @NotNull String itemID, @NotNull Map<DataComponentType<?>, Object> components) {
        if (maxStackSize == null) return;
        if (maxStackSize < 1 || maxStackSize > 99) {
            context.getLogger().warn("max_stack_size must be within 1 and 99 for item: {}", itemID);
            return;
        }

        components.put(DataComponents.MAX_STACK_SIZE, maxStackSize);
    }
}
