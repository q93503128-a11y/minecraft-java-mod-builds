package io.github.q93503128.turnbound.world;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Sparse authored shell for the canonical 1024x1024 Aster March map.
 *
 * This does not flatten one million columns. It fills the important seams between the already-authored chapter
 * ribbons, disguises the four outer borders with world fiction, and provides large navigation landmarks so the
 * campaign reads as one place instead of six disconnected test cells.
 */
public final class AsterMarchWorldShell {
    private static final int MARKER_X = 472;
    private static final int MARKER_Y = 45;
    private static final int MARKER_Z = 472;
    private static final Map<UUID, Integer> GATE_MASK = new ConcurrentHashMap<>();

    public enum Gate {
        GLOAM_NORTH(0, 65, -108, Axis.Z),
        AQUEDUCT_WEST(-124, 65, 20, Axis.X),
        QUARRY_PASS(-60, 67, 300, Axis.Z),
        RELAY_EAST(124, 65, -80, Axis.X);

        private final int x;
        private final int y;
        private final int z;
        private final Axis axis;

        Gate(int x, int y, int z, Axis axis) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.axis = axis;
        }

        public Vec3 center() { return new Vec3(x + 0.5, y + 1.0, z + 0.5); }
    }

    private enum Axis { X, Z }
    private record Node(int x, int y, int z) {}

    private static final List<Node> QUARRY_TRANSIT = List.of(
            new Node(190, 66, 230),
            new Node(118, 67, 266),
            new Node(42, 67, 286),
            new Node(-18, 67, 294),
            new Node(-60, 67, 300),
            new Node(-86, 68, 307),
            new Node(-110, 68, 315));

    private static final List<Node> RELAY_TRANSIT = List.of(
            new Node(124, 65, -80),
            new Node(166, 65, -104),
            new Node(202, 66, -132),
            new Node(232, 67, -156),
            new Node(250, 67, -170),
            new Node(270, 67, -185));

    private static final List<Node> SOUTHGATE_SIDE = List.of(
            new Node(190,66,230), new Node(188,66,275), new Node(172,66,308), new Node(154,66,326),
            new Node(128,66,315), new Node(112,67,280), new Node(118,67,266));
    private static final List<Node> GLOAM_EAST_LOOP = List.of(
            new Node(-40,69,-300), new Node(18,69,-286), new Node(58,69,-270), new Node(92,69,-262), new Node(52,69,-318), new Node(-38,69,-320));
    private static final List<Node> GLOAM_WEST_LOOP = List.of(
            new Node(-40,69,-300), new Node(-92,69,-314), new Node(-142,69,-332), new Node(-112,70,-366), new Node(-75,70,-365));
    private static final List<Node> AQUEDUCT_LOOP = List.of(
            new Node(-320,66,20), new Node(-356,64,72), new Node(-302,65,158), new Node(-382,64,112),
            new Node(-450,63,-42), new Node(-410,63,15));
    private static final List<Node> QUARRY_LOOP = List.of(
            new Node(20,69,405), new Node(78,66,398), new Node(150,65,390), new Node(94,64,424),
            new Node(20,64,452), new Node(-58,66,452), new Node(-124,67,438), new Node(-80,68,382), new Node(-30,68,365));

    private AsterMarchWorldShell() {}

    public static void build(ServerLevel level) {
        if (hasMarker(level)) return;

        buildTransitRoad(level, List.of(
                new Node(0, 65, -108), new Node(0, 65, -116), new Node(-3, 67, -145)),
                4, Blocks.POLISHED_ANDESITE, Blocks.MOSSY_STONE_BRICKS);
        buildTransitRoad(level, List.of(
                new Node(-124, 65, 20), new Node(-132, 65, 20), new Node(-150, 65, 20)),
                4, Blocks.STONE_BRICKS, Blocks.CRACKED_STONE_BRICKS);
        buildTransitRoad(level, QUARRY_TRANSIT, 4, Blocks.COARSE_DIRT, Blocks.BASALT);
        buildTransitRoad(level, RELAY_TRANSIT, 4, Blocks.DEEPSLATE_TILES, Blocks.POLISHED_DEEPSLATE);

        // Exploration loops turn the chapter ribbons into regions with optional walking space instead of one-way test lanes.
        buildTransitRoad(level, SOUTHGATE_SIDE, 3, Blocks.DIRT_PATH, Blocks.GRASS_BLOCK);
        buildTransitRoad(level, GLOAM_EAST_LOOP, 3, Blocks.PODZOL, Blocks.MOSS_BLOCK);
        buildTransitRoad(level, GLOAM_WEST_LOOP, 3, Blocks.PODZOL, Blocks.MOSS_BLOCK);
        buildTransitRoad(level, AQUEDUCT_LOOP, 3, Blocks.STONE_BRICKS, Blocks.ANDESITE);
        buildTransitRoad(level, QUARRY_LOOP, 3, Blocks.TUFF, Blocks.BASALT);

        buildGate(level, Gate.GLOAM_NORTH, false);
        buildGate(level, Gate.AQUEDUCT_WEST, false);
        buildGate(level, Gate.QUARRY_PASS, false);
        buildGate(level, Gate.RELAY_EAST, false);

        buildOuterBoundaries(level);
        buildNavigationLandmarks(level);
        buildOldRelayBranchWings(level);
        writeMarker(level);
    }

    public static void syncProgressionGates(ServerLevel level, UUID playerId) {
        if (playerId == null) return;
        int mask = (CampaignContentUnlocks.chapter1Complete(playerId) ? 1 : 0)
                | (CampaignContentUnlocks.chapter2Complete(playerId) ? 2 : 0)
                | (CampaignContentUnlocks.chapter3Complete(playerId) ? 4 : 0)
                | (CampaignContentUnlocks.oldRelayEntrance(playerId) ? 8 : 0);
        Integer previous = GATE_MASK.put(playerId, mask);
        if (previous != null && previous == mask) return;
        setGateOpen(level, Gate.GLOAM_NORTH, (mask & 1) != 0);
        setGateOpen(level, Gate.AQUEDUCT_WEST, (mask & 2) != 0);
        setGateOpen(level, Gate.QUARRY_PASS, (mask & 4) != 0);
        setGateOpen(level, Gate.RELAY_EAST, (mask & 8) != 0);
    }

    public static void forget(UUID playerId) { if (playerId != null) GATE_MASK.remove(playerId); }

    public static void setGateOpen(ServerLevel level, Gate gate, boolean open) {
        if (level == null || gate == null) return;
        buildGate(level, gate, open);
    }

    public static boolean nearGate(Vec3 position, Gate gate, double radius) {
        if (position == null || gate == null) return false;
        Vec3 center = gate.center();
        double dx = position.x - center.x;
        double dz = position.z - center.z;
        return dx * dx + dz * dz <= radius * radius;
    }

    /** Southgate keeps ownership of this road until the quarry gate hands the player to Chapter 4. */
    public static boolean quarryTransitContains(Vec3 position) {
        if (position == null || position.y < 54 || position.y > 92) return false;
        for (int i = 0; i < QUARRY_TRANSIT.size() - 1; i++) {
            if (distanceSqToSegment(position.x, position.z, QUARRY_TRANSIT.get(i), QUARRY_TRANSIT.get(i + 1)) <= 18.0 * 18.0) return true;
        }
        return false;
    }

    /** Optional Southgate exploration loop linking the relay, watch ruin and quarry fork. */
    public static boolean southgateExplorationContains(Vec3 position) {
        if (position == null || position.y < 54 || position.y > 92) return false;
        for (int i = 0; i < SOUTHGATE_SIDE.size() - 1; i++) {
            if (distanceSqToSegment(position.x, position.z, SOUTHGATE_SIDE.get(i), SOUTHGATE_SIDE.get(i + 1)) <= 15.0 * 15.0) return true;
        }
        return false;
    }

    /** The long east road is part of the Old Relay travel space once Chapter 5 opens. */
    public static boolean relayTransitContains(Vec3 position) {
        if (position == null || position.y < 54 || position.y > 92) return false;
        for (int i = 0; i < RELAY_TRANSIT.size() - 1; i++) {
            if (distanceSqToSegment(position.x, position.z, RELAY_TRANSIT.get(i), RELAY_TRANSIT.get(i + 1)) <= 18.0 * 18.0) return true;
        }
        return false;
    }

    private static void buildOuterBoundaries(ServerLevel level) {
        for (int x = -468; x <= 468; x += 12) {
            int height = 12 + Math.floorMod(x * 17, 12);
            crag(level, x, 66, -486, 2 + Math.floorMod(x, 2), height,
                    Blocks.DEEPSLATE, Blocks.BLACKSTONE);
        }
        for (int z = -468; z <= 468; z += 12) {
            int height = 10 + Math.floorMod(z * 13, 10);
            crag(level, -486, 56, z, 2, height, Blocks.STONE, Blocks.ANDESITE);
        }
        buildBrokenHighAqueduct(level);
        for (int x = -468; x <= 468; x += 12) {
            int height = 11 + Math.floorMod(x * 19, 11);
            crag(level, x, 58, 486, 2, height, Blocks.BASALT, Blocks.BLACKSTONE);
            if (Math.floorMod(x, 48) == 0) set(level, x, 60, 482, Blocks.MAGMA_BLOCK);
        }

        // East: rift pylons with sparse iron ribs; avoids a literal world-border wall while staying 26.2-compatible.
        for (int z = -464; z <= 464; z += 20) {
            int base = 64 + Math.floorMod(z, 3);
            for (int y = base; y <= base + 13; y++) set(level, 486, y, z, y % 4 == 0 ? Blocks.CRYING_OBSIDIAN : Blocks.OBSIDIAN);
            set(level, 486, base + 14, z, Blocks.SOUL_LANTERN);
            for (int dy = 2; dy <= 10; dy += 4) {
                set(level, 480, base + dy, z + 4, Blocks.IRON_BARS);
                set(level, 492, base + dy + 1, z - 4, Blocks.IRON_BARS);
            }
        }
    }

    private static void buildNavigationLandmarks(ServerLevel level) {
        stoneBeacon(level, 112, 67, 280, Blocks.MOSSY_STONE_BRICKS, Blocks.AMETHYST_BLOCK);
        ruinedWatch(level, 154, 66, 326);
        rootArch(level, 92, 69, -262);
        sporeGrove(level, -142, 69, -332);
        sluiceTower(level, -450, 63, -42);
        brokenArch(level, -302, 65, 158);
        quarryCrane(level, 150, 65, 390);
        coolingBasin(level, -124, 67, 438);
        relayPylon(level, 214, 66, -142, 8);
        relayPylon(level, 244, 67, -164, 11);
    }

    private static void buildOldRelayBranchWings(ServerLevel level) {
        relayBranchRoom(level, 365, 67, -305, 12, Blocks.POLISHED_DEEPSLATE, Blocks.AMETHYST_BLOCK);
        relayArch(level, 350, 67, -292, Axis.X);
        relayArch(level, 383, 67, -300, Axis.X);
        relayArch(level, 382, 66, -320, Axis.Z);
        buildTransitRoad(level, List.of(
                new Node(365,67,-305), new Node(345,67,-272), new Node(320,67,-245), new Node(305,67,-225)),
                5, Blocks.DEEPSLATE_TILES, Blocks.CRACKED_DEEPSLATE_BRICKS);
        relayBranchRoom(level, 345, 67, -272, 10, Blocks.DEEPSLATE_BRICKS, Blocks.IRON_BLOCK);
        relayBranchRoom(level, 305, 67, -225, 10, Blocks.DEEPSLATE_BRICKS, Blocks.LECTERN);
        buildTransitRoad(level, List.of(
                new Node(365,67,-305), new Node(395,66,-285), new Node(430,65,-296), new Node(455,65,-310)),
                5, Blocks.POLISHED_DEEPSLATE, Blocks.CRYING_OBSIDIAN);
        relayBranchRoom(level, 400, 66, -300, 11, Blocks.DEEPSLATE_TILES, Blocks.REDSTONE_LAMP);
        relayBranchRoom(level, 455, 65, -310, 10, Blocks.REINFORCED_DEEPSLATE, Blocks.AMETHYST_BLOCK);
        buildTransitRoad(level, List.of(
                new Node(365,67,-305), new Node(390,66,-318), new Node(410,65,-330), new Node(418,65,-325), new Node(420,65,-350)),
                5, Blocks.DEEPSLATE_BRICKS, Blocks.OBSIDIAN);
        relayBranchRoom(level, 410, 65, -330, 11, Blocks.REINFORCED_DEEPSLATE, Blocks.CRYING_OBSIDIAN);
        relayBranchRoom(level, 418, 65, -325, 8, Blocks.DEEPSLATE_TILES, Blocks.LECTERN);
    }

    private static void relayBranchRoom(ServerLevel level, int cx, int groundY, int cz, int radius, Block floor, Block core) {
        for (int dx = -radius; dx <= radius; dx++) for (int dz = -radius; dz <= radius; dz++) {
            int d2 = dx * dx + dz * dz;
            if (d2 > radius * radius) continue;
            prepareGround(level, cx + dx, groundY, cz + dz, d2 > (radius - 2) * (radius - 2) ? Blocks.DEEPSLATE_BRICKS : floor);
        }
        for (int[] q : new int[][]{{-radius+2,-radius+2},{radius-2,-radius+2},{-radius+2,radius-2},{radius-2,radius-2}}) {
            for (int dy = 1; dy <= 6; dy++) set(level, cx + q[0], groundY + dy, cz + q[1], Blocks.DEEPSLATE_BRICKS);
            set(level, cx + q[0], groundY + 7, cz + q[1], Blocks.SOUL_LANTERN);
        }
        set(level, cx, groundY + 1, cz, core);
    }

    private static void relayArch(ServerLevel level, int x, int groundY, int z, Axis axis) {
        for (int offset = -5; offset <= 5; offset++) {
            int px = axis == Axis.X ? x : x + offset;
            int pz = axis == Axis.X ? z + offset : z;
            int h = 5 + (5 - Math.abs(offset)) / 2;
            if (Math.abs(offset) <= 2) continue;
            for (int dy = 1; dy <= h; dy++) set(level, px, groundY + dy, pz, Blocks.REINFORCED_DEEPSLATE);
        }
    }

    private static void buildGate(ServerLevel level, Gate gate, boolean open) {
        Block frame = switch (gate) {
            case GLOAM_NORTH -> Blocks.MOSSY_STONE_BRICKS;
            case AQUEDUCT_WEST -> Blocks.STONE_BRICKS;
            case QUARRY_PASS -> Blocks.POLISHED_BLACKSTONE_BRICKS;
            case RELAY_EAST -> Blocks.DEEPSLATE_TILES;
        };
        Block accent = switch (gate) {
            case GLOAM_NORTH -> Blocks.DARK_OAK_LOG;
            case AQUEDUCT_WEST -> Blocks.IRON_BLOCK;
            case QUARRY_PASS -> Blocks.MAGMA_BLOCK;
            case RELAY_EAST -> Blocks.CRYING_OBSIDIAN;
        };

        if (gate.axis == Axis.Z) {
            for (int x = gate.x - 10; x <= gate.x + 10; x++) {
                boolean passage = Math.abs(x - gate.x) <= 4;
                for (int dy = 1; dy <= 6; dy++) {
                    if (passage) set(level, x, gate.y + dy, gate.z, open ? Blocks.AIR : Blocks.IRON_BARS);
                    else if (Math.floorMod(x + dy, 2) == 0) set(level, x, gate.y + dy, gate.z, frame);
                }
            }
            for (int x = gate.x - 11; x <= gate.x + 11; x++) set(level, x, gate.y + 7, gate.z, frame);
            set(level, gate.x - 9, gate.y + 8, gate.z, accent);
            set(level, gate.x + 9, gate.y + 8, gate.z, accent);
        } else {
            for (int z = gate.z - 10; z <= gate.z + 10; z++) {
                boolean passage = Math.abs(z - gate.z) <= 4;
                for (int dy = 1; dy <= 6; dy++) {
                    if (passage) set(level, gate.x, gate.y + dy, z, open ? Blocks.AIR : Blocks.IRON_BARS);
                    else if (Math.floorMod(z + dy, 2) == 0) set(level, gate.x, gate.y + dy, z, frame);
                }
            }
            for (int z = gate.z - 11; z <= gate.z + 11; z++) set(level, gate.x, gate.y + 7, z, frame);
            set(level, gate.x, gate.y + 8, gate.z - 9, accent);
            set(level, gate.x, gate.y + 8, gate.z + 9, accent);
        }
    }

    private static void buildTransitRoad(ServerLevel level, List<Node> nodes, int halfWidth, Block road, Block edge) {
        for (int i = 0; i < nodes.size() - 1; i++) buildRoadSegment(level, nodes.get(i), nodes.get(i + 1), halfWidth, road, edge);
    }

    private static void buildRoadSegment(ServerLevel level, Node a, Node b, int halfWidth, Block road, Block edge) {
        int steps = Math.max(1, Math.max(Math.abs(b.x - a.x), Math.abs(b.z - a.z)));
        for (int step = 0; step <= steps; step++) {
            double t = step / (double) steps;
            int cx = (int)Math.round(lerp(a.x, b.x, t));
            int cz = (int)Math.round(lerp(a.z, b.z, t));
            int groundY = (int)Math.round(lerp(a.y, b.y, t));
            double dx = b.x - a.x, dz = b.z - a.z;
            double len = Math.max(0.001, Math.sqrt(dx * dx + dz * dz));
            double rx = -dz / len, rz = dx / len;
            for (int offset = -halfWidth; offset <= halfWidth; offset++) {
                int x = (int)Math.round(cx + rx * offset);
                int z = (int)Math.round(cz + rz * offset);
                prepareGround(level, x, groundY, z, Math.abs(offset) <= Math.max(1, halfWidth - 2) ? road : edge);
            }
        }
    }

    private static void prepareGround(ServerLevel level, int x, int groundY, int z, Block top) {
        for (int y = groundY - 3; y < groundY; y++) set(level, x, y, z, Blocks.DIRT);
        set(level, x, groundY, z, top);
        for (int y = groundY + 1; y <= groundY + 8; y++) set(level, x, y, z, Blocks.AIR);
    }

    private static void crag(ServerLevel level, int cx, int baseY, int cz, int radius, int height, Block body, Block cap) {
        for (int dy = 0; dy <= height; dy++) {
            int r = Math.max(1, radius - dy / Math.max(4, height / 3));
            for (int dx = -r; dx <= r; dx++) for (int dz = -r; dz <= r; dz++) {
                if (dx * dx + dz * dz > r * r + 1) continue;
                set(level, cx + dx, baseY + dy, cz + dz, dy >= height - 2 ? cap : body);
            }
        }
    }

    private static void buildBrokenHighAqueduct(ServerLevel level) {
        for (int z = -140; z <= 150; z += 28) {
            if (z > -20 && z < 28) continue;
            int top = 86 + Math.floorMod(z, 3);
            for (int y = 58; y <= top; y++) {
                set(level, -474, y, z, y % 5 == 0 ? Blocks.MOSSY_STONE_BRICKS : Blocks.STONE_BRICKS);
                set(level, -470, y, z, y % 5 == 0 ? Blocks.CRACKED_STONE_BRICKS : Blocks.STONE_BRICKS);
            }
            for (int dz = -9; dz <= 9; dz++) {
                if (Math.floorMod(z + dz, 37) == 0) continue;
                set(level, -472, top, z + dz, Blocks.STONE_BRICKS);
            }
        }
    }

    private static void stoneBeacon(ServerLevel level, int x, int groundY, int z, Block body, Block core) {
        for (int dy = 1; dy <= 7; dy++) {
            int r = dy <= 2 ? 2 : 1;
            for (int dx = -r; dx <= r; dx++) for (int dz = -r; dz <= r; dz++) set(level, x + dx, groundY + dy, z + dz, body);
        }
        set(level, x, groundY + 5, z, core);
        set(level, x, groundY + 8, z, Blocks.LANTERN);
    }

    private static void ruinedWatch(ServerLevel level, int x, int groundY, int z) {
        for (int dx = -5; dx <= 5; dx++) for (int dz = -5; dz <= 5; dz++) {
            if (Math.abs(dx) == 5 || Math.abs(dz) == 5) {
                int h = 2 + Math.floorMod(dx * 5 + dz * 7, 6);
                for (int dy = 1; dy <= h; dy++) set(level, x + dx, groundY + dy, z + dz,
                        dy == h ? Blocks.MOSSY_STONE_BRICKS : Blocks.STONE_BRICKS);
            }
        }
        set(level, x, groundY + 1, z, Blocks.CAMPFIRE);
    }

    private static void rootArch(ServerLevel level, int x, int groundY, int z) {
        for (int dy = 1; dy <= 10; dy++) {
            set(level, x - 6 + dy / 4, groundY + dy, z, Blocks.DARK_OAK_LOG);
            set(level, x + 6 - dy / 4, groundY + dy, z, Blocks.DARK_OAK_LOG);
        }
        for (int dx = -4; dx <= 4; dx++) set(level, x + dx, groundY + 10 + Math.abs(dx) / 3, z, Blocks.DARK_OAK_LOG);
        for (int dx = -7; dx <= 7; dx += 2) {
            set(level, x + dx, groundY + 12, z, Blocks.DARK_OAK_LEAVES);
            set(level, x + dx, groundY + 11, z + 1, Blocks.MOSS_BLOCK);
        }
    }

    private static void sporeGrove(ServerLevel level, int x, int groundY, int z) {
        for (int i = 0; i < 9; i++) {
            int dx = -10 + Math.floorMod(i * 7, 21);
            int dz = -9 + Math.floorMod(i * 11, 19);
            int h = 2 + i % 4;
            for (int dy = 1; dy <= h; dy++) set(level, x + dx, groundY + dy, z + dz, Blocks.MOSS_BLOCK);
            set(level, x + dx, groundY + h + 1, z + dz, i % 2 == 0 ? Blocks.SHROOMLIGHT : Blocks.GLOWSTONE);
        }
    }

    private static void sluiceTower(ServerLevel level, int x, int groundY, int z) {
        for (int dy = 1; dy <= 15; dy++) {
            int r = dy < 4 ? 4 : 3;
            for (int dx = -r; dx <= r; dx++) for (int dz = -r; dz <= r; dz++) {
                boolean edge = Math.abs(dx) == r || Math.abs(dz) == r;
                if (edge) set(level, x + dx, groundY + dy, z + dz,
                        (dx + dz + dy) % 7 == 0 ? Blocks.CRACKED_STONE_BRICKS : Blocks.STONE_BRICKS);
            }
        }
        set(level, x, groundY + 16, z, Blocks.REDSTONE_LAMP);
    }

    private static void brokenArch(ServerLevel level, int x, int groundY, int z) {
        for (int dx = -10; dx <= 10; dx++) {
            int h = 4 + (int)Math.round(5.0 * (1.0 - Math.abs(dx) / 10.0));
            if (dx >= -2 && dx <= 2) continue;
            for (int dy = 1; dy <= h; dy++) set(level, x + dx, groundY + dy, z,
                    dy == h ? Blocks.CHISELED_STONE_BRICKS : Blocks.MOSSY_STONE_BRICKS);
        }
    }

    private static void quarryCrane(ServerLevel level, int x, int groundY, int z) {
        for (int dy = 1; dy <= 14; dy++) {
            set(level, x, groundY + dy, z, Blocks.IRON_BLOCK);
            if (dy % 3 == 0) set(level, x + 1, groundY + dy, z, Blocks.IRON_BARS);
        }
        for (int dx = 0; dx <= 13; dx++) set(level, x + dx, groundY + 14, z, dx % 3 == 0 ? Blocks.IRON_BLOCK : Blocks.IRON_BARS);
        for (int dy = 7; dy <= 13; dy++) set(level, x + 11, groundY + dy, z, Blocks.IRON_BARS);
        set(level, x + 11, groundY + 6, z, Blocks.MAGMA_BLOCK);
    }

    private static void coolingBasin(ServerLevel level, int x, int groundY, int z) {
        for (int dx = -9; dx <= 9; dx++) for (int dz = -7; dz <= 7; dz++) {
            double n = dx * dx / 81.0 + dz * dz / 49.0;
            if (n > 1.0) continue;
            set(level, x + dx, groundY - 1, z + dz, Blocks.BLACKSTONE);
            set(level, x + dx, groundY, z + dz, n > .72 ? Blocks.POLISHED_BLACKSTONE : Blocks.WATER);
        }
    }

    private static void relayPylon(ServerLevel level, int x, int groundY, int z, int height) {
        for (int dy = 1; dy <= height; dy++) set(level, x, groundY + dy, z,
                dy % 3 == 0 ? Blocks.IRON_BLOCK : Blocks.DEEPSLATE_BRICKS);
        set(level, x, groundY + height + 1, z, Blocks.AMETHYST_BLOCK);
        set(level, x, groundY + height + 2, z, Blocks.SOUL_LANTERN);
    }

    private static double distanceSqToSegment(double px, double pz, Node a, Node b) {
        double vx = b.x - a.x, vz = b.z - a.z;
        double wx = px - a.x, wz = pz - a.z;
        double vv = vx * vx + vz * vz;
        double t = vv <= 0.000001 ? 0.0 : Math.max(0.0, Math.min(1.0, (wx * vx + wz * vz) / vv));
        double dx = px - (a.x + vx * t), dz = pz - (a.z + vz * t);
        return dx * dx + dz * dz;
    }

    private static double lerp(double a, double b, double t) { return a + (b - a) * t; }

    private static boolean hasMarker(ServerLevel level) {
        return level.getBlockState(new BlockPos(MARKER_X, MARKER_Y, MARKER_Z)).is(Blocks.LODESTONE)
                && level.getBlockState(new BlockPos(MARKER_X + 1, MARKER_Y, MARKER_Z)).is(Blocks.AMETHYST_BLOCK)
                && level.getBlockState(new BlockPos(MARKER_X + 2, MARKER_Y, MARKER_Z)).is(Blocks.CRYING_OBSIDIAN)
                && level.getBlockState(new BlockPos(MARKER_X + 3, MARKER_Y, MARKER_Z)).is(Blocks.BLACKSTONE);
    }

    private static void writeMarker(ServerLevel level) {
        set(level, MARKER_X, MARKER_Y, MARKER_Z, Blocks.LODESTONE);
        set(level, MARKER_X + 1, MARKER_Y, MARKER_Z, Blocks.AMETHYST_BLOCK);
        set(level, MARKER_X + 2, MARKER_Y, MARKER_Z, Blocks.CRYING_OBSIDIAN);
        set(level, MARKER_X + 3, MARKER_Y, MARKER_Z, Blocks.BLACKSTONE);
    }

    private static void set(ServerLevel level, int x, int y, int z, Block block) {
        level.setBlock(new BlockPos(x, y, z), block.defaultBlockState(), 2);
    }
}
