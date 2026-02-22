package dev.lost.engine.blocks.customblocks;

import dev.lost.annotations.NotNull;
import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.TntBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.BlockHitResult;
import org.bukkit.craftbukkit.event.CraftEventFactory;
import org.bukkit.event.block.TNTPrimeEvent;

import javax.annotation.Nullable;
import java.util.function.BooleanSupplier;

@SuppressWarnings("JavadocReference")
public class TNTCustomBlock extends Block implements CustomBlock {
    public static final BooleanProperty UNSTABLE = BlockStateProperties.UNSTABLE;

    @Getter
    private final BlockState clientBlockState;

    private final float explosionPower;

    public TNTCustomBlock(Properties properties, BlockState clientBlockState, float explosionPower) {
        super(properties);
        this.registerDefaultState(this.defaultBlockState().setValue(UNSTABLE, false));
        this.clientBlockState = clientBlockState;
        this.explosionPower = explosionPower;
    }

    /**
     * ⚠️
     * The following code is a modified version of {@link TntBlock} to support custom block states
     * ⚠️
     * ------------------------------------------------------------------------------------------- <br>
     * {@link TntBlock#onPlace(BlockState, Level, BlockPos, BlockState, boolean)}
     */
    @Override
    protected void onPlace(@NotNull BlockState state, @NotNull Level level, @NotNull BlockPos pos, @NotNull BlockState oldState, boolean isMoving) {
        if (!oldState.is(state.getBlock())) {
            if (level.hasNeighborSignal(pos) && prime(level, pos, () -> org.bukkit.craftbukkit.event.CraftEventFactory.callTNTPrimeEvent(level, pos, org.bukkit.event.block.TNTPrimeEvent.PrimeCause.REDSTONE, null, null))) { // CraftBukkit - TNTPrimeEvent
                level.removeBlock(pos, false);
            }
        }
    }

    /**
     * {@link TntBlock#neighborChanged(BlockState, Level, BlockPos, Block, Orientation, boolean)}
     */
    @Override
    protected void neighborChanged(@NotNull BlockState state, @NotNull Level level, @NotNull BlockPos pos, @NotNull Block neighborBlock, @Nullable Orientation orientation, boolean movedByPiston) {
        if (level.hasNeighborSignal(pos) && prime(level, pos, () -> org.bukkit.craftbukkit.event.CraftEventFactory.callTNTPrimeEvent(level, pos, org.bukkit.event.block.TNTPrimeEvent.PrimeCause.REDSTONE, null, null))) { // CraftBukkit - TNTPrimeEvent
            level.removeBlock(pos, false);
        }
    }

    /**
     * {@link TntBlock#playerWillDestroy(Level, BlockPos, BlockState, Player)}
     */
    @Override
    public @NotNull BlockState playerWillDestroy(@NotNull Level level, @NotNull BlockPos pos, @NotNull BlockState state, @NotNull Player player) {
        if (!level.isClientSide() && !player.getAbilities().instabuild && state.getValue(UNSTABLE)) {
            prime(level, pos, () -> org.bukkit.craftbukkit.event.CraftEventFactory.callTNTPrimeEvent(level, pos, org.bukkit.event.block.TNTPrimeEvent.PrimeCause.BLOCK_BREAK, player, null)); // CraftBukkit - TNTPrimeEvent
        }

        return super.playerWillDestroy(level, pos, state, player);
    }

    /**
     * {@link TntBlock#wasExploded(ServerLevel, BlockPos, Explosion)}
     */
    @Override
    public void wasExploded(@NotNull ServerLevel level, @NotNull BlockPos pos, @NotNull Explosion explosion) {
        if (level.getGameRules().get(GameRules.TNT_EXPLODES)) {
            PrimedTnt primedTnt = new PrimedTnt(level, (double) pos.getX() + (double) 0.5F, pos.getY(), (double) pos.getZ() + (double) 0.5F, explosion.getIndirectSourceEntity());
            primedTnt.explosionPower = explosionPower;
            primedTnt.setBlockState(this.defaultBlockState());
            int fuse = primedTnt.getFuse();
            primedTnt.setFuse((short) (level.random.nextInt(fuse / 4) + fuse / 8));
            level.addFreshEntity(primedTnt);
        }
    }

    /**
     * {@link TntBlock#prime(Level, BlockPos, BooleanSupplier)}
     */
    public boolean prime(Level level, BlockPos pos, BooleanSupplier event) {
        return prime(level, pos, null, event);
    }

    /**
     * {@link TntBlock#prime(Level, BlockPos, LivingEntity, BooleanSupplier)}
     */
    private boolean prime(Level level, BlockPos pos, @Nullable LivingEntity entity, BooleanSupplier event) {
        if (level instanceof ServerLevel serverLevel) {
            if (serverLevel.getGameRules().get(GameRules.TNT_EXPLODES) && event.getAsBoolean()) {
                PrimedTnt primedTnt = new PrimedTnt(level, (double) pos.getX() + (double) 0.5F, pos.getY(), (double) pos.getZ() + (double) 0.5F, entity);
                primedTnt.explosionPower = explosionPower;
                primedTnt.setBlockState(this.defaultBlockState());
                level.addFreshEntity(primedTnt);
                level.playSound(null, primedTnt.getX(), primedTnt.getY(), primedTnt.getZ(), SoundEvents.TNT_PRIMED, SoundSource.BLOCKS, 1.0F, 1.0F);
                level.gameEvent(entity, GameEvent.PRIME_FUSE, pos);
                return true;
            }
        }

        return false;
    }

    /**
     * {@link TntBlock#useItemOn(ItemStack, BlockState, Level, BlockPos, Player, InteractionHand, BlockHitResult)}
     */
    @Override
    protected @NotNull InteractionResult useItemOn(
            @NotNull ItemStack stack, @NotNull BlockState state, @NotNull Level level, @NotNull BlockPos pos, @NotNull Player player, @NotNull InteractionHand hand, @NotNull BlockHitResult hitResult
    ) {
        if (!stack.is(Items.FLINT_AND_STEEL) && !stack.is(Items.FIRE_CHARGE)) {
            return super.useItemOn(stack, state, level, pos, player, hand, hitResult);
        } else {
            if (prime(level, pos, player, () -> CraftEventFactory.callTNTPrimeEvent(level, pos, TNTPrimeEvent.PrimeCause.PLAYER, player, null))) {
                level.setBlock(pos, Blocks.AIR.defaultBlockState(), UPDATE_NEIGHBORS | UPDATE_CLIENTS | UPDATE_IMMEDIATE);
                Item item = stack.getItem();
                if (stack.is(Items.FLINT_AND_STEEL)) {
                    stack.hurtAndBreak(1, player, hand.asEquipmentSlot());
                } else {
                    stack.consume(1, player);
                }

                player.awardStat(Stats.ITEM_USED.get(item));
            } else if (level instanceof ServerLevel serverLevel) {
                if (!serverLevel.getGameRules().get(GameRules.TNT_EXPLODES)) {
                    player.displayClientMessage(Component.translatable("block.minecraft.tnt.disabled"), true);
                    return InteractionResult.PASS;
                }
            }

            return InteractionResult.SUCCESS;
        }
    }

    /**
     * {@link TntBlock#onProjectileHit(Level, BlockState, BlockHitResult, Projectile)}
     */
    @Override
    protected void onProjectileHit(@NotNull Level level, @NotNull BlockState state, @NotNull BlockHitResult hit, @NotNull Projectile projectile) {
        if (level instanceof ServerLevel serverLevel) {
            BlockPos blockPos = hit.getBlockPos();
            Entity owner = projectile.getOwner();
            if (projectile.isOnFire() &&
                    projectile.mayInteract(serverLevel, blockPos) &&
                    prime(level, blockPos, owner instanceof LivingEntity ? (LivingEntity) owner : null, () -> CraftEventFactory.callEntityChangeBlockEvent(projectile, blockPos, state.getFluidState().createLegacyBlock()) && CraftEventFactory.callTNTPrimeEvent(level, blockPos, TNTPrimeEvent.PrimeCause.PROJECTILE, projectile, null))) {
                level.removeBlock(blockPos, false);
            }
        }
    }

    /**
     * {@link TntBlock#dropFromExplosion(Explosion)} 
     */
    @Override
    public boolean dropFromExplosion(@NotNull Explosion explosion) {
        return false;
    }

    /**
     * {@link TntBlock#createBlockStateDefinition(StateDefinition.Builder)}
     */
    @Override
    protected void createBlockStateDefinition(StateDefinition.@NotNull Builder<Block, BlockState> builder) {
        builder.add(UNSTABLE);
    }

}
