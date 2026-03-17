package dev.lost.engine.lua;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.lost.annotations.NotNull;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
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
import org.luaj.vm2.lib.*;
import org.luaj.vm2.lib.jse.JsePlatform;
import org.luaj.vm2.luajc.LuaJC;

import javax.annotation.Nullable;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.Hashtable;
import java.util.UUID;

import static net.minecraft.world.level.block.Block.UPDATE_ALL;

public class LuaScripts {

    private static final Globals GLOBALS = JsePlatform.standardGlobals();
    private static final ByteClassLoader LOADER = new ByteClassLoader(LuaScripts.class.getClassLoader());
    private static LuaJC compiler;

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
        LuaScripts.compiler = LuaJC.instance;
    }

    public static LuaValue loadScript(String script) throws IOException, NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
        Hashtable<?, ?> classes = compiler.compileAll(
                new java.io.StringReader(script),
                "script",
                UUID.randomUUID().toString(),
                GLOBALS,
                true
        );
        Object2ObjectOpenHashMap<String, Class<?>> loadedClasses = new Object2ObjectOpenHashMap<>();

        for (Object key : classes.keySet()) {
            String className = (String) key;
            byte[] bytecode = (byte[]) classes.get(key);

            Class<?> clazz = LOADER.define(className, bytecode);
            loadedClasses.put(className, clazz);
        }

        VarArgFunction chunk =
                (VarArgFunction) loadedClasses.get("script")
                        .getDeclaredConstructor()
                        .newInstance();
        chunk.initupvalue1(GLOBALS);
        chunk.call();
        return GLOBALS;
    }

    public static void onClick(@Nullable LuaValue luaValue, @NotNull ServerPlayer player, double x, double y, double z) {
        if (luaValue == null) return;
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
                    LuaValue block = args.arg(1);
                    BlockState blockState = BlockStateParser.parseForBlock(BuiltInRegistries.BLOCK, block.checkjstring(), false).blockState();
                    int x = args.arg(2).checkint();
                    int y = args.arg(3).checkint();
                    int z = args.arg(4).checkint();
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
                String entityName = args.arg(1).checkjstring();
                Entity entity = BuiltInRegistries.ENTITY_TYPE.getValue(Identifier.parse(entityName)).create(level, EntitySpawnReason.COMMAND);
                if (entity != null) {
                    double x = args.arg(2).checkdouble();
                    double y = args.arg(3).checkdouble();
                    double z = args.arg(4).checkdouble();
                    entity.setPos(x, y, z);
                    level.addFreshEntity(entity, CreatureSpawnEvent.SpawnReason.CUSTOM);
                }
                return LuaValue.NIL;
            }
        });
        return luaWorld;
    }

}
