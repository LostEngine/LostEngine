package dev.lost.engine.commands;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.tree.LiteralCommandNode;
import dev.lost.annotations.CanBreakOnUpdates;
import dev.lost.annotations.NotNull;
import io.papermc.paper.adventure.PaperAdventure;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.command.brigadier.argument.ArgumentTypes;
import io.papermc.paper.command.brigadier.argument.resolvers.selector.PlayerSelectorArgumentResolver;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.craftbukkit.inventory.CraftItemStack;

import java.util.Collection;

@CanBreakOnUpdates("1.21.10") // Make sure the code from this command has been changed in newer versions
public class GiveCommand {
    public static final int MAX_ALLOWED_ITEMSTACKS = 100;

    public static LiteralCommandNode<CommandSourceStack> getCommand() {
        return Commands.literal("give")
                .requires(commandSourceStack -> commandSourceStack.getSender().hasPermission("op"))
                .then(
                        Commands.argument("targets", ArgumentTypes.players())
                                .then(
                                        Commands.argument("item", ArgumentTypes.itemStack())
                                                .suggests((ctx, builder) -> ArgumentTypes.itemStack().listSuggestions(ctx, builder))
                                                .executes(
                                                        ctx -> {
                                                            PlayerSelectorArgumentResolver playerSelectorArgumentResolver = ctx.getArgument("targets", PlayerSelectorArgumentResolver.class);
                                                            ItemStack item = CraftItemStack.asNMSCopy(ctx.getArgument("item", CraftItemStack.class));
                                                            return giveItem(
                                                                    ctx.getSource(),
                                                                    item,
                                                                    playerSelectorArgumentResolver.resolve(ctx.getSource()).stream().map(player -> ((CraftPlayer) player).getHandle()).toList(),
                                                                    1
                                                            );
                                                        }
                                                )
                                                .then(
                                                        Commands.argument("count", IntegerArgumentType.integer(1))
                                                                .executes(
                                                                        ctx -> {
                                                                            PlayerSelectorArgumentResolver playerSelectorArgumentResolver = ctx.getArgument("targets", PlayerSelectorArgumentResolver.class);
                                                                            ItemStack item = CraftItemStack.asNMSCopy(ctx.getArgument("item", CraftItemStack.class));
                                                                            return giveItem(
                                                                                    ctx.getSource(),
                                                                                    item,
                                                                                    playerSelectorArgumentResolver.resolve(ctx.getSource()).stream().map(player -> ((CraftPlayer) player).getHandle()).toList(),
                                                                                    IntegerArgumentType.getInteger(ctx, "count")
                                                                            );
                                                                        }
                                                                )
                                                )
                                )
                ).build();
    }

    private static int giveItem(CommandSourceStack source, @NotNull ItemStack item, Collection<ServerPlayer> targets, int count) {
        item.setCount(count);
        final Component displayName = item.getDisplayName(); // Paper - get display name early
        int maxStackSize = item.getMaxStackSize();
        int i = maxStackSize * MAX_ALLOWED_ITEMSTACKS;
        if (count > i) {
            source.getSender().sendMessage(PaperAdventure.asAdventure(Component.translatable("commands.give.failed.toomanyitems", i, displayName)));
            return 0;
        } else {
            for (ServerPlayer serverPlayer : targets) {
                int i1 = count;

                while (i1 > 0) {
                    int min = Math.min(maxStackSize, i1);
                    i1 -= min;
                    boolean flag = serverPlayer.getInventory().add(item);
                    if (flag && item.isEmpty()) {
                        ItemEntity itemEntity = serverPlayer.drop(item, false, false, false, null); // Paper - do not fire PlayerDropItemEvent for /give command
                        //noinspection ConstantValue -- the null check was here in the original code I guess I should not remove it
                        if (itemEntity != null) {
                            itemEntity.makeFakeItem();
                        }

                        //noinspection resource -- false positive for ServerPlater#level()
                        serverPlayer.level()
                                .playSound(
                                        null,
                                        serverPlayer.getX(),
                                        serverPlayer.getY(),
                                        serverPlayer.getZ(),
                                        SoundEvents.ITEM_PICKUP,
                                        SoundSource.PLAYERS,
                                        0.2F,
                                        ((serverPlayer.getRandom().nextFloat() - serverPlayer.getRandom().nextFloat()) * 0.7F + 1.0F) * 2.0F
                                );
                        serverPlayer.containerMenu.broadcastChanges();
                    } else {
                        ItemEntity itemEntity = serverPlayer.drop(item, false, false, false, null); // Paper - do not fire PlayerDropItemEvent for /give command
                        //noinspection ConstantValue -- the null check was here in the original code I guess I should not remove it
                        if (itemEntity != null) {
                            itemEntity.setNoPickUpDelay();
                            itemEntity.setTarget(serverPlayer.getUUID());
                        }
                    }
                }
            }

            if (targets.size() == 1) {
                source.getSender().sendMessage(PaperAdventure.asAdventure(Component.translatable("commands.give.success.single", count, displayName, targets.iterator().next().getDisplayName()))); // Paper - use cached display name
            } else {
                source.getSender().sendMessage(PaperAdventure.asAdventure(Component.translatable("commands.give.success.single", count, displayName, targets.size())));
            }

            return targets.size();
        }
    }
}
