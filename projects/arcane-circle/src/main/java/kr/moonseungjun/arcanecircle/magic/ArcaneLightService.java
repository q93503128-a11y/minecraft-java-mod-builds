package kr.moonseungjun.arcanecircle.magic;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LightBlock;
import net.minecraft.world.level.block.state.BlockState;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Temporary server-authoritative illumination for the Light spell. */
public final class ArcaneLightService {
    private static final int LIGHT_LEVEL=15;
    private static final int REFRESH_INTERVAL=4;
    private static final int[][] OFFSETS={{0,1,0},{3,1,0},{-3,1,0},{0,1,3},{0,1,-3}};
    private static final Map<UUID,LightState> ACTIVE=new HashMap<>();

    private ArcaneLightService(){}

    public static void illuminate(ServerPlayer player,int durationTicks){
        ServerLevel level=(ServerLevel)player.level();long now=level.getGameTime();
        LightState state=ACTIVE.computeIfAbsent(player.getUUID(),ignored->new LightState());
        state.untilTick=Math.max(state.untilTick,now+Math.max(20,durationTicks));refresh(player,state);
    }

    public static void tick(ServerPlayer player){
        LightState state=ACTIVE.get(player.getUUID());if(state==null)return;ServerLevel level=(ServerLevel)player.level();
        if(level.getGameTime()>=state.untilTick){clear(player,state);ACTIVE.remove(player.getUUID());return;}
        if(player.tickCount%REFRESH_INTERVAL==0)refresh(player,state);
    }

    public static void clear(ServerPlayer player){LightState state=ACTIVE.remove(player.getUUID());if(state!=null)clear(player,state);}

    public static void clearAll(MinecraftServer server){for(LightState state:ACTIVE.values())clear(server,state);ACTIVE.clear();}

    private static void refresh(ServerPlayer player,LightState state){
        ServerLevel level=(ServerLevel)player.level();MinecraftServer server=level.getServer();
        if(state.dimension!=null&&!state.dimension.equals(level.dimension())){clear(server,state);state.positions.clear();}
        state.dimension=level.dimension();BlockPos base=player.blockPosition();Set<BlockPos> desired=new HashSet<>();
        for(int[] off:OFFSETS)desired.add(base.offset(off[0],off[1],off[2]));
        Set<BlockPos> old=new HashSet<>(state.positions);
        for(BlockPos pos:old)if(!desired.contains(pos)){removeOwned(level,pos);state.positions.remove(pos);}
        BlockState light=Blocks.LIGHT.defaultBlockState().setValue(LightBlock.LEVEL,LIGHT_LEVEL);
        for(BlockPos pos:desired){
            if(state.positions.contains(pos)&&level.getBlockState(pos).is(Blocks.LIGHT))continue;
            if(level.getBlockState(pos).isAir()&&level.setBlock(pos,light,3))state.positions.add(pos.immutable());
        }
    }

    private static void clear(ServerPlayer player,LightState state){clear(((ServerLevel)player.level()).getServer(),state);state.positions.clear();state.dimension=null;}
    private static void clear(MinecraftServer server,LightState state){
        if(state.dimension==null)return;ServerLevel level=server.getLevel(state.dimension);if(level==null)return;
        for(BlockPos pos:state.positions)removeOwned(level,pos);
    }
    private static void removeOwned(ServerLevel level,BlockPos pos){if(level.getBlockState(pos).is(Blocks.LIGHT))level.removeBlock(pos,false);}

    private static final class LightState{
        long untilTick;ResourceKey<Level> dimension;final Set<BlockPos> positions=new HashSet<>();
    }
}
