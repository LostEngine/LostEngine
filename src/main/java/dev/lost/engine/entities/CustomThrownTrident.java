package dev.lost.engine.entities;

import dev.lost.engine.LostEngine;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.arrow.ThrownTrident;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.bukkit.Bukkit;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

public class CustomThrownTrident extends ThrownTrident {

    public static final ConcurrentMap<Integer, ItemStack> CUSTOM_TRIDENTS = new ConcurrentHashMap<>();

    public CustomThrownTrident(Level level, LivingEntity shooter, ItemStack pickupItemStack) {
        super(level, shooter, pickupItemStack);
        CUSTOM_TRIDENTS.put(this.getId(), pickupItemStack);
    }

    public CustomThrownTrident(Level level, double x, double y, double z, ItemStack pickupItemStack) {
        super(level, x, y, z, pickupItemStack);
        CUSTOM_TRIDENTS.put(this.getId(), pickupItemStack);
    }

    public void onRemoval(@NotNull Entity.RemovalReason reason) {
        super.onRemoval(reason);
        Bukkit.getAsyncScheduler().runDelayed(
                LostEngine.getInstance(),
                scheduledTask -> CUSTOM_TRIDENTS.remove(this.getId()),
                1,
                TimeUnit.SECONDS
        ); // We remove the trident from the map asynchronously 1 second after to make sure all metadata packets have been sent
    }

    public void setPickupItemStack(@NotNull ItemStack pickupItemStack) {
        super.setPickupItemStack(pickupItemStack);
        CUSTOM_TRIDENTS.put(this.getId(), pickupItemStack);
    }
}
