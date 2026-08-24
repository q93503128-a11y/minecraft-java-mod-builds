#!/usr/bin/env python3
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / 'src/main/java/kr/moonseungjun/frontiersettlement'


def read(path: Path) -> str:
    return path.read_text(encoding='utf-8')


def write(path: Path, text: str) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(text, encoding='utf-8')


def replace_once(path: Path, old: str, new: str) -> None:
    text = read(path)
    count = text.count(old)
    if count != 1:
        raise SystemExit(f'{path}: expected one patch anchor, found {count}: {old[:120]!r}')
    write(path, text.replace(old, new, 1))


road = JAVA / 'settlement/SettlementRoadService.java'

replace_once(road,
'''    private static final int MAX_TUNNEL_SPAN = 24;
    private static final int MIN_TUNNEL_COVER = 4;
    private static final int TUNNEL_CLEAR_HEIGHT = 3;
    private static final int TUNNEL_SURCHARGE_PER_CENTER = 1;''',
'''    private static final int MAX_TUNNEL_SPAN = 24;
    private static final int MIN_TUNNEL_SPAN = 3;
    private static final int MAX_TUNNEL_BENDS = 1;
    private static final int MIN_BENT_TUNNEL_LEG = 3;
    private static final int MIN_TUNNEL_COVER = 4;
    private static final int TUNNEL_CLEAR_HEIGHT = 3;
    private static final int TUNNEL_PORTAL_HALF_WIDTH = 2;
    private static final int TUNNEL_PORTAL_HEIGHT = 4;
    private static final int TUNNEL_PORTAL_FRAME_BLOCKS = 22;
    private static final int TUNNEL_SURCHARGE_PER_CENTER = 1;''')

replace_once(road,
'''    private record Placement(BlockPos pos, BlockState state, boolean bridge, boolean support, boolean tunnel) {}''',
'''    private record Placement(BlockPos pos, BlockState state, boolean bridge, boolean support,
                             boolean tunnel, boolean portal) {}''')

replace_once(road,
'''        int tunnels = tunnelCenterCount(chosen.profile());
        if (tunnels > 0 && (SettlementTier.current(data).ordinal() < SettlementTier.FRONTIER_TOWN.ordinal()
                || data.buildingCount(BuildingType.CONSTRUCTION_OFFICE) < 1)) {''',
'''        int tunnels = tunnelCenterCount(chosen.profile());
        int tunnelBends = tunnelBendCount(chosen.centers(), chosen.profile());
        int tunnelRuns = tunnelRunCount(chosen.profile());
        if (tunnels > 0 && (SettlementTier.current(data).ordinal() < SettlementTier.FRONTIER_TOWN.ordinal()
                || data.buildingCount(BuildingType.CONSTRUCTION_OFFICE) < 1)) {''')

replace_once(road,
'''        String tunnelDetail = tunnels == 0 ? "" : " · 터널 " + tunnels;
        String terrain = (stairs == 0 && bridges == 0 && tunnels == 0) ? ""
                : " | 계단 " + stairs + bridgeDetail + tunnelDetail;''',
'''        String tunnelDetail = tunnels == 0 ? "" : " · 터널 " + tunnels
                + (tunnelBends == 0 ? "" : " · 굴곡 " + tunnelBends)
                + " · 석재 포털 " + (tunnelRuns * 2);
        String terrain = (stairs == 0 && bridges == 0 && tunnels == 0) ? ""
                : " | 계단 " + stairs + bridgeDetail + tunnelDetail;''')

replace_once(road,
'''        int tunnels = tunnelCenterCount(chosen.profile());
        String tunnel = tunnels == 0 ? "" : " 직선 터널 " + tunnels + "블록 포함.";''',
'''        int tunnels = tunnelCenterCount(chosen.profile());
        int tunnelBends = tunnelBendCount(chosen.centers(), chosen.profile());
        int tunnelRuns = tunnelRunCount(chosen.profile());
        String tunnel = tunnels == 0 ? "" : " 터널 " + tunnels + "블록"
                + (tunnelBends == 0 ? "" : "(90도 굴곡 " + tunnelBends + "회)")
                + " + 실제 석재 포털 " + (tunnelRuns * 2) + "개 포함.";''')

replace_once(road,
'''    private static boolean canReplaceForPlacement(BlockState current, Placement placement) {
        if (placement.support()) {
            return current.isAir() || current.canBeReplaced() || current.getFluidState().is(FluidTags.WATER);
        }
        return current.isAir() || isRoadGround(current);
    }''',
'''    private static boolean canReplaceForPlacement(BlockState current, Placement placement) {
        if (placement.portal()) return current.isAir() || current.canBeReplaced();
        if (placement.support()) {
            return current.isAir() || current.canBeReplaced() || current.getFluidState().is(FluidTags.WATER);
        }
        return current.isAir() || isRoadGround(current);
    }''')

replace_once(road,
'''    private static boolean moveBuilderToPlacement(ServerLevel level, Villager builder, Placement placement) {
        if (!level.hasChunkAt(placement.pos())) {
            builder.getNavigation().stop();
            return false;
        }
        if (placement.tunnel()) {''',
'''    private static boolean moveBuilderToPlacement(ServerLevel level, Villager builder, Placement placement) {
        if (!level.hasChunkAt(placement.pos())) {
            builder.getNavigation().stop();
            return false;
        }
        if (placement.portal()) {
            BlockPos approach = tunnelPortalApproach(level, placement.pos());
            if (approach == null) return false;
            double distance = builder.distanceToSqr(approach.getX() + 0.5D, approach.getY() + 1.0D, approach.getZ() + 0.5D);
            if (distance <= BUILDER_WORK_RANGE_SQR) return true;
            builder.getNavigation().moveTo(approach.getX() + 0.5D, approach.getY() + 1.0D, approach.getZ() + 0.5D, 0.82D);
            return false;
        }
        if (placement.tunnel()) {''')

replace_once(road,
'''    private static BlockPos bridgeApproach(ServerLevel level, BlockPos target) {''',
'''    private static BlockPos tunnelPortalApproach(ServerLevel level, BlockPos target) {
        for (int down = 1; down <= TUNNEL_PORTAL_HEIGHT; down++) {
            int y = target.getY() - down;
            for (int dx = -TUNNEL_PORTAL_HALF_WIDTH; dx <= TUNNEL_PORTAL_HALF_WIDTH; dx++) {
                for (int dz = -TUNNEL_PORTAL_HALF_WIDTH; dz <= TUNNEL_PORTAL_HALF_WIDTH; dz++) {
                    BlockPos candidate = new BlockPos(target.getX() + dx, y, target.getZ() + dz);
                    if (!level.hasChunkAt(candidate)) continue;
                    if (isRoadPavingBlock(level.getBlockState(candidate))) return candidate;
                }
            }
        }
        return null;
    }

    private static BlockPos bridgeApproach(ServerLevel level, BlockPos target) {''')

replace_once(road,
'''        if (level.getBlockEntity(target) != null) return false;
        if (placement.support()) {''',
'''        if (level.getBlockEntity(target) != null) return false;
        if (placement.portal()) return tunnelPortalCellSafe(level, target);
        if (placement.support()) {''')

replace_once(road,
'''    private static void applyGradePlacement(ServerLevel level, Placement placement) {
        if (placement.support() || placement.tunnel()) return;''',
'''    private static void applyGradePlacement(ServerLevel level, Placement placement) {
        if (placement.support() || placement.tunnel() || placement.portal()) return;''')

replace_once(road,
'''                + candidate.supports().size() * BRIDGE_SUPPORT_SURCHARGE
                + tunnelCenterCount(candidate.profile()) * TUNNEL_SURCHARGE_PER_CENTER
                + stairCenterCount(candidate.centers(), candidate.profile()) * STAIR_SURCHARGE_PER_CENTER;''',
'''                + candidate.supports().size() * BRIDGE_SUPPORT_SURCHARGE
                + tunnelCenterCount(candidate.profile()) * TUNNEL_SURCHARGE_PER_CENTER
                + tunnelRunCount(candidate.profile()) * TUNNEL_PORTAL_FRAME_BLOCKS
                + stairCenterCount(candidate.centers(), candidate.profile()) * STAIR_SURCHARGE_PER_CENTER;''')

replace_once(road,
'''                + road.bridgeSupportCount() * BRIDGE_SUPPORT_SURCHARGE
                + road.tunnelCenterCount() * TUNNEL_SURCHARGE_PER_CENTER
                + stairCenterCount(road.centers(), road.profile()) * STAIR_SURCHARGE_PER_CENTER;''',
'''                + road.bridgeSupportCount() * BRIDGE_SUPPORT_SURCHARGE
                + road.tunnelCenterCount() * TUNNEL_SURCHARGE_PER_CENTER
                + tunnelRunCount(road.profile()) * TUNNEL_PORTAL_FRAME_BLOCKS
                + stairCenterCount(road.centers(), road.profile()) * STAIR_SURCHARGE_PER_CENTER;''')

replace_once(road,
'''        if (road.step() < plan.size()) return road.bridgeSupportCount() > 0 ? "도로 장교량·교각 석재 운반·시공"
                : road.bridgeCenterCount() > 0 ? "도로 계단·교량 석재 운반·포설" : "도로 석재 운반·포설";''',
'''        if (road.step() < plan.size()) return road.hasTunnel() ? "도로 터널 포장·석재 포털 운반·시공"
                : road.bridgeSupportCount() > 0 ? "도로 장교량·교각 석재 운반·시공"
                : road.bridgeCenterCount() > 0 ? "도로 계단·교량 석재 운반·포설" : "도로 석재 운반·포설";''')

replace_once(road,
'''        if (!placement.bridge()) {
            BlockState support = level.getBlockState(target.below());''',
'''        if (!placement.bridge() && !placement.portal()) {
            BlockState support = level.getBlockState(target.below());''')

replace_once(road,
'''            if (!placement.bridge()) {
                BlockState support = level.getBlockState(placement.pos().below());''',
'''            if (!placement.bridge() && !placement.portal()) {
                BlockState support = level.getBlockState(placement.pos().below());''')

replace_once(road,
'''                int dx = Integer.signum(flat.get(i).getX() - flat.get(i - 1).getX());
                int dz = Integer.signum(flat.get(i).getZ() - flat.get(i - 1).getZ());
                boolean straight = true;
                for (int j = i; j <= tunnelEnd + 1; j++) {
                    int sdx = Integer.signum(flat.get(j).getX() - flat.get(j - 1).getX());
                    int sdz = Integer.signum(flat.get(j).getZ() - flat.get(j - 1).getZ());
                    if (sdx != dx || sdz != dz) { straight = false; break; }
                }
                if (!straight) continue;
                bestEnd = tunnelEnd; bestY = grade; break;''',
'''                int span = tunnelEnd - i + 1;
                if (span < MIN_TUNNEL_SPAN) continue;
                int turns = tunnelTurnCount(flat, i, tunnelEnd);
                if (turns > MAX_TUNNEL_BENDS) continue;
                if (turns == 1 && !bentTunnelLegsLongEnough(flat, i, tunnelEnd)) continue;
                bestEnd = tunnelEnd; bestY = grade; break;''')

replace_once(road,
'''        SupportPlan supports = planBridgeSupports(level, centers, profile);
        if (!supports.valid()) return invalidCandidate(supports.message());''',
'''        String tunnelPortalError = validateTunnelPortals(level, data, centers, profile);
        if (!tunnelPortalError.isBlank()) return invalidCandidate(tunnelPortalError);
        score += tunnelBendCount(centers, profile) * 12;
        SupportPlan supports = planBridgeSupports(level, centers, profile);
        if (!supports.valid()) return invalidCandidate(supports.message());''')

old_tunnel_helpers = '''    private static boolean isNaturalTunnelExcavation(BlockState state) {
        return state.is(Blocks.DIRT) || state.is(Blocks.COARSE_DIRT) || state.is(Blocks.PODZOL)
                || state.is(Blocks.ROOTED_DIRT) || state.is(Blocks.STONE) || state.is(Blocks.DEEPSLATE)
                || state.is(Blocks.ANDESITE) || state.is(Blocks.DIORITE) || state.is(Blocks.GRANITE)
                || state.is(Blocks.TUFF) || state.is(Blocks.CLAY) || state.is(BlockTags.DIRT);
    }

    private static List<TunnelCell> tunnelExcavationPlan(RoadConstructionState road) {
        List<BlockPos> centers = road.centers();
        List<Integer> profile = road.profile();
        List<TunnelCell> cells = new ArrayList<>();
        for (int i = 0; i < centers.size(); i++) {
            if (i >= profile.size() || profile.get(i) != RoadConstructionState.PROFILE_TUNNEL) continue;
            BlockPos center = centers.get(i);
            BlockPos work = i > 0 ? centers.get(i - 1) : center;
            int[] direction = directionAt(centers, i);
            for (int side : new int[] {0, -1, 1}) {
                int x = center.getX() - direction[1] * side;
                int z = center.getZ() + direction[0] * side;
                for (int y = 1; y <= TUNNEL_CLEAR_HEIGHT; y++) {
                    cells.add(new TunnelCell(new BlockPos(x, center.getY() + y, z), work));
                }
            }
        }
        return List.copyOf(cells);
    }
'''
new_tunnel_helpers = '''    private static boolean isNaturalTunnelExcavation(BlockState state) {
        return state.is(Blocks.DIRT) || state.is(Blocks.COARSE_DIRT) || state.is(Blocks.PODZOL)
                || state.is(Blocks.ROOTED_DIRT) || state.is(Blocks.STONE) || state.is(Blocks.DEEPSLATE)
                || state.is(Blocks.ANDESITE) || state.is(Blocks.DIORITE) || state.is(Blocks.GRANITE)
                || state.is(Blocks.TUFF) || state.is(Blocks.CLAY) || state.is(BlockTags.DIRT);
    }

    private static boolean isNaturalTunnelPortalBlock(BlockState state) {
        return isNaturalTunnelExcavation(state) || state.is(Blocks.GRASS_BLOCK) || state.is(Blocks.SNOW)
                || state.is(Blocks.SNOW_BLOCK);
    }

    private static int tunnelTurnCount(List<BlockPos> centers, int runStart, int runEnd) {
        int turns = 0;
        int previousDx = 0;
        int previousDz = 0;
        int firstEdge = Math.max(1, runStart);
        int lastEdge = Math.min(centers.size() - 1, runEnd + 1);
        for (int edge = firstEdge; edge <= lastEdge; edge++) {
            int dx = Integer.signum(centers.get(edge).getX() - centers.get(edge - 1).getX());
            int dz = Integer.signum(centers.get(edge).getZ() - centers.get(edge - 1).getZ());
            if (Math.abs(dx) + Math.abs(dz) != 1) return Integer.MAX_VALUE;
            if (previousDx != 0 || previousDz != 0) {
                if (dx != previousDx || dz != previousDz) turns++;
            }
            previousDx = dx;
            previousDz = dz;
        }
        return turns;
    }

    private static boolean bentTunnelLegsLongEnough(List<BlockPos> centers, int runStart, int runEnd) {
        int firstDx = Integer.signum(centers.get(runStart).getX() - centers.get(runStart - 1).getX());
        int firstDz = Integer.signum(centers.get(runStart).getZ() - centers.get(runStart - 1).getZ());
        for (int edge = runStart + 1; edge <= runEnd + 1 && edge < centers.size(); edge++) {
            int dx = Integer.signum(centers.get(edge).getX() - centers.get(edge - 1).getX());
            int dz = Integer.signum(centers.get(edge).getZ() - centers.get(edge - 1).getZ());
            if (dx == firstDx && dz == firstDz) continue;
            int beforeTurn = edge - runStart;
            int afterTurn = runEnd - edge + 2;
            return beforeTurn >= MIN_BENT_TUNNEL_LEG && afterTurn >= MIN_BENT_TUNNEL_LEG;
        }
        return true;
    }

    private static int tunnelRunCount(List<Integer> profile) {
        int runs = 0;
        boolean inside = false;
        for (int value : profile) {
            boolean tunnel = value == RoadConstructionState.PROFILE_TUNNEL;
            if (tunnel && !inside) runs++;
            inside = tunnel;
        }
        return runs;
    }

    private static int tunnelBendCount(List<BlockPos> centers, List<Integer> profile) {
        int bends = 0;
        int runStart = 0;
        while (runStart < centers.size()) {
            if (runStart >= profile.size() || profile.get(runStart) != RoadConstructionState.PROFILE_TUNNEL) {
                runStart++;
                continue;
            }
            int runEnd = runStart;
            while (runEnd + 1 < centers.size() && runEnd + 1 < profile.size()
                    && profile.get(runEnd + 1) == RoadConstructionState.PROFILE_TUNNEL) runEnd++;
            int count = tunnelTurnCount(centers, runStart, runEnd);
            if (count != Integer.MAX_VALUE) bends += count;
            runStart = runEnd + 1;
        }
        return bends;
    }

    private static List<BlockPos> tunnelPortalFrameAt(List<BlockPos> centers, int index) {
        if (index < 0 || index >= centers.size()) return List.of();
        BlockPos center = centers.get(index);
        int[] direction = directionAt(centers, index);
        List<BlockPos> frame = new ArrayList<>(11);
        for (int side : new int[] {-TUNNEL_PORTAL_HALF_WIDTH, TUNNEL_PORTAL_HALF_WIDTH}) {
            int x = center.getX() - direction[1] * side;
            int z = center.getZ() + direction[0] * side;
            for (int y = 1; y < TUNNEL_PORTAL_HEIGHT; y++) {
                frame.add(new BlockPos(x, center.getY() + y, z));
            }
        }
        for (int side = -TUNNEL_PORTAL_HALF_WIDTH; side <= TUNNEL_PORTAL_HALF_WIDTH; side++) {
            int x = center.getX() - direction[1] * side;
            int z = center.getZ() + direction[0] * side;
            frame.add(new BlockPos(x, center.getY() + TUNNEL_PORTAL_HEIGHT, z));
        }
        return List.copyOf(frame);
    }

    private static List<BlockPos> tunnelPortalFramePositions(List<BlockPos> centers, List<Integer> profile) {
        Set<BlockPos> frames = new LinkedHashSet<>();
        int runStart = 0;
        while (runStart < centers.size()) {
            if (runStart >= profile.size() || profile.get(runStart) != RoadConstructionState.PROFILE_TUNNEL) {
                runStart++;
                continue;
            }
            int runEnd = runStart;
            while (runEnd + 1 < centers.size() && runEnd + 1 < profile.size()
                    && profile.get(runEnd + 1) == RoadConstructionState.PROFILE_TUNNEL) runEnd++;
            frames.addAll(tunnelPortalFrameAt(centers, runStart));
            frames.addAll(tunnelPortalFrameAt(centers, runEnd));
            runStart = runEnd + 1;
        }
        return List.copyOf(frames);
    }

    private static boolean tunnelPortalCellSafe(ServerLevel level, BlockPos pos) {
        if (!level.hasChunkAt(pos) || level.getBlockEntity(pos) != null) return false;
        BlockState state = level.getBlockState(pos);
        if (!state.getFluidState().isEmpty()) return false;
        return state.isAir() || state.canBeReplaced() || state.is(Blocks.STONE_BRICKS)
                || isNaturalTunnelPortalBlock(state);
    }

    private static String validateTunnelPortals(ServerLevel level, SettlementData data,
                                                List<BlockPos> centers, List<Integer> profile) {
        for (BlockPos pos : tunnelPortalFramePositions(centers, profile)) {
            if (overlapsBuildingOrOutpost(data, pos)) return "터널 석재 포털이 기존 건물이나 전초기지와 겹칩니다.";
            if (overlapsExistingRoad(data.roads(), pos)) return "터널 석재 포털이 기존 도로와 겹칩니다.";
            if (!tunnelPortalCellSafe(level, pos)) {
                return "터널 석재 포털 범위에 광석·유체·컨테이너·플레이어/비자연 블록이 있습니다.";
            }
        }
        return "";
    }

    private static List<TunnelCell> tunnelExcavationPlan(RoadConstructionState road) {
        List<BlockPos> centers = road.centers();
        List<Integer> profile = road.profile();
        Map<BlockPos, BlockPos> ordered = new LinkedHashMap<>();
        int runStart = 0;
        while (runStart < centers.size()) {
            if (runStart >= profile.size() || profile.get(runStart) != RoadConstructionState.PROFILE_TUNNEL) {
                runStart++;
                continue;
            }
            int runEnd = runStart;
            while (runEnd + 1 < centers.size() && runEnd + 1 < profile.size()
                    && profile.get(runEnd + 1) == RoadConstructionState.PROFILE_TUNNEL) runEnd++;
            BlockPos startWork = runStart > 0 ? centers.get(runStart - 1) : centers.get(runStart);
            for (BlockPos frame : tunnelPortalFrameAt(centers, runStart)) ordered.putIfAbsent(frame, startWork);
            for (int i = runStart; i <= runEnd; i++) {
                BlockPos center = centers.get(i);
                BlockPos work = i > runStart ? centers.get(i - 1) : startWork;
                int[] direction = directionAt(centers, i);
                for (int side : new int[] {0, -1, 1}) {
                    int x = center.getX() - direction[1] * side;
                    int z = center.getZ() + direction[0] * side;
                    for (int y = 1; y <= TUNNEL_CLEAR_HEIGHT; y++) {
                        ordered.putIfAbsent(new BlockPos(x, center.getY() + y, z), work);
                    }
                }
            }
            BlockPos endWork = centers.get(runEnd);
            for (BlockPos frame : tunnelPortalFrameAt(centers, runEnd)) ordered.putIfAbsent(frame, endWork);
            runStart = runEnd + 1;
        }
        List<TunnelCell> cells = new ArrayList<>(ordered.size());
        for (Map.Entry<BlockPos, BlockPos> entry : ordered.entrySet()) {
            cells.add(new TunnelCell(entry.getKey(), entry.getValue()));
        }
        return List.copyOf(cells);
    }
'''
replace_once(road, old_tunnel_helpers, new_tunnel_helpers)

replace_once(road,
'''        List<Placement> placements = new ArrayList<>(footprints.size() + road.bridgeSupportCount());''',
'''        List<BlockPos> tunnelPortals = tunnelPortalFramePositions(centers, road.profile());
        List<Placement> placements = new ArrayList<>(footprints.size() + road.bridgeSupportCount() + tunnelPortals.size());''')

replace_once(road,
'''            placements.add(new Placement(entry.getKey(), state, spec.bridge(), false, spec.tunnel()));''',
'''            placements.add(new Placement(entry.getKey(), state, spec.bridge(), false, spec.tunnel(), false));''')

replace_once(road,
'''            placements.add(new Placement(support, Blocks.STONE_BRICKS.defaultBlockState(), true, true, false));
        }
        return placements;''',
'''            placements.add(new Placement(support, Blocks.STONE_BRICKS.defaultBlockState(), true, true, false, false));
        }
        for (BlockPos portal : tunnelPortals) {
            placements.add(new Placement(portal, Blocks.STONE_BRICKS.defaultBlockState(), false, false, true, true));
        }
        return placements;''')

# Build/version contract.
props = ROOT / 'gradle.properties'
replace_once(props, 'mod_version=0.1.0-alpha.53', 'mod_version=0.1.0-alpha.54')
replace_once(props,
'physical save-compatible straight tunnel excavation.',
'physical save-compatible straight tunnel excavation, plus bounded one-bend tunnel public works with physically hauled stone-brick portals.')

lock = ROOT / 'COMPANION_LOCK.json'
replace_once(lock, '"frontier_settlement": "0.1.0-alpha.53"', '"frontier_settlement": "0.1.0-alpha.54"')
replace_once(lock,
'    "Alpha.53 adds bounded automatic straight tunnel excavation inside the existing road authority: only already-loaded natural non-ore/non-fluid blocks are removed without drops, the same builder advances from the open tunnel face, road stone remains physical ItemStack authority, and no worldgen/companion hard dependency is added.",\n',
'    "Alpha.53 adds bounded automatic straight tunnel excavation inside the existing road authority: only already-loaded natural non-ore/non-fluid blocks are removed without drops, the same builder advances from the open tunnel face, road stone remains physical ItemStack authority, and no worldgen/companion hard dependency is added.",\n    "Alpha.54 keeps the same 24-cell tunnel ceiling but permits one bounded 90-degree bend with at least three tunnel centers on each leg; two deterministic 5-wide by 4-high STONE_BRICKS portal frames are excavated safely and then built from the same physically hauled road-stone authority, with no new logistics or companion dependency.",\n')
replace_once(lock, 'so Alpha.53 keeps only HUD collision avoidance', 'so Alpha.54 keeps only HUD collision avoidance')

# README.
readme = ROOT / 'README.md'
replace_once(readme, '## Current version: 0.1.0-alpha.53', '## Current version: 0.1.0-alpha.54')
replace_once(readme, 'No new Alpha.53 key was added.', 'No new Alpha.54 key was added.')
replace_once(readme, 'Alpha.40–52 deepen existing systems', 'Alpha.40–54 deepen existing systems')
alpha54_readme = '''## Alpha.54 — bounded one-bend tunnels and physical portals

Alpha.54 deepens the same automatic road/tunnel authority instead of making tunnels longer or adding another tool.

- the total automatic tunnel ceiling remains **24 centerline cells**;
- one tunnel run may contain at most **one 90-degree bend**, and both legs around that bend must contain at least **3 tunnel centers** so the corner is not a tiny accidental notch;
- the persisted centerline path + existing `PROFILE_TUNNEL=2` fully determines the bend after save/reload; there is no new save authority or route controller;
- the same width-3 / clear-height-3 conservative excavation remains in force, including loaded-only, non-ore, non-fluid, non-container, non-player-block validation;
- each tunnel run receives exactly **two deterministic stone-brick portal frames**, one at each end. The frame envelope is **5 blocks wide × 4 blocks high** and is validated before approval;
- portal frame cells are included in the no-drop physical excavation phase, then the same road builder carries real settlement stone and places `STONE_BRICKS` during the established paving phase;
- the two frames add **22 real-stone units per tunnel run**; no visual-only/free portal block, virtual stone or excavated-stone refund exists;
- active tunnel interior/floor/portal cells remain project-protected, and unsafe edits pause rather than being overwritten;
- completed bent tunnels are still ordinary `RoadSegment`s. Alpha.27 remains the **single authority for outpost transport**, **Transport workers belong to a specific outpost**, and workers **pause at unloaded route boundaries**;
- no new key, building family, currency, dashboard, second builder, second logistics authority, force-load or teleport is introduced.

Alpha.54 closes the first qualitative curved/monumental tunnel slice without raising the destruction ceiling. Very-long bores, underground stations and unrestricted mountain deletion remain outside the intended product.

'''
replace_once(readme, '## Alpha.53 — bounded straight road tunnels\n', alpha54_readme + '## Alpha.53 — bounded straight road tunnels\n')
replace_once(readme,
'This is the first bounded road-tunnel slice. Curved tunnels, very long bores, underground stations and unrestricted mountain deletion remain outside the current pass.',
'This is the first bounded road-tunnel slice. Alpha.54 adds the bounded one-bend/portal pass; very long bores, underground stations and unrestricted mountain deletion remain outside the intended product.')

# Canonical plan.
canonical = ROOT / 'CANONICAL_PLAN.md'
replace_once(canonical, 'Current canonical implementation: **0.1.0-alpha.53**.', 'Current canonical implementation: **0.1.0-alpha.54**.')
replace_once(canonical, 'Alpha.40–53 deepen systems', 'Alpha.40–54 deepen systems')
alpha54_canonical = '''### Alpha.54 bounded one-bend tunnel / physical portal pass

Alpha.54 is deliberately qualitative rather than a larger WorldEdit envelope. The total tunnel run remains max24 cells, while one persisted Manhattan bend and physical portal presentation are added inside the same road authority.

- max tunnel run remains24 centerline cells; no larger excavation cap;
- at most one90-degree centerline bend is accepted, with at least3 tunnel centers on both legs around the turn;
- bend geometry is reconstructed only from persisted road centers + `PROFILE_TUNNEL=2`, so old saves and Alpha.53 phase encoding remain compatible;
- tunnel interior remains width3 / clear height3 and keeps the same loaded natural non-ore/non-fluid/no-cave/no-player-block safety contract;
- each tunnel run deterministically owns two 5-wide × 4-high stone-brick portal frames;
- portal cells are prevalidated for loaded/block-entity/fluid/non-natural/infrastructure overlap and are included in the no-drop tunnel excavation plan;
- the existing road builder physically advances through excavation, then hauls real settlement stone and places `STONE_BRICKS` portal cells through the same paving authority;
- each run adds22 real-stone portal units; excavation never mints replacement stone, earthBank or currency;
- active tunnel interior, floor and portal cells are project-protected;
- successful road/portal placement precedes carried-stone consumption/state advance, retaining Alpha.52/53 physical authority;
- completed tunnel is still one ordinary `RoadSegment`: Alpha.27 remains the **single authority for outpost transport** and **there is still only one authority for long-distance outpost transport**;
- **Transport workers belong to a specific outpost** and **pause at unloaded route boundaries**;
- no new key/building/currency/dashboard, second builder/economy/logistics authority, force-load, teleport or hard companion dependency.

This closes the first bounded straight + single-bend tunnel breadth. Very-long bores, underground stations and unrestricted mountain deletion are not required to call the original road/civil loop functionally broad; real-play acceptance still governs release readiness.

'''
replace_once(canonical, '## 10. Exploration, crafting and settlement feedback\n', alpha54_canonical + '## 10. Exploration, crafting and settlement feedback\n')
replace_once(canonical,
'Curved tunnels, >24-cell bores, underground stations and unrestricted mountain deletion remain unfinished/outside this slice.',
'Alpha.54 adds one bounded 90-degree bend and physical portals. >24-cell bores, underground stations and unrestricted mountain deletion remain outside the intended product.')
replace_once(canonical, 'Alpha.53 road/civil work reads already-loaded', 'Alpha.54 road/civil work reads already-loaded')
replace_once(canonical, '## 14. Current playable slice after Alpha.53', '## 14. Current playable slice after Alpha.54')
replace_once(canonical,
'- physical roads/stairs/short bridges, Alpha.52 bounded long bridges, and Alpha.53 bounded straight physical road tunnels;',
'- physical roads/stairs/short bridges, Alpha.52 bounded long bridges, Alpha.53 straight tunnels, and Alpha.54 bounded one-bend tunnels with physically built stone-brick portals;')
replace_once(canonical, '## 15. Unfinished original-scope priorities after Alpha.53', '## 15. Unfinished original-scope priorities after Alpha.54')
replace_once(canonical,
'''1. **deeper monumental crossing civil-engineering pass** — curved/longer special crossings only if they stay bounded, physical and non-WorldEdit;
2. deeper exploration bridges — rare NPC/structure/boss-specific settlement value only where soft, non-farmable and meaningful;
3. better companion-biome-aware outpost specialization where a stable data seam exists;
4. physical military armory/loadout only if it can stay automated and ItemStack-authoritative without per-soldier micromanagement;
5. long survival + two-player multiplayer acceptance;''',
'''1. deeper exploration bridges — rare NPC/structure/boss-specific settlement value only where soft, non-farmable and meaningful;
2. better companion-biome-aware outpost specialization where a stable data seam exists;
3. physical military armory/loadout only if it can stay automated and ItemStack-authoritative without per-soldier micromanagement;
4. long survival + two-player multiplayer acceptance;
5. optional deeper monumental crossings only if real play shows Alpha.52–54 breadth is insufficient; never expand by default into WorldEdit-scale civil works;''')
replace_once(canonical,
'''12. Alpha.53 tunnel detection/excavation/save-reload/pathing/no-drop/player-protection acceptance;
13. full companion lock fresh-world client/server runtime;
14. true Xaero markers only if a stable supported API appears;
15. moving boat/waterborne merchant only if presentation value justifies it and it never becomes a second logistics authority.''',
'''12. Alpha.53 tunnel detection/excavation/save-reload/pathing/no-drop/player-protection acceptance;
13. Alpha.54 one-bend detection/corner clearance/portal excavation/22-stone physical portal/save-reload acceptance;
14. full companion lock fresh-world client/server runtime;
15. true Xaero markers only if a stable supported API appears;
16. moving boat/waterborne merchant only if presentation value justifies it and it never becomes a second logistics authority.''')

# Gap audit.
gap = ROOT / 'COMPLETION_GAP_AUDIT.md'
replace_once(gap, '현재 구현 기준: `0.1.0-alpha.53`', '현재 구현 기준: `0.1.0-alpha.54`')
replace_once(gap,
'이 문서는 현재 구현에 맞춰 원본 v0.2 범위를 축소하지 않는다. Alpha.53에서 bounded 직선 터널 1차까지 추가되어도 더 깊은 기념비급 토목, 실물 군사 armory, 일부 탐험/전초 breadth, 장시간 multiplayer 및 full companion runtime이 남아 있는 동안 완성이라고 부르지 않는다.',
'이 문서는 현재 구현에 맞춰 원본 v0.2 범위를 축소하지 않는다. Alpha.54에서 bounded 단일굴곡 터널과 실제 석재 포털까지 추가되어도 실물 군사 armory, 일부 탐험/전초 breadth, 장시간 multiplayer 및 full companion runtime이 남아 있는 동안 완성이라고 부르지 않는다.')
replace_once(gap,
'''| 직선 도로 터널 | **완료/부분** | Alpha.53 max24, width3/clear-height3, no-drop physical excavation first pass |
| 더 깊은/곡선 기념비급 토목 | **미구현/부분** | Alpha.53 범위 밖 |''',
'''| 직선 도로 터널 | **완료/부분** | Alpha.53 max24, width3/clear-height3, no-drop physical excavation first pass |
| 단일굴곡 터널/석재 포털 | **완료/부분** | Alpha.54 max24 유지, 90도 1회, 양 leg 최소3, 5×4 portal 2개/실물 stone22 |
| 더 거대한 기념비급 토목 | **선택/부분** | real-play에서 실제 필요성이 확인될 때만; WorldEdit식 확대는 범위 밖 |''')
alpha54_gap = '''### Alpha.54 one-bend tunnel / physical portal 감사

- Alpha.53 max24 ceiling 그대로 유지, 단순 수치 확대 없음;
- 한 tunnel run에서 90도 turn 최대1회;
- bend 양쪽 tunnel leg 최소3 center;
- persisted centers + `PROFILE_TUNNEL=2`만으로 save/reload bend 재구성, 새 save authority 없음;
- 기존 width3 / clear-height3 / loaded-only / non-ore / non-fluid / no-cave / player-block protection 유지;
- tunnel run당 입구/출구 5폭 × 4높이 `STONE_BRICKS` portal frame 2개 결정론적 계획;
- portal frame 전체도 block entity/fluid/non-natural/infrastructure overlap 사전 거부;
- portal 자리 자연 블록은 기존 tunneling phase에서 one-cell `setBlock(AIR)` no-drop 굴착;
- portal stone은 run당22 실제 stone 비용, 같은 road builder가 settlement storage에서 물리 운반;
- portal world placement 성공 뒤 기존 paving ItemStack consume/state advance 권위 사용;
- active interior/floor/portal protection 유지;
- completed road는 same RoadSegment, `single authority for outpost transport` / `there is still only one authority for long-distance outpost transport` 유지;
- **Transport workers belong to a specific outpost** / **pause at unloaded route boundaries** 유지;
- 새 key/building/currency/dashboard/force-load/teleport/second authority 없음.

따라서 Alpha.52–54에서 장교량 + 직선 터널 + bounded 단일굴곡/실물 포털까지 첫 대형 횡단 breadth가 형성됐다. 더 큰 토목은 자동 다음 우선순위가 아니라 실플레이 필요성으로만 재개한다.

'''
replace_once(gap, '## 4. 주민 / 생산 / 방어\n', alpha54_gap + '## 4. 주민 / 생산 / 방어\n')
replace_once(gap,
'따라서 **bounded straight tunnel은 완료/부분**으로 전진했다. curved/very-long/underground-station/monumental crossing은 여전히 미구현/부분이다.',
'따라서 **bounded straight tunnel은 완료/부분**으로 전진했다. Alpha.54는 single-bend/portal breadth를 추가하며 very-long/underground-station/WorldEdit-scale bores는 범위 밖으로 유지한다.')
replace_once(gap,
'''1. **deeper monumental crossing civil-engineering pass** — Alpha.52 long bridge보다 큰/복잡한 crossing breadth를 실물 자원·player protection 안에서 구현;
2. deeper exploration bridges — rare NPC/structure/boss별 정착 가치;
3. stable seam이 있을 때 companion-biome-aware outpost specialization;
4. per-soldier micromanagement 없이 가능한 physical military armory/loadout;
5. long survival + two-player multiplayer acceptance;''',
'''1. deeper exploration bridges — rare NPC/structure/boss별 정착 가치;
2. stable seam이 있을 때 companion-biome-aware outpost specialization;
3. per-soldier micromanagement 없이 가능한 physical military armory/loadout;
4. long survival + two-player multiplayer acceptance;
5. optional deeper monumental crossing은 Alpha.52–54 실플레이에서 실제 부족이 확인될 때만;''')
replace_once(gap,
'''11. Alpha.53 tunnel detection/excavation/save-reload/pathing/no-drop/protection acceptance;
12. full companion lock fresh-world client/server runtime;
12. true Xaero marker는 stable supported API가 생길 때만;
13. moving boat/waterborne merchant는 두 번째 logistics authority가 되지 않는 경우에만 선택적 presentation.''',
'''11. Alpha.53 tunnel detection/excavation/save-reload/pathing/no-drop/protection acceptance;
12. Alpha.54 one-bend/corner clearance/portal excavation/physical stone22/save-reload acceptance;
13. full companion lock fresh-world client/server runtime;
14. true Xaero marker는 stable supported API가 생길 때만;
15. moving boat/waterborne merchant는 두 번째 logistics authority가 되지 않는 경우에만 선택적 presentation.''')

# Alpha.54 cumulative source audit.
source_audit = ROOT / 'tools/test_alpha54_source.py'
write(source_audit, '''#!/usr/bin/env python3
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
JAVA=ROOT/'src/main/java/kr/moonseungjun/frontiersettlement'
A53=ROOT/'tools/test_alpha53_source.py'
def text(p): return p.read_text(encoding='utf-8')
def must(s,tokens,label):
    for t in tokens:
        if t not in s: raise SystemExit(f'{label} missing: {t}')
def forbid(s,tokens,label):
    for t in tokens:
        if t in s: raise SystemExit(f'{label}: {t}')
if not A53.exists(): raise SystemExit('historical Alpha.53 audit missing')
a=text(A53).replace("print('Frontier Settlement alpha.23-53 cumulative source audit: PASS')",'pass').replace('0.1.0-alpha.53','0.1.0-alpha.54')
ns={'__file__':str(A53),'__name__':'__main__'}
exec(compile(a,str(A53),'exec'),ns,ns)
road=text(JAVA/'settlement/SettlementRoadService.java'); props=text(ROOT/'gradle.properties'); lock=text(ROOT/'COMPANION_LOCK.json')
must(road,('MAX_TUNNEL_SPAN = 24','MIN_TUNNEL_SPAN = 3','MAX_TUNNEL_BENDS = 1','MIN_BENT_TUNNEL_LEG = 3',
           'TUNNEL_PORTAL_HALF_WIDTH = 2','TUNNEL_PORTAL_HEIGHT = 4','TUNNEL_PORTAL_FRAME_BLOCKS = 22',
           'boolean tunnel, boolean portal','tunnelTurnCount(','bentTunnelLegsLongEnough(','tunnelBendCount(',
           'tunnelRunCount(','tunnelPortalFrameAt(','tunnelPortalFramePositions(','validateTunnelPortals(',
           'tunnelPortalCellSafe(','placement.portal()','tunnelPortalApproach(',
           'tunnelRunCount(candidate.profile()) * TUNNEL_PORTAL_FRAME_BLOCKS',
           'tunnelRunCount(road.profile()) * TUNNEL_PORTAL_FRAME_BLOCKS',
           'new Placement(portal, Blocks.STONE_BRICKS.defaultBlockState(), false, false, true, true)',
           'turns > MAX_TUNNEL_BENDS','!bentTunnelLegsLongEnough(flat, i, tunnelEnd)',
           '터널 석재 포털 범위에 광석·유체·컨테이너·플레이어/비자연 블록이 있습니다.'), 'alpha.54 bent tunnel/portal authority')
forbid(road,('destroyBlock(','dropResources(','forceChunk','setChunkForced','teleportTo('),'alpha.54 safety')
must(props,('mod_version=0.1.0-alpha.54','physical save-compatible straight tunnel excavation','bounded one-bend tunnel public works'),'alpha.54 props')
must(lock,('"frontier_settlement": "0.1.0-alpha.54"','Alpha.54 keeps the same 24-cell tunnel ceiling','STONE_BRICKS portal frames','no new logistics or companion dependency','"status": "candidate_runtime_lock"'),'alpha.54 lock')
print('Frontier Settlement alpha.23-54 cumulative source audit: PASS')
''')

# Alpha.54 docs audit.
docs_audit = ROOT / 'tools/test_alpha54_docs.py'
write(docs_audit, '''#!/usr/bin/env python3
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
def text(n): return (ROOT/n).read_text(encoding='utf-8')
def must(s,tokens,label):
    for t in tokens:
        if t not in s: raise SystemExit(f'{label} missing: {t}')
original=text('ORIGINAL_DESIGN_v0.2.md'); readme=text('README.md'); canonical=text('CANONICAL_PLAN.md'); gap=text('COMPLETION_GAP_AUDIT.md')
must(original,('도로는 시작점·끝점·필요 시 중간점만 지정한다.','경로는 급경사와 건물을 피하고, 작은 계단·교량을 자동으로 포함한다.','과도한 월드 수정은 금지한다.'),'original scope')
must(readme,('## Current version: 0.1.0-alpha.54','## Alpha.54 — bounded one-bend tunnels and physical portals','24 centerline cells','one 90-degree bend','3 tunnel centers','5 blocks wide × 4 blocks high','22 real-stone units','single authority for outpost transport','Transport workers belong to a specific outpost','pause at unloaded route boundaries'),'alpha.54 README')
must(canonical,('Current canonical implementation: **0.1.0-alpha.54**','### Alpha.54 bounded one-bend tunnel / physical portal pass','at most one90-degree','at least3 tunnel centers','two 5-wide × 4-high stone-brick portal frames','each run adds22 real-stone portal units','there is still only one authority for long-distance outpost transport','## 14. Current playable slice after Alpha.54','## 15. Unfinished original-scope priorities after Alpha.54'),'alpha.54 canonical')
must(gap,('현재 구현 기준: `0.1.0-alpha.54`','### Alpha.54 one-bend tunnel / physical portal 감사','90도 turn 최대1회','양쪽 tunnel leg 최소3','5폭 × 4높이','run당22 실제 stone 비용','더 큰 토목은 자동 다음 우선순위가 아니라','## 11. 완료 판정 금지선'),'alpha.54 gap')
print('Frontier Settlement alpha.54 canonical docs audit: PASS')
''')

print('Applied Frontier Settlement 0.1.0-alpha.54 bounded one-bend tunnel and physical portal pass.')
