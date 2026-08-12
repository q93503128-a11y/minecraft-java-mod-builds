package kr.moonseungjun.livingkingdoms.world;

import kr.moonseungjun.livingkingdoms.LivingKingdoms;

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
 * Read-only topology catalog for the licensed source structures used by Erden urban fabric.
 *
 * <p>The runtime buildings are cropped and rotated from these schematics. Before replacing the
 * synthetic one-room/two-floor compatibility layer with source-native rooms, we need a stable
 * picture of which source assets actually contain door-connected floors and stair routes. This
 * catalog analyzes the schematic resources themselves: it performs no world access, creates no
 * chunks and changes no placement counts.</p>
 */
public final class ErdenUrbanSourceTopologyCatalog {
    private static final List<String> RESOURCES = List.of(
            "/data/livingkingdoms/structures/external/all_in_one_house.schem",
            "/data/livingkingdoms/structures/external/fantasy_castle_house.schem",
            "/data/livingkingdoms/structures/external/medieval_manor.schem"
    );
    private static final int MIN_FLOOR_CELLS = 12;
    private static final int MIN_LEVEL_SEPARATION = 3;
    private static final Map<String, Profile> PROFILES = new LinkedHashMap<>();
    private static boolean bootstrapped;

    private ErdenUrbanSourceTopologyCatalog() {
    }

    public static synchronized void bootstrap() {
        if (bootstrapped) return;
        PROFILES.clear();
        for (String resource : RESOURCES) {
            Profile profile = analyze(resource);
            PROFILES.put(resource, profile);
            LivingKingdoms.LOGGER.info(
                    "LK_ERDEN_SOURCE_TOPOLOGY resource={} dimensions={}x{}x{} doors={} stairs={} reachable={} vertical_span={} floor_bands={} multilevel_candidate={} source_only=true world_reads=false",
                    resource,
                    profile.width(), profile.height(), profile.length(),
                    profile.doors(), profile.stairBlocks(), profile.reachableCells(),
                    profile.verticalSpan(), profile.floorBands(), profile.multilevelCandidate());
        }
        bootstrapped = true;
        long multi = PROFILES.values().stream().filter(Profile::multilevelCandidate).count();
        int stairs = PROFILES.values().stream().mapToInt(Profile::stairBlocks).sum();
        LivingKingdoms.LOGGER.info(
                "Prepared Erden source topology catalog resources={} multilevel_sources={} stair_blocks={} read_only=true placement_counts_unchanged=true",
                PROFILES.size(), multi, stairs);
    }

    public static Profile profile(String resource) {
        bootstrap();
        return PROFILES.get(resource);
    }

    public static Map<String, Profile> profiles() {
        bootstrap();
        return Map.copyOf(PROFILES);
    }

    private static Profile analyze(String resource) {
        SpongeStructureTemplate source = SpongeStructureTemplate.load(resource);
        int width = source.width();
        int height = source.height();
        int length = source.length();
        List<String> palette = source.palette();

        List<Node> lowDoors = findLowDoors(source, palette);
        int doors = countBlocks(source, palette, ErdenUrbanSourceTopologyCatalog::isDoor);
        int stairBlocks = countBlocks(source, palette, id -> id.endsWith("_stairs"));

        ArrayDeque<Node> pending = new ArrayDeque<>();
        Set<Long> visited = new HashSet<>();
        for (Node door : lowDoors) {
            int feetY = resolveFeetY(source, palette, door.x(), door.z(), door.y());
            if (feetY == Integer.MIN_VALUE) continue;
            Node seed = new Node(door.x(), feetY, door.z());
            if (visited.add(key(seed))) pending.addLast(seed);
        }

        Map<Integer, Integer> reachableByY = new HashMap<>();
        int minY = Integer.MAX_VALUE;
        int maxY = Integer.MIN_VALUE;
        while (!pending.isEmpty()) {
            Node current = pending.removeFirst();
            minY = Math.min(minY, current.y());
            maxY = Math.max(maxY, current.y());
            reachableByY.merge(current.y(), 1, Integer::sum);
            for (int[] direction : DIRECTIONS) {
                int x = current.x() + direction[0];
                int z = current.z() + direction[1];
                if (x < 0 || x >= width || z < 0 || z >= length) continue;
                int feetY = resolveFeetY(source, palette, x, z, current.y());
                if (feetY == Integer.MIN_VALUE || Math.abs(feetY - current.y()) > 1) continue;
                Node next = new Node(x, feetY, z);
                if (visited.add(key(next))) pending.addLast(next);
            }
        }

        List<FloorBand> bands = floorBands(reachableByY);
        int verticalSpan = visited.isEmpty() ? 0 : maxY - minY;
        boolean multilevel = bands.size() >= 2
                && verticalSpan >= MIN_LEVEL_SEPARATION
                && stairBlocks > 0;
        return new Profile(
                resource, width, height, length,
                doors, stairBlocks, visited.size(), verticalSpan,
                List.copyOf(bands), multilevel);
    }

    private static List<Node> findLowDoors(
            SpongeStructureTemplate source, List<String> palette) {
        List<Node> doors = new ArrayList<>();
        int lowest = Integer.MAX_VALUE;
        for (int y = 0; y < source.height(); y++) {
            for (int z = 0; z < source.length(); z++) {
                for (int x = 0; x < source.width(); x++) {
                    String id = id(source, palette, x, y, z);
                    if (!isDoor(id)) continue;
                    lowest = Math.min(lowest, y);
                    doors.add(new Node(x, y, z));
                }
            }
        }
        if (doors.isEmpty()) return List.of();
        int limit = lowest + 1;
        return doors.stream().filter(door -> door.y() <= limit).toList();
    }

    private static int countBlocks(
            SpongeStructureTemplate source,
            List<String> palette,
            java.util.function.Predicate<String> predicate) {
        int count = 0;
        for (int y = 0; y < source.height(); y++) {
            for (int z = 0; z < source.length(); z++) {
                for (int x = 0; x < source.width(); x++) {
                    if (predicate.test(id(source, palette, x, y, z))) count++;
                }
            }
        }
        return count;
    }

    private static int resolveFeetY(
            SpongeStructureTemplate source,
            List<String> palette,
            int x, int z, int preferredFeetY) {
        for (int offset : FEET_OFFSETS) {
            int feetY = preferredFeetY + offset;
            if (sourceWalkable(source, palette, x, feetY, z)) return feetY;
        }
        return Integer.MIN_VALUE;
    }

    private static boolean sourceWalkable(
            SpongeStructureTemplate source,
            List<String> palette,
            int x, int feetY, int z) {
        if (x < 0 || x >= source.width() || z < 0 || z >= source.length()) return false;
        if (feetY <= 0 || feetY + 1 >= source.height()) return false;
        String feet = id(source, palette, x, feetY, z);
        String head = id(source, palette, x, feetY + 1, z);
        String floor = id(source, palette, x, feetY - 1, z);
        return bodyPassable(feet) && bodyPassable(head) && supportsBody(floor);
    }

    private static boolean bodyPassable(String id) {
        return isAir(id)
                || isDoor(id)
                || id.contains("torch")
                || id.contains("button")
                || id.contains("pressure_plate")
                || id.endsWith("_sign")
                || id.endsWith("_wall_sign")
                || id.contains("carpet");
    }

    private static boolean supportsBody(String id) {
        if (isAir(id) || isFluid(id) || decorativePlant(id)) return false;
        if (id.contains("torch") || id.contains("button") || id.contains("pressure_plate")) return false;
        return true;
    }

    private static List<FloorBand> floorBands(Map<Integer, Integer> reachableByY) {
        List<Map.Entry<Integer, Integer>> candidates = reachableByY.entrySet().stream()
                .filter(entry -> entry.getValue() >= MIN_FLOOR_CELLS)
                .sorted(Map.Entry.comparingByKey())
                .toList();
        List<FloorBand> result = new ArrayList<>();
        for (Map.Entry<Integer, Integer> candidate : candidates) {
            if (result.isEmpty()) {
                result.add(new FloorBand(candidate.getKey(), candidate.getValue()));
                continue;
            }
            FloorBand previous = result.getLast();
            if (candidate.getKey() - previous.feetY() >= MIN_LEVEL_SEPARATION) {
                result.add(new FloorBand(candidate.getKey(), candidate.getValue()));
            } else if (candidate.getValue() > previous.reachableCells()) {
                result.set(result.size() - 1,
                        new FloorBand(candidate.getKey(), candidate.getValue()));
            }
        }
        result.sort(Comparator.comparingInt(FloorBand::feetY));
        return result;
    }

    private static String id(
            SpongeStructureTemplate source,
            List<String> palette,
            int x, int y, int z) {
        int paletteIndex = source.paletteIndex(x, y, z);
        if (paletteIndex < 0 || paletteIndex >= palette.size()) return "minecraft:air";
        String specification = palette.get(paletteIndex);
        int bracket = specification.indexOf('[');
        return (bracket < 0 ? specification : specification.substring(0, bracket)).trim();
    }

    private static boolean isDoor(String id) {
        return id.endsWith("_door") && !id.endsWith("_trapdoor");
    }

    private static boolean isAir(String id) {
        return id.equals("minecraft:air")
                || id.equals("minecraft:cave_air")
                || id.equals("minecraft:void_air")
                || id.equals("minecraft:structure_void");
    }

    private static boolean isFluid(String id) {
        return id.equals("minecraft:water") || id.equals("minecraft:lava");
    }

    private static boolean decorativePlant(String id) {
        return id.endsWith("_leaves")
                || id.endsWith("_sapling")
                || id.contains("grass")
                || id.contains("flower")
                || id.contains("fern")
                || id.contains("vine")
                || id.contains("mushroom")
                || id.contains("lily_pad")
                || id.contains("bamboo")
                || id.contains("cactus");
    }

    private static long key(Node node) {
        long x = node.x() & 0x1fffffL;
        long y = node.y() & 0x3fffffL;
        long z = node.z() & 0x1fffffL;
        return (x << 43) ^ (y << 21) ^ z;
    }

    private static final int[][] DIRECTIONS = {
            {1, 0}, {-1, 0}, {0, 1}, {0, -1}
    };
    private static final int[] FEET_OFFSETS = {0, 1, -1};

    public record FloorBand(int feetY, int reachableCells) {
    }

    public record Profile(
            String resource,
            int width,
            int height,
            int length,
            int doors,
            int stairBlocks,
            int reachableCells,
            int verticalSpan,
            List<FloorBand> floorBands,
            boolean multilevelCandidate) {
    }

    private record Node(int x, int y, int z) {
    }
}
