package dev.lost.engine.lua.luatables;

import dev.lost.annotations.NotNull;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.Varargs;
import org.luaj.vm2.lib.LibFunction;
import org.luaj.vm2.lib.ZeroArgFunction;

public class LuaBlockHit {

    public static @NotNull LuaTable getLuaBlockHit(@NotNull BlockHitResult blockHit) {
        LuaTable luaBlockHit = new LuaTable();
        luaBlockHit.set("getLocation", new LibFunction() {
            @Override
            public Varargs invoke(Varargs args) {
                Vec3 location = blockHit.getLocation();
                return LuaValue.varargsOf(new LuaValue[]{
                        LuaValue.valueOf(location.x),
                        LuaValue.valueOf(location.y),
                        LuaValue.valueOf(location.z)
                });
            }
        });
        luaBlockHit.set("getBlockPos", new LibFunction() {
            @Override
            public Varargs invoke(Varargs args) {
                BlockPos blockPos = blockHit.getBlockPos();
                return LuaValue.varargsOf(new LuaValue[]{
                        LuaValue.valueOf(blockPos.getX()),
                        LuaValue.valueOf(blockPos.getY()),
                        LuaValue.valueOf(blockPos.getZ())
                });
            }
        });
        luaBlockHit.set("getDirection", new ZeroArgFunction() {
            @Override
            public LuaValue call() {
                return LuaValue.valueOf(blockHit.getDirection().getName());
            }
        });
        return luaBlockHit;
    }

}
