package dev.lost.engine.bootstrap.components;

import dev.lost.engine.bootstrap.components.annotations.Property;
import io.papermc.paper.plugin.bootstrap.BootstrapContext;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.util.Unit;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.Nullable;

import java.util.Map;

@SuppressWarnings("UnstableApiUsage")
@Property(key = "unbreakable")
public class UnbreakableProperty implements SimpleComponentProperty<Boolean> {
    @Override
    public void applyComponent(@NotNull BootstrapContext context, @Nullable Boolean unbreakable, @NotNull String itemID, @NotNull Map<DataComponentType<?>, Object> components) {
        if (Boolean.FALSE.equals(unbreakable))
            return;

        components.put(DataComponents.UNBREAKABLE, Unit.INSTANCE);
    }
}
