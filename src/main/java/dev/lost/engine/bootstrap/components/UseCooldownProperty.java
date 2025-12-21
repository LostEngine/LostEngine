package dev.lost.engine.bootstrap.components;

import io.papermc.paper.plugin.bootstrap.BootstrapContext;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.component.UseCooldown;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.Optional;

@SuppressWarnings("UnstableApiUsage")
public class UseCooldownProperty implements ComponentProperty {
    @Override
    public void applyComponent(@NotNull BootstrapContext context, @NotNull ConfigurationSection itemSection, @NotNull Map<DataComponentType<?>, Object> components) {
        if (!itemSection.contains("use_cooldown"))
            return;

        float cooldownSeconds = (float) itemSection.getDouble("use_cooldown.cooldown_seconds", 1.0F);

        String groupString = itemSection.getString("use_cooldown.group");
        Optional<ResourceLocation> group = groupString == null ?
                Optional.of(ResourceLocation.fromNamespaceAndPath("lost_engine", itemSection.getName())) :
                Optional.of(ResourceLocation.parse(groupString));

        components.put(DataComponents.USE_COOLDOWN, new UseCooldown(cooldownSeconds, group));
    }
}
