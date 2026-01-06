package dev.lost.engine.bootstrap.components;

import dev.lost.engine.bootstrap.components.annotations.Property;
import io.papermc.paper.plugin.bootstrap.BootstrapContext;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

@SuppressWarnings("UnstableApiUsage")
@Property(key = "max_damage")
public class MaxDamageProperty implements SimpleComponentProperty<Integer> {
    @Override
    public void applyComponent(@NotNull BootstrapContext context, @NotNull Integer maxDamage, @NotNull String itemID, @NotNull Map<DataComponentType<?>, Object> components) {
        components.put(DataComponents.MAX_DAMAGE, maxDamage);
    }
}
