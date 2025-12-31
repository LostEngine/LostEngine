package dev.lost.engine.bootstrap.components;

import dev.lost.engine.bootstrap.components.annotations.Parameter;
import dev.lost.engine.bootstrap.components.annotations.Property;
import io.papermc.paper.plugin.bootstrap.BootstrapContext;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.component.UseCooldown;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.Optional;

@SuppressWarnings({"UnstableApiUsage", "unused", "FieldMayBeFinal", "FieldCanBeLocal"})
@Property(key = "use_cooldown")
public class UseCooldownProperty implements ComponentProperty {
    @Parameter(key = "cooldown_seconds", type = Float.class, required = true)
    private float cooldownSeconds = 1F;

    @Parameter(key = "group", type = String.class)
    private String groupString;

    @Override
    public void applyComponent(@NotNull BootstrapContext context, @NotNull ConfigurationSection itemSection, @NotNull String itemID, @NotNull Map<DataComponentType<?>, Object> components) {
        Optional<ResourceLocation> group = groupString == null ?
                Optional.of(ResourceLocation.fromNamespaceAndPath("lost_engine", itemSection.getName())) :
                Optional.of(ResourceLocation.parse(groupString));

        components.put(DataComponents.USE_COOLDOWN, new UseCooldown(cooldownSeconds, group));
    }
}
