package io.github.q93503128.turnbound.world;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/** Authored v0.4 Radia hub containing the ten canonical facilities around FT_RADIA. */
public final class RadiaHubWorld {
    private static final int GROUND_Y = 65;
    private static final int MARKER_Y = 57;

    public record Facility(String id, String label, Vec3 position) {}
    public record BuiltHub(
            Vec3 spawn,
            Vec3 director,
            Vec3 partyConsole,
            Vec3 relay,
            List<Vec3> tutorialPedestals,
            List<Vec3> tutorialBattleAnchors,
            List<Facility> facilities) {
        public BuiltHub {
            tutorialPedestals = List.copyOf(tutorialPedestals);
            tutorialBattleAnchors = List.copyOf(tutorialBattleAnchors);
            facilities = List.copyOf(facilities);
        }
    }

    private static final List<Facility> FACILITIES = List.of(
            facility("RELAY_HALL", "Relay Hall", 0, -8),
            facility("ECHO_ARCHIVE", "Echo Archive", -56, 8),
            facility("FORGE_ANNEX", "Forge Annex", 56, 8),
            facility("MARKET_ROW", "Market Row", -57, 55),
            facility("TRAINING_YARD", "Training Yard", 57, 58),
            facility("RIFT_GATE", "Rift Gate", -82, -58),
            facility("SOUTH_GATE", "South Gate", 0, 112),
            facility("MEMORIAL_STEPS", "Memorial Steps", -28, -60),
            facility("CLOCK_TOWER", "Clock Tower", 22, -62),
            facility("BARRACKS", "Barracks", 72, -26));

    private RadiaHubWorld() {}

    public static BuiltHub build(ServerLevel level) {
        if (!hasMarker(level)) {
            buildGround(level);
            buildRelayHall(level);
            buildEchoArchive(level);
            buildForge(level);
            buildMarket(level);
            buildTrainingYard(level);
            buildRiftGate(level);
            buildMemorial(level);
            buildClockTower(level);
            buildBarracks(level);
            buildSouthGate(level, false);
            buildRelay(level);
            writeMarker(level);
        }
        return built();
    }

    public static boolean contains(Vec3 position) {
        return position != null
                && AsterMarchRegionCatalog.RADIA.contains(position.x, position.z)
                && position.y >= 56 && position.y <= 96;
    }

    public static void setSouthGateOpen(ServerLevel level, boolean open) {
        buildSouthGate(level, open);
    }

    private static BuiltHub built() {
        AsterMarchRegionCatalog.Anchor anchor = AsterMarchRegionCatalog.fastTravel(AsterMarchRegionCatalog.FT_RADIA);
        return new BuiltHub(
                new Vec3(anchor.x(), anchor.y(), anchor.z()),
                new Vec3(0.5, 66.0, -1.5),
                new Vec3(7.5, 66.0, 2.5),
                new Vec3(anchor.x(), anchor.y(), anchor.z() + 4.0),
                List.of(new Vec3(43.0, 66.0, 49.0), new Vec3(43.0, 66.0, 59.0), new Vec3(43.0, 66.0, 69.0)),
                List.of(new Vec3(62.0, 66.0, 48.0), new Vec3(62.0, 66.0, 59.0), new Vec3(62.0, 66.0, 70.0)),
                FACILITIES);
    }

    private static Facility facility(String id, String label, double x, double z) {
        return new Facility(id, label, new Vec3(x, 66.0, z));
    }

    private static void buildGround(ServerLevel level) {
        // Central plaza and the north/south story spine.
        pad(level, 0, 20, 22, Blocks.STONE_BRICKS, Blocks.POLISHED_ANDESITE);
        road(level, 0, -86, 0, 120, 5);
        road(level, -88, 20, 88, 20, 4);
        road(level, -58, 20, -58, 66, 3);
        road(level, 58, 20, 58, 72, 3);
        road(level, -72, -25, 72, -25, 3);

        // Keep facility footprints safe and readable even when the host overworld is not Superflat.
        for (Facility facility : FACILITIES) {
            int radius = switch (facility.id()) {
                case "RELAY_HALL" -> 15;
                case "TRAINING_YARD" -> 18;
                case "MARKET_ROW" -> 16;
                default -> 12;
            };
            levelCircle(level, (int)Math.round(facility.position().x), (int)Math.round(facility.position().z), radius);
        }
    }

    private static void buildRelayHall(ServerLevel level) {
        building(level, -12, -20, 25, 24, Blocks.STONE_BRICKS, Blocks.POLISHED_ANDESITE, Blocks.DEEPSLATE_TILE_SLAB);
        for (int x = -5; x <= 5; x++) set(level, x, GROUND_Y, 5, Blocks.CHISELED_STONE_BRICKS);
        for (int x : new int[]{-8, 8}) lanternPost(level, x, GROUND_Y, 7, false);
    }

    private static void buildEchoArchive(ServerLevel level) {
        building(level, -69, -2, 26, 22, Blocks.DARK_OAK_PLANKS, Blocks.AMETHYST_BLOCK, Blocks.DARK_OAK_SLAB);
        for (int z = 3; z <= 12; z += 3) {
            set(level, -61, GROUND_Y + 1, z, Blocks.BOOKSHELF);
            set(level, -51, GROUND_Y + 1, z, Blocks.BOOKSHELF);
        }
    }

    private static void buildForge(ServerLevel level) {
        building(level, 44, -2, 25, 22, Blocks.STONE_BRICKS, Blocks.IRON_BLOCK, Blocks.STONE_SLAB);
        for (int x = 50; x <= 62; x += 4) {
            set(level, x, GROUND_Y + 1, 11, Blocks.FURNACE);
            set(level, x, GROUND_Y + 1, 14, Blocks.BLAST_FURNACE);
        }
    }

    private static void buildMarket(ServerLevel level) {
        pad(level, -57, 55, 18, Blocks.SMOOTH_STONE, Blocks.STONE_BRICKS);
        for (int x = -72; x <= -42; x += 10) stall(level, x, GROUND_Y, 48, Blocks.SPRUCE_PLANKS);
        for (int x = -67; x <= -47; x += 10) stall(level, x, GROUND_Y, 62, Blocks.OAK_PLANKS);
    }

    private static void buildTrainingYard(ServerLevel level) {
        pad(level, 57, 59, 20, Blocks.COARSE_DIRT, Blocks.SMOOTH_STONE);
        for (int x = 38; x <= 76; x++) {
            if (x % 4 != 0) continue;
            set(level, x, GROUND_Y + 1, 39, Blocks.OAK_FENCE);
            set(level, x, GROUND_Y + 1, 79, Blocks.OAK_FENCE);
        }
        for (int z = 39; z <= 79; z++) {
            if (z % 4 != 0) continue;
            set(level, 37, GROUND_Y + 1, z, Blocks.OAK_FENCE);
            set(level, 77, GROUND_Y + 1, z, Blocks.OAK_FENCE);
        }
        for (int z : new int[]{48, 59, 70}) {
            for (int dx = -6; dx <= 6; dx++) set(level, 62 + dx, GROUND_Y, z, Blocks.SMOOTH_STONE);
        }
    }

    private static void buildRiftGate(ServerLevel level) {
        pad(level, -82, -58, 13, Blocks.DEEPSLATE_TILES, Blocks.OBSIDIAN);
        for (int x = -88; x <= -76; x++) {
            set(level, x, GROUND_Y + 1, -66, Blocks.OBSIDIAN);
            set(level, x, GROUND_Y + 6, -66, Blocks.OBSIDIAN);
        }
        for (int y = GROUND_Y + 1; y <= GROUND_Y + 6; y++) {
            set(level, -88, y, -66, Blocks.OBSIDIAN);
            set(level, -76, y, -66, Blocks.OBSIDIAN);
        }
        set(level, -82, GROUND_Y + 1, -65, Blocks.CRYING_OBSIDIAN);
    }

    private static void buildMemorial(ServerLevel level) {
        for (int z = -71; z <= -51; z++) {
            int rise = Math.max(0, (-51 - z) / 4);
            for (int x = -38; x <= -18; x++) set(level, x, GROUND_Y + rise, z, Blocks.POLISHED_ANDESITE);
        }
        for (int x = -35; x <= -21; x += 7) {
            set(level, x, GROUND_Y + 6, -70, Blocks.CHISELED_STONE_BRICKS);
            set(level, x, GROUND_Y + 7, -70, Blocks.SOUL_LANTERN);
        }
    }

    private static void buildClockTower(ServerLevel level) {
        building(level, 14, -70, 17, 18, Blocks.STONE_BRICKS, Blocks.QUARTZ_BLOCK, Blocks.DEEPSLATE_TILE_SLAB);
        for (int y = GROUND_Y + 5; y <= GROUND_Y + 18; y++) {
            set(level, 22, y, -62, y % 4 == 0 ? Blocks.QUARTZ_BLOCK : Blocks.STONE_BRICKS);
        }
        set(level, 22, GROUND_Y + 19, -62, Blocks.GLOWSTONE);
    }

    private static void buildBarracks(ServerLevel level) {
        building(level, 60, -38, 25, 25, Blocks.STONE_BRICKS, Blocks.SPRUCE_PLANKS, Blocks.SPRUCE_SLAB);
        for (int z = -33; z <= -19; z += 4) {
            set(level, 66, GROUND_Y + 1, z, Blocks.ARMOR_STAND);
            set(level, 78, GROUND_Y + 1, z, Blocks.TARGET);
        }
    }

    private static void buildSouthGate(ServerLevel level, boolean open) {
        int z = 116;
        for (int x = -14; x <= 14; x++) {
            if (Math.abs(x) <= 5) {
                for (int y = GROUND_Y + 1; y <= GROUND_Y + 5; y++) set(level, x, y, z, open ? Blocks.AIR : Blocks.IRON_BARS);
                continue;
            }
            for (int y = GROUND_Y + 1; y <= GROUND_Y + 7; y++) set(level, x, y, z, Blocks.STONE_BRICKS);
        }
        for (int x : new int[]{-12, 12}) lanternPost(level, x, GROUND_Y + 7, z, true);
        for (int x = -16; x <= 16; x++) set(level, x, GROUND_Y, z, Blocks.POLISHED_ANDESITE);
    }

    private static void buildRelay(ServerLevel level) {
        int x = 0, z = 24;
        pad(level, x, z, 5, Blocks.POLISHED_ANDESITE, Blocks.STONE_BRICKS);
        set(level, x, GROUND_Y + 1, z, Blocks.AMETHYST_BLOCK);
        set(level, x, GROUND_Y + 2, z, Blocks.BEACON);
    }

    private static void building(ServerLevel level, int x0, int z0, int width, int depth, Block wall, Block accent, Block roof) {
        for (int x = x0; x < x0 + width; x++) for (int z = z0; z < z0 + depth; z++) {
            set(level, x, GROUND_Y, z, Blocks.SMOOTH_STONE);
            for (int y = GROUND_Y + 1; y <= GROUND_Y + 6; y++) set(level, x, y, z, Blocks.AIR);
            boolean edge = x == x0 || x == x0 + width - 1 || z == z0 || z == z0 + depth - 1;
            if (edge) for (int y = GROUND_Y + 1; y <= GROUND_Y + 5; y++) set(level, x, y, z, (x + z + y) % 7 == 0 ? accent : wall);
        }
        int doorX = x0 + width / 2;
        for (int y = GROUND_Y + 1; y <= GROUND_Y + 3; y++) set(level, doorX, y, z0 + depth - 1, Blocks.AIR);
        for (int x = x0 - 1; x <= x0 + width; x++) for (int z = z0 - 1; z <= z0 + depth; z++) set(level, x, GROUND_Y + 6, z, roof);
    }

    private static void stall(ServerLevel level, int x, int y, int z, Block wood) {
        for (int dx = -3; dx <= 3; dx++) for (int dz = -2; dz <= 2; dz++) set(level, x + dx, y, z + dz, wood);
        for (int dx : new int[]{-3, 3}) for (int dz : new int[]{-2, 2}) {
            set(level, x + dx, y + 1, z + dz, Blocks.OAK_FENCE);
            set(level, x + dx, y + 2, z + dz, Blocks.OAK_FENCE);
        }
        for (int dx = -4; dx <= 4; dx++) for (int dz = -3; dz <= 3; dz++) set(level, x + dx, y + 3, z + dz, Blocks.SPRUCE_SLAB);
    }

    private static void road(ServerLevel level, int x0, int z0, int x1, int z1, int halfWidth) {
        int steps = Math.max(Math.abs(x1 - x0), Math.abs(z1 - z0));
        for (int i = 0; i <= Math.max(1, steps); i++) {
            double t = i / (double)Math.max(1, steps);
            int x = (int)Math.round(x0 + (x1 - x0) * t);
            int z = (int)Math.round(z0 + (z1 - z0) * t);
            for (int dx = -halfWidth; dx <= halfWidth; dx++) for (int dz = -halfWidth; dz <= halfWidth; dz++) {
                if (Math.abs(dx) + Math.abs(dz) > halfWidth + 1) continue;
                levelColumn(level, x + dx, z + dz, Math.abs(dx) + Math.abs(dz) <= 1 ? Blocks.POLISHED_ANDESITE : Blocks.STONE_BRICKS);
            }
        }
    }

    private static void pad(ServerLevel level, int cx, int cz, int radius, Block inner, Block rim) {
        for (int x = cx - radius; x <= cx + radius; x++) for (int z = cz - radius; z <= cz + radius; z++) {
            int d2 = (x - cx) * (x - cx) + (z - cz) * (z - cz);
            if (d2 > radius * radius) continue;
            levelColumn(level, x, z, d2 > (radius - 2) * (radius - 2) ? rim : inner);
        }
    }

    private static void levelCircle(ServerLevel level, int cx, int cz, int radius) {
        for (int x = cx - radius; x <= cx + radius; x++) for (int z = cz - radius; z <= cz + radius; z++) {
            if ((x - cx) * (x - cx) + (z - cz) * (z - cz) > radius * radius) continue;
            levelColumn(level, x, z, Blocks.GRASS_BLOCK);
        }
    }

    private static void levelColumn(ServerLevel level, int x, int z, Block ground) {
        for (int y = GROUND_Y - 3; y < GROUND_Y; y++) set(level, x, y, z, Blocks.DIRT);
        set(level, x, GROUND_Y, z, ground);
        for (int y = GROUND_Y + 1; y <= GROUND_Y + 10; y++) set(level, x, y, z, Blocks.AIR);
    }

    private static void lanternPost(ServerLevel level, int x, int groundY, int z, boolean soul) {
        set(level, x, groundY + 1, z, Blocks.COBBLESTONE_WALL);
        set(level, x, groundY + 2, z, soul ? Blocks.SOUL_LANTERN : Blocks.LANTERN);
    }

    private static boolean hasMarker(ServerLevel level) {
        return level.getBlockState(new BlockPos(0, MARKER_Y, 20)).is(Blocks.LODESTONE)
                && level.getBlockState(new BlockPos(1, MARKER_Y, 20)).is(Blocks.AMETHYST_BLOCK)
                && level.getBlockState(new BlockPos(2, MARKER_Y, 20)).is(Blocks.GOLD_BLOCK);
    }

    private static void writeMarker(ServerLevel level) {
        set(level, 0, MARKER_Y, 20, Blocks.LODESTONE);
        set(level, 1, MARKER_Y, 20, Blocks.AMETHYST_BLOCK);
        set(level, 2, MARKER_Y, 20, Blocks.GOLD_BLOCK);
    }

    private static void set(ServerLevel level, int x, int y, int z, Block block) {
        level.setBlock(new BlockPos(x, y, z), block.defaultBlockState(), 2);
    }
}
