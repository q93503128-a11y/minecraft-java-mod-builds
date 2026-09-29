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
    private record WorldBossResolution(
            DrehmalFirstRouteCatalog.Site site,
            DrehmalFirstRouteCatalog.Footprint footprint) {}

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
        ensureOpeningTutorialSite(level,sites);

        for(var authored:DrehmalFirstRouteCatalog.route().footprints()){
            if(footprints.containsKey(authored.locator())) continue;
            var p=DrehmalMapPlacementCatalog.placement(authored.siteLocator());
            if(p==null||!sites.containsKey(authored.siteLocator())) continue;
            var candidates=resolveArenas(player,level,p);
            if(candidates.size()<2) continue;
            footprints.put(authored.locator(),new DrehmalFirstRouteCatalog.Footprint(authored.locator(),authored.siteLocator(),
                    authored.radius(),authored.allySlots(),authored.enemySlots(),candidates,true,true));
        }
        ensureOpeningTutorialFootprint(player,level,sites,footprints);

        WorldBossResolution worldBoss = resolveOptionalWorldBoss(player, level, List.copyOf(sites.values()));
        if (worldBoss != null) {
            sites.put(worldBoss.site().locator(), worldBoss.site());
            footprints.put(worldBoss.footprint().locator(), worldBoss.footprint());
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

    private static void ensureOpeningTutorialSite(
            ServerLevel level,
            Map<String,DrehmalFirstRouteCatalog.Site> sites
    ){
        if(sites.containsKey(DrabyelOpeningTutorial.ENCOUNTER_SITE))return;
        var authored=DrehmalFirstRouteCatalog.site(DrabyelOpeningTutorial.ENCOUNTER_SITE);
        var placement=DrehmalMapPlacementCatalog.placement(DrabyelOpeningTutorial.ENCOUNTER_SITE);
        if(authored==null||placement==null)return;
        var zone=DrehmalMapPlacementCatalog.zone(placement.zoneId());
        List<DrehmalMapPlacementCatalog.Seed> seeds=new ArrayList<>(placement.siteSeeds());
        seeds.addAll(placement.patrolSeeds());
        int searchRadius=Math.max(12,placement.searchRadius()+6);
        for(var seed:seeds){
            for(int[] offset:offsets(searchRadius)){
                int x=seed.x()+offset[0],z=seed.z()+offset[1];
                int y=level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,x,z);
                BlockPos feet=new BlockPos(x,y,z);
                if(!standing(level,feet)||!sourceContentClear(level,x,y,z,3.5D))continue;
                if(DrehmalRouteZoneRules.insideSafetyZone(List.copyOf(sites.values()),x+0.5D,z+0.5D))continue;
                double roadDistance=DrehmalRoutePlacementRules.corridorDistance(zone,x+0.5D,z+0.5D);
                if(!DrehmalRoutePlacementRules.acceptableRoadDistance(authored.kind(),roadDistance))continue;
                var position=new DrehmalFirstRouteCatalog.Position(x,y,z);
                sites.put(authored.locator(),new DrehmalFirstRouteCatalog.Site(
                        authored.locator(),authored.kind(),authored.surveySeedAnchor(),authored.playerLabel(),
                        position,authored.safetyRadius(),authored.encounterRadius(),true,true));
                Turnbound.LOGGER.info("TURNBOUND opening patrol used safe source-route fallback at {}, {}, {}",x,y,z);
                return;
            }
        }
        Turnbound.LOGGER.warn("TURNBOUND opening patrol remained dormant: no safe visible-actor site passed live checks");
    }

    private static void ensureOpeningTutorialFootprint(
            ServerPlayer player,
            ServerLevel level,
            Map<String,DrehmalFirstRouteCatalog.Site> sites,
            Map<String,DrehmalFirstRouteCatalog.Footprint> footprints
    ){
        if(footprints.containsKey(DrabyelOpeningTutorial.FOOTPRINT_ID))return;
        var authored=DrehmalFirstRouteCatalog.footprint(DrabyelOpeningTutorial.FOOTPRINT_ID);
        var site=sites.get(DrabyelOpeningTutorial.ENCOUNTER_SITE);
        var placement=DrehmalMapPlacementCatalog.placement(DrabyelOpeningTutorial.ENCOUNTER_SITE);
        if(authored==null||site==null||site.runtimePosition()==null||placement==null)return;

        List<DrehmalFirstRouteCatalog.ArenaCandidate> candidates=new ArrayList<>();
        for(var seed:placement.arenaSeeds()){
            var candidate=openingArena(player,level,seed.x(),seed.z(),seed.yaw());
            if(candidate==null)continue;
            boolean duplicate=candidates.stream().anyMatch(existing->
                    existing.center().x()==candidate.center().x()
                            && existing.center().z()==candidate.center().z());
            if(!duplicate)candidates.add(candidate);
            if(candidates.size()>=4)break;
        }

        if(candidates.size()<2){
            var home=site.runtimePosition();
            int[][] offsets={{20,0},{-20,0},{0,20},{0,-20},{16,16},{-16,16},{16,-16},{-16,-16}};
            float[] yaws={0.0F,90.0F,180.0F,270.0F};
            for(int i=0;i<offsets.length&&candidates.size()<4;i++){
                var candidate=openingArena(player,level,home.x()+offsets[i][0],home.z()+offsets[i][1],yaws[i%yaws.length]);
                if(candidate==null)continue;
                boolean duplicate=candidates.stream().anyMatch(existing->
                        existing.center().x()==candidate.center().x()
                                && existing.center().z()==candidate.center().z());
                if(!duplicate)candidates.add(candidate);
            }
        }

        if(candidates.size()<2){
            Turnbound.LOGGER.warn("TURNBOUND opening patrol footprint fallback still found only {} arena(s)",candidates.size());
            return;
        }
        footprints.put(authored.locator(),new DrehmalFirstRouteCatalog.Footprint(
                authored.locator(),authored.siteLocator(),authored.radius(),
                authored.allySlots(),2,List.copyOf(candidates),true,true));
        Turnbound.LOGGER.info("TURNBOUND opening patrol used {} fallback battle arenas",candidates.size());
    }

    private static DrehmalFirstRouteCatalog.ArenaCandidate openingArena(
            ServerPlayer player,ServerLevel level,int seedX,int seedZ,float yaw
    ){
        for(int[] offset:offsets(5)){
            int x=seedX+offset[0],z=seedZ+offset[1];
            int y=level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,x,z);
            BlockPos feet=new BlockPos(x,y,z);
            if(!standing(level,feet)||localHeightSpread(level,x,z,5,2)>3)continue;
            if(!sourceContentClear(level,x,y,z,3.25D))continue;
            Vec3 center=new Vec3(x+0.5D,y,z+0.5D);
            if(!BattleSessionManager.surveyArenaOpen(player,center,yaw,4))continue;
            return new DrehmalFirstRouteCatalog.ArenaCandidate(
                    new DrehmalFirstRouteCatalog.Position(x,y,z),yaw);
        }
        return null;
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

                double roadDistance=DrehmalRoutePlacementRules.corridorDistance(zone,x+0.5D,z+0.5D);
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

    private static WorldBossResolution resolveOptionalWorldBoss(
            ServerPlayer player,
            ServerLevel level,
            List<DrehmalFirstRouteCatalog.Site> activeSites
    ) {
        if (player == null || level == null || level.getServer() == null) return null;
        if (TurnboundWorldSavedData.get(level.getServer()).encounterCleared(DrehmalWorldBossPlacementRules.ENCOUNTER_ID)) {
            return null;
        }

        var authoredSite = DrehmalFirstRouteCatalog.site(DrehmalWorldBossPlacementRules.SITE_LOCATOR);
        var authoredFootprint = DrehmalFirstRouteCatalog.footprint(DrehmalWorldBossPlacementRules.FOOTPRINT_LOCATOR);
        if (authoredSite == null || authoredFootprint == null) return null;
        var tower = DrehmalWorldProfile.enabled(authoredSite.surveySeedAnchor());
        var hub = DrehmalWorldProfile.enabled(DrehmalWorldProfile.HUB_LOCATOR);
        if (tower == null || hub == null) return null;

        List<ScoredPosition> shortlist = new ArrayList<>();
        Set<Long> visited = new HashSet<>();
        for (var seed : worldBossSeeds()) {
            for (int[] offset : spacedOffsets(
                    DrehmalWorldBossPlacementRules.LOCAL_SEARCH_RADIUS,
                    DrehmalWorldBossPlacementRules.LOCAL_SEARCH_STEP)) {
                int x = seed.x() + offset[0];
                int z = seed.z() + offset[1];
                long key = (((long)x) << 32) ^ (z & 0xffffffffL);
                if (!visited.add(key)) continue;

                double towerDistance = Math.hypot(x + 0.5D - tower.x(), z + 0.5D - tower.z());
                double routeDistance = worldBossRouteDistance(x + 0.5D, z + 0.5D);
                double hubDistance = Math.hypot(x + 0.5D - hub.x(), z + 0.5D - hub.z());
                boolean safetyZone = DrehmalRouteZoneRules.insideSafetyZone(activeSites, x + 0.5D, z + 0.5D);
                int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
                BlockPos feet = new BlockPos(x, y, z);
                if (!standing(level, feet)) continue;

                int spread = localHeightSpread(level, x, z, 8, 4);
                if (!DrehmalWorldBossPlacementRules.candidate(
                        towerDistance, routeDistance, hubDistance, spread, safetyZone)) continue;
                shortlist.add(new ScoredPosition(
                        new DrehmalFirstRouteCatalog.Position(x, y, z),
                        DrehmalWorldBossPlacementRules.score(towerDistance, routeDistance, spread)));
            }
        }

        shortlist.sort(Comparator
                .comparingDouble(ScoredPosition::score)
                .thenComparingInt(candidate -> candidate.position().x())
                .thenComparingInt(candidate -> candidate.position().z()));

        int limit = Math.min(DrehmalWorldBossPlacementRules.MAX_SHORTLIST, shortlist.size());
        for (int i = 0; i < limit; i++) {
            var position = shortlist.get(i).position();
            if (!sourceContentClear(level, position.x(), position.y(), position.z(), 8.5D)) continue;
            List<DrehmalFirstRouteCatalog.ArenaCandidate> arenas = worldBossArenas(player, level, position);
            if (arenas.size() < 2) continue;

            var site = new DrehmalFirstRouteCatalog.Site(
                    authoredSite.locator(),
                    authoredSite.kind(),
                    authoredSite.surveySeedAnchor(),
                    authoredSite.playerLabel(),
                    position,
                    authoredSite.safetyRadius(),
                    authoredSite.encounterRadius(),
                    true,
                    true);
            var footprint = new DrehmalFirstRouteCatalog.Footprint(
                    authoredFootprint.locator(),
                    authoredFootprint.siteLocator(),
                    authoredFootprint.radius(),
                    authoredFootprint.allySlots(),
                    authoredFootprint.enemySlots(),
                    arenas,
                    true,
                    true);
            Turnbound.LOGGER.info(
                    "TURNBOUND resolved optional Graul world-boss meadow at {},{},{} with {} battle footprints",
                    position.x(), position.y(), position.z(), arenas.size());
            return new WorldBossResolution(site, footprint);
        }

        Turnbound.LOGGER.warn("TURNBOUND left optional Graul world boss dormant: no safe off-road meadow passed live-world checks");
        return null;
    }

    /**
     * Derives coarse meadow probes from the pinned Capital Valley source-route geometry rather than hard-coding a
     * new boss coordinate. The final location is always selected against the live 26.2 world.
     */
    private static List<DrehmalMapPlacementCatalog.Seed> worldBossSeeds() {
        Map<Long, DrehmalMapPlacementCatalog.Seed> out = new LinkedHashMap<>();
        Set<String> roles = Set.of("FIRST_COMBAT", "CHOICE_ELITE", "BREATHING");
        int[] lateralOffsets = {72, 96, 120};
        for (var zone : DrehmalMapPlacementCatalog.plan().zones()) {
            if (!roles.contains(zone.role())) continue;
            for (int i = 1; i < zone.corridor().size(); i++) {
                var a = zone.corridor().get(i - 1);
                var b = zone.corridor().get(i);
                double dx = b.x() - a.x();
                double dz = b.z() - a.z();
                double length = Math.hypot(dx, dz);
                if (length < 1.0D) continue;
                double mx = (a.x() + b.x()) * 0.5D;
                double mz = (a.z() + b.z()) * 0.5D;
                double nx = -dz / length;
                double nz = dx / length;
                for (int lateral : lateralOffsets) {
                    addWorldBossSeed(out, (int)Math.round(mx + nx * lateral), (int)Math.round(mz + nz * lateral));
                    addWorldBossSeed(out, (int)Math.round(mx - nx * lateral), (int)Math.round(mz - nz * lateral));
                }
            }
        }
        return List.copyOf(out.values());
    }

    private static void addWorldBossSeed(Map<Long, DrehmalMapPlacementCatalog.Seed> out, int x, int z) {
        long key = (((long)x) << 32) ^ (z & 0xffffffffL);
        out.putIfAbsent(key, new DrehmalMapPlacementCatalog.Seed(x, z));
    }

    private static double worldBossRouteDistance(double x, double z) {
        double best = Double.POSITIVE_INFINITY;
        for (var zone : DrehmalMapPlacementCatalog.plan().zones()) {
            if (!Set.of("FIRST_COMBAT", "CHOICE_ELITE", "BREATHING").contains(zone.role())) continue;
            best = Math.min(best, DrehmalRoutePlacementRules.corridorDistance(zone, x, z));
        }
        return best;
    }

    private static int localHeightSpread(ServerLevel level, int x, int z, int radius, int step) {
        int min = Integer.MAX_VALUE;
        int max = Integer.MIN_VALUE;
        for (int dz = -radius; dz <= radius; dz += step) {
            for (int dx = -radius; dx <= radius; dx += step) {
                int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x + dx, z + dz);
                min = Math.min(min, y);
                max = Math.max(max, y);
            }
        }
        return max - min;
    }

    private static List<int[]> spacedOffsets(int radius, int step) {
        List<int[]> out = new ArrayList<>();
        for (int dz = -radius; dz <= radius; dz += step) {
            for (int dx = -radius; dx <= radius; dx += step) {
                if (dx * dx + dz * dz <= radius * radius) out.add(new int[]{dx, dz});
            }
        }
        out.sort(Comparator.comparingInt(value -> value[0] * value[0] + value[1] * value[1]));
        return out;
    }

    private static List<DrehmalFirstRouteCatalog.ArenaCandidate> worldBossArenas(
            ServerPlayer player,
            ServerLevel level,
            DrehmalFirstRouteCatalog.Position home
    ) {
        List<DrehmalFirstRouteCatalog.ArenaCandidate> out = new ArrayList<>();
        int[][] offsets = {{0,0},{8,0},{-8,0},{0,8},{0,-8},{8,8},{-8,8},{8,-8},{-8,-8}};
        float[] yaws = {0.0F, 90.0F, 180.0F, 270.0F};
        for (int[] offset : offsets) {
            int x = home.x() + offset[0];
            int z = home.z() + offset[1];
            int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
            BlockPos feet = new BlockPos(x, y, z);
            if (!standing(level, feet) || localHeightSpread(level, x, z, 6, 3) > 3) continue;
            if (!sourceContentClear(level, x, y, z, 6.5D)) continue;
            Vec3 center = new Vec3(x + 0.5D, y, z + 0.5D);
            for (float yaw : yaws) {
                if (!BattleSessionManager.surveyArenaOpen(player, center, yaw, 4)) continue;
                out.add(new DrehmalFirstRouteCatalog.ArenaCandidate(
                        new DrehmalFirstRouteCatalog.Position(x, y, z), yaw));
                break;
            }
            if (out.size() >= 4) break;
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
