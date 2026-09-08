from pathlib import Path
import re

ROOT = Path('projects/frontier-settlement')
CONSTRUCTION = ROOT / 'src/main/java/kr/moonseungjun/frontiersettlement/settlement/SettlementConstructionService.java'
ENTRY = ROOT / 'src/main/java/kr/moonseungjun/frontiersettlement/FrontierSettlement.java'
TEST = ROOT / 'tools/test_current_source.py'
GRADLE = ROOT / 'gradle.properties'
README = ROOT / 'README.md'
PROJECT = ROOT / 'PROJECT.md'
NOTE = ROOT / 'CONSTRUCTION_ROUTING_ALPHA130.md'


def read(path):
    return path.read_text(encoding='utf-8')


def write(path, text):
    path.write_text(text, encoding='utf-8')


def replace_once(text, old, new, label):
    count = text.count(old)
    if count != 1:
        raise SystemExit(f'{label}: expected one literal match, got {count}')
    return text.replace(old, new, 1)


def sub_once(text, pattern, repl, label):
    out, count = re.subn(pattern, repl, text, count=1, flags=re.S)
    if count != 1:
        raise SystemExit(f'{label}: expected one regex match, got {count}')
    return out


construction = read(CONSTRUCTION)
construction = replace_once(
    construction,
    '    private static final int SITE_WORK_MARGIN = 12;\n',
    '''    private static final int SITE_WORK_MARGIN = 12;\n    // Protection may stay wide, but visible workers should only count as locally on-site when they\n    // are close to the real lot on ground. The old 12-block work shortcut made roofs and nearby\n    // structures look like valid construction positions.\n    private static final int ACTIVE_SITE_WORK_MARGIN = 4;\n''',
    'active site work margin')

construction = replace_once(
    construction,
    '''        for (FrontierWorkerEntity builder : existing) {\n            if (!builder.entityTags().contains(BUILDER_TAG)) builder.addTag(BUILDER_TAG);\n            builder.setNoAi(false);\n            builder.setInvulnerable(false);\n            recoverBuilderFromBlockedCell(level, data, builder);\n        }\n''',
    '''        Set<BlockPos> recoveryOccupied = new HashSet<>();\n        for (FrontierWorkerEntity builder : existing) recoveryOccupied.add(builder.blockPosition());\n        for (FrontierWorkerEntity builder : existing) {\n            if (!builder.entityTags().contains(BUILDER_TAG)) builder.addTag(BUILDER_TAG);\n            builder.setNoAi(false);\n            builder.setInvulnerable(false);\n            recoveryOccupied.remove(builder.blockPosition());\n            recoverBuilderFromBlockedCell(level, data, builder, recoveryOccupied);\n            recoveryOccupied.add(builder.blockPosition());\n        }\n''',
    'distinct runtime recovery occupancy')

construction = sub_once(
    construction,
    r'    private static void recoverBuilderFromBlockedCell\(ServerLevel level, SettlementData data, FrontierWorkerEntity builder\) \{.*?\n    private static int nearestNaturalGroundBelow',
    '''    private static void recoverBuilderFromBlockedCell(ServerLevel level, SettlementData data,\n                                                       FrontierWorkerEntity builder, Set<BlockPos> occupied) {\n        BlockPos feet = builder.blockPosition();\n        BlockPos head = feet.above();\n        if (!level.hasChunkAt(feet) || !level.hasChunkAt(head)) return;\n        boolean physicallyBlocked = blocksCurrentPathCell(level, feet, level.getBlockState(feet))\n                || blocksCurrentPathCell(level, head, level.getBlockState(head));\n        boolean elevatedCandidate = !physicallyBlocked && builderOnArtificialElevation(level, builder);\n        if (!physicallyBlocked && !elevatedCandidate) return;\n\n        BlockPos safe = findSafeBuilderHome(level, data, occupied);\n        if (safe == null) return;\n        // A connected bridge/balcony is still physical world traversal. Only a genuinely disconnected\n        // elevated perch is recovered; ordinary accessible structures never become a teleport shortcut.\n        if (elevatedCandidate && !builderStrandedOnArtificialElevation(level, builder, safe)) return;\n        builder.getNavigation().stop();\n        builder.setPos(safe.getX() + 0.5D, safe.getY(), safe.getZ() + 0.5D);\n    }\n\n    private static boolean builderOnArtificialElevation(ServerLevel level, FrontierWorkerEntity builder) {\n        BlockPos feet = builder.blockPosition();\n        int naturalGroundY = nearestNaturalGroundBelow(level, feet, 16);\n        if (naturalGroundY == Integer.MIN_VALUE) return false;\n        int artificialRise = (feet.getY() - 1) - naturalGroundY;\n        return artificialRise >= 3;\n    }\n\n    /** Accessible balconies/bridges stay physical; only a disconnected elevated perch is recovered. */\n    private static boolean builderStrandedOnArtificialElevation(ServerLevel level, FrontierWorkerEntity builder,\n                                                                 BlockPos safe) {\n        return builderOnArtificialElevation(level, builder) && createReachablePath(builder, safe) == null;\n    }\n\n    private static int nearestNaturalGroundBelow''',
    'builder blocked/elevated recovery')

construction = replace_once(
    construction,
    '''        if (!SettlementProjectAuthority.anyActive(level.getServer(), data)) {\n            for (int i = 0; i < keep; i++) returnBuilderHome(level, data, builders.get(i));\n        }\n''',
    '''        if (!SettlementProjectAuthority.anyActive(level.getServer(), data)) {\n            Set<BlockPos> reservedHomes = new HashSet<>();\n            for (int i = 0; i < keep; i++) returnBuilderHome(level, data, builders.get(i), reservedHomes);\n        }\n''',
    'normalize distinct home slots')

construction = replace_once(
    construction,
    '''    public static void settleIdleBuilders(MinecraftServer server, SettlementData data) {\n        if (SettlementProjectAuthority.anyActive(server, data)) return;\n        ServerLevel level = server.overworld();\n        for (FrontierWorkerEntity builder : findBuilders(level, data)) {\n            builder.setNoAi(false);\n            builder.setInvulnerable(false);\n            builder.setCustomName(Component.literal(BUILDER_NAME));\n            if (!builder.getMainHandItem().isEmpty()) {\n                returnCarriedToTownStorage(server, data, builder);\n                continue;\n            }\n            returnBuilderHome(level, data, builder);\n        }\n    }\n\n    static boolean returnBuilderHome(ServerLevel level, SettlementData data, FrontierWorkerEntity builder) {\n        BlockPos home = findSafeBuilderHome(level, data);\n        if (home == null) {\n            builder.getNavigation().stop();\n            return false;\n        }\n        double distance = builder.distanceToSqr(home.getX() + 0.5D, home.getY(), home.getZ() + 0.5D);\n        if (distance <= 4.0D) {\n            builder.getNavigation().stop();\n            return true;\n        }\n        if (!moveToReachable(builder, home, 1.10D)) builder.getNavigation().stop();\n        return false;\n    }\n''',
    '''    public static void settleIdleBuilders(MinecraftServer server, SettlementData data) {\n        if (SettlementProjectAuthority.anyActive(server, data)) return;\n        ServerLevel level = server.overworld();\n        Set<BlockPos> reservedHomes = new HashSet<>();\n        for (FrontierWorkerEntity builder : findBuilders(level, data)) {\n            builder.setNoAi(false);\n            builder.setInvulnerable(false);\n            builder.setCustomName(Component.literal(BUILDER_NAME));\n            if (!builder.getMainHandItem().isEmpty()) {\n                returnCarriedToTownStorage(server, data, builder);\n                continue;\n            }\n            returnBuilderHome(level, data, builder, reservedHomes);\n        }\n    }\n\n    static boolean returnBuilderHome(ServerLevel level, SettlementData data, FrontierWorkerEntity builder) {\n        return moveBuilderHome(builder, findSafeBuilderHome(level, data));\n    }\n\n    private static boolean returnBuilderHome(ServerLevel level, SettlementData data, FrontierWorkerEntity builder,\n                                             Set<BlockPos> reservedHomes) {\n        BlockPos home = findSafeBuilderHome(level, data, reservedHomes);\n        if (home != null) reservedHomes.add(home);\n        return moveBuilderHome(builder, home);\n    }\n\n    private static boolean moveBuilderHome(FrontierWorkerEntity builder, BlockPos home) {\n        if (home == null) {\n            builder.getNavigation().stop();\n            return false;\n        }\n        double distance = builder.distanceToSqr(home.getX() + 0.5D, home.getY(), home.getZ() + 0.5D);\n        if (distance <= 4.0D) {\n            builder.getNavigation().stop();\n            return true;\n        }\n        if (!moveToReachable(builder, home, 1.10D)) builder.getNavigation().stop();\n        return false;\n    }\n''',
    'idle builder distinct homes')

construction = replace_once(
    construction,
    '''    private static boolean builderWithinSiteWorkEnvelope(ServerLevel level, ConstructionState construction, BuildingType type,\n                                                          FrontierWorkerEntity builder) {\n        BuildingRotation rotation = construction.buildingRotation();\n        int width = rotation.rotatedWidth(type);\n        int depth = rotation.rotatedDepth(type);\n        double minX = construction.originX() - SITE_WORK_MARGIN;\n        double maxX = construction.originX() + width - 1 + SITE_WORK_MARGIN + 1.0D;\n        double minZ = construction.originZ() - SITE_WORK_MARGIN;\n        double maxZ = construction.originZ() + depth - 1 + SITE_WORK_MARGIN + 1.0D;\n        if (builder.getX() < minX || builder.getX() > maxX || builder.getZ() < minZ || builder.getZ() > maxZ) return false;\n        int x = (int) Math.floor(builder.getX());\n        int z = (int) Math.floor(builder.getZ());\n        BlockPos surface = safeSurfaceCell(level, x, z);\n        return surface != null && Math.abs(builder.getY() - surface.getY()) <= 2.25D;\n    }\n''',
    '''    private static boolean builderWithinSiteWorkEnvelope(ServerLevel level, ConstructionState construction, BuildingType type,\n                                                          FrontierWorkerEntity builder) {\n        BuildingRotation rotation = construction.buildingRotation();\n        int width = rotation.rotatedWidth(type);\n        int depth = rotation.rotatedDepth(type);\n        double minX = construction.originX() - ACTIVE_SITE_WORK_MARGIN;\n        double maxX = construction.originX() + width - 1 + ACTIVE_SITE_WORK_MARGIN + 1.0D;\n        double minZ = construction.originZ() - ACTIVE_SITE_WORK_MARGIN;\n        double maxZ = construction.originZ() + depth - 1 + ACTIVE_SITE_WORK_MARGIN + 1.0D;\n        if (builder.getX() < minX || builder.getX() > maxX || builder.getZ() < minZ || builder.getZ() > maxZ) return false;\n        int x = (int) Math.floor(builder.getX());\n        int z = (int) Math.floor(builder.getZ());\n        BlockPos ground = safeGroundWorkCell(level, x, z);\n        return ground != null && Math.abs(builder.getY() - ground.getY()) <= 1.25D;\n    }\n''',
    'ground-only local work envelope')

construction = replace_once(
    construction,
    '''                    int x = target.getX() + dx;\n                    int z = target.getZ() + dz;\n                    int y = terrainSurfaceHeight(level, x, z);\n                    BlockPos candidate = new BlockPos(x, y, z);\n                    if (isWalkableApproachCell(level, candidate)) unique.add(candidate);\n''',
    '''                    int x = target.getX() + dx;\n                    int z = target.getZ() + dz;\n                    BlockPos candidate = safeGroundWorkCell(level, x, z);\n                    if (candidate != null) unique.add(candidate);\n''',
    'ground-only grading approaches')

construction = replace_once(
    construction,
    '''    private static void addGroundWorkCandidate(ServerLevel level, Set<BlockPos> result, int x, int z) {\n        BlockPos candidate = safeSurfaceCell(level, x, z);\n        if (candidate != null) result.add(candidate);\n    }\n''',
    '''    private static void addGroundWorkCandidate(ServerLevel level, Set<BlockPos> result, int x, int z) {\n        BlockPos candidate = safeGroundWorkCell(level, x, z);\n        if (candidate != null) result.add(candidate);\n    }\n''',
    'ground-only building approach candidates')

construction = replace_once(
    construction,
    '''    /** Never choose an arbitrary highest collision surface such as a roof or tall log pillar as home. */\n    private static BlockPos safeBuilderHomeCell(ServerLevel level, int x, int z, int referenceY) {\n        BlockPos candidate = safeSurfaceCell(level, x, z);\n        if (candidate == null) return null;\n        BlockState support = level.getBlockState(candidate.below());\n        if (isNaturalGround(support) || support.is(Blocks.DIRT_PATH)) return candidate;\n        return null;\n    }\n\n    private static BlockPos safeSurfaceCell(ServerLevel level, int x, int z) {\n        if (!level.hasChunkAt(new BlockPos(x, 0, z))) return null;\n        int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);\n        BlockPos candidate = new BlockPos(x, y, z);\n        return isWalkableApproachCell(level, candidate) ? candidate : null;\n    }\n''',
    '''    /** Never choose an arbitrary highest collision surface such as a roof or tall log pillar as home. */\n    private static BlockPos safeBuilderHomeCell(ServerLevel level, int x, int z, int referenceY) {\n        return safeGroundWorkCell(level, x, z);\n    }\n\n    private static boolean isBuilderGroundSupport(BlockState support) {\n        return isNaturalGround(support) || support.is(Blocks.DIRT_PATH);\n    }\n\n    /**\n     * Returns a real ground staging cell, never merely the highest collision surface. Normal terrain\n     * stays on the one-heightmap fast path; trees/structures fall back to the existing bounded\n     * terrain scan and are accepted only when the final support is ground/path material.\n     */\n    private static BlockPos safeGroundWorkCell(ServerLevel level, int x, int z) {\n        BlockPos chunkProbe = new BlockPos(x, 0, z);\n        if (!level.hasChunkAt(chunkProbe)) return null;\n        int topY = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);\n        BlockPos top = new BlockPos(x, topY, z);\n        if (isWalkableApproachCell(level, top) && isBuilderGroundSupport(level.getBlockState(top.below()))) return top;\n\n        int groundY = terrainSurfaceHeight(level, x, z);\n        if (groundY == topY) return null;\n        BlockPos candidate = new BlockPos(x, groundY, z);\n        if (!isWalkableApproachCell(level, candidate)) return null;\n        return isBuilderGroundSupport(level.getBlockState(candidate.below())) ? candidate : null;\n    }\n''',
    'ground cell authority')

# Version/document synchronization.
gradle = read(GRADLE)
gradle = replace_once(gradle, 'mod_version=0.1.0-alpha.129', 'mod_version=0.1.0-alpha.130', 'gradle version')
if '# Alpha.130 construction worker routing:' not in gradle:
    gradle = gradle.rstrip() + '\n\n# Alpha.130 construction worker routing: distinct idle/recovery slots and ground-only local work approaches.\n'
write(GRADLE, gradle)

entry = read(ENTRY)
entry = replace_once(entry,
    '    // Alpha.129 canonical/integrated validation trigger after real-play worker/runtime recovery.\n',
    '    // Alpha.130 construction-worker ground routing and distinct-home recovery.\n',
    'entry version comment')
write(ENTRY, entry)

readme = read(README)
readme = replace_once(readme, '## Current version: 0.1.0-alpha.129', '## Current version: 0.1.0-alpha.130', 'readme current version')
readme = replace_once(readme,
    '## Alpha.129 worker runtime recovery\n',
    '''## Alpha.130 construction-worker routing recovery\n\nReal-play feedback showed construction workers clustering in odd places or appearing to lose their route. The building service no longer treats any nearby highest collision surface as a valid work position: the broad protection envelope stays unchanged, while active workers must reach a much tighter ground/path staging envelope and perimeter candidates reject roofs, tree tops and other artificial perches. Idle and maintenance return routing now reserves distinct safe home cells instead of sending every builder to the same coordinate. Existing disconnected elevated workers still use the conservative Alpha.128 recovery rule; connected bridges/balconies are not teleported.\n\n## Alpha.129 worker runtime recovery\n''',
    'readme alpha130 section')
write(README, readme)

project = read(PROJECT)
project = replace_once(project, 'Current implementation delta: **0.1.0-alpha.126**.',
                       'Current implementation delta: **0.1.0-alpha.130**.', 'project current delta')
if '### Alpha.130 construction-worker routing recovery' not in project:
    project = project.rstrip() + '''\n\n### Alpha.130 construction-worker routing recovery\n\nReal-play worker-position feedback is handled as a physical navigation problem, not by widening teleport authority. Active ordinary-building workers only gain local work authority from a bounded four-block ground/path envelope; work and grading approach candidates no longer accept arbitrary heightmap tops such as roofs or log pillars. Idle and explicit maintenance return routes reserve separate safe cells for the bounded builder crew, preventing collision-prone pileups at one settlement-center coordinate. Disconnected elevated legacy workers retain conservative safe-ground recovery, while any structure with a real route remains physically traversed. No force loading, new save field, virtual cargo or new management layer is added.\n'''
write(PROJECT, project)

# Regression contract follows the new routing authority.
test = read(TEST)
test = replace_once(test,
    'require("mod_version=0.1.0-alpha.129" in gradle, "current verifier/version drift")',
    'require("mod_version=0.1.0-alpha.130" in gradle, "current verifier/version drift")',
    'test version')
test = replace_once(test,
    '''require("gradeApproachPositions" in construction and "radius <= 3" in construction\n        and "terrainSurfaceHeight(level, x, z)" in construction,\n        "tree-aware bounded grading approach recovery missing")\n''',
    '''require("gradeApproachPositions" in construction and "radius <= 3" in construction\n        and "safeGroundWorkCell(level, x, z)" in construction,\n        "ground-only bounded grading approach recovery missing")\n''',
    'grading regression assertion')
test = replace_once(test,
    '''require("safeBuilderHomeCell" in construction and "radius <= 24" in construction\n        and "support.is(Blocks.DIRT_PATH)" in construction,\n        "builder home can regress to arbitrary roof-height surfaces")\n''',
    '''require("safeBuilderHomeCell" in construction and "radius <= 24" in construction\n        and "isBuilderGroundSupport" in construction and "support.is(Blocks.DIRT_PATH)" in construction,\n        "builder home can regress to arbitrary roof-height surfaces")\nrequire("ACTIVE_SITE_WORK_MARGIN = 4" in construction and "safeGroundWorkCell" in construction\n        and "safeSurfaceCell(" not in construction,\n        "building workers can again treat arbitrary nearby roofs/heightmap tops as local work cells")\nrequire("Set<BlockPos> reservedHomes = new HashSet<>()" in construction\n        and "returnBuilderHome(level, data, builder, reservedHomes)" in construction,\n        "idle construction workers can again pile onto one shared home coordinate")\n''',
    'builder routing regression assertions')
write(TEST, test)

write(CONSTRUCTION, construction)

write(NOTE, '''# Alpha.130 — Construction-worker routing recovery\n\nReal play after Alpha.129 showed construction workers repeatedly standing in odd places and appearing to lose routes. This pass stays inside the existing construction system and treats that report as a movement-authority problem rather than adding teleportation or another worker layer.\n\n## Root cause\n\nAlpha.128 made builder spawn/home recovery reject obvious roofs, but ordinary building work still used `safeSurfaceCell`, which accepted the highest walkable collision surface at a coordinate. The Alpha.94 local-work shortcut also reused the 12-block protection margin. A builder on a nearby roof, log top or other artificial perch could therefore be considered locally valid and stop navigating. Separately, every idle builder was routed back to the exact same safe home coordinate, causing avoidable crowding/collision and poor-looking return paths as builder capacity increased.\n\n## Fix\n\n- active ordinary-building work uses a dedicated 4-block local envelope; the wider protection envelope is unchanged;\n- work, grading and home candidates share a ground/path-only staging-cell authority instead of arbitrary highest collision surfaces;\n- normal natural ground remains a heightmap fast path, while tree/structure cases use the existing bounded terrain scan;\n- idle and normalization return routing reserves distinct safe home cells for each builder;\n- blocked/elevated recovery avoids already occupied builder cells;\n- disconnected elevated legacy workers may still be recovered, while physically connected bridges/balconies remain real navigation;\n- no force loading, new save field, virtual material/cargo, repeated teleport loop or extra management UI is introduced.\n\n## Acceptance\n\nAutomated source/docs regression and Java 25 compile are required before the patch commits. A produced JAR may then go through the existing canonical build/runtime pipeline. Real graphical play must still verify that several builders visibly approach ordinary construction from ground, spread out when idle, and do not stall on a nearby roof/log structure. Multiplayer remains separately unverified until an actual LAN/server session is run.\n''')

# Fail closed if the intended authority did not land exactly once.
final_construction = read(CONSTRUCTION)
checks = {
    'active ground margin': 'ACTIVE_SITE_WORK_MARGIN = 4',
    'ground work cell': 'safeGroundWorkCell',
    'distinct idle homes': 'returnBuilderHome(level, data, builder, reservedHomes)',
    'occupied recovery': 'recoverBuilderFromBlockedCell(level, data, builder, recoveryOccupied)',
}
for label, needle in checks.items():
    if needle not in final_construction:
        raise SystemExit(f'{label}: patch did not land')
if 'safeSurfaceCell(' in final_construction:
    raise SystemExit('retired arbitrary highest-surface work authority remains')

print('Alpha.130 builder routing patch staged successfully')
