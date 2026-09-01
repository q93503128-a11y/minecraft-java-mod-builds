package kr.moonseungjun.frontiersettlement.settlement;

import net.minecraft.core.BlockPos;
import net.neoforged.neoforge.event.server.ServerStartedEvent;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** Temporary NeoForge-runtime QA: enumerate every building/rotation against construction geometry invariants. */
public final class ConstructionMatrixQa {
    private static final double HIGH_WORK_RANGE_SQR = 196.0D;
    private static final int MAX_SCAFFOLD_STEP = 7;
    private static final BlockPos ORIGIN = new BlockPos(100, 80, 100);

    private ConstructionMatrixQa() {}

    public static void onServerStarted(ServerStartedEvent event) {
        runMatrix();
        event.getServer().halt(false);
    }

    private static void runMatrix() {
        int cases = 0, failures = 0, highTargets = 0, zeroReachTargets = 0, singleReachTargets = 0;
        int outsideValidationCases = 0, scaffoldBlueprintCollisionCases = 0;
        System.out.println("FRONTIER_CONSTRUCTION_MATRIX_QA_V2");

        for (BuildingType type : BuildingType.values()) {
            Integer expectedCount = null;
            int typeMinReach = Integer.MAX_VALUE, typeZero = 0, typeSingle = 0, typeMaxY = Integer.MIN_VALUE;
            for (BuildingRotation rotation : BuildingRotation.values()) {
                cases++;
                List<BuildingBlueprints.Placement> plan = RotatedBlueprints.create(type, ORIGIN, rotation.id());
                int width = rotation.rotatedWidth(type), depth = rotation.rotatedDepth(type);
                Set<BlockPos> positions = new HashSet<>();
                int minX = Integer.MAX_VALUE, maxX = Integer.MIN_VALUE, minY = Integer.MAX_VALUE, maxY = Integer.MIN_VALUE;
                int minZ = Integer.MAX_VALUE, maxZ = Integer.MIN_VALUE;
                boolean boundsOk = true, heightOk = true;
                for (BuildingBlueprints.Placement placement : plan) {
                    BlockPos p = placement.pos();
                    positions.add(p);
                    int x = p.getX() - ORIGIN.getX(), y = p.getY() - ORIGIN.getY(), z = p.getZ() - ORIGIN.getZ();
                    minX = Math.min(minX, x); maxX = Math.max(maxX, x);
                    minY = Math.min(minY, y); maxY = Math.max(maxY, y);
                    minZ = Math.min(minZ, z); maxZ = Math.max(maxZ, z);
                    if (x < -1 || x > width || z < -1 || z > depth) boundsOk = false;
                    if (y < 0 || y > type.clearHeight()) heightOk = false;
                }
                typeMaxY = Math.max(typeMaxY, maxY);
                boolean uniqueOk = positions.size() == plan.size();
                boolean countOk = expectedCount == null || expectedCount == plan.size();
                if (expectedCount == null) expectedCount = plan.size();

                BlockPos supply = supplyPosition(ORIGIN, type, rotation);
                List<Scaffold> scaffolds = scaffolds(ORIGIN, type, rotation, supply);
                boolean scaffoldCollision = false, scaffoldOutsideValidation = false;
                for (Scaffold scaffold : scaffolds) {
                    for (BlockPos piece : scaffold.pieces()) {
                        int x = piece.getX() - ORIGIN.getX(), y = piece.getY() - ORIGIN.getY(), z = piece.getZ() - ORIGIN.getZ();
                        if (x < -1 || x > width || z < -1 || z > depth || y < 0 || y > type.clearHeight()) scaffoldOutsideValidation = true;
                        if (positions.contains(piece)) scaffoldCollision = true;
                    }
                }
                if (scaffoldOutsideValidation) outsideValidationCases++;
                if (scaffoldCollision) scaffoldBlueprintCollisionCases++;

                int caseHigh = 0, caseZero = 0, caseSingle = 0, caseMinReach = Integer.MAX_VALUE;
                for (BuildingBlueprints.Placement placement : plan) {
                    int relativeY = placement.pos().getY() - ORIGIN.getY();
                    if (relativeY <= 3) continue;
                    caseHigh++; highTargets++;
                    int reach = 0;
                    for (Scaffold scaffold : scaffolds) {
                        if (scaffold.steps().isEmpty()) continue;
                        int index = Math.min(scaffold.steps().size() - 1, Math.max(0, relativeY - 3));
                        BlockPos candidate = scaffold.steps().get(index).above();
                        if (distSqr(candidate, placement.pos()) <= HIGH_WORK_RANGE_SQR) reach++;
                    }
                    caseMinReach = Math.min(caseMinReach, reach);
                    typeMinReach = Math.min(typeMinReach, reach);
                    if (reach == 0) { caseZero++; typeZero++; zeroReachTargets++; }
                    if (reach == 1) { caseSingle++; typeSingle++; singleReachTargets++; }
                }
                if (caseHigh == 0) caseMinReach = 4;

                boolean supplyOverlap = positions.contains(supply);
                boolean ok = !plan.isEmpty() && uniqueOk && countOk && boundsOk && heightOk
                        && !scaffoldCollision && !supplyOverlap && caseZero == 0;
                if (!ok) failures++;
                System.out.printf("CASE type=%s rot=%s count=%d unique=%s bounds=[%d..%d,%d..%d,%d..%d] boundsOk=%s heightOk=%s high=%d minReach=%d zeroReach=%d singleReach=%d scaffoldOutsideValidatedEnvelope=%s scaffoldBlueprintCollision=%s supplyOverlap=%s OK=%s%n",
                        type.id(), rotation.name(), plan.size(), uniqueOk,
                        minX, maxX, minY, maxY, minZ, maxZ, boundsOk, heightOk,
                        caseHigh, caseMinReach, caseZero, caseSingle,
                        scaffoldOutsideValidation, scaffoldCollision, supplyOverlap, ok);
            }
            if (typeMinReach == Integer.MAX_VALUE) typeMinReach = 4;
            System.out.printf("TYPE_SUMMARY type=%s placements=%d maxY=%d minReach=%d zeroReach=%d singleReach=%d%n",
                    type.id(), expectedCount == null ? 0 : expectedCount, typeMaxY, typeMinReach, typeZero, typeSingle);
        }
        System.out.printf("TOTAL cases=%d failures=%d highTargets=%d zeroReachTargets=%d singleReachTargets=%d casesWithScaffoldOutsideValidatedEnvelope=%d scaffoldBlueprintCollisionCases=%d%n",
                cases, failures, highTargets, zeroReachTargets, singleReachTargets, outsideValidationCases, scaffoldBlueprintCollisionCases);
    }

    private static BlockPos supplyPosition(BlockPos origin, BuildingType type, BuildingRotation rotation) {
        return origin.offset(-2, 0, Math.max(1, rotation.rotatedDepth(type) / 2));
    }

    private static List<Scaffold> scaffolds(BlockPos origin, BuildingType type, BuildingRotation rotation, BlockPos supply) {
        int width = rotation.rotatedWidth(type), depth = rotation.rotatedDepth(type);
        int midX = Math.max(1, width / 2), midZ = Math.max(1, depth / 2);
        return List.of(
                scaffold(supply.offset(-1, 0, 0), supply),
                scaffold(origin.offset(width + 2, 0, midZ), supply),
                scaffold(origin.offset(midX, 0, -3), supply),
                scaffold(origin.offset(midX, 0, depth + 2), supply));
    }

    private static Scaffold scaffold(BlockPos center, BlockPos supply) {
        int[][] ring = {{0,-1},{1,-1},{1,0},{1,1},{0,1},{-1,1},{-1,0},{-1,-1}};
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
            pieces.add(tread); steps.add(tread); step++;
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
