package kr.moonseungjun.frontiersettlement.settlement;

import net.minecraft.core.BlockPos;
import net.minecraft.server.Bootstrap;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** Temporary executable QA: enumerate every building/rotation against construction geometry invariants. */
public final class ConstructionMatrixQa {
    private static final double HIGH_WORK_RANGE_SQR = 196.0D;
    private static final int MAX_SCAFFOLD_STEP = 7;
    private static final BlockPos ORIGIN = new BlockPos(100, 80, 100);

    private ConstructionMatrixQa() {}

    public static void main(String[] args) {
        Bootstrap.bootStrap();
        int cases = 0;
        int failures = 0;
        int highTargets = 0;
        int zeroReachTargets = 0;
        int singleReachTargets = 0;
        int outsideValidationTowers = 0;
        int scaffoldBlueprintCollisions = 0;

        System.out.println("FRONTIER_CONSTRUCTION_MATRIX_QA_V1");
        for (BuildingType type : BuildingType.values()) {
            Integer expectedCount = null;
            int typeMinReach = Integer.MAX_VALUE;
            int typeZeroReach = 0;
            int typeSingleReach = 0;
            int typeMaxY = Integer.MIN_VALUE;
            for (BuildingRotation rotation : BuildingRotation.values()) {
                cases++;
                List<BuildingBlueprints.Placement> plan = RotatedBlueprints.create(type, ORIGIN, rotation.id());
                int width = rotation.rotatedWidth(type);
                int depth = rotation.rotatedDepth(type);
                Set<BlockPos> unique = new HashSet<>();
                int minX = Integer.MAX_VALUE, maxX = Integer.MIN_VALUE;
                int minY = Integer.MAX_VALUE, maxY = Integer.MIN_VALUE;
                int minZ = Integer.MAX_VALUE, maxZ = Integer.MIN_VALUE;
                boolean boundsOk = true;
                boolean heightOk = true;
                for (BuildingBlueprints.Placement placement : plan) {
                    BlockPos p = placement.pos();
                    unique.add(p);
                    int x = p.getX() - ORIGIN.getX();
                    int y = p.getY() - ORIGIN.getY();
                    int z = p.getZ() - ORIGIN.getZ();
                    minX = Math.min(minX, x); maxX = Math.max(maxX, x);
                    minY = Math.min(minY, y); maxY = Math.max(maxY, y);
                    minZ = Math.min(minZ, z); maxZ = Math.max(maxZ, z);
                    if (x < -1 || x > width || z < -1 || z > depth) boundsOk = false;
                    if (y < 0 || y > type.clearHeight()) heightOk = false;
                }
                typeMaxY = Math.max(typeMaxY, maxY);
                boolean uniqueOk = unique.size() == plan.size();
                boolean rotationCountOk = expectedCount == null || expectedCount == plan.size();
                if (expectedCount == null) expectedCount = plan.size();

                BlockPos supply = supplyPosition(ORIGIN, type, rotation);
                List<Scaffold> scaffolds = scaffolds(ORIGIN, type, rotation, supply);
                Set<BlockPos> blueprintPositions = new HashSet<>(unique);
                boolean scaffoldCollision = false;
                boolean everyTowerInsideValidation = true;
                for (Scaffold scaffold : scaffolds) {
                    for (BlockPos piece : scaffold.pieces()) {
                        int x = piece.getX() - ORIGIN.getX();
                        int y = piece.getY() - ORIGIN.getY();
                        int z = piece.getZ() - ORIGIN.getZ();
                        if (x < -1 || x > width || z < -1 || z > depth || y < 0 || y > type.clearHeight()) {
                            everyTowerInsideValidation = false;
                        }
                        if (blueprintPositions.contains(piece)) scaffoldCollision = true;
                    }
                }
                if (!everyTowerInsideValidation) outsideValidationTowers++;
                if (scaffoldCollision) scaffoldBlueprintCollisions++;

                int caseMinReach = Integer.MAX_VALUE;
                int caseZero = 0;
                int caseSingle = 0;
                int caseHigh = 0;
                for (BuildingBlueprints.Placement placement : plan) {
                    int relativeY = placement.pos().getY() - ORIGIN.getY();
                    if (relativeY <= 3) continue;
                    caseHigh++;
                    highTargets++;
                    int reach = 0;
                    for (Scaffold scaffold : scaffolds) {
                        if (scaffold.steps().isEmpty()) continue;
                        int index = Math.min(scaffold.steps().size() - 1, Math.max(0, relativeY - 3));
                        BlockPos candidate = scaffold.steps().get(index).above();
                        if (distSqr(candidate, placement.pos()) <= HIGH_WORK_RANGE_SQR) reach++;
                    }
                    caseMinReach = Math.min(caseMinReach, reach);
                    typeMinReach = Math.min(typeMinReach, reach);
                    if (reach == 0) { caseZero++; typeZeroReach++; zeroReachTargets++; }
                    if (reach == 1) { caseSingle++; typeSingleReach++; singleReachTargets++; }
                }
                if (caseHigh == 0) caseMinReach = 4;

                boolean supplyOverlap = blueprintPositions.contains(supply);
                boolean supplyInsideFootprint = supply.getX() >= ORIGIN.getX() && supply.getX() < ORIGIN.getX() + width
                        && supply.getZ() >= ORIGIN.getZ() && supply.getZ() < ORIGIN.getZ() + depth;
                boolean ok = !plan.isEmpty() && uniqueOk && rotationCountOk && boundsOk && heightOk
                        && !scaffoldCollision && !supplyOverlap && !supplyInsideFootprint && caseZero == 0;
                if (!ok) failures++;

                System.out.printf("CASE type=%s rot=%s count=%d unique=%s bounds=[%d..%d,%d..%d,%d..%d] boundsOk=%s heightOk=%s high=%d minReach=%d zeroReach=%d singleReach=%d scaffoldOutsideValidatedEnvelope=%s scaffoldBlueprintCollision=%s supplyOverlap=%s OK=%s%n",
                        type.id(), rotation.name(), plan.size(), uniqueOk,
                        minX, maxX, minY, maxY, minZ, maxZ, boundsOk, heightOk,
                        caseHigh, caseMinReach, caseZero, caseSingle,
                        !everyTowerInsideValidation, scaffoldCollision, supplyOverlap, ok);
            }
            if (typeMinReach == Integer.MAX_VALUE) typeMinReach = 4;
            System.out.printf("TYPE_SUMMARY type=%s placements=%d maxY=%d minReach=%d zeroReach=%d singleReach=%d%n",
                    type.id(), expectedCount == null ? 0 : expectedCount, typeMaxY, typeMinReach, typeZeroReach, typeSingleReach);
        }
        System.out.printf("TOTAL cases=%d failures=%d highTargets=%d zeroReachTargets=%d singleReachTargets=%d casesWithScaffoldOutsideValidatedEnvelope=%d scaffoldBlueprintCollisionCases=%d%n",
                cases, failures, highTargets, zeroReachTargets, singleReachTargets,
                outsideValidationTowers, scaffoldBlueprintCollisions);
        if (failures > 0) System.exit(2);
    }

    private static BlockPos supplyPosition(BlockPos origin, BuildingType type, BuildingRotation rotation) {
        return origin.offset(-2, 0, Math.max(1, rotation.rotatedDepth(type) / 2));
    }

    private static List<Scaffold> scaffolds(BlockPos origin, BuildingType type, BuildingRotation rotation, BlockPos supply) {
        int width = rotation.rotatedWidth(type);
        int depth = rotation.rotatedDepth(type);
        int midX = Math.max(1, width / 2);
        int midZ = Math.max(1, depth / 2);
        return List.of(
                scaffold(supply.offset(-1, 0, 0), supply),
                scaffold(origin.offset(width + 2, 0, midZ), supply),
                scaffold(origin.offset(midX, 0, -3), supply),
                scaffold(origin.offset(midX, 0, depth + 2), supply));
    }

    private static Scaffold scaffold(BlockPos center, BlockPos supply) {
        int[][] ring = new int[][] {
                {0, -1}, {1, -1}, {1, 0}, {1, 1},
                {0, 1}, {-1, 1}, {-1, 0}, {-1, -1}
        };
        Set<BlockPos> pieces = new LinkedHashSet<>();
        List<BlockPos> steps = new ArrayList<>();
        for (int y = 0; y <= MAX_SCAFFOLD_STEP; y++) pieces.add(center.above(y));
        int step = 0;
        for (int[] offset : ring) {
            if (step > MAX_SCAFFOLD_STEP) break;
            BlockPos column = center.offset(offset[0], 0, offset[1]);
            if (column.equals(supply)) continue;
            for (int y = 0; y < step; y++) pieces.add(column.above(y));
            BlockPos tread = column.above(step);
            pieces.add(tread);
            steps.add(tread);
            step++;
        }
        return new Scaffold(List.copyOf(pieces), List.copyOf(steps));
    }

    private static double distSqr(BlockPos a, BlockPos b) {
        double dx = (a.getX() + 0.5D) - (b.getX() + 0.5D);
        double dy = a.getY() - (b.getY() + 0.5D);
        double dz = (a.getZ() + 0.5D) - (b.getZ() + 0.5D);
        return dx * dx + dy * dy + dz * dz;
    }

    private record Scaffold(List<BlockPos> pieces, List<BlockPos> steps) {}
}
