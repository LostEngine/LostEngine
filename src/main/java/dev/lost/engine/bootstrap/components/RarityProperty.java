package dev.lost.engine.bootstrap.components;

import dev.lost.engine.utils.EnumUtils;
import io.papermc.paper.plugin.bootstrap.BootstrapContext;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.Rarity;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@SuppressWarnings("UnstableApiUsage")
public class RarityProperty implements ComponentProperty {
    @Override
    public void applyComponent(@NotNull BootstrapContext context, @NotNull ConfigurationSection itemSection, @NotNull Map<DataComponentType<?>, Object> components) {
        if (!itemSection.contains("rarity"))
            return;

        String rarityString = itemSection.getString("rarity");
        Optional<Rarity> rarity = Optional.ofNullable(rarityString)
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .flatMap(s -> EnumUtils.match(s, Rarity.class));

        rarity.ifPresentOrElse(
                r -> components.put(DataComponents.RARITY, r),
                () -> {
                    String allowed = Arrays.stream(Rarity.values())
                            .map(Enum::name)
                            .collect(Collectors.joining(", "));

                    context.getLogger().warn("Invalid rarity '{}', valid values: {}", rarityString, allowed);
                }
        );
    }
}
