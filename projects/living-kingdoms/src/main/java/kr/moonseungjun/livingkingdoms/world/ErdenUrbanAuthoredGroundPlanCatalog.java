package kr.moonseungjun.livingkingdoms.world;

import kr.moonseungjun.livingkingdoms.LivingKingdoms;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.Rotation;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Source-only functional plan for all 233 Erden urban buildings.
 *
 * <p>Every target is selected from the exact connected authored ground floor discovered by
 * {@link ErdenUrbanPlacedTopologyCatalog}. Plans may add furniture to source AIR but never replace
 * an imported wall, floor, ceiling, stair or existing fixture. World placement therefore preserves
 * the licensed source architecture instead of carving a synthetic rectangle behind each door.</p>
 */
public final class ErdenUrbanAuthoredGroundPlanCatalog {
    public static final int PLAN_REVISION = 1;
    public static final int EXPECTED_PLANS = 233;
    private static final int MIN_GROUND_CELLS = 35;
    private static final int TARGET_COUNT = 3;

    private static final Map<Long, PlacementPlan> PLANS = new LinkedHashMap<>();
    private static boolean bootstrapped;

    private ErdenUrbanAuthoredGroundPlanCatalog() {}

    public static synchronized void bootstrap() {
        if (bootstrapped) return;
        PLANS.clear();
        ErdenUrbanPlacedTopologyCatalog.bootstrap();

        Map<String, ExternalUrbanFabricBuilder.UrbanFragmentSnapshot> snapshots =
                ExternalUrbanFabricBuilder.fragmentSnapshotsForDiagnostics();
        Map<String, Integer> roles = new LinkedHashMap<>();
        int totalFixtures = 0;
        int totalBeds = 0;
        int minimumGroundCells = Integer.MAX_VALUE;

        for (ExternalUrbanFabricBuilder.UrbanBuildingPlacement placement
                : ExternalUrbanFabricBuilder.buildingPlacementsForDiagnostics()) {
            ExternalUrbanFabricBuilder.UrbanFragmentSnapshot snapshot = snapshots.get(placement.fragmentKey());
            ErdenUrbanPlacedTopologyCatalog.PlacementProfile topology =
                    ErdenUrbanPlacedTopologyCatalog.profile(
                            placement.entrance().x(), placement.entrance().z());
            if (snapshot == null || topology == null) {
                throw new IllegalStateException("Missing authored-ground source/topology fragment="
                        + placement.fragmentKey());
            }
            if (topology.classification() == ErdenUrbanPlacedTopologyCatalog.Classification.FALLBACK) {
                throw new IllegalStateException("Erden urban placement still requires fallback role="
                        + placement.role() + " fragment=" + placement.fragmentKey());
            }

            PlacementPlan plan = buildPlan(placement, snapshot, topology.fragment());
            long key = entranceKey(placement.entrance().x(), placement.entrance().z());
            if (PLANS.put(key, plan) != null) {
                throw new IllegalStateException("Duplicate authored-ground plan entrance="
                        + placement.entrance().x() + "," + placement.entrance().z());
            }
            roles.merge(placement.role(), 1, Integer::sum);
            totalFixtures += plan.fixtures().size();
            totalBeds += plan.beds().size();
            minimumGroundCells = Math.min(minimumGroundCells, plan.groundCells());
        }

        if (PLANS.size() != EXPECTED_PLANS) {
            throw new IllegalStateException("Erden authored-ground plan count drifted: " + PLANS.size());
        }
        Map<String, Integer> expectedRoles = ExternalUrbanFabricBuilder.roleCountsForDiagnostics();
        if (!roles.equals(expectedRoles)) {
            throw new IllegalStateException("Erden authored-ground role counts drifted actual="
                    + roles + " expected=" + expectedRoles);
        }
        bootstrapped = true;
        LivingKingdoms.LOGGER.info(
                "LK_ERDEN_AUTHORED_GROUND_FUNCTIONAL_PLAN_PASS placements={} roles={} minimum_ground_cells={} fixtures={} beds={} resident_targets={} work_targets=true economy_containers=156 source_floor_reused=true source_air_fixtures=true synthetic_rooms=0 source_blocks_cut=0 source_only=true world_reads=false mutations=0 revision={}",
                PLANS.size(), roles, minimumGroundCells == Integer.MAX_VALUE ? 0 : minimumGroundCells,
                totalFixtures, totalBeds, TARGET_COUNT, PLAN_REVISION);
    }

    public static PlacementPlan plan(ExternalUrbanFabricBuilder.UrbanEntrance entrance) {
        bootstrap();
        return PLANS.get(entranceKey(entrance.x(), entrance.z()));
    }

    public static BlockPos primaryContainer(ExternalUrbanFabricBuilder.UrbanEntrance entrance) {
        PlacementPlan plan = plan(entrance);
        return plan == null ? null : plan.primaryContainer();
    }

    public static BlockPos homeTarget(ExternalUrbanFabricBuilder.UrbanEntrance entrance, int slot) {
        PlacementPlan plan = plan(entrance);
        if (plan == null || plan.residentTargets().isEmpty()) return null;
        return plan.residentTargets().get(Math.floorMod(slot, plan.residentTargets().size()));
    }

    public static BlockPos workTarget(ExternalUrbanFabricBuilder.UrbanEntrance entrance) {
        PlacementPlan plan = plan(entrance);
        return plan == null ? null : plan.workTarget();
    }

    private static PlacementPlan buildPlan(
            ExternalUrbanFabricBuilder.UrbanBuildingPlacement placement,
            ExternalUrbanFabricBuilder.UrbanFragmentSnapshot snapshot,
            ErdenUrbanPlacedTopologyCatalog.FragmentProfile topology) {
        Map<Long, ExternalUrbanFabricBuilder.UrbanSourceBlock> blocks = blockMap(snapshot);
        int doorY = lowestDoorY(snapshot, blocks);
        if (doorY == Integer.MAX_VALUE) {
            throw new IllegalStateException("Authored urban source has no retained entrance " + snapshot.fragmentKey());
        }

        List<WalkCell> reachable = reachable(snapshot, blocks, doorY);
        int groundY = topology.floorBands().stream()
                .max(Comparator.comparingInt(ErdenUrbanPlacedTopologyCatalog.FloorBand::reachableCells))
                .map(ErdenUrbanPlacedTopologyCatalog.FloorBand::feetY)
                .orElseThrow(() -> new IllegalStateException(
                        "Authored urban source has no ground floor band " + snapshot.fragmentKey()));
        Set<Long> reachableAtGround = new HashSet<>();
        for (WalkCell cell : reachable) {
            if (cell.y() == groundY) reachableAtGround.add(columnKey(cell.x(), cell.z()));
        }

        int[] inward = inward(snapshot.exteriorSide());
        int[] right = {-inward[1], inward[0]};
        List<LocalCell> candidates = new ArrayList<>();
        for (long key : reachableAtGround) {
            int x = (int) (key >> 32);
            int z = (int) key;
            if (!strictSourceAir(blocks, x, groundY, z)
                    || !strictSourceAir(blocks, x, groundY + 1, z)
                    || !supportsBody(blocks.get(localKey(x, groundY - 1, z)))) continue;
            int dx = x - snapshot.entranceX();
            int dz = z - snapshot.entranceZ();
            int depth = dx * inward[0] + dz * inward[1];
            int lateral = dx * right[0] + dz * right[1];
            if (depth < 2) continue;
            candidates.add(new LocalCell(x, groundY, z, depth, lateral));
        }
        candidates.sort(Comparator.comparingInt(LocalCell::depth)
                .thenComparingInt(cell -> Math.abs(cell.lateral()))
                .thenComparingInt(LocalCell::x)
                .thenComparingInt(LocalCell::z));
        if (candidates.size() < MIN_GROUND_CELLS) {
            throw new IllegalStateException("Authored urban source lacks source-air functional cells fragment="
                    + snapshot.fragmentKey() + " cells=" + candidates.size());
        }

        Set<Long> reserved = new HashSet<>();
        List<LocalBed> beds = new ArrayList<>();
        int bedCount = switch (placement.role()) {
            case "tenement" -> 3;
            case "inn" -> 2;
            case "guard_post" -> 1;
            default -> 0;
        };
        for (int i = 0; i < bedCount; i++) {
            LocalBed bed = selectBed(candidates, reserved);
            if (bed == null) throw new IllegalStateException("No authored bed pair role="
                    + placement.role() + " fragment=" + snapshot.fragmentKey());
            beds.add(bed);
            reserve(reserved, bed.foot());
            reserve(reserved, bed.head());
        }

        List<LocalFixture> fixtures = new ArrayList<>();
        LocalFixture primary = null;
        switch (placement.role()) {
            case "tenement" -> {
                add(fixtures, FixtureKind.BARREL, candidates, reserved);
                add(fixtures, FixtureKind.BARREL, candidates, reserved);
                add(fixtures, FixtureKind.CRAFTING_TABLE, candidates, reserved);
            }
            case "shop" -> {
                primary = add(fixtures, FixtureKind.PRIMARY_BARREL, candidates, reserved);
                add(fixtures, FixtureKind.CHEST, candidates, reserved);
                add(fixtures, FixtureKind.CRAFTING_TABLE, candidates, reserved);
                add(fixtures, FixtureKind.BOOKSHELF, candidates, reserved);
                add(fixtures, FixtureKind.BOOKSHELF, candidates, reserved);
                add(fixtures, FixtureKind.LECTERN, candidates, reserved);
            }
            case "bakery" -> {
                primary = add(fixtures, FixtureKind.PRIMARY_BARREL, candidates, reserved);
                add(fixtures, FixtureKind.CHEST, candidates, reserved);
                add(fixtures, FixtureKind.FURNACE, candidates, reserved);
                add(fixtures, FixtureKind.SMOKER, candidates, reserved);
                add(fixtures, FixtureKind.CRAFTING_TABLE, candidates, reserved);
                add(fixtures, FixtureKind.HAY, candidates, reserved);
            }
            case "inn" -> {
                primary = add(fixtures, FixtureKind.PRIMARY_BARREL, candidates, reserved);
                add(fixtures, FixtureKind.CHEST, candidates, reserved);
                add(fixtures, FixtureKind.BARREL, candidates, reserved);
            }
            case "stable" -> {
                primary = add(fixtures, FixtureKind.PRIMARY_BARREL, candidates, reserved);
                for (int i = 0; i < 4; i++) add(fixtures, FixtureKind.HAY, candidates, reserved);
                add(fixtures, FixtureKind.WATER_CAULDRON, candidates, reserved);
            }
            case "guard_post" -> {
                primary = add(fixtures, FixtureKind.PRIMARY_BARREL, candidates, reserved);
                add(fixtures, FixtureKind.CHEST, candidates, reserved);
                add(fixtures, FixtureKind.ANVIL, candidates, reserved);
                add(fixtures, FixtureKind.TARGET, candidates, reserved);
                add(fixtures, FixtureKind.STONECUTTER, candidates, reserved);
            }
            case "bathhouse" -> {
                primary = add(fixtures, FixtureKind.PRIMARY_BARREL, candidates, reserved);
                add(fixtures, FixtureKind.BARREL, candidates, reserved);
                add(fixtures, FixtureKind.BARREL, candidates, reserved);
                for (int i = 0; i < 4; i++) add(fixtures, FixtureKind.WATER_CAULDRON, candidates, reserved);
            }
            case "warehouse" -> {
                primary = add(fixtures, FixtureKind.PRIMARY_BARREL, candidates, reserved);
                for (int i = 0; i < 6; i++) add(fixtures, FixtureKind.BARREL, candidates, reserved);
                add(fixtures, FixtureKind.CHEST, candidates, reserved);
                add(fixtures, FixtureKind.CHEST, candidates, reserved);
            }
            default -> throw new IllegalStateException("Unknown Erden urban role " + placement.role());
        }

        List<LocalCell> targets = candidates.stream()
                .filter(cell -> !reserved.contains(columnKey(cell.x(), cell.z())))
                .sorted(Comparator.comparingInt((LocalCell cell) -> Math.abs(cell.lateral()))
                        .thenComparingInt(cell -> Math.abs(cell.depth() - 5))
                        .thenComparingInt(LocalCell::depth))
                .limit(TARGET_COUNT + 1L)
                .toList();
        if (targets.size() < TARGET_COUNT + 1) {
            throw new IllegalStateException("Authored urban source lacks movement targets role=" + placement.role());
        }

        List<BlockPos> residentTargets = targets.subList(0, TARGET_COUNT).stream()
                .map(cell -> worldCell(placement, snapshot, cell))
                .toList();
        BlockPos workTarget = worldCell(placement, snapshot, targets.get(TARGET_COUNT));
        BlockPos primaryContainer = primary == null ? null : worldCell(placement, snapshot, primary.cell());
        List<BedPlan> bedPlans = beds.stream()
                .map(bed -> new BedPlan(
                        worldCell(placement, snapshot, bed.foot()),
                        worldCell(placement, snapshot, bed.head())))
                .toList();
        List<FixturePlan> fixturePlans = fixtures.stream()
                .map(fixture -> new FixturePlan(fixture.kind(),
                        worldCell(placement, snapshot, fixture.cell())))
                .toList();

        return new PlacementPlan(
                placement.role(), placement.fragmentKey(), placement.entrance(),
                groundY, reachableAtGround.size(), primaryContainer,
                workTarget, List.copyOf(residentTargets), bedPlans, fixturePlans);
    }

    private static List<WalkCell> reachable(
            ExternalUrbanFabricBuilder.UrbanFragmentSnapshot snapshot,
            Map<Long, ExternalUrbanFabricBuilder.UrbanSourceBlock> blocks,
            int doorY) {
        int[] inward = inward(snapshot.exteriorSide());
        WalkCell seed = null;
        for (int depth = 1; depth <= 3 && seed == null; depth++) {
            int x = snapshot.entranceX() + inward[0] * depth;
            int z = snapshot.entranceZ() + inward[1] * depth;
            int y = resolveFeetY(snapshot, blocks, x, z, doorY);
            if (y != Integer.MIN_VALUE) seed = new WalkCell(x, y, z);
        }
        if (seed == null) {
            int y = resolveFeetY(snapshot, blocks, snapshot.entranceX(), snapshot.entranceZ(), doorY);
            if (y != Integer.MIN_VALUE) seed = new WalkCell(snapshot.entranceX(), y, snapshot.entranceZ());
        }
        if (seed == null) return List.of();

        ArrayDeque<WalkCell> pending = new ArrayDeque<>();
        Set<Long> visited = new HashSet<>();
        List<WalkCell> result = new ArrayList<>();
        pending.add(seed);
        visited.add(nodeKey(seed.x(), seed.y(), seed.z()));
        while (!pending.isEmpty()) {
            WalkCell current = pending.removeFirst();
            result.add(current);
            for (int[] direction : DIRECTIONS) {
                int x = current.x() + direction[0];
                int z = current.z() + direction[1];
                if (!interiorSide(snapshot, x, z)) continue;
                int y = resolveFeetY(snapshot, blocks, x, z, current.y());
                if (y == Integer.MIN_VALUE || Math.abs(y - current.y()) > 1) continue;
                long key = nodeKey(x, y, z);
                if (visited.add(key)) pending.addLast(new WalkCell(x, y, z));
            }
        }
        return List.copyOf(result);
    }

    private static LocalBed selectBed(List<LocalCell> candidates, Set<Long> reserved) {
        Map<Long, LocalCell> byColumn = new HashMap<>();
        for (LocalCell cell : candidates) byColumn.put(columnKey(cell.x(), cell.z()), cell);
        for (LocalCell foot : candidates.stream()
                .sorted(Comparator.comparingInt(LocalCell::depth).reversed()
                        .thenComparing(Comparator.comparingInt(
                                (LocalCell cell) -> Math.abs(cell.lateral())).reversed()))
                .toList()) {
            if (reserved.contains(columnKey(foot.x(), foot.z()))) continue;
            for (int[] direction : DIRECTIONS) {
                LocalCell head = byColumn.get(columnKey(foot.x() + direction[0], foot.z() + direction[1]));
                if (head == null || reserved.contains(columnKey(head.x(), head.z()))) continue;
                return new LocalBed(foot, head);
            }
        }
        return null;
    }

    private static LocalFixture add(
            List<LocalFixture> fixtures, FixtureKind kind,
            List<LocalCell> candidates, Set<Long> reserved) {
        for (LocalCell cell : candidates.stream()
                .sorted(Comparator.comparingInt(LocalCell::depth).reversed()
                        .thenComparing(Comparator.comparingInt(
                                (LocalCell candidate) -> Math.abs(candidate.lateral())).reversed()))
                .toList()) {
            long key = columnKey(cell.x(), cell.z());
            if (!reserved.add(key)) continue;
            LocalFixture fixture = new LocalFixture(kind, cell);
            fixtures.add(fixture);
            return fixture;
        }
        throw new IllegalStateException("No authored source-air cell remains for " + kind);
    }

    private static void reserve(Set<Long> reserved, LocalCell cell) {
        reserved.add(columnKey(cell.x(), cell.z()));
    }

    private static Map<Long, ExternalUrbanFabricBuilder.UrbanSourceBlock> blockMap(
            ExternalUrbanFabricBuilder.UrbanFragmentSnapshot snapshot) {
        Map<Long, ExternalUrbanFabricBuilder.UrbanSourceBlock> result = new HashMap<>();
        for (ExternalUrbanFabricBuilder.UrbanSourceBlock block : snapshot.blocks()) {
            result.put(localKey(block.x(), block.y(), block.z()), block);
        }
        return result;
    }

    private static int lowestDoorY(
            ExternalUrbanFabricBuilder.UrbanFragmentSnapshot snapshot,
            Map<Long, ExternalUrbanFabricBuilder.UrbanSourceBlock> blocks) {
        int lowest = Integer.MAX_VALUE;
        for (int y = 0; y < snapshot.height(); y++) {
            ExternalUrbanFabricBuilder.UrbanSourceBlock block =
                    blocks.get(localKey(snapshot.entranceX(), y, snapshot.entranceZ()));
            if (block != null && block.state().getBlock() instanceof DoorBlock) lowest = Math.min(lowest, y);
        }
        return lowest;
    }

    private static int resolveFeetY(
            ExternalUrbanFabricBuilder.UrbanFragmentSnapshot snapshot,
            Map<Long, ExternalUrbanFabricBuilder.UrbanSourceBlock> blocks,
            int x, int z, int preferredY) {
        for (int offset : FEET_OFFSETS) {
            int y = preferredY + offset;
            if (walkable(snapshot, blocks, x, y, z)) return y;
        }
        return Integer.MIN_VALUE;
    }

    private static boolean walkable(
            ExternalUrbanFabricBuilder.UrbanFragmentSnapshot snapshot,
            Map<Long, ExternalUrbanFabricBuilder.UrbanSourceBlock> blocks,
            int x, int y, int z) {
        if (x < 0 || x >= snapshot.width() || z < 0 || z >= snapshot.length()) return false;
        if (y <= 0 || y + 1 >= snapshot.height() || !interiorSide(snapshot, x, z)) return false;
        return bodyPassable(blocks.get(localKey(x, y, z)))
                && bodyPassable(blocks.get(localKey(x, y + 1, z)))
                && supportsBody(blocks.get(localKey(x, y - 1, z)));
    }

    private static boolean strictSourceAir(
            Map<Long, ExternalUrbanFabricBuilder.UrbanSourceBlock> blocks,
            int x, int y, int z) {
        ExternalUrbanFabricBuilder.UrbanSourceBlock block = blocks.get(localKey(x, y, z));
        return block == null || block.state().isAir();
    }

    private static boolean bodyPassable(ExternalUrbanFabricBuilder.UrbanSourceBlock block) {
        if (block == null || block.state().isAir()) return true;
        if (block.state().getBlock() instanceof DoorBlock) return true;
        String id = BuiltInRegistries.BLOCK.getKey(block.state().getBlock()).toString();
        return id.contains("torch") || id.contains("button") || id.contains("pressure_plate")
                || id.contains("carpet") || id.contains("lantern")
                || id.endsWith("_sign") || id.endsWith("_wall_sign");
    }

    private static boolean supportsBody(ExternalUrbanFabricBuilder.UrbanSourceBlock block) {
        if (block == null || block.state().isAir()) return false;
        Block source = block.state().getBlock();
        if (source instanceof DoorBlock) return false;
        String id = BuiltInRegistries.BLOCK.getKey(source).toString();
        return !(id.equals("minecraft:water") || id.equals("minecraft:lava")
                || id.contains("torch") || id.contains("button") || id.contains("pressure_plate")
                || id.contains("carpet") || id.contains("lantern") || id.contains("chain")
                || id.contains("fence") || id.contains("iron_bars") || id.contains("glass_pane")
                || id.endsWith("_sign") || id.endsWith("_wall_sign") || id.endsWith("_trapdoor"));
    }

    private static boolean interiorSide(
            ExternalUrbanFabricBuilder.UrbanFragmentSnapshot snapshot, int x, int z) {
        if (x < 0 || x >= snapshot.width() || z < 0 || z >= snapshot.length()) return false;
        return switch (snapshot.exteriorSide()) {
            case "NORTH" -> z >= snapshot.entranceZ();
            case "SOUTH" -> z <= snapshot.entranceZ();
            case "WEST" -> x >= snapshot.entranceX();
            case "EAST" -> x <= snapshot.entranceX();
            default -> false;
        };
    }

    private static int[] inward(String side) {
        return switch (side) {
            case "NORTH" -> new int[]{0, 1};
            case "SOUTH" -> new int[]{0, -1};
            case "WEST" -> new int[]{1, 0};
            case "EAST" -> new int[]{-1, 0};
            default -> throw new IllegalArgumentException("Unknown exterior side " + side);
        };
    }

    private static BlockPos worldCell(
            ExternalUrbanFabricBuilder.UrbanBuildingPlacement placement,
            ExternalUrbanFabricBuilder.UrbanFragmentSnapshot snapshot,
            LocalCell cell) {
        RotatedOffset rotated = rotate(
                cell.x(), cell.z(), snapshot.width(), snapshot.length(), placement.rotation());
        return new BlockPos(
                placement.minX() + rotated.x(),
                placement.baseY() + cell.y(),
                placement.minZ() + rotated.z());
    }

    private static RotatedOffset rotate(int x, int z, int width, int length, Rotation rotation) {
        return switch (rotation) {
            case NONE -> new RotatedOffset(x, z);
            case CLOCKWISE_90 -> new RotatedOffset(length - 1 - z, x);
            case CLOCKWISE_180 -> new RotatedOffset(width - 1 - x, length - 1 - z);
            case COUNTERCLOCKWISE_90 -> new RotatedOffset(z, width - 1 - x);
        };
    }

    private static long entranceKey(int x, int z) {
        return ((long) x << 32) ^ (z & 0xffffffffL);
    }

    private static long localKey(int x, int y, int z) {
        return ((long) (x & 0x1fffff) << 42) ^ ((long) (y & 0x3fffff) << 20) ^ (z & 0xfffffL);
    }

    private static long nodeKey(int x, int y, int z) {
        return ((long) x & 0x1fffffL) << 43 ^ ((long) y & 0x3fffffL) << 21 ^ ((long) z & 0x1fffffL);
    }

    private static long columnKey(int x, int z) {
        return ((long) x << 32) ^ (z & 0xffffffffL);
    }

    private static final int[][] DIRECTIONS = {{1,0},{-1,0},{0,1},{0,-1}};
    private static final int[] FEET_OFFSETS = {0, 1, -1};

    public enum FixtureKind {
        PRIMARY_BARREL, BARREL, CHEST, CRAFTING_TABLE, FURNACE, SMOKER,
        BOOKSHELF, LECTERN, HAY, WATER_CAULDRON, ANVIL, TARGET, STONECUTTER
    }

    public record FixturePlan(FixtureKind kind, BlockPos pos) {}
    public record BedPlan(BlockPos foot, BlockPos head) {}
    public record PlacementPlan(
            String role, String fragmentKey, ExternalUrbanFabricBuilder.UrbanEntrance entrance,
            int groundY, int groundCells, BlockPos primaryContainer, BlockPos workTarget,
            List<BlockPos> residentTargets, List<BedPlan> beds, List<FixturePlan> fixtures) {}

    private record WalkCell(int x, int y, int z) {}
    private record LocalCell(int x, int y, int z, int depth, int lateral) {}
    private record LocalBed(LocalCell foot, LocalCell head) {}
    private record LocalFixture(FixtureKind kind, LocalCell cell) {}
    private record RotatedOffset(int x, int z) {}
}
