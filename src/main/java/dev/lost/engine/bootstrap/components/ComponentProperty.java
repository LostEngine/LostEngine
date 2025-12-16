package dev.lost.engine.bootstrap.components;

import io.papermc.paper.plugin.bootstrap.BootstrapContext;
import net.minecraft.core.component.DataComponentType;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

@SuppressWarnings("UnstableApiUsage")
public interface ComponentProperty {
    void applyComponent(@NotNull BootstrapContext context, @NotNull ConfigurationSection itemSection, @NotNull Map<DataComponentType<?>, Object> components);
}
