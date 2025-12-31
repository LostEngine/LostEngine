package dev.lost.engine.bootstrap.components;

import dev.lost.engine.bootstrap.components.annotations.Property;
import dev.lost.engine.utils.EnumUtils;
import io.papermc.paper.plugin.bootstrap.BootstrapContext;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.Rarity;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@SuppressWarnings("UnstableApiUsage")
@Property(key = "rarity")
public class RarityProperty implements SimpleComponentProperty<String> {
    @Override
    public void applyComponent(@NotNull BootstrapContext context, @NotNull String rarityString, @NotNull String itemID, @NotNull Map<DataComponentType<?>, Object> components) {
        Optional<Rarity> rarity = EnumUtils.match(rarityString.trim().toUpperCase(Locale.ROOT), Rarity.class);

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
