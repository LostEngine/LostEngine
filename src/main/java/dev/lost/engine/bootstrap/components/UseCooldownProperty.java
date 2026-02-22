package dev.lost.engine.bootstrap.components;

import dev.lost.annotations.NotNull;
import dev.lost.annotations.Nullable;
import dev.lost.engine.bootstrap.components.annotations.Parameter;
import dev.lost.engine.bootstrap.components.annotations.Property;
import io.papermc.paper.plugin.bootstrap.BootstrapContext;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.component.UseCooldown;
import org.bukkit.configuration.ConfigurationSection;

import java.util.Map;
import java.util.Optional;

@SuppressWarnings({"UnstableApiUsage", "unused", "FieldMayBeFinal", "FieldCanBeLocal"})
@Property(key = "use_cooldown")
public class UseCooldownProperty implements ComponentProperty {
    @Parameter(key = "cooldown_seconds", type = Float.class, required = true)
    private Float cooldownSeconds = 1F;

    @Nullable
    @Parameter(key = "group", type = String.class)
    private String groupString;

    @Override
    public void applyComponent(@NotNull BootstrapContext context, @NotNull ConfigurationSection itemSection, @NotNull String itemID, @NotNull Map<DataComponentType<?>, Object> components) {
        Optional<Identifier> group = groupString == null ?
                Optional.of(Identifier.fromNamespaceAndPath("lost_engine", itemSection.getName())) :
                Optional.of(Identifier.parse(groupString));

        components.put(DataComponents.USE_COOLDOWN, new UseCooldown(cooldownSeconds, group));
    }
}
