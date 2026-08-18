from pathlib import Path

ROOT=Path('projects/living-kingdoms/src/main/java/kr/moonseungjun/livingkingdoms/world')
pop=ROOT/'ErdenPopulationManager.java'
life=ROOT/'ErdenUrbanLifeManager.java'
resolver=ROOT/'ErdenUrbanResidenceResolver.java'

def rep(path, old, new):
    s=path.read_text()
    if old not in s: raise SystemExit(f'pattern missing in {path}: {old[:100]}')
    path.write_text(s.replace(old,new,1))

resolver.write_text(r'''package kr.moonseungjun.livingkingdoms.world;

import kr.moonseungjun.livingkingdoms.worldgen.AuthoredContinentDensity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;

/** Resolves a real Erden residence without assuming that every building must have a synthetic upper floor. */
public final class ErdenUrbanResidenceResolver {
    public static final int EXPECTED_GROUND_ONLY_BUILDINGS = 77;

    private ErdenUrbanResidenceResolver() {}

    public static int groundOnlyBuildingCount() {
        int count = 0;
        for (ExternalUrbanFabricBuilder.UrbanEntrance entrance : ExternalUrbanFabricBuilder.entrances()) {
            if (!ErdenUrbanAuthoredUpperRouteManager.isEligible(entrance)) count++;
        }
        if (count != EXPECTED_GROUND_ONLY_BUILDINGS) {
            throw new IllegalStateException("Erden ground-only building count drifted: " + count);
        }
        return count;
    }

    public static int groundOnlyHomeCount() {
        int count = 0;
        for (ExternalUrbanFabricBuilder.UrbanEntrance entrance : ExternalUrbanFabricBuilder.entrances()) {
            if (entrance.role().equals("tenement") && !ErdenUrbanAuthoredUpperRouteManager.isEligible(entrance)) count++;
        }
        return count;
    }

    public static boolean isGroundOnly(ExternalUrbanFabricBuilder.UrbanEntrance entrance) {
        return !ErdenUrbanAuthoredUpperRouteManager.isEligible(entrance);
    }

    public static boolean isResidenceReady(ServerLevel level, ExternalUrbanFabricBuilder.UrbanEntrance entrance) {
        if (ErdenUrbanAuthoredUpperRouteManager.isEligible(entrance)) {
            return ErdenUrbanAuthoredUpperRouteManager.isCompleted(level, entrance)
                    && ErdenUrbanAuthoredUpperRouteManager.verifiedUpperTarget(level, entrance) != null;
        }
        long key = key(entrance.x(), entrance.z());
        return level.getDataStorage().computeIfAbsent(ErdenUrbanInteriorSavedData.TYPE)
                .isComplete(key, ErdenUrbanInteriorBuilder.INTERIOR_REVISION);
    }

    public static BlockPos resolveHomeTarget(ServerLevel level, ExternalUrbanFabricBuilder.UrbanEntrance entrance, int bedSlot) {
        BlockPos authored = ErdenUrbanAuthoredUpperRouteManager.verifiedUpperTarget(level, entrance);
        if (authored != null) return nearbyWalkable(level, authored, bedSlot);
        if (ErdenUrbanAuthoredUpperRouteManager.isEligible(entrance)) return null;
        if (!isResidenceReady(level, entrance)) return null;
        return groundTarget(level, entrance, 4 + Math.floorMod(bedSlot, 3));
    }

    public static BlockPos resolveWorkTarget(ServerLevel level, ExternalUrbanFabricBuilder.UrbanEntrance entrance) {
        long key = key(entrance.x(), entrance.z());
        if (!level.getDataStorage().computeIfAbsent(ErdenUrbanInteriorSavedData.TYPE)
                .isComplete(key, ErdenUrbanInteriorBuilder.INTERIOR_REVISION)) return null;
        return groundTarget(level, entrance, 3);
    }

    public static void verifyTargetOrThrow(ServerLevel level, BlockPos target, String label) {
        if (target == null || !walkable(level, target)) {
            throw new IllegalStateException("Erden residence target is not walkable label=" + label + " target=" + target);
        }
    }

    private static BlockPos groundTarget(ServerLevel level, ExternalUrbanFabricBuilder.UrbanEntrance entrance, int preferredDepth) {
        int doorY = findLowestDoorY(level, entrance.x(), entrance.z());
        if (doorY == Integer.MIN_VALUE) return null;
        int dx = entrance.roadX() - entrance.x();
        int dz = entrance.roadZ() - entrance.z();
        int inwardX, inwardZ;
        if (Math.abs(dx) >= Math.abs(dz)) { inwardX = dx >= 0 ? -1 : 1; inwardZ = 0; }
        else { inwardX = 0; inwardZ = dz >= 0 ? -1 : 1; }
        int[] depths = {preferredDepth, 4, 5, 6, 3, 7, 2, 8};
        for (int depth : depths) {
            BlockPos pos = new BlockPos(entrance.x()+inwardX*depth, doorY, entrance.z()+inwardZ*depth);
            if (walkable(level,pos)) return pos;
        }
        return null;
    }

    private static BlockPos nearbyWalkable(ServerLevel level, BlockPos target, int slot) {
        int[][] offsets = {{0,0},{1,0},{-1,0},{0,1},{0,-1},{1,1},{-1,-1},{1,-1},{-1,1}};
        int start=Math.floorMod(slot, offsets.length);
        for (int i=0;i<offsets.length;i++) {
            int[] o=offsets[(start+i)%offsets.length];
            BlockPos pos=target.offset(o[0],0,o[1]);
            if (walkable(level,pos)) return pos;
        }
        return walkable(level,target) ? target : null;
    }

    private static boolean walkable(ServerLevel level, BlockPos pos) {
        if (!level.hasChunk(pos.getX()>>4,pos.getZ()>>4)) return false;
        BlockState floor=level.getBlockState(pos.below());
        return !floor.isAir() && floor.getFluidState().isEmpty()
                && level.getBlockState(pos).isAir() && level.getBlockState(pos.above()).isAir();
    }

    private static int findLowestDoorY(ServerLevel level,int x,int z) {
        if (!level.hasChunk(x>>4,z>>4)) return Integer.MIN_VALUE;
        int designed=(int)Math.round(AuthoredContinentDensity.surfaceHeight(x,z));
        int min=Math.max(level.getMinY(),designed-8), max=Math.min(level.getMaxY()-1,designed+64);
        BlockPos.MutableBlockPos p=new BlockPos.MutableBlockPos();
        for(int y=min;y<=max;y++){p.set(x,y,z); if(level.getBlockState(p).getBlock() instanceof DoorBlock) return y;}
        return Integer.MIN_VALUE;
    }

    private static long key(int x,int z){return ((long)x<<32)^(z&0xffffffffL);}
}
''')

# Population: residence readiness and real target resolution.
rep(pop, '''        ErdenUrbanLifeSavedData urbanLife = level.getDataStorage()\n                .computeIfAbsent(ErdenUrbanLifeSavedData.TYPE);\n        int spawned = 0;''', '''        int spawned = 0;''')
rep(pop, '''            long homeKey = positionKey(household.homeX(), household.homeZ());\n            if (!urbanLife.isUpperFloorComplete(\n                    homeKey, ErdenUrbanLifeManager.UPPER_FLOOR_REVISION)) continue;\n            ExternalUrbanFabricBuilder.UrbanEntrance entrance = findEntrance(\n                    household.homeX(), household.homeZ());\n            if (entrance == null) continue;\n            int doorY = findLowestDoorY(level, household.homeX(), household.homeZ());\n            if (doorY == Integer.MIN_VALUE) continue;\n            Room room = room(entrance, doorY - 1);''', '''            ExternalUrbanFabricBuilder.UrbanEntrance entrance = findEntrance(\n                    household.homeX(), household.homeZ());\n            if (entrance == null || !ErdenUrbanResidenceResolver.isResidenceReady(level, entrance)) continue;''')
rep(pop, '''                if (population.isDead(resident.id()) || existing.containsKey(resident.name())) continue;\n                if (spawnResident(level, room, resident)) {\n                    spawned++;\n                }''', '''                if (population.isDead(resident.id()) || existing.containsKey(resident.name())) continue;\n                BlockPos target = ErdenUrbanResidenceResolver.resolveHomeTarget(level, entrance, resident.bedSlot());\n                if (target != null && spawnResident(level, target, resident)) spawned++;''')
rep(pop, '''    private static boolean spawnResident(\n            ServerLevel level,\n            Room home,\n            ErdenPopulationSavedData.Resident resident) {''', '''    private static boolean spawnResident(\n            ServerLevel level,\n            BlockPos target,\n            ErdenPopulationSavedData.Resident resident) {''')
rep(pop, '''        Point point = home.point(resident.bedSlot() - 1, 4 + resident.bedSlot());\n        int preferredY = home.floorY + 6;\n        int standingY = safeStandingY(level, point.x, preferredY, point.z);\n        villager.setPos(point.x + 0.5D, standingY, point.z + 0.5D);''', '''        int standingY = safeStandingY(level, target.getX(), target.getY(), target.getZ());\n        villager.setPos(target.getX() + 0.5D, standingY, target.getZ() + 0.5D);''')
old='''        long key = positionKey(x, z);\n        if (workplace) {\n            ErdenUrbanInteriorSavedData interiors = level.getDataStorage()\n                    .computeIfAbsent(ErdenUrbanInteriorSavedData.TYPE);\n            if (!interiors.isComplete(key, ErdenUrbanInteriorBuilder.INTERIOR_REVISION)) return null;\n        } else {\n            ErdenUrbanLifeSavedData urbanLife = level.getDataStorage()\n                    .computeIfAbsent(ErdenUrbanLifeSavedData.TYPE);\n            if (!urbanLife.isUpperFloorComplete(\n                    key, ErdenUrbanLifeManager.UPPER_FLOOR_REVISION)) return null;\n        }\n        int doorY = findLowestDoorY(level, x, z);\n        if (doorY == Integer.MIN_VALUE) return null;\n        Room room = room(entrance, doorY - 1);\n        Point point = workplace\n                ? room.point(0, 3)\n                : room.point(0, 4 + reference.resident.bedSlot());\n        return new Target(\n                point.x,\n                workplace ? room.floorY + 1 : room.floorY + 6,\n                point.z);'''
new='''        BlockPos target = workplace\n                ? ErdenUrbanResidenceResolver.resolveWorkTarget(level, entrance)\n                : ErdenUrbanResidenceResolver.resolveHomeTarget(level, entrance, reference.resident.bedSlot());\n        return target == null ? null : new Target(target.getX(), target.getY(), target.getZ());'''
rep(pop,old,new)
rep(pop, '''        ErdenUrbanLifeSavedData urbanLife = level.getDataStorage()\n                .computeIfAbsent(ErdenUrbanLifeSavedData.TYPE);\n        if (!urbanLife.isUpperFloorComplete(\n                positionKey(sample.homeX(), sample.homeZ()),\n                ErdenUrbanLifeManager.UPPER_FLOOR_REVISION)) return;''', '''        ExternalUrbanFabricBuilder.UrbanEntrance sampleHome = findEntrance(sample.homeX(), sample.homeZ());\n        if (sampleHome == null || !ErdenUrbanResidenceResolver.isResidenceReady(level, sampleHome)) return;''')
rep(pop, '''                "LK_ERDEN_POPULATION_DIAGNOSTIC_PASS households={} residents={} workers={} dependents={} spawned_sample={} ledger=true shortages={} shifts=true ownership=true",''', '''                "LK_ERDEN_POPULATION_DIAGNOSTIC_PASS households={} residents={} workers={} dependents={} spawned_sample={} ledger=true shortages={} shifts=true ownership=true residence_mode=verified_upper_or_ground",''')

# Urban life: fresh worlds no longer create fake upper floors for the 77 real ground-only shells.
rep(life, '''        completeOneUpperFloor(level, life, entrances);''', '''        // Legacy synthetic upper records remain readable, but fresh worlds never create them.''')
rep(life, '''        int complete = life.completedUpperFloorCount(UPPER_FLOOR_REVISION);\n        int authoredEligible = ErdenUrbanAuthoredUpperRouteManager.eligibleCount();\n        int syntheticRequired = entrances.size() - authoredEligible;\n        if (!completionLogged && complete >= syntheticRequired) {\n            completionLogged = true;\n            LivingKingdoms.LOGGER.info(\n                    "Completed Erden walkable upper floors buildings={} synthetic_completed={} synthetic_required={} authored_upper_buildings={} stairs=true role_spaces={} revision={}",\n                    entrances.size(), complete, syntheticRequired, authoredEligible,\n                    HABITABLE_ROLES.size(), UPPER_FLOOR_REVISION);\n        }''', '''        int authoredEligible = ErdenUrbanAuthoredUpperRouteManager.eligibleCount();\n        int groundOnly = ErdenUrbanResidenceResolver.groundOnlyBuildingCount();\n        if (!completionLogged && ErdenUrbanInteriorBuilder.completedCount(level) == entrances.size()) {\n            completionLogged = true;\n            LivingKingdoms.LOGGER.info(\n                    "Completed Erden residence modes buildings={} authored_upper_buildings={} ground_only_buildings={} fresh_synthetic_upper_created=0 legacy_synthetic_records={} role_spaces={} revision={}",\n                    entrances.size(), authoredEligible, groundOnly,\n                    life.completedUpperFloorCount(UPPER_FLOOR_REVISION), HABITABLE_ROLES.size(), UPPER_FLOOR_REVISION);\n        }''')
rep(life, '''                "Prepared Erden upper-floor conversions buildings={} synthetic_candidates={} authored_candidates={} stair_steps=4 role_spaces={} citizen_assignments={}",\n                entrances.size(), entrances.size() - authoredEligible, authoredEligible,\n                HABITABLE_ROLES.size(), CITIZENS.size());''', '''                "Prepared Erden residence modes buildings={} ground_only_candidates={} authored_upper_candidates={} fresh_synthetic_upper_created=0 role_spaces={} citizen_assignments={}",\n                entrances.size(), entrances.size() - authoredEligible, authoredEligible,\n                HABITABLE_ROLES.size(), CITIZENS.size());''')
start='''        // A completed authored route always wins. This is also the non-destructive migration path:\n        // old synthetic blocks may remain in a legacy save, while citizen destinations immediately\n        // switch to the real source upper room once its route verifies.\n        BlockPos authoredTarget =\n                ErdenUrbanAuthoredUpperRouteManager.verifiedUpperTarget(level, entrance);\n        if (authoredTarget != null) {\n            return new Target(authoredTarget.getX(), authoredTarget.getY(), authoredTarget.getZ());\n        }\n\n        long key = entranceKey(x, z);\n        if (!life.isUpperFloorComplete(key, UPPER_FLOOR_REVISION)) return null;\n        int doorY = findLowestDoorY(level, x, z);\n        if (doorY == Integer.MIN_VALUE) return null;\n        Room room = room(entrance, doorY - 1);\n        Point point = workplace ? room.point(0, 3) : room.point(0, 7);\n        int y = workplace\n                ? room.floorY + 1\n                : room.floorY + UPPER_FLOOR_OFFSET + 1;\n        return new Target(point.x, y, point.z);'''
replacement='''        BlockPos resolved = workplace\n                ? ErdenUrbanResidenceResolver.resolveWorkTarget(level, entrance)\n                : ErdenUrbanResidenceResolver.resolveHomeTarget(level, entrance, 1);\n        return resolved == null ? null : new Target(resolved.getX(), resolved.getY(), resolved.getZ());'''
rep(life,start,replacement)
old='''        if (!life.isUpperFloorComplete(\n                entranceKey(sample.x(), sample.z()), UPPER_FLOOR_REVISION)) return;\n        if (life.assignments().size() != EXPECTED_CITIZEN_ASSIGNMENTS) return;\n        int doorY = findLowestDoorY(level, sample.x(), sample.z());\n        if (doorY == Integer.MIN_VALUE) return;\n        verifyUpperFloor(level, room(sample, doorY - 1));\n        ciPassed = true;\n        LivingKingdoms.LOGGER.info(\n                "LK_URBAN_LIFE_DIAGNOSTIC_PASS upper_floor=true stairs=true upper_role={} assignments={} routines=true synthetic_fallback=true",\n                sample.role(), life.assignments().size());'''
new='''        if (life.assignments().size() != EXPECTED_CITIZEN_ASSIGNMENTS) return;\n        if (!ErdenUrbanResidenceResolver.isGroundOnly(sample)\n                || !ErdenUrbanResidenceResolver.isResidenceReady(level, sample)) return;\n        BlockPos target = ErdenUrbanResidenceResolver.resolveHomeTarget(level, sample, 1);\n        if (target == null) return;\n        ErdenUrbanResidenceResolver.verifyTargetOrThrow(level, target, "ground-only-ci");\n        if (life.isUpperFloorComplete(entranceKey(sample.x(), sample.z()), UPPER_FLOOR_REVISION)) {\n            throw new IllegalStateException("Fresh Erden ground-only building was falsely marked synthetic upper-complete");\n        }\n        ciPassed = true;\n        LivingKingdoms.LOGGER.info(\n                "LK_URBAN_LIFE_DIAGNOSTIC_PASS upper_floor=false ground_only=true upper_role={} assignments={} routines=true synthetic_fallback=false fresh_synthetic_upper_created=0",\n                sample.role(), life.assignments().size());'''
rep(life,old,new)
# Authored routine audit must validate a home target, because work now correctly stays on the ground work floor.
old='''            ExternalUrbanFabricBuilder.UrbanEntrance workplace =\n                    findEntrance(assignment.workX(), assignment.workZ());\n            if (workplace == null\n                    || !ErdenUrbanAuthoredUpperRouteManager.isEligible(workplace)\n                    || !ErdenUrbanAuthoredUpperRouteManager.isCompleted(level, workplace)) {\n                continue;\n            }\n            BlockPos authored =\n                    ErdenUrbanAuthoredUpperRouteManager.verifiedUpperTarget(level, workplace);\n            if (authored == null) continue;\n\n            ErdenUrbanAuthoredUpperRouteManager.verifyOrThrow(level, workplace);\n            Target resolved = resolveTarget(level, life, assignment, true);\n            if (resolved == null\n                    || resolved.x != authored.getX()\n                    || resolved.y != authored.getY()\n                    || resolved.z != authored.getZ()) {\n                throw new IllegalStateException(\n                        "Erden authored resident routine did not resolve to verified upper target citizen="\n                                + assignment.citizenName());\n            }\n            if (life.isUpperFloorComplete(\n                    entranceKey(workplace.x(), workplace.z()), UPPER_FLOOR_REVISION)) {\n                throw new IllegalStateException(\n                        "Fresh Erden authored-route workplace was falsely marked synthetic-complete role="\n                                + workplace.role());\n            }\n            verifyRuntimeTargetGeometry(level, authored);\n            authoredCiPassed = true;\n            LivingKingdoms.LOGGER.info(\n                    "LK_ERDEN_URBAN_AUTHORED_HOME_PASS citizen={} role={} authored_upper=true synthetic_upper_required=false route_verified=true resident_target_verified=true runtime_geometry_verified=true source_blocks_cut=0 routine=work",\n                    assignment.citizenName(), workplace.role());\n            return;'''
new='''            ExternalUrbanFabricBuilder.UrbanEntrance home = findEntrance(assignment.homeX(), assignment.homeZ());\n            if (home == null || !ErdenUrbanAuthoredUpperRouteManager.isEligible(home)\n                    || !ErdenUrbanAuthoredUpperRouteManager.isCompleted(level, home)) continue;\n            BlockPos authored = ErdenUrbanAuthoredUpperRouteManager.verifiedUpperTarget(level, home);\n            if (authored == null) continue;\n            ErdenUrbanAuthoredUpperRouteManager.verifyOrThrow(level, home);\n            Target resolved = resolveTarget(level, life, assignment, false);\n            if (resolved == null || resolved.y != authored.getY()) {\n                throw new IllegalStateException("Erden authored home routine did not resolve to verified upper level citizen=" + assignment.citizenName());\n            }\n            if (life.isUpperFloorComplete(entranceKey(home.x(), home.z()), UPPER_FLOOR_REVISION)) {\n                throw new IllegalStateException("Fresh Erden authored home was falsely marked synthetic-complete role=" + home.role());\n            }\n            verifyRuntimeTargetGeometry(level, new BlockPos(resolved.x, resolved.y, resolved.z));\n            authoredCiPassed = true;\n            LivingKingdoms.LOGGER.info(\n                    "LK_ERDEN_URBAN_AUTHORED_HOME_PASS citizen={} role={} authored_upper=true synthetic_upper_required=false route_verified=true resident_target_verified=true runtime_geometry_verified=true source_blocks_cut=0 routine=home",\n                    assignment.citizenName(), home.role());\n            return;'''
rep(life,old,new)
print('patched residence modes')
