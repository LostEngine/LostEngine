package dev.lost.engine.bootstrap.components;

import dev.lost.engine.bootstrap.components.annotations.Property;
import io.papermc.paper.plugin.bootstrap.BootstrapContext;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.item.component.DamageResistant;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

@SuppressWarnings("UnstableApiUsage")
@Property(key = "fire_resistant")
public class FireResistantProperty implements SimpleComponentProperty<Boolean> {
    @Override
    public void applyComponent(@NotNull BootstrapContext context, @NotNull Boolean fireResistant, @NotNull String itemID, @NotNull Map<DataComponentType<?>, Object> components) {
        if (!fireResistant) return;
        components.put(DataComponents.DAMAGE_RESISTANT, new DamageResistant(DamageTypeTags.IS_FIRE));
    }
}
