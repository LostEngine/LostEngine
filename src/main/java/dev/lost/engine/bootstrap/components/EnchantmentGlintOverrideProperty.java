package dev.lost.engine.bootstrap.components;

import dev.lost.engine.bootstrap.components.annotations.Property;
import io.papermc.paper.plugin.bootstrap.BootstrapContext;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

@SuppressWarnings({"UnstableApiUsage"})
@Property(key = "enchantment_glint_override")
public class EnchantmentGlintOverrideProperty implements SimpleComponentProperty<Boolean> {
    @Override
    public void applyComponent(@NotNull BootstrapContext context, @NotNull Boolean enchantmentGlintOverride, @NotNull String itemID, @NotNull Map<DataComponentType<?>, Object> components) {
        if (!enchantmentGlintOverride) return;
        components.put(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, enchantmentGlintOverride);
    }
}
