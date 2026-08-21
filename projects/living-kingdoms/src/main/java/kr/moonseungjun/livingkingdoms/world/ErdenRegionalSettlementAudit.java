package kr.moonseungjun.livingkingdoms.world;

import kr.moonseungjun.livingkingdoms.world.ExternalUrbanFabricBuilder.UrbanFragmentSnapshot;
import kr.moonseungjun.livingkingdoms.world.ExternalUrbanFabricBuilder.UrbanSourceBlock;
import kr.moonseungjun.livingkingdoms.worldgen.AuthoredContinentDensity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.Rotation;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** Independent physical probes for the representative second-ring Erden village. */
public final class ErdenRegionalSettlementAudit {
    private static final String REPRESENTATIVE_ID = "harvest_crossing";
    private static final String REPRESENTATIVE_BUILDING_ROLE = "reeve_hall";
    private static volatile ProbeSet probes;

    private ErdenRegionalSettlementAudit() {
    }

    public static Set<Long> requiredChunkKeys() {
        ProbeSet p = probes();
        Set<Long> result = new LinkedHashSet<>();
        result.add(pack(p.wellX >> 4, p.wellZ >> 4));
        result.add(pack(p.roadX >> 4, p.roadZ >> 4));
        result.add(pack(p.fieldX >> 4, p.fieldZ >> 4));
        result.add(pack(p.doorX >> 4, p.doorZ >> 4));
        return Set.copyOf(result);
    }

    public static boolean verify(ServerLevel level) {
        ProbeSet p = probes();
        if (!allChunksLoaded(level)) return false;
        int wellY = surfaceY(p.wellX, p.wellZ);
        int roadY = surfaceY(p.roadX, p.roadZ);
        int fieldY = surfaceY(p.fieldX, p.fieldZ);
        int buildingY = surfaceY(p.buildingCenterX, p.buildingCenterZ);

        boolean physicalSquare = level.getBlockState(p.wellX, wellY, p.wellZ).is(Blocks.WATER);
        boolean physicalRoad = level.getBlockState(p.roadX, roadY, p.roadZ).is(Blocks.PACKED_MUD);
        boolean physicalField = level.getBlockState(p.fieldX, fieldY, p.fieldZ).is(Blocks.FARMLAND)
                && level.getBlockState(p.fieldX, fieldY + 1, p.fieldZ).is(Blocks.WHEAT);
        boolean physicalDoor = level.getBlockState(p.doorX, buildingY + p.doorLocalY, p.doorZ)
                .getBlock() instanceof DoorBlock;

        ChunkPos doorChunk = new ChunkPos(p.doorX >> 4, p.doorZ >> 4);
        int structural = 0;
        for (int x = doorChunk.getMinBlockX(); x <= doorChunk.getMinBlockX() + 15; x++) {
            for (int z = doorChunk.getMinBlockZ(); z <= doorChunk.getMinBlockZ() + 15; z++) {
                for (int y = buildingY; y <= buildingY + p.fragmentHeight; y++) {
                    var state = level.getBlockState(x, y, z);
                    if (state.isAir() || state.is(Blocks.WATER) || state.is(Blocks.FARMLAND)
                            || state.is(Blocks.GRASS_BLOCK) || state.is(Blocks.DIRT)
                            || state.is(Blocks.PACKED_MUD) || state.is(Blocks.DIRT_PATH)) continue;
                    structural++;
                }
            }
        }
        return physicalSquare && physicalRoad && physicalField && physicalDoor && structural >= 40;
    }

    public static String representativeId() {
        return REPRESENTATIVE_ID;
    }

    private static boolean allChunksLoaded(ServerLevel level) {
        for (long packed : requiredChunkKeys()) {
            if (!level.hasChunk(unpackX(packed), unpackZ(packed))) return false;
        }
        return true;
    }

    private static ProbeSet probes() {
        ProbeSet result = probes;
        if (result != null) return result;
        synchronized (ErdenRegionalSettlementAudit.class) {
            result = probes;
            if (result != null) return result;
            ErdenRegionalSettlementCatalog.Settlement settlement =
                    ErdenRegionalSettlementCatalog.settlements().stream()
                            .filter(candidate -> REPRESENTATIVE_ID.equals(candidate.id()))
                            .findFirst()
                            .orElseThrow(() -> new IllegalStateException("Missing representative regional settlement"));
            ErdenRegionalSettlementCatalog.BuildingLot lot = settlement.buildings().stream()
                    .filter(candidate -> REPRESENTATIVE_BUILDING_ROLE.equals(candidate.role()))
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException("Missing representative regional building"));

            UrbanFragmentSnapshot fragment = sourceFragment(lot.style());
            Rotation rotation = rotationFor(fragment.exteriorSide(), lot.desiredFront());
            int width = rotatedWidth(fragment, rotation);
            int length = rotatedLength(fragment, rotation);
            int buildingCenterX = settlement.x() + lot.dx();
            int buildingCenterZ = settlement.z() + lot.dz();
            int originX = buildingCenterX - width / 2;
            int originZ = buildingCenterZ - length / 2;
            RotatedPoint door = rotate(fragment.entranceX(), fragment.entranceZ(),
                    fragment.width(), fragment.length(), rotation);
            int doorY = fragment.blocks().stream()
                    .filter(block -> block.x() == fragment.entranceX()
                            && block.z() == fragment.entranceZ()
                            && block.state().getBlock() instanceof DoorBlock)
                    .mapToInt(UrbanSourceBlock::y)
                    .min()
                    .orElseThrow(() -> new IllegalStateException("Representative source entrance lost its door"));

            result = new ProbeSet(
                    settlement.x(), settlement.z(),
                    settlement.x() + 20, settlement.z(),
                    settlement.x() + 170, settlement.z() + 7,
                    buildingCenterX, buildingCenterZ,
                    originX + door.x, originZ + door.z,
                    doorY, fragment.height());
            probes = result;
            return result;
        }
    }

    private static UrbanFragmentSnapshot sourceFragment(String style) {
        List<UrbanFragmentSnapshot> snapshots = new ArrayList<>(
                ExternalUrbanFabricBuilder.fragmentSnapshotsForDiagnostics().values());
        snapshots.sort(Comparator.comparing(UrbanFragmentSnapshot::fragmentKey));
        String suffix = switch (style) {
            case "house" -> "/all_in_one_house.schem";
            case "manor" -> "/medieval_manor.schem";
            case "castle" -> "/fantasy_castle_house.schem";
            case "player_castle" -> "/player_castle_house.schem";
            case "tavern" -> "/medieval_tavern_inn.schem";
            default -> throw new IllegalArgumentException("Unknown regional source style " + style);
        };
        return snapshots.stream()
                .filter(snapshot -> snapshot.resource().endsWith(suffix))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Missing representative source fragment " + suffix));
    }

    private static Rotation rotationFor(String sourceSide, String desiredSide) {
        Side source = Side.valueOf(sourceSide);
        Side desired = Side.valueOf(desiredSide.toUpperCase());
        for (Rotation rotation : Rotation.values()) {
            if (rotateSide(source, rotation) == desired) return rotation;
        }
        throw new IllegalStateException("Unable to rotate regional audit source entrance");
    }

    private static Side rotateSide(Side side, Rotation rotation) {
        return switch (rotation) {
            case NONE -> side;
            case CLOCKWISE_90 -> switch (side) {
                case NORTH -> Side.EAST;
                case EAST -> Side.SOUTH;
                case SOUTH -> Side.WEST;
                case WEST -> Side.NORTH;
            };
            case CLOCKWISE_180 -> switch (side) {
                case NORTH -> Side.SOUTH;
                case SOUTH -> Side.NORTH;
                case EAST -> Side.WEST;
                case WEST -> Side.EAST;
            };
            case COUNTERCLOCKWISE_90 -> switch (side) {
                case NORTH -> Side.WEST;
                case WEST -> Side.SOUTH;
                case SOUTH -> Side.EAST;
                case EAST -> Side.NORTH;
            };
        };
    }

    private static RotatedPoint rotate(int x, int z, int width, int length, Rotation rotation) {
        return switch (rotation) {
            case NONE -> new RotatedPoint(x, z);
            case CLOCKWISE_90 -> new RotatedPoint(length - 1 - z, x);
            case CLOCKWISE_180 -> new RotatedPoint(width - 1 - x, length - 1 - z);
            case COUNTERCLOCKWISE_90 -> new RotatedPoint(z, width - 1 - x);
        };
    }

    private static int rotatedWidth(UrbanFragmentSnapshot fragment, Rotation rotation) {
        return rotation == Rotation.CLOCKWISE_90 || rotation == Rotation.COUNTERCLOCKWISE_90
                ? fragment.length() : fragment.width();
    }

    private static int rotatedLength(UrbanFragmentSnapshot fragment, Rotation rotation) {
        return rotation == Rotation.CLOCKWISE_90 || rotation == Rotation.COUNTERCLOCKWISE_90
                ? fragment.width() : fragment.length();
    }

    private static int surfaceY(int x, int z) {
        return (int) Math.round(AuthoredContinentDensity.surfaceHeight(x, z));
    }

    private static long pack(int x, int z) {
        return ((long) x << 32) ^ (z & 0xffffffffL);
    }

    private static int unpackX(long packed) {
        return (int) (packed >> 32);
    }

    private static int unpackZ(long packed) {
        return (int) packed;
    }

    private enum Side { NORTH, SOUTH, WEST, EAST }

    private record RotatedPoint(int x, int z) {
    }

    private record ProbeSet(
            int wellX,
            int wellZ,
            int roadX,
            int roadZ,
            int fieldX,
            int fieldZ,
            int buildingCenterX,
            int buildingCenterZ,
            int doorX,
            int doorZ,
            int doorLocalY,
            int fragmentHeight) {
    }
}
