package kr.moonseungjun.frontiersettlement.settlement;

import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.event.level.block.BreakBlockEvent;

import java.util.List;

/** Tier-visible public works only. Barracks is now the sole high-tier garrison authority. */
public final class SettlementTierInfrastructureService {
    private static final int PUBLIC_WORKS_INTERVAL_TICKS = 100;
    private static final int FRONTIER_TOWN_LAMP_SPACING = 16;
    private static final int DOMAIN_LAMP_SPACING = 8;
    private static final int CAPITAL_LAMP_SPACING = 6;
    private static final int LAMP_START_OFFSET = 8;
    private static final int PUBLIC_WORKS_UPDATE = 3;

    private SettlementTierInfrastructureService() {}

    public static void tick(MinecraftServer server, SettlementData data) {
        SettlementTier tier = SettlementTier.current(data);
        if (tier.ordinal() < SettlementTier.FRONTIER_TOWN.ordinal()) return;
        if (server.getTickCount() % PUBLIC_WORKS_INTERVAL_TICKS == 0) maintainRoadPublicWorks(server.overworld(), data, tier);
    }

    private static void maintainRoadPublicWorks(ServerLevel level, SettlementData data, SettlementTier tier) {
        int spacing = tier.ordinal() >= SettlementTier.FRONTIER_CAPITAL.ordinal()
                ? CAPITAL_LAMP_SPACING
                : tier.ordinal() >= SettlementTier.DOMAIN.ordinal() ? DOMAIN_LAMP_SPACING : FRONTIER_TOWN_LAMP_SPACING;
        int changed = 0;
        for (int roadIndex = 0; roadIndex < data.roads().size() && changed < 2; roadIndex++) {
            List<BlockPos> centers = data.roads().get(roadIndex).centers();
            if (centers.size() < 5) continue;
            for (int index = LAMP_START_OFFSET; index < centers.size() - 2 && changed < 2; index += spacing) {
                LampSite site = lampSite(level, data, centers, roadIndex, index, spacing);
                if (site == null) continue;
                if (!level.getBlockState(site.post()).is(Blocks.OAK_FENCE)) {
                    if (!canPlacePublicWork(level, site.post())) continue;
                    level.setBlock(site.post(), Blocks.OAK_FENCE.defaultBlockState(), PUBLIC_WORKS_UPDATE); changed++; continue;
                }
                if (!level.getBlockState(site.light()).is(Blocks.LANTERN) && canPlacePublicWork(level, site.light())) {
                    level.setBlock(site.light(), Blocks.LANTERN.defaultBlockState(), PUBLIC_WORKS_UPDATE); changed++;
                }
            }
        }
    }

    private static LampSite lampSite(ServerLevel level, SettlementData data, List<BlockPos> centers, int roadIndex, int index, int spacing) {
        BlockPos center = centers.get(index); int[] direction = directionAt(centers,index);
        if (direction[0] == 0 && direction[1] == 0) return null;
        int sequence = Math.max(0,(index-LAMP_START_OFFSET)/Math.max(1,spacing)); int side=((roadIndex+sequence)&1)==0?2:-2;
        LampSite preferred=candidate(level,data,center,direction,side); return preferred!=null?preferred:candidate(level,data,center,direction,-side);
    }

    private static LampSite candidate(ServerLevel level, SettlementData data, BlockPos center, int[] direction, int side) {
        BlockPos ground=center.offset(-direction[1]*side,0,direction[0]*side);
        if (!level.hasChunkAt(ground) || protectedXZ(data,ground) || level.getBlockEntity(ground)!=null) return null;
        BlockState gs=level.getBlockState(ground); if(!gs.getFluidState().isEmpty() || !isShoulderGround(gs)) return null;
        BlockPos post=ground.above(), light=ground.above(2);
        if(!level.hasChunkAt(post)||!level.hasChunkAt(light)) return null;
        if((!level.getBlockState(post).is(Blocks.OAK_FENCE)&&!canPlacePublicWork(level,post))||(!level.getBlockState(light).is(Blocks.LANTERN)&&!canPlacePublicWork(level,light))) return null;
        return new LampSite(post,light);
    }

    private static boolean canPlacePublicWork(ServerLevel level, BlockPos pos) { if(level.getBlockEntity(pos)!=null)return false; BlockState s=level.getBlockState(pos); return s.getFluidState().isEmpty()&&(s.isAir()||s.canBeReplaced()); }
    private static boolean isShoulderGround(BlockState s){ return s.is(Blocks.GRASS_BLOCK)||s.is(Blocks.DIRT)||s.is(Blocks.COARSE_DIRT)||s.is(Blocks.PODZOL)||s.is(Blocks.ROOTED_DIRT)||s.is(Blocks.STONE)||s.is(Blocks.COBBLESTONE)||s.is(Blocks.ANDESITE)||s.is(Blocks.DIORITE)||s.is(Blocks.GRANITE)||s.is(Blocks.TUFF)||s.is(Blocks.GRAVEL)||s.is(Blocks.DIRT_PATH); }
    private static boolean protectedXZ(SettlementData data, BlockPos pos){ if(Math.abs(pos.getX()-data.centerPos().getX())<=7&&Math.abs(pos.getZ()-data.centerPos().getZ())<=7)return true; for(BuildingRecord b:data.buildings())if(b.protectsXZ(pos,1))return true; for(OutpostRecord o:data.outposts())if(o.protectsXZ(pos,1))return true; return false; }
    private static int[] directionAt(List<BlockPos> c,int i){ BlockPos f=c.get(Math.max(0,i-1)),t=c.get(Math.min(c.size()-1,i+1)); int dx=Integer.signum(t.getX()-f.getX()),dz=Integer.signum(t.getZ()-f.getZ()); if(Math.abs(dx)+Math.abs(dz)==1)return new int[]{dx,dz}; BlockPos cur=c.get(i); if(i+1<c.size()){BlockPos n=c.get(i+1);dx=Integer.signum(n.getX()-cur.getX());dz=Integer.signum(n.getZ()-cur.getZ());if(Math.abs(dx)+Math.abs(dz)==1)return new int[]{dx,dz};} return new int[]{0,0}; }

    public static void onBreakBlock(BreakBlockEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return; MinecraftServer server=level.getServer(); if(level!=server.overworld())return;
        Block eventBlock=event.getState().getBlock();
        if(eventBlock!=Blocks.OAK_FENCE&&eventBlock!=Blocks.LANTERN)return;
        SettlementData data=SettlementData.get(server); if(!data.founded())return; BlockPos pos=event.getPos(); Block block=level.getBlockState(pos).getBlock();
        if(block!=Blocks.OAK_FENCE&&block!=Blocks.LANTERN)return;
        if(matchesLampPlan(level,data,pos,block,CAPITAL_LAMP_SPACING)
                ||matchesLampPlan(level,data,pos,block,DOMAIN_LAMP_SPACING)
                ||matchesLampPlan(level,data,pos,block,FRONTIER_TOWN_LAMP_SPACING)){event.setCanceled(true);event.setNotifyClient(true);}
    }
    private static boolean matchesLampPlan(ServerLevel level,SettlementData data,BlockPos pos,Block block,int spacing){ for(int r=0;r<data.roads().size();r++){List<BlockPos> c=data.roads().get(r).centers();for(int i=LAMP_START_OFFSET;i<c.size()-2;i+=spacing){LampSite s=lampSite(level,data,c,r,i,spacing);if(s==null)continue;if(pos.equals(s.post())&&block==Blocks.OAK_FENCE&&level.getBlockState(s.light()).is(Blocks.LANTERN))return true;if(pos.equals(s.light())&&block==Blocks.LANTERN&&level.getBlockState(s.post()).is(Blocks.OAK_FENCE))return true;}}return false; }
    private record LampSite(BlockPos post, BlockPos light) {}
}
