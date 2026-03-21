package dev.lost.engine.lua.luatables;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.lost.annotations.NotNull;
import io.papermc.paper.adventure.PaperAdventure;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.minecraft.commands.arguments.item.ItemParser;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.Varargs;
import org.luaj.vm2.lib.LibFunction;
import org.luaj.vm2.lib.OneArgFunction;
import org.luaj.vm2.lib.TwoArgFunction;
import org.luaj.vm2.lib.ZeroArgFunction;

public class LuaPlayer {
    public static @NotNull LuaTable getLuaPlayer(@NotNull ServerPlayer player) {
        LuaTable luaPlayer = new LuaTable();
        luaPlayer.set("getPosition", new LibFunction() {
            @Override
            public Varargs invoke(Varargs args) {
                return LuaValue.varargsOf(new LuaValue[]{
                        LuaValue.valueOf(player.getX()),
                        LuaValue.valueOf(player.getY()),
                        LuaValue.valueOf(player.getZ()),
                        LuaValue.valueOf(player.getYRot()),
                        LuaValue.valueOf(player.getXRot())
                });
            }
        });
        luaPlayer.set("giveItem", new TwoArgFunction() {
            public LuaValue call(LuaValue item, LuaValue count) {
                ItemParser parser = new ItemParser(MinecraftServer.getServer().registryAccess());
                try {
                    ItemParser.ItemResult result = parser.parse(new StringReader(item.checkjstring()));
                    ItemStack itemStack = result.item().value().getDefaultInstance();
                    itemStack.restorePatch(result.components());
                    itemStack.setCount(count.isint() ? count.checkint() : 1);
                    player.getInventory().add(itemStack);
                } catch (CommandSyntaxException e) {
                    e.printStackTrace();
                }
                return LuaValue.NIL;
            }
        });
        luaPlayer.set("sendMessage", new OneArgFunction() {
            public LuaValue call(LuaValue msg) {
                player.sendSystemMessage(
                        PaperAdventure.asVanilla(
                                MiniMessage.miniMessage()
                                        .deserialize(msg.checkjstring())
                        )
                );
                return LuaValue.NIL;
            }
        });
        luaPlayer.set("sendActionBar", new OneArgFunction() {
            public LuaValue call(LuaValue msg) {
                player.sendSystemMessage(
                        PaperAdventure.asVanilla(
                                MiniMessage.miniMessage()
                                        .deserialize(msg.checkjstring())
                        ),
                        true
                );
                return LuaValue.NIL;
            }
        });
        luaPlayer.set("toggleFly", new OneArgFunction() {
            public LuaValue call(LuaValue msg) {
                player.getAbilities().mayfly = msg.checkboolean();
                return LuaValue.NIL;
            }
        });
        luaPlayer.set("canFly", new ZeroArgFunction() {
            public LuaValue call() {
                return LuaValue.valueOf(player.getAbilities().mayfly);
            }
        });
        luaPlayer.set("feed", new TwoArgFunction() {
            @Override
            public LuaValue call(LuaValue arg1, LuaValue arg2) {
                player.getFoodData().eat(arg1.checkint(), (float) arg2.checkdouble());
                return LuaValue.NIL;
            }
        });
        luaPlayer.set("heal", new OneArgFunction() {
            @Override
            public LuaValue call(LuaValue arg) {
                player.heal((float) arg.checkdouble());
                return LuaValue.NIL;
            }
        });
        luaPlayer.set("damage", new OneArgFunction() {
            @Override
            public LuaValue call(LuaValue arg) {
                player.hurtServer(player.level(), player.damageSources().generic(), (float) arg.checkdouble());
                return LuaValue.NIL;
            }
        });
        luaPlayer.set("execute", new OneArgFunction() {
            @Override
            public LuaValue call(LuaValue msg) {
                MinecraftServer.getServer().getCommands().performPrefixedCommand(
                        player.createCommandSourceStack(),
                        msg.checkjstring()
                );
                return LuaValue.NIL;
            }
        });
        LuaTable world = LuaWorld.getLuaWorld(player.level());
        luaPlayer.set("getWorld", new ZeroArgFunction() {
            @Override
            public LuaValue call() {
                return world;
            }
        });
        return luaPlayer;
    }
}
