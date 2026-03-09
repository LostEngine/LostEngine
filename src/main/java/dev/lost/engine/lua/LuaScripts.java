package dev.lost.engine.lua;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.lost.annotations.NotNull;
import net.minecraft.commands.arguments.blocks.BlockStateParser;
import net.minecraft.commands.arguments.item.ItemParser;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.luaj.vm2.Globals;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.Varargs;
import org.luaj.vm2.lib.LibFunction;
import org.luaj.vm2.lib.OneArgFunction;
import org.luaj.vm2.lib.TwoArgFunction;
import org.luaj.vm2.lib.ZeroArgFunction;
import org.luaj.vm2.lib.jse.JsePlatform;
import org.luaj.vm2.luajc.LuaJC;

import static net.minecraft.world.level.block.Block.UPDATE_ALL;

public class LuaScripts {

    private static final Globals GLOBALS = JsePlatform.standardGlobals();

    static {
        LuaTable server = new LuaTable();
        server.set("time", new ZeroArgFunction() {
            @Override
            public LuaValue call() {
                return LuaValue.valueOf(System.currentTimeMillis());
            }
        });
        server.set("execute", new OneArgFunction() {
            @Override
            public LuaValue call(LuaValue msg) {
                MinecraftServer.getServer().getCommands().performPrefixedCommand(
                        MinecraftServer.getServer().createCommandSourceStack(),
                        msg.checkjstring()
                );
                return LuaValue.NIL;
            }
        });
        GLOBALS.set("server", server);
        GLOBALS.set("os", LuaValue.NIL);
        GLOBALS.set("package", LuaValue.NIL);
        GLOBALS.set("io", LuaValue.NIL);
        LuaJC.install(GLOBALS);
    }

    public static LuaValue loadScript(String script) {
        return GLOBALS.load(script);
    }

    public static void onClick(@NotNull LuaValue luaValue, @NotNull ServerPlayer player, double x, float y, float z) {
        LuaValue function = luaValue.get("onClick");
        if (function != null)
            function.invoke(new LuaValue[]{
                    getLuaPlayer(player),
                    LuaValue.valueOf(x),
                    LuaValue.valueOf(y),
                    LuaValue.valueOf(z)
            });
    }

    private static @NotNull LuaTable getLuaPlayer(ServerPlayer player) {
        LuaTable luaPlayer = new LuaTable();
        luaPlayer.set("getPosition", new LibFunction() {
            @Override
            public Varargs invoke(Varargs args) {
                return LuaValue.varargsOf(new LuaValue[]{
                        LuaValue.valueOf(player.getX()),
                        LuaValue.valueOf(player.getY()),
                        LuaValue.valueOf(player.getZ())
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
                } catch (CommandSyntaxException e) {
                    e.printStackTrace();
                }
                return LuaValue.NIL;
            }
        });
        luaPlayer.set("sendMessage", new OneArgFunction() {
            public LuaValue call(LuaValue msg) {
                player.sendSystemMessage(Component.literal(msg.checkjstring()));
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
        LuaTable world = getLuaWorld(player.level());
        luaPlayer.set("getWorld", new ZeroArgFunction() {
            @Override
            public LuaValue call() {
                return world;
            }
        });
        return luaPlayer;
    }

    private static @NotNull LuaTable getLuaWorld(ServerLevel level) {
        LuaTable luaWorld = new LuaTable();

        luaWorld.set("setBlock", new LibFunction() {
            @Override
            public Varargs invoke(Varargs args) {
                try {
                    LuaValue block = args.arg(0);
                    BlockState blockState = BlockStateParser.parseForBlock(BuiltInRegistries.BLOCK, block.checkjstring(), false).blockState();
                    int x = args.arg(1).checkint();
                    int y = args.arg(2).checkint();
                    int z = args.arg(3).checkint();
                    BlockPos blockPos = new BlockPos(x, y, z);
                    level.setBlock(
                            blockPos,
                            blockState,
                            UPDATE_ALL
                    );
                } catch (CommandSyntaxException e) {
                    e.printStackTrace();
                }
                return LuaValue.NIL;
            }
        });
        luaWorld.set("spawnEntity", new LibFunction() {
            @Override
            public Varargs invoke(Varargs args) {
                String entityName = args.arg(0).checkjstring();
                Entity entity = BuiltInRegistries.ENTITY_TYPE.getValue(Identifier.parse(entityName)).create(level, EntitySpawnReason.COMMAND);
                if (entity != null) {
                    double x = args.arg(1).checkdouble();
                    double y = args.arg(2).checkdouble();
                    double z = args.arg(3).checkdouble();
                    entity.setPos(x, y, z);
                    level.addFreshEntity(entity, CreatureSpawnEvent.SpawnReason.CUSTOM);
                }
                return LuaValue.NIL;
            }
        });
        return luaWorld;
    }

}
