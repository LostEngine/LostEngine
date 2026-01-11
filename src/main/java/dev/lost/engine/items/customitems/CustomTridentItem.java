package dev.lost.engine.items.customitems;

import dev.lost.engine.annotations.CanBreakOnUpdates;
import dev.lost.engine.entities.CustomThrownTrident;
import lombok.Getter;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.Position;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.entity.projectile.arrow.ThrownTrident;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TridentItem;
import net.minecraft.world.item.enchantment.EnchantmentEffectComponents;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

public class CustomTridentItem extends TridentItem implements CustomItem {

    @Getter
    private final String id;

    public CustomTridentItem(Properties properties, String id) {
        super(properties);
        this.id = id;
    }

    @Override
    public ItemStack getDynamicMaterial() {
        return Items.TRIDENT.getDefaultInstance();
    }

    /**
     * {@link TridentItem#releaseUsing(ItemStack, Level, LivingEntity, int)}
     */
    @SuppressWarnings("UnstableApiUsage")
    @CanBreakOnUpdates(lastCheckedVersion = "1.21.11")
    @Override
    public boolean releaseUsing(@NotNull ItemStack stack, @NotNull Level level, @NotNull LivingEntity entity, int timeLeft) {
        if (entity instanceof Player player) {
            int i = this.getUseDuration(stack, entity) - timeLeft;
            if (i < 10) {
                return false;
            } else {
                float tridentSpinAttackStrength = EnchantmentHelper.getTridentSpinAttackStrength(stack, player);
                if (tridentSpinAttackStrength > 0.0F && !player.isInWaterOrRain()) {
                    return false;
                } else if (stack.nextDamageWillBreak()) {
                    return false;
                } else {
                    Holder<SoundEvent> holder = EnchantmentHelper.pickHighestLevel(stack, EnchantmentEffectComponents.TRIDENT_SOUND)
                            .orElse(SoundEvents.TRIDENT_THROW);
                    player.awardStat(Stats.ITEM_USED.get(this));
                    if (level instanceof ServerLevel serverLevel) {
                        // stack.hurtWithoutBreaking(1, player); // CraftBukkit - moved down
                        if (tridentSpinAttackStrength == 0.0F) {
                            ItemStack itemStack = stack.copyWithCount(1); // Paper
                            Projectile.Delayed<ThrownTrident> tridentDelayed = Projectile.spawnProjectileFromRotationDelayed( // Paper - PlayerLaunchProjectileEvent(
                                    CustomThrownTrident::new, serverLevel, itemStack, player, 0.0F, 2.5F, 1.0F
                            );
                            // Paper start - PlayerLaunchProjectileEvent
                            com.destroystokyo.paper.event.player.PlayerLaunchProjectileEvent event = new com.destroystokyo.paper.event.player.PlayerLaunchProjectileEvent((org.bukkit.entity.Player) player.getBukkitEntity(), org.bukkit.craftbukkit.inventory.CraftItemStack.asCraftMirror(stack), (org.bukkit.entity.Projectile) tridentDelayed.projectile().getBukkitEntity());
                            if (!event.callEvent() || !tridentDelayed.attemptSpawn()) {
                                // CraftBukkit start
                                // Paper end - PlayerLaunchProjectileEvent
                                return false;
                            }
                            ThrownTrident thrownTrident = tridentDelayed.projectile(); // Paper - PlayerLaunchProjectileEvent

                            if (event.shouldConsume()) {
                                stack.hurtWithoutBreaking(1, player); // Paper - PlayerLaunchProjectileEvent
                            }
                            thrownTrident.pickupItemStack = stack.copy(); // SPIGOT-4511 update since damage call moved
                            if (event.shouldConsume()) {
                                stack.consume(1, player);
                            }
                            // CraftBukkit end
                            if (player.hasInfiniteMaterials()) {
                                thrownTrident.pickup = AbstractArrow.Pickup.CREATIVE_ONLY;
                            }

                            level.playSound(null, thrownTrident, holder.value(), SoundSource.PLAYERS, 1.0F, 1.0F);
                            return true;
                            // CraftBukkit start - SPIGOT-5458 also need in this branch :(
                        } else {
                            stack.hurtWithoutBreaking(1, player);
                            // CraftBukkit end
                        }
                    }

                    if (tridentSpinAttackStrength > 0.0F) {
                        float yRot = player.getYRot();
                        float xRot = player.getXRot();
                        float f = -Mth.sin(yRot * (float) (Math.PI / 180.0)) * Mth.cos(xRot * (float) (Math.PI / 180.0));
                        float f1 = -Mth.sin(xRot * (float) (Math.PI / 180.0));
                        float f2 = Mth.cos(yRot * (float) (Math.PI / 180.0)) * Mth.cos(xRot * (float) (Math.PI / 180.0));
                        float squareRoot = Mth.sqrt(f * f + f1 * f1 + f2 * f2);
                        f *= tridentSpinAttackStrength / squareRoot;
                        f1 *= tridentSpinAttackStrength / squareRoot;
                        f2 *= tridentSpinAttackStrength / squareRoot;
                        if (!org.bukkit.craftbukkit.event.CraftEventFactory.callPlayerRiptideEvent(player, stack, f, f1, f2))
                            return false; // Paper - Add player riptide event
                        player.push(f, f1, f2);
                        player.startAutoSpinAttack(20, 8.0F, stack);
                        if (player.onGround()) {
                            //float f3 = 1.1999999F;
                            player.move(MoverType.SELF, new Vec3(0.0, 1.1999999F, 0.0));
                        }

                        level.playSound(null, player, holder.value(), SoundSource.PLAYERS, 1.0F, 1.0F);
                        return true;
                    } else {
                        return false;
                    }
                }
            }
        } else {
            return false;
        }
    }

    /**
     * {@link TridentItem#asProjectile(Level, Position, ItemStack, Direction)}
     */
    @Override
    public @NotNull Projectile asProjectile(@NotNull Level level, @NotNull Position pos, @NotNull ItemStack stack, @NotNull Direction direction) {
        CustomThrownTrident thrownTrident = new CustomThrownTrident(level, pos.x(), pos.y(), pos.z(), stack.copyWithCount(1));
        thrownTrident.pickup = AbstractArrow.Pickup.ALLOWED;
        return thrownTrident;
    }

}
