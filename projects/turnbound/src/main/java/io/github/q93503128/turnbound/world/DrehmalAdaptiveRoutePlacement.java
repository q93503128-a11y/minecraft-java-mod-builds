package io.github.q93503128.turnbound.world;

import io.github.q93503128.turnbound.Turnbound;
import io.github.q93503128.turnbound.session.BattleSessionManager;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.entity.npc.villager.AbstractVillager;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import java.util.*;

final class DrehmalAdaptiveRoutePlacement {
    private static final Map<ServerLevel, Snapshot> CACHE = new IdentityHashMap<>();
    private record ScoredPosition(DrehmalFirstRouteCatalog.Position position, double score) {}

    record Snapshot(Map<String,DrehmalFirstRouteCatalog.Site> sites,
                    Map<String,DrehmalFirstRouteCatalog.Footprint> footprints,
                    Map<String,DrehmalFirstRouteCatalog.Patrol> patrols,
                    List<DrehmalFirstRouteCatalog.EncounterSlot> encounters) {
        Snapshot {
            sites=Map.copyOf(sites); footprints=Map.copyOf(footprints);
            patrols=Map.copyOf(patrols); encounters=List.copyOf(encounters);
        }
    }

    private DrehmalAdaptiveRoutePlacement() {}

    static synchronized Snapshot snapshot(ServerPlayer player) {
        if (player == null || !(player.level() instanceof ServerLevel level)) return new Snapshot(Map.of(),Map.of(),Map.of(),List.of());
        return CACHE.computeIfAbsent(level, ignored -> resolve(player, level));
    }
    static List<DrehmalFirstRouteCatalog.Site> productionSites(ServerPlayer p){ return List.copyOf(snapshot(p).sites().values()); }
    static synchronized List<DrehmalFirstRouteCatalog.Site> productionSites(ServerLevel level){
        Snapshot snapshot=CACHE.get(level);
        return snapshot==null?List.of():List.copyOf(snapshot.sites().values());
    }
    static List<DrehmalFirstRouteCatalog.EncounterSlot> productionEncounters(ServerPlayer p){ return snapshot(p).encounters(); }
    static DrehmalFirstRouteCatalog.Site site(ServerPlayer p,String id){ return snapshot(p).sites().get(id); }
    static DrehmalFirstRouteCatalog.Footprint footprint(ServerPlayer p,String id){ return snapshot(p).footprints().get(id); }
    static DrehmalFirstRouteCatalog.Patrol patrol(ServerPlayer p,String id){ return snapshot(p).patrols().get(id); }
    static synchronized void clear(){ CACHE.clear(); }

    private static Snapshot resolve(ServerPlayer player, ServerLevel level) {
        Map<String,DrehmalFirstRouteCatalog.Site> sites=new LinkedHashMap<>();
        Map<String,DrehmalFirstRouteCatalog.Footprint> footprints=new LinkedHashMap<>();
        Map<String,DrehmalFirstRouteCatalog.Patrol> patrols=new LinkedHashMap<>();

        for(var s:DrehmalFirstRouteCatalog.route().sites()) if(s.productionEnabled()&&s.verifiedIn26_2()&&s.runtimePosition()!=null) sites.put(s.locator(),s);
        for(var f:DrehmalFirstRouteCatalog.route().footprints()) if(f.productionEnabled()&&f.verifiedIn26_2()&&f.candidates().size()>=2) footprints.put(f.locator(),f);
        for(var p:DrehmalFirstRouteCatalog.route().patrols()) if(p.productionEnabled()&&p.verifiedIn26_2()&&p.points().size()>=2) patrols.put(p.locator(),p);

        for(var p:DrehmalMapPlacementCatalog.plan().placements()){
            var authored=DrehmalFirstRouteCatalog.site(p.siteLocator());
            if(authored==null||sites.containsKey(authored.locator())) continue;
            var pos=resolveSite(level,p);
            if(pos==null) continue;
            sites.put(authored.locator(),new DrehmalFirstRouteCatalog.Site(authored.locator(),authored.kind(),authored.surveySeedAnchor(),
                    authored.playerLabel(),pos,authored.safetyRadius(),authored.encounterRadius(),true,true));
        }

        for(var authored:DrehmalFirstRouteCatalog.route().footprints()){
            if(footprints.containsKey(authored.locator())) continue;
            var p=DrehmalMapPlacementCatalog.placement(authored.siteLocator());
            if(p==null||!sites.containsKey(authored.siteLocator())) continue;
            var candidates=resolveArenas(player,level,p);
            if(candidates.size()<2) continue;
            footprints.put(authored.locator(),new DrehmalFirstRouteCatalog.Footprint(authored.locator(),authored.siteLocator(),
                    authored.radius(),authored.allySlots(),authored.enemySlots(),candidates,true,true));
        }

        for(var authored:DrehmalFirstRouteCatalog.route().patrols()){
            if(patrols.containsKey(authored.locator())) continue;
            var p=DrehmalMapPlacementCatalog.placement(authored.surveySeedSite());
            if(p==null||!sites.containsKey(authored.surveySeedSite())) continue;
            var points=resolvePatrol(level,p,List.copyOf(sites.values()));
            if(points.size()<2) continue;
            patrols.put(authored.locator(),new DrehmalFirstRouteCatalog.Patrol(authored.locator(),authored.surveySeedSite(),
                    authored.mode(),authored.dwellMinTicks(),authored.dwellMaxTicks(),points,true,true));
        }

        List<DrehmalFirstRouteCatalog.EncounterSlot> encounters=new ArrayList<>();
        for(var authored:DrehmalFirstRouteCatalog.route().encounters()){
            var site=sites.get(authored.siteLocator());
            var footprint=footprints.get(authored.footprintLocator());
            var patrol=authored.patrolLocator().isBlank()?null:patrols.get(authored.patrolLocator());
            if(site==null||footprint==null||(!authored.patrolLocator().isBlank()&&patrol==null)) continue;
            var runtime=new DrehmalFirstRouteCatalog.EncounterSlot(authored.locator(),authored.siteLocator(),authored.tier(),
                    authored.footprintLocator(),authored.patrolLocator(),authored.combatEncounterId(),authored.playerLabel(),
                    authored.fieldVisibleCount(),true,true);
            if(DrehmalEncounterActivationRules.ready(runtime,site,footprint,patrol)) encounters.add(runtime);
        }

        Turnbound.LOGGER.info("TURNBOUND resolved Capital Valley map zones: {} sites, {} footprints, {} patrols, {} encounters",
                sites.size(),footprints.size(),patrols.size(),encounters.size());
        return new Snapshot(sites,footprints,patrols,encounters);
    }

    private static DrehmalFirstRouteCatalog.Position resolveSite(ServerLevel level,DrehmalMapPlacementCatalog.Placement p){
        if(!p.strictSite()){
            var seed=p.siteSeeds().getFirst();
            return new DrehmalFirstRouteCatalog.Position(
                    seed.x(),
                    level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,seed.x(),seed.z()),
                    seed.z());
        }

        var authored=DrehmalFirstRouteCatalog.site(p.siteLocator());
        if(authored==null)return null;
        var zone=DrehmalMapPlacementCatalog.zone(p.zoneId());
        ScoredPosition best=null;

        for(var seed:p.siteSeeds()){
            for(int[] offset:offsets(p.searchRadius())){
                int x=seed.x()+offset[0],z=seed.z()+offset[1];
                int y=level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,x,z);
                BlockPos feet=new BlockPos(x,y,z);
                if(!standing(level,feet)||!sourceContentClear(level,x,y,z,2.75D))continue;

                double roadDistance=corridorDistance(zone,x+0.5D,z+0.5D);
                if(!DrehmalRoutePlacementRules.acceptableRoadDistance(authored.kind(),roadDistance))continue;

                double seedDistanceSq=offset[0]*offset[0]+offset[1]*offset[1];
                double score=DrehmalRoutePlacementRules.score(authored.kind(),seedDistanceSq,roadDistance);
                var candidate=new ScoredPosition(new DrehmalFirstRouteCatalog.Position(x,y,z),score);
                if(best==null||candidate.score()<best.score()
                        ||(candidate.score()==best.score()&&positionTieBreak(candidate.position(),best.position())<0)){
                    best=candidate;
                }
            }
        }
        return best==null?null:best.position();
    }

    private static List<DrehmalFirstRouteCatalog.ArenaCandidate> resolveArenas(ServerPlayer player,ServerLevel level,DrehmalMapPlacementCatalog.Placement p){
        List<DrehmalFirstRouteCatalog.ArenaCandidate> out=new ArrayList<>();
        for(var s:p.arenaSeeds()){
            var c=nearestArena(player,level,s,Math.min(6,p.searchRadius()));
            if(c==null) continue;
            boolean dup=out.stream().anyMatch(e->e.center().x()==c.center().x()&&e.center().z()==c.center().z()&&Math.abs(e.yaw()-c.yaw())<0.1F);
            if(!dup) out.add(c);
            if(out.size()>=4) break;
        }
        return List.copyOf(out);
    }

    private static List<DrehmalFirstRouteCatalog.Position> resolvePatrol(ServerLevel level,DrehmalMapPlacementCatalog.Placement p,List<DrehmalFirstRouteCatalog.Site> sites){
        List<DrehmalFirstRouteCatalog.Position> out=new ArrayList<>();
        for(var seed:p.patrolSeeds()){
            DrehmalFirstRouteCatalog.Position point=null;
            for(int[] offset:offsets(Math.min(4,p.searchRadius()))){
                int x=seed.x()+offset[0],z=seed.z()+offset[1];
                int y=level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,x,z);
                BlockPos feet=new BlockPos(x,y,z);
                if(!standing(level,feet)||!sourceContentClear(level,x,y,z,2.5D))continue;
                point=new DrehmalFirstRouteCatalog.Position(x,y,z);
                break;
            }
            if(point==null||DrehmalRouteZoneRules.insideSafetyZone(sites,point.x()+0.5D,point.z()+0.5D)) continue;
            boolean duplicate=false;
            for(var existing:out){
                if(existing.x()==point.x()&&existing.z()==point.z()){
                    duplicate=true;
                    break;
                }
            }
            if(!duplicate)out.add(point);
        }
        return List.copyOf(out);
    }

    private static DrehmalFirstRouteCatalog.ArenaCandidate nearestArena(ServerPlayer player,ServerLevel level,DrehmalMapPlacementCatalog.ArenaSeed seed,int radius){
        for(int[] o:offsets(radius)){
            int x=seed.x()+o[0],z=seed.z()+o[1],y=level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,x,z);
            if(!sourceContentClear(level,x,y,z,5.5D))continue;
            Vec3 center=new Vec3(x+0.5D,y,z+0.5D);
            if(BattleSessionManager.surveyArenaOpen(player,center,seed.yaw(),4))
                return new DrehmalFirstRouteCatalog.ArenaCandidate(new DrehmalFirstRouteCatalog.Position(x,y,z),seed.yaw());
        }
        return null;
    }

    private static boolean standing(ServerLevel level,BlockPos feet){
        BlockPos below=feet.below();
        if(level.getBlockState(below).isAir()||level.getBlockState(below).is(BlockTags.LEAVES)||!level.getFluidState(below).isEmpty()) return false;
        for(int dy=0;dy<=2;dy++){
            BlockPos p=feet.above(dy);
            if(!level.getBlockState(p).getCollisionShape(level,p).isEmpty()||!level.getFluidState(p).isEmpty()) return false;
        }
        int min=Integer.MAX_VALUE,max=Integer.MIN_VALUE;
        for(int dx=-1;dx<=1;dx++) for(int dz=-1;dz<=1;dz++){
            int y=level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,feet.getX()+dx,feet.getZ()+dz);
            min=Math.min(min,y);max=Math.max(max,y);
        }
        return max-min<=2;
    }

    static boolean sourceContentClear(ServerLevel level,int x,int y,int z,double horizontalRadius){
        BlockPos feet=new BlockPos(x,y,z);
        int radius=Math.max(1,(int)Math.ceil(horizontalRadius));
        for(int dx=-radius;dx<=radius;dx++)for(int dz=-radius;dz<=radius;dz++){
            if(dx*dx+dz*dz>horizontalRadius*horizontalRadius)continue;
            for(int dy=-2;dy<=3;dy++){
                if(level.getBlockEntity(feet.offset(dx,dy,dz))!=null)return false;
            }
        }

        AABB area=new AABB(
                x-horizontalRadius,y-2.0D,z-horizontalRadius,
                x+1.0D+horizontalRadius,y+4.0D,z+1.0D+horizontalRadius);
        if (!level.getEntitiesOfClass(AbstractVillager.class, area).isEmpty()) return false;
        if(!level.getEntitiesOfClass(ItemFrame.class,area).isEmpty())return false;
        if(!level.getEntitiesOfClass(ArmorStand.class,area).isEmpty())return false;
        return true;
    }

    static double corridorDistance(DrehmalMapPlacementCatalog.Zone zone,double x,double z){
        if(zone==null||zone.corridor().isEmpty())return Double.POSITIVE_INFINITY;
        if(zone.corridor().size()==1){
            var point=zone.corridor().getFirst();
            return Math.sqrt(distanceSq(x,z,point.x()+0.5D,point.z()+0.5D));
        }
        double best=Double.POSITIVE_INFINITY;
        for(int i=1;i<zone.corridor().size();i++){
            var a=zone.corridor().get(i-1);
            var b=zone.corridor().get(i);
            best=Math.min(best,distanceToSegment(
                    x,z,a.x()+0.5D,a.z()+0.5D,b.x()+0.5D,b.z()+0.5D));
        }
        return best;
    }

    private static double distanceToSegment(double px,double pz,double ax,double az,double bx,double bz){
        double vx=bx-ax,vz=bz-az;
        double lengthSq=vx*vx+vz*vz;
        if(lengthSq<=0.000001D)return Math.sqrt(distanceSq(px,pz,ax,az));
        double t=((px-ax)*vx+(pz-az)*vz)/lengthSq;
        t=Math.max(0.0D,Math.min(1.0D,t));
        double qx=ax+t*vx,qz=az+t*vz;
        return Math.sqrt(distanceSq(px,pz,qx,qz));
    }

    private static double distanceSq(double ax,double az,double bx,double bz){
        double dx=ax-bx,dz=az-bz;
        return dx*dx+dz*dz;
    }

    private static int positionTieBreak(DrehmalFirstRouteCatalog.Position left,DrehmalFirstRouteCatalog.Position right){
        int x=Integer.compare(left.x(),right.x());
        if(x!=0)return x;
        int z=Integer.compare(left.z(),right.z());
        return z!=0?z:Integer.compare(left.y(),right.y());
    }

    private static List<int[]> offsets(int radius){
        List<int[]> out=new ArrayList<>();
        for(int dz=-radius;dz<=radius;dz++) for(int dx=-radius;dx<=radius;dx++) if(dx*dx+dz*dz<=radius*radius) out.add(new int[]{dx,dz});
        out.sort(Comparator.comparingInt(v->v[0]*v[0]+v[1]*v[1]));
        return out;
    }
}
