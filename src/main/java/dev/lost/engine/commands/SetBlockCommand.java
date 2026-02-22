package dev.lost.engine.commands;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.tree.LiteralCommandNode;
import dev.lost.annotations.CanBreakOnUpdates;
import io.papermc.paper.adventure.PaperAdventure;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.command.brigadier.argument.ArgumentTypes;
import io.papermc.paper.command.brigadier.argument.resolvers.BlockPositionResolver;
import io.papermc.paper.math.BlockPosition;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.pattern.BlockInWorld;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.craftbukkit.block.CraftBlockState;

import javax.annotation.Nullable;
import java.util.function.Predicate;

@CanBreakOnUpdates("1.21.10") // Make sure the code from this command has been changed in newer versions
public class SetBlockCommand {
    private static final SimpleCommandExceptionType ERROR_FAILED = new SimpleCommandExceptionType(Component.translatable("commands.setblock.failed"));

    public static LiteralCommandNode<CommandSourceStack> getCommand() {
        Predicate<BlockInWorld> predicate = block -> block.getLevel().isEmptyBlock(block.getPos());
        return Commands.literal("setblock")
                .requires(commandSourceStack -> commandSourceStack.getSender().hasPermission("op"))
                .then(
                        Commands.argument("pos", ArgumentTypes.blockPosition())
                                .then(
                                        Commands.argument("block", ArgumentTypes.blockState())
                                                .suggests((ctx, builder) -> ArgumentTypes.blockState().listSuggestions(ctx, builder))
                                                .executes(
                                                        context -> setBlock(
                                                                context.getSource(),
                                                                context.getArgument("pos", BlockPositionResolver.class).resolve(context.getSource()),
                                                                context.getArgument("block", CraftBlockState.class).getHandle(),
                                                                Mode.REPLACE,
                                                                null,
                                                                false
                                                        )
                                                )
                                                .then(
                                                                Commands.literal("destroy")
                                                                        .executes(
                                                                                context -> setBlock(
                                                                                        context.getSource(),
                                                                                        context.getArgument("pos", BlockPositionResolver.class).resolve(context.getSource()),
                                                                                        context.getArgument("block", CraftBlockState.class).getHandle(),
                                                                                        Mode.DESTROY,
                                                                                        null,
                                                                                        false
                                                                                )
                                                                        )
                                                        )
                                                .then(
                                                                Commands.literal("keep")
                                                                        .executes(
                                                                                context -> setBlock(
                                                                                        context.getSource(),
                                                                                        context.getArgument("pos", BlockPositionResolver.class).resolve(context.getSource()),
                                                                                        context.getArgument("block", CraftBlockState.class).getHandle(),
                                                                                        Mode.REPLACE,
                                                                                        predicate,
                                                                                        false
                                                                                )
                                                                        )
                                                        )
                                                        .then(
                                                                Commands.literal("replace")
                                                                        .executes(
                                                                                context -> setBlock(
                                                                                        context.getSource(),
                                                                                        context.getArgument("pos", BlockPositionResolver.class).resolve(context.getSource()),
                                                                                        context.getArgument("block", CraftBlockState.class).getHandle(),
                                                                                        Mode.REPLACE,
                                                                                        null,
                                                                                        false
                                                                                )
                                                                        )
                                                        )
                                                        .then(
                                                                Commands.literal("strict")
                                                                        .executes(
                                                                                context -> setBlock(
                                                                                        context.getSource(),
                                                                                        context.getArgument("pos", BlockPositionResolver.class).resolve(context.getSource()),
                                                                                        context.getArgument("block", CraftBlockState.class).getHandle(),
                                                                                        Mode.REPLACE,
                                                                                        null,
                                                                                        true
                                                                                )
                                                                        )
                                                        )
                                )
                ).build();
    }

    @SuppressWarnings("UnstableApiUsage") // io.papermc.paper.math.BlockPosition
    private static int setBlock(
            CommandSourceStack source, BlockPosition paperPos, BlockState block, Mode mode, @Nullable Predicate<BlockInWorld> filter, boolean strict
    ) throws CommandSyntaxException {
        ServerLevel level = ((CraftWorld) source.getLocation().getWorld()).getHandle();
        BlockPos pos = new BlockPos(paperPos.blockX(), paperPos.blockY(), paperPos.blockZ());
        if (level.isDebug()) {
            throw ERROR_FAILED.create();
        } else if (filter != null && !filter.test(new BlockInWorld(level, pos, true))) {
            throw ERROR_FAILED.create();
        } else {
            boolean flag;
            if (mode == SetBlockCommand.Mode.DESTROY) {
                level.destroyBlock(pos, true);
                flag = !block.isAir() || !level.getBlockState(pos).isAir();
            } else {
                flag = true;
            }

            BlockState blockState = level.getBlockState(pos);
            if (flag && !level.setBlock(pos, block, 2 | (strict ? 816 : 256))) {
                throw ERROR_FAILED.create();
            } else {
                if (!strict) {
                    level.updateNeighboursOnBlockSet(pos, blockState);
                }

                source.getSender().sendMessage(PaperAdventure.asAdventure(Component.translatable("commands.setblock.success", pos.getX(), pos.getY(), pos.getZ())));
                return 1;
            }
        }
    }

    public enum Mode {
        REPLACE,
        DESTROY;
    }
}
