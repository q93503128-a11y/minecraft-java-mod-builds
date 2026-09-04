package io.github.q93503128.turnbound.world;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import java.util.Map;
import java.util.WeakHashMap;

/** Staged Radia foundation build for a normal generated world. */
public final class AsterMarchFoundationBuilder {
    private static final int MIN_X=AsterMarchRegionCatalog.RADIA.minX(),MAX_X=AsterMarchRegionCatalog.RADIA.maxX(),MIN_Z=AsterMarchRegionCatalog.RADIA.minZ(),MAX_Z=AsterMarchRegionCatalog.RADIA.maxZ(),GROUND_Y=65,COLUMNS_PER_TICK=600;
    private static final BlockPos MARKER_A=new BlockPos(-127,58,-111),MARKER_B=new BlockPos(-126,58,-111),MARKER_C=new BlockPos(-125,58,-111);
    private static final Map<ServerLevel,State> STATES=new WeakHashMap<>();
    private AsterMarchFoundationBuilder(){}
    public static boolean ready(ServerLevel l){return l.getBlockState(MARKER_A).is(Blocks.LODESTONE)&&l.getBlockState(MARKER_B).is(Blocks.EMERALD_BLOCK)&&l.getBlockState(MARKER_C).is(Blocks.GOLD_BLOCK);}
    public static boolean step(ServerLevel l,ServerPlayer p){if(ready(l))return true;State s=STATES.computeIfAbsent(l,k->new State());holdPlayer(p);int total=(MAX_X-MIN_X+1)*(MAX_Z-MIN_Z+1),n=0;while(n<COLUMNS_PER_TICK&&s.index<total){int w=MAX_X-MIN_X+1,x=MIN_X+s.index%w,z=MIN_Z+s.index/w;prepareColumn(l,x,z);s.index++;n++;}int pc=Math.min(99,(int)Math.floor(s.index*100.0/total));if(pc!=s.lastPercent&&(pc==0||pc>=s.lastPercent+4)){s.lastPercent=pc;FieldNetwork.sync(p,FieldUiSnapshot.loading("라디아 지형 정리",pc));}if(s.index<total)return false;l.setBlock(MARKER_A,Blocks.LODESTONE.defaultBlockState(),2);l.setBlock(MARKER_B,Blocks.EMERALD_BLOCK.defaultBlockState(),2);l.setBlock(MARKER_C,Blocks.GOLD_BLOCK.defaultBlockState(),2);STATES.remove(l);FieldNetwork.sync(p,FieldUiSnapshot.loading("아스테르 변경 배치",100));return true;}
    private static void prepareColumn(ServerLevel l,int x,int z){int top=Math.max(GROUND_Y,l.getHeight(Heightmap.Types.WORLD_SURFACE,x,z));l.setBlock(new BlockPos(x,62,z),Blocks.DIRT.defaultBlockState(),2);l.setBlock(new BlockPos(x,63,z),Blocks.DIRT.defaultBlockState(),2);l.setBlock(new BlockPos(x,64,z),Blocks.DIRT.defaultBlockState(),2);l.setBlock(new BlockPos(x,65,z),Blocks.GRASS_BLOCK.defaultBlockState(),2);for(int y=66;y<=top;y++){BlockPos q=new BlockPos(x,y,z);if(!l.getBlockState(q).isAir())l.setBlock(q,Blocks.AIR.defaultBlockState(),2);}}
    private static void holdPlayer(ServerPlayer p){p.setNoGravity(true);p.setDeltaMovement(Vec3.ZERO);p.fallDistance=0;p.setPos(.5,250,20.5);}
    private static final class State{private int index;private int lastPercent=-4;}
}
