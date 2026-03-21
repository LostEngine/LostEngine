package dev.lost.engine.lua;

import dev.lost.annotations.NotNull;
import dev.lost.engine.lua.luatables.LuaBlockHit;
import dev.lost.engine.lua.luatables.LuaPlayer;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import org.bukkit.Bukkit;
import org.luaj.vm2.Globals;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.OneArgFunction;
import org.luaj.vm2.lib.VarArgFunction;
import org.luaj.vm2.lib.ZeroArgFunction;
import org.luaj.vm2.lib.jse.JsePlatform;
import org.luaj.vm2.luajc.LuaJC;

import javax.annotation.Nullable;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.Hashtable;
import java.util.UUID;

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
        server.set("broadcast", new OneArgFunction() {
            @Override
            public LuaValue call(LuaValue msg) {
                Bukkit.broadcast(
                        MiniMessage.miniMessage()
                                .deserialize(
                                        msg.checkjstring()
                                )
                );
                return LuaValue.NIL;
            }
        });
        GLOBALS.set("server", server);
        GLOBALS.set("os", LuaValue.NIL);
        GLOBALS.set("package", LuaValue.NIL);
        GLOBALS.set("io", LuaValue.NIL);
        GLOBALS.set("luajava", LuaValue.NIL);
        GLOBALS.set("dofile", LuaValue.NIL);
        GLOBALS.set("loadfile", LuaValue.NIL);
        GLOBALS.set("load", LuaValue.NIL);
        GLOBALS.set("require", LuaValue.NIL);
        GLOBALS.set("collectgarbage", LuaValue.NIL);
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

    public static void onClick(@Nullable LuaValue luaValue, @NotNull ServerPlayer player) {
        if (luaValue == null) return;
        LuaValue function = luaValue.get("onClick");
        if (function != null) {
            LuaValue luaBlockHit = LuaValue.NIL;
            getBlockHit:
            {
                HitResult hit = player.pick(player.blockInteractionRange(), 0, false);
                if (!(hit instanceof BlockHitResult blockHit) || blockHit.getType() != HitResult.Type.BLOCK)
                    break getBlockHit;
                luaBlockHit = LuaBlockHit.getLuaBlockHit(blockHit);
            }

            function.invoke(new LuaValue[]{
                    LuaPlayer.getLuaPlayer(player),
                    luaBlockHit
            });
        }
    }


}
