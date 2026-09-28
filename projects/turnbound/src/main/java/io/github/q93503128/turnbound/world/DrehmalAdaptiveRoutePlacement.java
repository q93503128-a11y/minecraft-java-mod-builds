package io.github.q93503128.turnbound.world;

import io.github.q93503128.turnbound.Turnbound;
import io.github.q93503128.turnbound.session.BattleSessionManager;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import java.util.*;

final class DrehmalAdaptiveRoutePlacement {
    private static final Map<ServerLevel, Snapshot> CACHE = new IdentityHashMap<>();

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
            var s=p.siteSeeds().getFirst();
            return new DrehmalFirstRouteCatalog.Position(s.x(),level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,s.x(),s.z()),s.z());
        }
        for(var s:p.siteSeeds()){
            var r=nearestStanding(level,s.x(),s.z(),p.searchRadius());
            if(r!=null) return r;
        }
        return null;
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
        for(var s:p.patrolSeeds()){
            var point=nearestStanding(level,s.x(),s.z(),Math.min(4,p.searchRadius()));
            if(point==null||DrehmalRouteZoneRules.insideSafetyZone(sites,point.x()+0.5D,point.z()+0.5D)) continue;
            if(out.stream().noneMatch(e->e.x()==point.x()&&e.z()==point.z())) out.add(point);
        }
        return List.copyOf(out);
    }

    private static DrehmalFirstRouteCatalog.ArenaCandidate nearestArena(ServerPlayer player,ServerLevel level,DrehmalMapPlacementCatalog.ArenaSeed seed,int radius){
        for(int[] o:offsets(radius)){
            int x=seed.x()+o[0],z=seed.z()+o[1],y=level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,x,z);
            Vec3 center=new Vec3(x+0.5D,y,z+0.5D);
            if(BattleSessionManager.surveyArenaOpen(player,center,seed.yaw(),4))
                return new DrehmalFirstRouteCatalog.ArenaCandidate(new DrehmalFirstRouteCatalog.Position(x,y,z),seed.yaw());
        }
        return null;
    }

    private static DrehmalFirstRouteCatalog.Position nearestStanding(ServerLevel level,int sx,int sz,int radius){
        for(int[] o:offsets(radius)){
            int x=sx+o[0],z=sz+o[1],y=level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,x,z);
            BlockPos feet=new BlockPos(x,y,z);
            if(standing(level,feet)) return new DrehmalFirstRouteCatalog.Position(x,y,z);
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

    private static List<int[]> offsets(int radius){
        List<int[]> out=new ArrayList<>();
        for(int dz=-radius;dz<=radius;dz++) for(int dx=-radius;dx<=radius;dx++) if(dx*dx+dz*dz<=radius*radius) out.add(new int[]{dx,dz});
        out.sort(Comparator.comparingInt(v->v[0]*v[0]+v[1]*v[1]));
        return out;
    }
}
