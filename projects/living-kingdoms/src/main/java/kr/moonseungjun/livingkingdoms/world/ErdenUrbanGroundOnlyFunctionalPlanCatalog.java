package kr.moonseungjun.livingkingdoms.world;

import kr.moonseungjun.livingkingdoms.LivingKingdoms;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;

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
 * Source-only functional plan for the urban buildings that do not need an authored upper floor.
 *
 * <p>The source fragment itself owns the rooms, walls, floor and roof. This catalog never invents a
 * rectangular room and never asks a runtime builder to clear source blocks. After the ground-void
 * audit proves the retained doorway reaches a meaningful authored floor, this planner selects a
 * small set of fixture cells inside that already-walkable floor. Every selected body/head cell is
 * immutable source AIR and every support cell is an existing source floor. Runtime code therefore
 * only has to add role fixtures into real empty space.</p>
 */
public final class ErdenUrbanGroundOnlyFunctionalPlanCatalog {
    public static final int PLAN_REVISION = 1;
    public static final int EXPECTED_PLANS = 77;

    private static final int MIN_PLAN_CELLS = 18;
    private static final int MIN_DEPTH = 6;
    private static final int MAX_DEPTH = 8;
    private static final int MAX_LATERAL = 3;
    private static final Set<String> SUPPORTED_ROLES = Set.of(
            "tenement", "inn", "stable", "guard_post");

    private static final Map<Long, PlacementPlan> PLANS = new LinkedHashMap<>();
    private static boolean bootstrapped;

    private ErdenUrbanGroundOnlyFunctionalPlanCatalog() {
    }

    public static synchronized void bootstrap() {
        if (bootstrapped) return;
        PLANS.clear();
        ErdenUrbanGroundVoidOpportunityCatalog.bootstrap();

        Map<String, ExternalUrbanFabricBuilder.UrbanFragmentSnapshot> snapshots =
                ExternalUrbanFabricBuilder.fragmentSnapshotsForDiagnostics();
        Map<String, Integer> roles = new LinkedHashMap<>();
        int totalFixtures = 0;
        int totalBeds = 0;
        int minimumPlanningCells = Integer.MAX_VALUE;

        for (ExternalUrbanFabricBuilder.UrbanBuildingPlacement placement
                : ExternalUrbanFabricBuilder.buildingPlacementsForDiagnostics()) {
            if (ErdenUrbanAuthoredUpperRouteManager.isEligible(placement.entrance())) continue;
            if (!SUPPORTED_ROLES.contains(placement.role())) {
                throw new IllegalStateException(
                        "Unsupported Erden ground-only functional role " + placement.role());
            }
            ErdenUrbanGroundVoidOpportunityCatalog.VoidProfile voidProfile =
                    ErdenUrbanGroundVoidOpportunityCatalog.profile(placement.fragmentKey());
            if (voidProfile == null || !voidProfile.usable()) {
                throw new IllegalStateException(
                        "Ground-only Erden source is not usable without cutting blocks role="
                                + placement.role() + " fragment=" + placement.fragmentKey());
            }
            ExternalUrbanFabricBuilder.UrbanFragmentSnapshot snapshot =
                    snapshots.get(placement.fragmentKey());
            if (snapshot == null) {
                throw new IllegalStateException(
                        "Missing Erden source snapshot for ground-only functional plan "
                                + placement.fragmentKey());
            }

            PlacementPlan plan = buildPlan(placement, snapshot, voidProfile.reachableCells());
            long key = entranceKey(placement.entrance().x(), placement.entrance().z());
            if (PLANS.put(key, plan) != null) {
                throw new IllegalStateException("Duplicate Erden ground-only plan entrance="
                        + placement.entrance().x() + "," + placement.entrance().z());
            }
            roles.merge(placement.role(), 1, Integer::sum);
            totalFixtures += plan.fixtures().size();
            totalBeds += plan.beds().size();
            minimumPlanningCells = Math.min(minimumPlanningCells, plan.planningCells());
        }

        if (PLANS.size() != EXPECTED_PLANS) {
            throw new IllegalStateException("Erden ground-only functional plan count drifted: expected="
                    + EXPECTED_PLANS + " actual=" + PLANS.size());
        }
        int roleTotal = roles.values().stream().mapToInt(Integer::intValue).sum();
        if (roleTotal != EXPECTED_PLANS) {
            throw new IllegalStateException("Erden ground-only functional role counts drifted " + roles);
        }
        bootstrapped = true;
        LivingKingdoms.LOGGER.info(
                "LK_ERDEN_GROUND_ONLY_FUNCTIONAL_PLAN_PASS placements={} roles={} minimum_planning_cells={} fixtures={} beds={} resident_targets=3 work_targets=true source_floor_reused=true source_air_fixtures=true source_blocks_cut=0 source_only=true world_reads=false chunks_loaded=0 mutations=0 plots=233 housing=77 work=156 revision={}",
                PLANS.size(), roles,
                minimumPlanningCells == Integer.MAX_VALUE ? 0 : minimumPlanningCells,
                totalFixtures, totalBeds, PLAN_REVISION);
    }

    public static PlacementPlan plan(
            ExternalUrbanFabricBuilder.UrbanEntrance entrance) {
        bootstrap();
        return PLANS.get(entranceKey(entrance.x(), entrance.z()));
    }

    public static BlockPos primaryContainer(
            ExternalUrbanFabricBuilder.UrbanEntrance entrance) {
        PlacementPlan plan = plan(entrance);
        return plan == null ? null : plan.primaryContainer();
    }

    public static BlockPos homeTarget(
            ExternalUrbanFabricBuilder.UrbanEntrance entrance, int slot) {
        PlacementPlan plan = plan(entrance);
        if (plan == null || plan.residentTargets().isEmpty()) return null;
        return plan.residentTargets().get(Math.floorMod(slot, plan.residentTargets().size()));
    }

    public static BlockPos workTarget(
            ExternalUrbanFabricBuilder.UrbanEntrance entrance) {
        PlacementPlan plan = plan(entrance);
        return plan == null ? null : plan.workTarget();
    }

    private static PlacementPlan buildPlan(
            ExternalUrbanFabricBuilder.UrbanBuildingPlacement placement,
            ExternalUrbanFabricBuilder.UrbanFragmentSnapshot snapshot,
            int auditedReachableCells) {
        Map<Long, ExternalUrbanFabricBuilder.UrbanSourceBlock> blocks = new HashMap<>();
        int doorY = Integer.MAX_VALUE;
        for (ExternalUrbanFabricBuilder.UrbanSourceBlock block : snapshot.blocks()) {
            blocks.put(localKey(block.x(), block.y(), block.z()), block);
            if (block.x() == snapshot.entranceX()
                    && block.z() == snapshot.entranceZ()
                    && block.state().getBlock() instanceof DoorBlock) {
                doorY = Math.min(doorY, block.y());
            }
        }
        if (doorY == Integer.MAX_VALUE) {
            throw new IllegalStateException("Ground-only source has no retained door "
                    + snapshot.fragmentKey());
        }

        int[] inward = inward(snapshot.exteriorSide());
        int[] right = {-inward[1], inward[0]};
        List<LocalCell> planning = connectedPlanningCells(
                snapshot, blocks, doorY, inward, right);
        if (planning.size() < MIN_PLAN_CELLS) {
            throw new IllegalStateException("Ground-only source lacks enough zero-cut fixture cells fragment="
                    + snapshot.fragmentKey() + " planning_cells=" + planning.size()
                    + " audited_reachable=" + auditedReachableCells);
        }
        int deepest = planning.stream().mapToInt(LocalCell::depth).max().orElse(0);
        if (deepest < MIN_DEPTH) {
            throw new IllegalStateException("Ground-only source is too shallow for functional plan fragment="
                    + snapshot.fragmentKey() + " depth=" + deepest);
        }

        List<LocalCell> sideCells = planning.stream()
                .filter(cell -> Math.abs(cell.lateral()) >= 2)
                .sorted(Comparator.comparingInt(LocalCell::depth).reversed()
                        .thenComparing(Comparator.comparingInt(
                                (LocalCell cell) -> Math.abs(cell.lateral())).reversed())
                        .thenComparingInt(LocalCell::x)
                        .thenComparingInt(LocalCell::z))
                .toList();
        if (sideCells.size() < 10) {
            throw new IllegalStateException("Ground-only source lacks side fixture cells fragment="
                    + snapshot.fragmentKey() + " cells=" + sideCells.size());
        }

        Set<Long> reserved = new HashSet<>();
        List<LocalBed> localBeds = new ArrayList<>();
        int bedCount = switch (placement.role()) {
            case "tenement" -> 3;
            case "inn" -> 2;
            case "guard_post" -> 1;
            case "stable" -> 0;
            default -> throw new IllegalStateException("Unhandled ground-only role " + placement.role());
        };
        for (int index = 0; index < bedCount; index++) {
            LocalBed bed = selectBed(sideCells, planning, reserved, inward);
            if (bed == null) {
                throw new IllegalStateException("No source-air bed pair remains role=" + placement.role()
                        + " fragment=" + snapshot.fragmentKey() + " index=" + index);
            }
            localBeds.add(bed);
            reserved.add(columnKey(bed.foot().x(), bed.foot().z()));
            reserved.add(columnKey(bed.head().x(), bed.head().z()));
        }

        List<LocalFixture> localFixtures = new ArrayList<>();
        BlockPos primaryContainer = null;
        switch (placement.role()) {
            case "tenement" -> {
                addFixture(localFixtures, FixtureKind.BARREL, sideCells, reserved);
                addFixture(localFixtures, FixtureKind.BARREL, sideCells, reserved);
                addFixture(localFixtures, FixtureKind.CRAFTING_TABLE, sideCells, reserved);
            }
            case "inn" -> {
                LocalFixture primary = addFixture(
                        localFixtures, FixtureKind.PRIMARY_BARREL, sideCells, reserved);
                primaryContainer = worldCell(placement, snapshot, doorY, primary.cell());
                addFixture(localFixtures, FixtureKind.CHEST, sideCells, reserved);
            }
            case "stable" -> {
                LocalFixture primary = addFixture(
                        localFixtures, FixtureKind.PRIMARY_BARREL, sideCells, reserved);
                primaryContainer = worldCell(placement, snapshot, doorY, primary.cell());
                for (int i = 0; i < 4; i++) {
                    addFixture(localFixtures, FixtureKind.HAY, sideCells, reserved);
                }
                addFixture(localFixtures, FixtureKind.WATER_CAULDRON, sideCells, reserved);
            }
            case "guard_post" -> {
                LocalFixture primary = addFixture(
                        localFixtures, FixtureKind.PRIMARY_BARREL, sideCells, reserved);
                primaryContainer = worldCell(placement, snapshot, doorY, primary.cell());
                addFixture(localFixtures, FixtureKind.CHEST, sideCells, reserved);
                addFixture(localFixtures, FixtureKind.ANVIL, sideCells, reserved);
                addFixture(localFixtures, FixtureKind.TARGET, sideCells, reserved);
            }
            default -> throw new IllegalStateException("Unhandled ground-only role " + placement.role());
        }

        List<BlockPos> residentTargets = selectResidentTargets(
                planning, reserved, placement, snapshot, doorY);
        BlockPos workTarget = selectWorkTarget(
                planning, reserved, placement, snapshot, doorY);
        if (residentTargets.size() != 3 || workTarget == null) {
            throw new IllegalStateException("Ground-only source has incomplete runtime targets role="
                    + placement.role());
        }

        List<BedPlan> beds = localBeds.stream()
                .map(bed -> new BedPlan(
                        worldCell(placement, snapshot, doorY, bed.foot()),
                        worldCell(placement, snapshot, doorY, bed.head())))
                .toList();
        List<FixturePlan> fixtures = localFixtures.stream()
                .map(fixture -> new FixturePlan(
                        fixture.kind(),
                        worldCell(placement, snapshot, doorY, fixture.cell())))
                .toList();
        return new PlacementPlan(
                placement.role(), placement.fragmentKey(), placement.entrance(),
                auditedReachableCells, planning.size(), primaryContainer,
                workTarget, List.copyOf(residentTargets), beds, fixtures);
    }

    private static List<LocalCell> connectedPlanningCells(
            ExternalUrbanFabricBuilder.UrbanFragmentSnapshot snapshot,
            Map<Long, ExternalUrbanFabricBuilder.UrbanSourceBlock> blocks,
            int doorY, int[] inward, int[] right) {
        record Node(int x, int y, int z) {}
        Node seed = null;
        for (int depth = 0; depth <= 3 && seed == null; depth++) {
            int x = snapshot.entranceX() + inward[0] * depth;
            int z = snapshot.entranceZ() + inward[1] * depth;
            int feetY = resolveFeetY(snapshot, blocks, x, z, doorY);
            if (feetY != Integer.MIN_VALUE) seed = new Node(x, feetY, z);
        }
        if (seed == null) return List.of();

        ArrayDeque<Node> pending = new ArrayDeque<>();
        Set<Long> visited = new HashSet<>();
        List<LocalCell> planning = new ArrayList<>();
        pending.add(seed);
        visited.add(nodeKey(seed.x(), seed.y(), seed.z()));
        while (!pending.isEmpty()) {
            Node current = pending.removeFirst();
            int dx = current.x() - snapshot.entranceX();
            int dz = current.z() - snapshot.entranceZ();
            int depth = dx * inward[0] + dz * inward[1];
            int lateral = dx * right[0] + dz * right[1];
            if (current.y() == doorY
                    && depth >= 2 && depth <= MAX_DEPTH
                    && Math.abs(lateral) <= MAX_LATERAL
                    && strictSourceAir(blocks, current.x(), doorY, current.z())
                    && strictSourceAir(blocks, current.x(), doorY + 1, current.z())
                    && supportsBody(blocks.get(localKey(
                            current.x(), doorY - 1, current.z())))) {
                planning.add(new LocalCell(current.x(), current.z(), depth, lateral));
            }
            for (int[] direction : DIRECTIONS) {
                int x = current.x() + direction[0];
                int z = current.z() + direction[1];
                if (!interiorSide(snapshot, x, z)) continue;
                int y = resolveFeetY(snapshot, blocks, x, z, current.y());
                if (y == Integer.MIN_VALUE || Math.abs(y - current.y()) > 1) continue;
                long key = nodeKey(x, y, z);
                if (visited.add(key)) pending.addLast(new Node(x, y, z));
            }
        }
        return List.copyOf(planning);
    }

    private static LocalBed selectBed(
            List<LocalCell> sideCells,
            List<LocalCell> planning,
            Set<Long> reserved,
            int[] inward) {
        Map<Long, LocalCell> byColumn = new HashMap<>();
        for (LocalCell cell : planning) byColumn.put(columnKey(cell.x(), cell.z()), cell);
        for (LocalCell foot : sideCells) {
            if (reserved.contains(columnKey(foot.x(), foot.z()))) continue;
            LocalCell head = byColumn.get(columnKey(
                    foot.x() + inward[0], foot.z() + inward[1]));
            if (head == null || reserved.contains(columnKey(head.x(), head.z()))) continue;
            if (head.depth() > MAX_DEPTH) continue;
            return new LocalBed(foot, head);
        }
        return null;
    }

    private static LocalFixture addFixture(
            List<LocalFixture> fixtures,
            FixtureKind kind,
            List<LocalCell> candidates,
            Set<Long> reserved) {
        for (LocalCell cell : candidates) {
            long key = columnKey(cell.x(), cell.z());
            if (!reserved.add(key)) continue;
            LocalFixture fixture = new LocalFixture(kind, cell);
            fixtures.add(fixture);
            return fixture;
        }
        throw new IllegalStateException("No source-air cell remains for fixture " + kind);
    }

    private static List<BlockPos> selectResidentTargets(
            List<LocalCell> planning,
            Set<Long> reserved,
            ExternalUrbanFabricBuilder.UrbanBuildingPlacement placement,
            ExternalUrbanFabricBuilder.UrbanFragmentSnapshot snapshot,
            int doorY) {
        List<LocalCell> ordered = planning.stream()
                .filter(cell -> !reserved.contains(columnKey(cell.x(), cell.z())))
                .sorted(Comparator.comparingInt((LocalCell cell) -> Math.abs(cell.lateral()))
                        .thenComparingInt(cell -> Math.abs(cell.depth() - 4))
                        .thenComparingInt(LocalCell::depth))
                .toList();
        List<BlockPos> result = new ArrayList<>();
        for (LocalCell cell : ordered) {
            BlockPos world = worldCell(placement, snapshot, doorY, cell);
            if (!result.contains(world)) result.add(world);
            if (result.size() == 3) break;
        }
        return List.copyOf(result);
    }

    private static BlockPos selectWorkTarget(
            List<LocalCell> planning,
            Set<Long> reserved,
            ExternalUrbanFabricBuilder.UrbanBuildingPlacement placement,
            ExternalUrbanFabricBuilder.UrbanFragmentSnapshot snapshot,
            int doorY) {
        return planning.stream()
                .filter(cell -> !reserved.contains(columnKey(cell.x(), cell.z())))
                .min(Comparator.comparingInt((LocalCell cell) -> Math.abs(cell.lateral()))
                        .thenComparingInt(cell -> Math.abs(cell.depth() - 3)))
                .map(cell -> worldCell(placement, snapshot, doorY, cell))
                .orElse(null);
    }

    private static BlockPos worldCell(
            ExternalUrbanFabricBuilder.UrbanBuildingPlacement placement,
            ExternalUrbanFabricBuilder.UrbanFragmentSnapshot snapshot,
            int localY,
            LocalCell cell) {
        RotatedOffset rotated = rotate(
                cell.x(), cell.z(), snapshot.width(), snapshot.length(), placement.rotation());
        return new BlockPos(
                placement.minX() + rotated.x(),
                placement.baseY() + localY,
                placement.minZ() + rotated.z());
    }

    private static RotatedOffset rotate(
            int x, int z, int width, int length, Rotation rotation) {
        return switch (rotation) {
            case NONE -> new RotatedOffset(x, z);
            case CLOCKWISE_90 -> new RotatedOffset(length - 1 - z, x);
            case CLOCKWISE_180 -> new RotatedOffset(width - 1 - x, length - 1 - z);
            case COUNTERCLOCKWISE_90 -> new RotatedOffset(z, width - 1 - x);
        };
    }

    private static int resolveFeetY(
            ExternalUrbanFabricBuilder.UranFragmentSnapshot snapshot,
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
            int x, int feetY, int z) {
        if (x < 0 || x >= snapshot.width() || z < 0 || z >= snapshot.length()) return false;
        if (feetY <= 0 || feetY + 1 >= snapshot.height()) return false;
        return bodyPassable(blocks.get(localKey(x, feetY, z)))
                && bodyPassable(blocks.get(localKey(x, feetY + 1, z)))
                && supportsBody(blocks.get(localKey(x, feetY - 1, z)));
    }

    private static boolean bodyPassable(ExternalUrbanFabricBuilder.UrbanSourceBlock block) {
        if (block == null || block.state().isAir()) return true;
        if (block.state().getBlock() instanceof DoorBlock) return true;
        String id = BuiltInRegistries.BLOCK.getKey(block.state().getBlock()).toString();
        return id.contains("torch")
                || id.contains("button")
                || id.contains("pressure_plate")
                || id.contains("carpet")
                || id.contains("lantern")
                || id.endsWith("_sign")
                || id.endsWith("_wall_sign");
    }

    private static boolean strictSourceAir(
            Map<Long, ExternalUrbanFabricBuilder.UrbanSourceBlock> blocks,
            int x, int y, int z) {
        ExternalUrbanFabricBuilder.UrbanSourceBlock block = blocks.get(localKey(x, y, z));
        return block == null || block.state().isAir();
    }

    private static boolean supportsBody(ExternalUrbanFabricBuilder.UrbanSourceBlock block) {
        if (block == null || block.state().isAir()) return false;
        Block source = block.state().getBlock();
        if (source instanceof DoorBlock) return false;
        String id = BuiltInRegistries.BLOCK.getKey(source).toString();
        return !(id.equals("minecraft:water") || id.equals("minecraft:lava")
                || id.contains("torch") || id.contains("button")
                || id.contains("pressure_plate") || id.contains("carpet")
                || id.contains("lantern") || id.contains("chain")
                || id.contains("fence") || id.contains("iron_bars")
                || id.contains("glass_pane") || id.endsWith("_sign")
                || id.endsWith("_wall_sign") || id.endsWith("_trapdoor"));
    }

    private static boolean interiorSide(
            ExternalUrbanFabricBuilder.UrbanFragmentSnapshot snapshot,
            int x, int z) {
        if (x < 0 || x >= snapshot.width() || z < 0 || z >= snapshot.length()) return false;
        return switch (snapshot.exteriorSide()) {
            case "NORTH" -> z >= snapshot.entranceZ();
            case "SOUTH" -> z <= snapshot.entranceZ();
            case "WEST" -> x >= snapshot.entranceX();
            case "EAST" -> x <= snapshot.entranceX();
            default -> false;
        };
    }

    private static int[] inward(String exteriorSide) {
        return switch (exteriorSide) {
            case "NORTH" -> new int[]{0, 1};
            case "SOUTH" -> new int[]{0, -1};
            case "WEST" -> new int[]{1, 0};
            case "EAST" -> new int[]{-1, 0};
            default -> throw new IllegalArgumentException("Unknown Erden exterior side " + exteriorSide);
        };
    }

    private static long entranceKey(int x, int z) {
        return ((long) x << 32) ^ (z & 0xffffffffL);
    }

    private static long localKey(int x, int y, int z) {
        return ((long) (x & 0x1fffff) << 42)
                ^ ((long) (y & 0x3fffff) << 20)
                ^ (z & 0xfffffL);
    }

    private static long nodeKey(int x, int y, int z) {
        long a = ((long) x & 0x1fffffL) << 43;
        long b = ((long) y & 0x3fffffL) << 21;
        long c = (long) z & 0x1fffffL;
        return a ^ b ^ c;
    }

    private static long columnKey(int x, int z) {
        return ((long) x << 32) ^ (z & 0xffffffffL);
    }

    private static final int[] FEET_OFFSETS = {0, 1, -1};
    private static final int[][] DIRECTIONS = {
            {1, 0}, {-1, 0}, {0, 1}, {0, -1}
    };

    public enum FixtureKind {
        PRIMARY_BARREL,
        BARREL,
        CHEST,
        CRAFTING_TABLE,
        HAY,
        WATER_CAULDRON,
        ANVIL,
        TARGET
    }

    public record FixturePlan(FixtureKind kind, BlockPos pos) {
    }

    public record BedPlan(BlockPos foot, BlockPos head) {
    }

    public record PlacementPlan(
            String role,
            String fragmentKey,
            ExternalUrbanFabricBuilder.UrbanEntrance entrance,
            int auditedReachableCells,
            int planningCells,
            BlockPos primaryContainer,
            BlockPos workTarget,
            List<BlockPos> residentTargets,
            List<BedPlan> beds,
            List<FixturePlan> fixtures) {
    }

    private record LocalCell(int x, int z, int depth, int lateral) {
    }

    private record LocalBed(LocalCell foot, LocalCell head) {
    }

    private record LocalFixture(FixtureKind kind, LocalCell cell) {
    }

    private record RotatedOffset(int x, int z) {
    }
}
