package io.github.q93503128.turnbound.world;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Authored v0.4 Radia hub rebuilt as a coastal cliff town.
 *
 * Terrain/topology is generated first from AsterMarchTerrainPlan. Facilities are then placed on
 * authored terraces and connected by graded, non-axial routes. This deliberately replaces the
 * legacy Y=65 road grid / circular pads / rectangular plaza composition.
 */
public final class RadiaHubWorld {
    private static final int MARKER_Y = 52;
    private static final int CLEAR_TOP = 88;

    private record Node(int x, int y, int z) {}
    private record LegacyBox(int minX, int maxX, int minZ, int maxZ) {}

    public record Facility(String id, String label, Vec3 position) {}
    public record BuiltHub(Vec3 spawn, Vec3 director, Vec3 partyConsole, Vec3 relay, Vec3 southGate,
                           List<Vec3> tutorialPedestals, List<Vec3> tutorialBattleAnchors, List<Facility> facilities) {
        public BuiltHub {
            tutorialPedestals = List.copyOf(tutorialPedestals);
            tutorialBattleAnchors = List.copyOf(tutorialBattleAnchors);
            facilities = List.copyOf(facilities);
        }
    }

    /** X/Z values stay canonical so map/quest POIs remain stable; Y is authored per terrace. */
    private static final List<Facility> FACILITIES = List.of(
            facility("RELAY_HALL", "Relay Hall", 0, 76, -8),
            facility("ECHO_ARCHIVE", "Echo Archive", -56, 73, 22),
            facility("FORGE_ANNEX", "Forge Annex", 56, 73, 22),
            facility("MARKET_ROW", "Market Row", -57, 70, 55),
            facility("TRAINING_YARD", "Training Yard", 57, 71, 38),
            facility("RIFT_GATE", "Rift Gate", -82, 67, -54),
            facility("SOUTH_GATE", "South Gate", 0, 68, 104),
            facility("MEMORIAL_STEPS", "Memorial Steps", -28, 71, -47),
            facility("CLOCK_TOWER", "Clock Tower", 22, 73, -49),
            facility("BARRACKS", "Barracks", 72, 71, -11));

    private static final List<LegacyBox> LEGACY_STRUCTURES = List.of(
            new LegacyBox(-16, 16, -24, 8),
            new LegacyBox(-74, -40, -8, 28),
            new LegacyBox(40, 74, -8, 28),
            new LegacyBox(-80, -34, 32, 80),
            new LegacyBox(34, 82, 34, 82),
            new LegacyBox(-100, -64, -78, -38),
            new LegacyBox(-44, -12, -78, -40),
            new LegacyBox(8, 38, -78, -42),
            new LegacyBox(52, 92, -44, 4),
            new LegacyBox(-18, 18, 96, 118));

    private RadiaHubWorld() {}

    public static BuiltHub build(ServerLevel level) {
        if (!hasMarker(level)) {
            clearLegacyStructures(level);
            terrain(level);
            authoredTerraces(level);
            routes(level);
            harbor(level);
            relayHall(level);
            archive(level);
            forge(level);
            market(level);
            training(level);
            rift(level);
            memorial(level);
            clock(level);
            barracks(level);
            southGate(level, false);
            vegetation(level);
            offshoreRocks(level);
            writeMarker(level);
        }
        return built();
    }

    public static Vec3 spawnPoint() {
        return built().spawn();
    }

    public static boolean contains(Vec3 p) {
        return p != null && AsterMarchRegionCatalog.RADIA.contains(p.x, p.z) && p.y >= 56 && p.y <= 98;
    }

    public static void setSouthGateOpen(ServerLevel level, boolean open) {
        southGate(level, open);
    }

    private static BuiltHub built() {
        return new BuiltHub(
                new Vec3(0.5, 76, 12.5),
                new Vec3(0.5, 76, 4.5),
                new Vec3(7.5, 76, 2.5),
                new Vec3(0.5, 74, 24.5),
                new Vec3(0.5, 68, 104.5),
                List.of(new Vec3(50.5, 71, 49.5), new Vec3(50.5, 71, 59.5), new Vec3(50.5, 71, 69.5)),
                List.of(new Vec3(62.5, 71, 48.5), new Vec3(62.5, 71, 59.5), new Vec3(62.5, 71, 70.5)),
                FACILITIES);
    }

    private static Facility facility(String id, String label, double x, double y, double z) {
        return new Facility(id, label, new Vec3(x, y, z));
    }

    /** Migration cleanup only: remove the known legacy boxes before laying the new topology. */
    private static void clearLegacyStructures(ServerLevel level) {
        for (LegacyBox box : LEGACY_STRUCTURES) {
            for (int x = box.minX; x <= box.maxX; x++) {
                for (int z = box.minZ; z <= box.maxZ; z++) {
                    for (int y = AsterMarchTerrainPlan.WORLD_BASE_Y + 1; y <= CLEAR_TOP; y++) {
                        BlockPos pos = new BlockPos(x, y, z);
                        if (!level.getBlockState(pos).isAir()) set(level, x, y, z, Blocks.AIR);
                    }
                }
            }
        }
    }

    /** Re-author the entire Radia footprint so old flat grass/roads cannot survive below the new town. */
    private static void terrain(ServerLevel level) {
        for (int x = AsterMarchRegionCatalog.RADIA.minX(); x <= AsterMarchRegionCatalog.RADIA.maxX(); x++) {
            for (int z = AsterMarchRegionCatalog.RADIA.minZ(); z <= AsterMarchRegionCatalog.RADIA.maxZ(); z++) {
                if (AsterMarchTerrainPlan.radiaLand(x, z)) {
                    landColumn(level, x, z, AsterMarchTerrainPlan.radiaSurfaceY(x, z));
                } else {
                    waterColumn(level, x, z);
                }
            }
        }
        cliffFaces(level);
    }

    private static void landColumn(ServerLevel level, int x, int z, int top) {
        int bottom = AsterMarchTerrainPlan.RADIA_FLOOR_Y;
        for (int y = bottom; y <= top - 4; y++) {
            int texture = Math.floorMod(x * 17 + z * 31 + y * 7, 13);
            Block rock = texture == 0 ? Blocks.TUFF : texture <= 2 ? Blocks.ANDESITE : Blocks.STONE;
            set(level, x, y, z, rock);
        }
        for (int y = Math.max(bottom, top - 3); y < top; y++) set(level, x, y, z, Blocks.DIRT);
        set(level, x, top, z, top <= 65 && Math.floorMod(x + z, 7) == 0 ? Blocks.MOSS_BLOCK : Blocks.GRASS_BLOCK);
        for (int y = top + 1; y <= Math.min(CLEAR_TOP, top + 6); y++) {
            BlockPos pos = new BlockPos(x, y, z);
            if (!level.getBlockState(pos).isAir()) set(level, x, y, z, Blocks.AIR);
        }
    }

    private static void waterColumn(ServerLevel level, int x, int z) {
        int floor = AsterMarchTerrainPlan.RADIA_FLOOR_Y;
        set(level, x, floor - 1, z, Blocks.STONE);
        set(level, x, floor, z, Math.floorMod(x * 3 + z * 5, 4) == 0 ? Blocks.GRAVEL : Blocks.SAND);
        for (int y = floor + 1; y <= CLEAR_TOP; y++) {
            BlockPos pos = new BlockPos(x, y, z);
            if (!level.getBlockState(pos).isAir()) set(level, x, y, z, Blocks.AIR);
        }
        for (int y = floor + 1; y <= AsterMarchTerrainPlan.RADIA_SEA_Y; y++) set(level, x, y, z, Blocks.WATER);
    }

    private static void cliffFaces(ServerLevel level) {
        int[][] dirs = {{1,0},{-1,0},{0,1},{0,-1}};
        for (int x = AsterMarchRegionCatalog.RADIA.minX() + 1; x < AsterMarchRegionCatalog.RADIA.maxX(); x++) {
            for (int z = AsterMarchRegionCatalog.RADIA.minZ() + 1; z < AsterMarchRegionCatalog.RADIA.maxZ(); z++) {
                if (!AsterMarchTerrainPlan.radiaLand(x, z)) continue;
                boolean edge = false;
                for (int[] d : dirs) if (!AsterMarchTerrainPlan.radiaLand(x + d[0], z + d[1])) { edge = true; break; }
                if (!edge) continue;
                int top = AsterMarchTerrainPlan.radiaSurfaceY(x, z);
                for (int y = AsterMarchTerrainPlan.RADIA_FLOOR_Y; y < top; y++) {
                    int texture = Math.floorMod(x * 11 + z * 23 + y, 9);
                    set(level, x, y, z, texture == 0 ? Blocks.MOSSY_COBBLESTONE : texture <= 2 ? Blocks.ANDESITE : Blocks.STONE);
                }
            }
        }
    }

    private static void authoredTerraces(ServerLevel level) {
        terrace(level, 0, 12, 17, 13, 75, Blocks.POLISHED_ANDESITE, Blocks.STONE_BRICKS);
        terrace(level, 0, -8, 17, 14, 75, Blocks.STONE_BRICKS, Blocks.POLISHED_ANDESITE);
        terrace(level, -56, 22, 15, 12, 72, Blocks.STONE_BRICKS, Blocks.MOSSY_STONE_BRICKS);
        terrace(level, 56, 22, 15, 12, 72, Blocks.POLISHED_ANDESITE, Blocks.STONE_BRICKS);
        terrace(level, -57, 55, 20, 17, 69, Blocks.SMOOTH_STONE, Blocks.STONE_BRICKS);
        terrace(level, 57, 59, 22, 21, 70, Blocks.COARSE_DIRT, Blocks.SMOOTH_STONE);
        terrace(level, -82, -54, 13, 11, 66, Blocks.DEEPSLATE_TILES, Blocks.OBSIDIAN);
        terrace(level, -28, -47, 15, 11, 70, Blocks.POLISHED_ANDESITE, Blocks.STONE_BRICKS);
        terrace(level, 22, -49, 13, 12, 72, Blocks.STONE_BRICKS, Blocks.POLISHED_ANDESITE);
        terrace(level, 72, -11, 16, 14, 70, Blocks.STONE_BRICKS, Blocks.SPRUCE_PLANKS);
        terrace(level, 0, 104, 25, 10, 67, Blocks.POLISHED_ANDESITE, Blocks.STONE_BRICKS);
        terrace(level, 0, 24, 6, 5, 73, Blocks.POLISHED_ANDESITE, Blocks.STONE_BRICKS);
    }

    private static void routes(ServerLevel level) {
        route(level, 3, new Node(0,75,12), new Node(-5,75,4), new Node(0,75,-8));
        route(level, 2, new Node(0,75,12), new Node(5,74,19), new Node(0,73,24));
        route(level, 3, new Node(-3,75,15), new Node(-17,74,18), new Node(-34,73,18), new Node(-56,72,22));
        route(level, 3, new Node(-42,73,21), new Node(-47,72,34), new Node(-53,70,43), new Node(-57,69,55));
        route(level, 3, new Node(4,75,15), new Node(18,74,19), new Node(37,73,18), new Node(56,72,22));
        route(level, 3, new Node(38,73,21), new Node(44,72,30), new Node(50,71,38), new Node(57,70,48));
        route(level, 3, new Node(0,73,24), new Node(8,72,39), new Node(-5,71,55),
                new Node(5,69,73), new Node(-4,68,89), new Node(0,67,104));
        route(level, 2, new Node(-12,75,-12), new Node(-20,73,-27), new Node(-28,70,-47));
        route(level, 2, new Node(12,75,-13), new Node(18,74,-29), new Node(22,72,-49));
        route(level, 2, new Node(-28,70,-47), new Node(-47,68,-48), new Node(-65,67,-52), new Node(-82,66,-54));
        route(level, 2, new Node(22,72,-49), new Node(42,71,-35), new Node(60,70,-23), new Node(72,70,-11));
    }

    private static void harbor(ServerLevel level) {
        int z = -62;
        for (int x = -29; x <= 29; x++) {
            int y = 68 + (Math.abs(x) > 20 ? 1 : 0);
            support(level, x, z, y - 1, Blocks.DARK_OAK_LOG);
            set(level, x, y, z, Blocks.DARK_OAK_PLANKS);
            if ((x + 29) % 6 == 0) {
                set(level, x, y + 1, z - 1, Blocks.OAK_FENCE);
                set(level, x, y + 1, z + 1, Blocks.OAK_FENCE);
                set(level, x, y + 2, z - 1, Blocks.LANTERN);
            }
        }
        pier(level, -31, -70, -8);
        pier(level, 31, -70, 8);
        for (int x : new int[]{-42, -34, 34, 42}) {
            int y = 65;
            set(level, x, y, -68, Blocks.OAK_FENCE);
            set(level, x, y + 1, -68, Blocks.LANTERN);
        }
    }

    private static void pier(ServerLevel level, int startX, int z, int directionX) {
        int step = directionX < 0 ? -1 : 1;
        for (int i = 0; i <= Math.abs(directionX); i++) {
            int x = startX + step * i;
            int y = 64;
            support(level, x, z, y - 1, Blocks.SPRUCE_LOG);
            set(level, x, y, z, Blocks.SPRUCE_PLANKS);
            set(level, x, y, z + 1, Blocks.SPRUCE_PLANKS);
        }
    }

    private static void relayHall(ServerLevel level) {
        house(level, -10, -18, 21, 18, 75, Blocks.STONE_BRICKS, Blocks.POLISHED_ANDESITE, Blocks.DEEPSLATE_TILE_SLAB);
        for (int x = -4; x <= 4; x++) for (int y = 76; y <= 79; y++) set(level, x, y, 0, Blocks.AIR);
        set(level, 0, 76, 24, Blocks.AMETHYST_BLOCK);
        set(level, 0, 77, 24, Blocks.BEACON);
    }

    private static void archive(ServerLevel level) {
        house(level, -68, 10, 25, 19, 72, Blocks.DARK_OAK_PLANKS, Blocks.AMETHYST_BLOCK, Blocks.DARK_OAK_SLAB);
        for (int z = 14; z <= 26; z += 4) set(level, -66, 73, z, Blocks.BOOKSHELF);
    }

    private static void forge(ServerLevel level) {
        house(level, 44, 10, 25, 19, 72, Blocks.STONE_BRICKS, Blocks.IRON_BLOCK, Blocks.DEEPSLATE_TILE_SLAB);
        for (int z = 15; z <= 25; z += 5) {
            set(level, 47, 73, z, Blocks.BLAST_FURNACE);
            set(level, 65, 73, z, Blocks.ANVIL);
        }
    }

    private static void market(ServerLevel level) {
        for (int[] stall : new int[][]{{-70,48},{-60,47},{-49,49},{-66,62},{-54,63},{-43,60}}) {
            stall(level, stall[0], stall[1], 69, ((stall[0] + stall[1]) & 1) == 0 ? Blocks.SPRUCE_PLANKS : Blocks.OAK_PLANKS);
        }
        for (int x = -61; x <= -53; x++) for (int z = 52; z <= 60; z++) {
            if ((x + 57) * (x + 57) + (z - 56) * (z - 56) <= 16) set(level, x, 69, z, Blocks.COBBLESTONE);
        }
        set(level, -57, 70, 56, Blocks.WATER);
    }

    private static void training(ServerLevel level) {
        for (int z : new int[]{48,59,70}) for (int dx = -7; dx <= 7; dx++) set(level, 62 + dx, 70, z, Blocks.SMOOTH_STONE);
        for (int z = 42; z <= 76; z += 4) {
            set(level, 38, 71, z, Blocks.OAK_FENCE);
            set(level, 78, 71, z, Blocks.OAK_FENCE);
        }
        for (int x = 38; x <= 78; x += 4) {
            if (x < 52 || x > 64) set(level, x, 71, 40, Blocks.OAK_FENCE);
            set(level, x, 71, 78, Blocks.OAK_FENCE);
        }
        for (int x = 52; x <= 64; x++) set(level, x, 71, 40, Blocks.AIR);
    }

    private static void rift(ServerLevel level) {
        for (int x = -88; x <= -76; x++) {
            set(level, x, 67, -66, Blocks.OBSIDIAN);
            set(level, x, 72, -66, Blocks.OBSIDIAN);
        }
        for (int y = 67; y <= 72; y++) {
            set(level, -88, y, -66, Blocks.OBSIDIAN);
            set(level, -76, y, -66, Blocks.OBSIDIAN);
        }
        set(level, -82, 67, -65, Blocks.CRYING_OBSIDIAN);
        for (int x = -94; x <= -70; x += 6) rock(level, x, -61 + Math.floorMod(x, 3), 64 + Math.floorMod(x, 2), 2);
    }

    private static void memorial(ServerLevel level) {
        for (int step = 0; step < 5; step++) {
            int z = -43 - step * 3;
            for (int x = -37 + step; x <= -19 - step; x++) set(level, x, 70 + step, z, Blocks.POLISHED_ANDESITE);
        }
        for (int x = -35; x <= -21; x += 7) {
            set(level, x, 75, -57, Blocks.CHISELED_STONE_BRICKS);
            set(level, x, 76, -57, Blocks.SOUL_LANTERN);
        }
    }

    private static void clock(ServerLevel level) {
        house(level, 15, -56, 15, 15, 72, Blocks.STONE_BRICKS, Blocks.QUARTZ_BLOCK, Blocks.DEEPSLATE_TILE_SLAB);
        for (int y = 77; y <= 90; y++) set(level, 22, y, -49, y % 4 == 0 ? Blocks.QUARTZ_BLOCK : Blocks.STONE_BRICKS);
        set(level, 22, 91, -49, Blocks.GLOWSTONE);
        set(level, 21, 84, -48, Blocks.GLOWSTONE);
    }

    private static void barracks(ServerLevel level) {
        house(level, 61, -21, 23, 21, 70, Blocks.STONE_BRICKS, Blocks.SPRUCE_PLANKS, Blocks.DARK_OAK_SLAB);
        for (int z = -17; z <= -5; z += 4) {
            set(level, 65, 71, z, Blocks.IRON_BARS);
            set(level, 79, 71, z, Blocks.TARGET);
        }
    }

    /** South Gate is now a real boundary embedded into a curved fortified ridge. */
    private static void southGate(ServerLevel level, boolean open) {
        int gateZ = 108;
        for (int x = -54; x <= 54; x++) {
            if (Math.abs(x) <= 6) continue;
            int z = gateZ + (Math.abs(x) / 16);
            int ground = 67 + (Math.abs(x) > 38 ? 1 : 0);
            for (int y = ground + 1; y <= ground + 4; y++) {
                Block wall = Math.floorMod(x + y, 7) == 0 ? Blocks.CHISELED_STONE_BRICKS : Blocks.STONE_BRICKS;
                set(level, x, y, z, wall);
            }
            if (Math.floorMod(x, 7) == 0) set(level, x, ground + 5, z, Blocks.STONE_BRICK_WALL);
        }
        for (int x = -11; x <= 11; x++) {
            for (int y = 68; y <= 76; y++) set(level, x, y, gateZ, Blocks.AIR);
            set(level, x, 67, gateZ, Blocks.POLISHED_ANDESITE);
        }
        for (int x = -10; x <= 10; x++) {
            boolean passage = Math.abs(x) <= 4;
            if (passage) {
                for (int y = 68; y <= 72; y++) set(level, x, y, gateZ, open ? Blocks.AIR : Blocks.IRON_BARS);
            } else {
                for (int y = 68; y <= 73; y++) set(level, x, y, gateZ,
                        Math.floorMod(x + y, 5) == 0 ? Blocks.CHISELED_STONE_BRICKS : Blocks.STONE_BRICKS);
            }
        }
        for (int x = -6; x <= 6; x++) set(level, x, 74, gateZ, Blocks.STONE_BRICK_SLAB);
        gateTower(level, -10, gateZ);
        gateTower(level, 10, gateZ);
        for (int z = 109; z <= 118; z++) for (int x = -4; x <= 4; x++) {
            grade(level, x, z, 67, Math.abs(x) <= 1 ? Blocks.POLISHED_ANDESITE : Blocks.STONE_BRICKS);
        }
    }

    private static void gateTower(ServerLevel level, int cx, int cz) {
        for (int x = cx - 2; x <= cx + 2; x++) for (int z = cz - 2; z <= cz + 2; z++) {
            for (int y = 68; y <= 75; y++) {
                boolean edge = x == cx - 2 || x == cx + 2 || z == cz - 2 || z == cz + 2;
                if (edge) set(level, x, y, z, Blocks.STONE_BRICKS);
            }
            set(level, x, 76, z, Blocks.DEEPSLATE_TILE_SLAB);
        }
        set(level, cx, 77, cz, Blocks.LANTERN);
    }

    private static void vegetation(ServerLevel level) {
        int[][] trees = {{-89,18},{-92,42},{-74,78},{-53,91},{83,18},{92,47},{76,82},{55,92},
                {-66,-24},{63,-27},{-100,-15},{98,-6},{-39,74},{37,83}};
        for (int[] p : trees) {
            if (!AsterMarchTerrainPlan.radiaLand(p[0], p[1])) continue;
            int y = AsterMarchTerrainPlan.radiaSurfaceY(p[0], p[1]);
            tree(level, p[0], y + 1, p[1], ((p[0] + p[1]) & 1) == 0);
        }
        for (int x = -105; x <= 105; x += 9) for (int z = -82; z <= 96; z += 11) {
            if (!AsterMarchTerrainPlan.radiaLand(x, z) || nearRouteOrFacility(x, z)) continue;
            if (Math.floorMod(x * 13 + z * 7, 5) != 0) continue;
            int y = AsterMarchTerrainPlan.radiaSurfaceY(x, z);
            set(level, x, y + 1, z, Math.floorMod(x + z, 2) == 0 ? Blocks.FERN : Blocks.SHORT_GRASS);
        }
    }

    private static boolean nearRouteOrFacility(int x, int z) {
        if (Math.abs(x) < 14 && z > -20 && z < 112) return true;
        for (Facility facility : FACILITIES) {
            double dx = facility.position().x - x;
            double dz = facility.position().z - z;
            if (dx * dx + dz * dz < 18 * 18) return true;
        }
        return false;
    }

    private static void offshoreRocks(ServerLevel level) {
        rockIslet(level, -111, -78, 4, 65);
        rockIslet(level, 108, -71, 3, 64);
        rockIslet(level, -116, 35, 3, 64);
        rockIslet(level, 112, 64, 4, 65);
    }

    private static void rockIslet(ServerLevel level, int cx, int cz, int r, int top) {
        for (int x = cx - r; x <= cx + r; x++) for (int z = cz - r; z <= cz + r; z++) {
            int d = (x - cx) * (x - cx) + (z - cz) * (z - cz);
            if (d > r * r) continue;
            int y = top - Math.max(0, d / Math.max(1, r * r / 2));
            support(level, x, z, y, Blocks.STONE);
            set(level, x, y, z, d % 3 == 0 ? Blocks.ANDESITE : Blocks.STONE);
        }
    }

    private static void route(ServerLevel level, int halfWidth, Node... nodes) {
        for (int i = 0; i < nodes.length - 1; i++) road(level, nodes[i], nodes[i + 1], halfWidth);
    }

    private static void road(ServerLevel level, Node a, Node b, int halfWidth) {
        int steps = Math.max(1, Math.max(Math.abs(b.x - a.x), Math.abs(b.z - a.z)));
        for (int i = 0; i <= steps; i++) {
            double t = i / (double)steps;
            int cx = (int)Math.round(a.x + (b.x - a.x) * t);
            int cz = (int)Math.round(a.z + (b.z - a.z) * t);
            int y = (int)Math.round(a.y + (b.y - a.y) * t);
            for (int dx = -halfWidth; dx <= halfWidth; dx++) for (int dz = -halfWidth; dz <= halfWidth; dz++) {
                if (dx * dx + dz * dz > halfWidth * halfWidth + 1) continue;
                Block surface = Math.abs(dx) + Math.abs(dz) <= 1 ? Blocks.POLISHED_ANDESITE : Blocks.STONE_BRICKS;
                grade(level, cx + dx, cz + dz, y, surface);
            }
        }
    }

    private static void terrace(ServerLevel level, int cx, int cz, int rx, int rz, int y, Block inner, Block rim) {
        for (int x = cx - rx; x <= cx + rx; x++) for (int z = cz - rz; z <= cz + rz; z++) {
            double d = ((x - cx) * (x - cx)) / (double)(rx * rx)
                    + ((z - cz) * (z - cz)) / (double)(rz * rz);
            double edgeNoise = 0.04 * Math.sin(x * 0.77 + z * 0.31);
            if (d > 1.0 + edgeNoise) continue;
            grade(level, x, z, y, d > 0.78 ? rim : inner);
            if (d > 0.83) for (int fy = Math.max(AsterMarchTerrainPlan.RADIA_SEA_Y, y - 6); fy < y; fy++) {
                set(level, x, fy, z, Math.floorMod(x + z + fy, 5) == 0 ? Blocks.ANDESITE : Blocks.STONE_BRICKS);
            }
        }
    }

    private static void grade(ServerLevel level, int x, int z, int y, Block surface) {
        support(level, x, z, y - 1, Blocks.STONE);
        set(level, x, y, z, surface);
        for (int clearY = y + 1; clearY <= y + 4; clearY++) set(level, x, clearY, z, Blocks.AIR);
    }

    private static void support(ServerLevel level, int x, int z, int topY, Block block) {
        int bottom = Math.max(AsterMarchTerrainPlan.RADIA_FLOOR_Y, topY - 7);
        for (int y = bottom; y <= topY; y++) {
            BlockPos pos = new BlockPos(x, y, z);
            if (level.getBlockState(pos).isAir() || level.getBlockState(pos).is(Blocks.WATER)) set(level, x, y, z, block);
        }
    }

    private static void house(ServerLevel level, int x0, int z0, int w, int d, int ground,
                              Block wall, Block accent, Block roof) {
        for (int x = x0; x < x0 + w; x++) for (int z = z0; z < z0 + d; z++) {
            grade(level, x, z, ground, Blocks.SMOOTH_STONE);
            boolean edge = x == x0 || x == x0 + w - 1 || z == z0 || z == z0 + d - 1;
            if (edge) {
                for (int y = ground + 1; y <= ground + 5; y++) set(level, x, y, z,
                        Math.floorMod(x + z + y, 9) == 0 ? accent : wall);
            } else {
                for (int y = ground + 1; y <= ground + 5; y++) set(level, x, y, z, Blocks.AIR);
            }
            set(level, x, ground + 6, z, roof);
        }
        int doorX = x0 + w / 2;
        for (int y = ground + 1; y <= ground + 3; y++) set(level, doorX, y, z0 + d - 1, Blocks.AIR);
        for (int x = x0 + 4; x < x0 + w - 3; x += 6) for (int z = z0 + 4; z < z0 + d - 3; z += 6) {
            set(level, x, ground + 6, z, Blocks.SEA_LANTERN);
        }
    }

    private static void stall(ServerLevel level, int x, int z, int ground, Block wood) {
        for (int dx = -3; dx <= 3; dx++) for (int dz = -2; dz <= 2; dz++) set(level, x + dx, ground, z + dz, wood);
        for (int dx : new int[]{-3,3}) for (int dz : new int[]{-2,2}) {
            set(level, x + dx, ground + 1, z + dz, Blocks.OAK_FENCE);
            set(level, x + dx, ground + 2, z + dz, Blocks.OAK_FENCE);
        }
        for (int dx = -4; dx <= 4; dx++) for (int dz = -3; dz <= 3; dz++) set(level, x + dx, ground + 3, z + dz, Blocks.SPRUCE_SLAB);
    }

    private static void tree(ServerLevel level, int x, int y, int z, boolean spruce) {
        Block log = spruce ? Blocks.SPRUCE_LOG : Blocks.OAK_LOG;
        Block leaves = spruce ? Blocks.SPRUCE_LEAVES : Blocks.OAK_LEAVES;
        int h = spruce ? 6 : 5;
        for (int dy = 0; dy < h; dy++) set(level, x, y + dy, z, log);
        for (int dy = h - 3; dy <= h; dy++) {
            int r = spruce ? Math.max(1, h - dy) : (dy == h ? 1 : 2);
            for (int dx = -r; dx <= r; dx++) for (int dz = -r; dz <= r; dz++) {
                if (Math.abs(dx) + Math.abs(dz) > r + 1 || (dx == 0 && dz == 0 && dy < h)) continue;
                set(level, x + dx, y + dy, z + dz, leaves);
            }
        }
    }

    private static void rock(ServerLevel level, int cx, int cz, int y, int r) {
        for (int dx = -r; dx <= r; dx++) for (int dz = -r; dz <= r; dz++) {
            if (dx * dx + dz * dz > r * r) continue;
            int h = 1 + Math.floorMod(cx + cz + dx * 3 + dz * 5, 3);
            for (int dy = 0; dy < h; dy++) set(level, cx + dx, y + dy, cz + dz,
                    dy == h - 1 ? Blocks.ANDESITE : Blocks.STONE);
        }
    }

    private static boolean hasMarker(ServerLevel level) {
        return level.getBlockState(new BlockPos(0, MARKER_Y, 20)).is(Blocks.LODESTONE)
                && level.getBlockState(new BlockPos(1, MARKER_Y, 20)).is(Blocks.AMETHYST_BLOCK)
                && level.getBlockState(new BlockPos(2, MARKER_Y, 20)).is(Blocks.COPPER_BLOCK)
                && level.getBlockState(new BlockPos(3, MARKER_Y, 20)).is(Blocks.EMERALD_BLOCK)
                && level.getBlockState(new BlockPos(4, MARKER_Y, 20)).is(Blocks.GOLD_BLOCK)
                && level.getBlockState(new BlockPos(5, MARKER_Y, 20)).is(Blocks.DIAMOND_BLOCK);
    }

    private static void writeMarker(ServerLevel level) {
        set(level, 0, MARKER_Y, 20, Blocks.LODESTONE);
        set(level, 1, MARKER_Y, 20, Blocks.AMETHYST_BLOCK);
        set(level, 2, MARKER_Y, 20, Blocks.COPPER_BLOCK);
        set(level, 3, MARKER_Y, 20, Blocks.EMERALD_BLOCK);
        set(level, 4, MARKER_Y, 20, Blocks.GOLD_BLOCK);
        set(level, 5, MARKER_Y, 20, Blocks.DIAMOND_BLOCK);
    }

    private static void set(ServerLevel level, int x, int y, int z, Block block) {
        level.setBlock(new BlockPos(x, y, z), block.defaultBlockState(), 2);
    }
}
