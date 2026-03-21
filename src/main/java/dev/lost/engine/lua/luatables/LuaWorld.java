package dev.lost.engine.lua.luatables;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.lost.annotations.NotNull;
import net.minecraft.commands.arguments.blocks.BlockStateParser;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.item.BoneMealItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.Varargs;
import org.luaj.vm2.lib.LibFunction;

import static net.minecraft.world.level.block.Block.UPDATE_ALL;

public class LuaWorld {
    static @NotNull LuaTable getLuaWorld(ServerLevel level) {
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
        luaWorld.set("applyBonemeal", new LibFunction() {
            @Override
            public Varargs invoke(Varargs args) {
                int x = args.arg(1).checkint();
                int y = args.arg(2).checkint();
                int z = args.arg(3).checkint();
                BlockPos blockPos = new BlockPos(x, y, z);
                BoneMealItem.applyBonemeal(
                        new UseOnContext(
                                level,
                                null,
                                InteractionHand.MAIN_HAND,
                                ItemStack.EMPTY,
                                BlockHitResult.miss(blockPos.getCenter(), Direction.UP, blockPos)
                        )
                );
                return LuaValue.NIL;
            }
        });

        return luaWorld;
    }
}
