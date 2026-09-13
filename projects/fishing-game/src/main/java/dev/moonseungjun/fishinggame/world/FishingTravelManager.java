package dev.moonseungjun.fishinggame.world;

import java.util.Set;

import dev.moonseungjun.fishinggame.FishingGameMod;
import dev.moonseungjun.fishinggame.fishing.FishingLocation;
import dev.moonseungjun.fishinggame.fishing.FishingStage;
import dev.moonseungjun.fishinggame.network.FishingStatePayload;
import dev.moonseungjun.fishinggame.profile.FishingProfiles;
import dev.moonseungjun.fishinggame.progression.FishingRods;
import dev.moonseungjun.fishinggame.progression.RodDefinition;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.clock.WorldClock;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

public final class FishingTravelManager {
    public static final ResourceKey<Level> COAST_LEVEL = ResourceKey.create(
            Registries.DIMENSION,
            FishingGameMod.id("coast")
    );
    public static final ResourceKey<Level> DEEP_SEA_LEVEL = ResourceKey.create(
            Registries.DIMENSION,
            FishingGameMod.id("deep_sea")
    );

    private static final BlockPos BUILD_MARKER = new BlockPos(52, 40, 52);
    private static final int DECK_Y = 64;
    private static int maintenanceTicks;

    private FishingTravelManager() {
    }

    public static void initialize() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            maintenanceTicks++;
            if (maintenanceTicks < 100) return;
            maintenanceTicks = 0;

            maintainLevel(server.getLevel(COAST_LEVEL), FishingLocation.COAST);
            maintainLevel(server.getLevel(DEEP_SEA_LEVEL), FishingLocation.DEEP_SEA);

            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                FishingLocation location = locationFor(player);
                if (location == FishingLocation.LAKESIDE) continue;
                if (outsidePlayableArea(player, location) || player.getY() < 48.0 || player.getY() > 100.0) {
                    ServerLevel target = server.getLevel(keyFor(location));
                    if (target != null) teleportToArrival(player, target, location);
                }
            }
        });
    }

    public static void requestTravel(ServerPlayer player, int locationOrdinal) {
        if (locationOrdinal < 0 || locationOrdinal >= FishingLocation.values().length) return;
        if (player.fishing != null) {
            sendIdle(player, "낚시 중에는 다른 낚시터로 이동할 수 없습니다.");
            return;
        }

        FishingLocation target = FishingLocation.values()[locationOrdinal];
        FishingLocation current = locationFor(player);
        if (target == current) {
            sendIdle(player, "이미 " + target.displayName() + "에 있습니다.");
            return;
        }

        int rodTier = FishingProfiles.get(player).rodTier();
        if (rodTier < target.minRodTier()) {
            RodDefinition required = FishingRods.byTier(target.minRodTier());
            sendIdle(player, required.displayName() + "을(를) 얻으면 " + target.displayName() + "에 갈 수 있습니다.");
            return;
        }

        if (travel(player, target)) {
            sendIdle(player, target.displayName() + "에 도착했습니다.");
        } else {
            sendIdle(player, "낚시터로 이동하지 못했습니다.");
        }
    }

    public static FishingLocation locationFor(ServerPlayer player) {
        ResourceKey<Level> dimension = player.level().dimension();
        if (dimension.equals(COAST_LEVEL)) return FishingLocation.COAST;
        if (dimension.equals(DEEP_SEA_LEVEL)) return FishingLocation.DEEP_SEA;
        return FishingLocation.LAKESIDE;
    }

    public static boolean isDedicatedFishingLevel(ResourceKey<Level> dimension) {
        return dimension.equals(FishingWorldManager.LAKESIDE_LEVEL)
                || dimension.equals(COAST_LEVEL)
                || dimension.equals(DEEP_SEA_LEVEL);
    }

    public static boolean travel(ServerPlayer player, FishingLocation target) {
        MinecraftServer server = player.level().getServer();
        if (server == null) return false;
        if (target == FishingLocation.LAKESIDE) {
            return FishingWorldManager.prepareAndPlacePlayer(player, server);
        }

        ServerLevel level = server.getLevel(keyFor(target));
        if (level == null) {
            FishingGameMod.LOGGER.error("Fishing destination {} is unavailable", target);
            return false;
        }

        buildIfNeeded(level, target);
        lockClock(level, target);
        return teleportToArrival(player, level, target);
    }

    private static void sendIdle(ServerPlayer player, String notice) {
        ServerPlayNetworking.send(player, new FishingStatePayload(
                FishingStage.values().length,
                0.0f,
                0.0f,
                "",
                locationFor(player).displayName(),
                notice
        ));
    }

    private static ResourceKey<Level> keyFor(FishingLocation location) {
        return switch (location) {
            case LAKESIDE -> FishingWorldManager.LAKESIDE_LEVEL;
            case COAST -> COAST_LEVEL;
            case DEEP_SEA -> DEEP_SEA_LEVEL;
        };
    }

    private static void maintainLevel(ServerLevel level, FishingLocation location) {
        if (level == null) return;
        lockClock(level, location);
    }

    private static void lockClock(ServerLevel level, FishingLocation location) {
        Holder<WorldClock> clock = level.dimensionType().defaultClock().orElseThrow(() ->
                new IllegalStateException("Fishing dimension is missing a default clock")
        );
        int ticks = location == FishingLocation.DEEP_SEA ? 14000 : 5000;
        level.clockManager().setTotalTicks(clock, ticks);
        level.clockManager().setPaused(clock, true);
    }

    private static boolean teleportToArrival(ServerPlayer player, ServerLevel level, FishingLocation location) {
        double z = location == FishingLocation.COAST ? 34.5 : 18.5;
        float yaw = location == FishingLocation.COAST ? 180.0f : 0.0f;
        return player.teleportTo(level, 0.5, 65.0, z, Set.of(), yaw, 0.0f, true);
    }

    private static boolean outsidePlayableArea(ServerPlayer player, FishingLocation location) {
        double radiusX = location == FishingLocation.COAST ? 58.0 : 46.0;
        double radiusZ = location == FishingLocation.COAST ? 55.0 : 46.0;
        double nx = player.getX() / radiusX;
        double nz = player.getZ() / radiusZ;
        return nx * nx + nz * nz > 1.0;
    }

    private static void buildIfNeeded(ServerLevel level, FishingLocation location) {
        if (level.getBlockState(BUILD_MARKER).is(Blocks.LODESTONE)) return;

        if (location == FishingLocation.COAST) {
            FishingGameMod.LOGGER.info("Authoring Gull Harbor fishing location");
            buildCoast(level);
        } else if (location == FishingLocation.DEEP_SEA) {
            FishingGameMod.LOGGER.info("Authoring Deepwater Channel fishing location");
            buildDeepSea(level);
        }
        set(level, BUILD_MARKER, Blocks.LODESTONE);
    }

    private static void buildCoast(ServerLevel level) {
        buildHarborShore(level);
        buildStoneQuay(level);
        buildFishingPiers(level);
        buildHarborShelter(level);
        buildLighthouse(level);
        buildHarborDetails(level);
    }

    private static void buildHarborShore(ServerLevel level) {
        for (int x = -46; x <= 46; x++) {
            int shoreZ = 15 + Math.abs(x) / 7;
            for (int z = shoreZ; z <= 46; z++) {
                for (int y = 58; y <= 63; y++) {
                    set(level, new BlockPos(x, y, z), y <= 61 ? Blocks.STONE : Blocks.DIRT);
                }
                Block top;
                if (z <= shoreZ + 2) top = Blocks.SAND;
                else if (((x * 13 + z * 5) & 15) == 0) top = Blocks.COARSE_DIRT;
                else top = Blocks.GRASS_BLOCK;
                set(level, new BlockPos(x, DECK_Y, z), top);
            }
        }
    }

    private static void buildStoneQuay(ServerLevel level) {
        for (int x = -34; x <= 34; x++) {
            for (int z = 14; z <= 19; z++) {
                for (int y = 60; y <= 63; y++) set(level, new BlockPos(x, y, z), Blocks.STONE_BRICKS);
                set(level, new BlockPos(x, DECK_Y, z), ((x + z) & 5) == 0
                        ? Blocks.MOSSY_STONE_BRICKS : Blocks.STONE_BRICKS);
            }
        }
    }

    private static void buildFishingPiers(ServerLevel level) {
        int[] centers = {-22, 0, 22};
        for (int centerX : centers) {
            for (int z = -14; z <= 14; z++) {
                for (int x = centerX - 2; x <= centerX + 2; x++) set(level, new BlockPos(x, DECK_Y, z), Blocks.SPRUCE_PLANKS);
                if ((z + 14) % 5 == 0) {
                    for (int y = 56; y < DECK_Y; y++) {
                        set(level, new BlockPos(centerX - 2, y, z), Blocks.SPRUCE_LOG);
                        set(level, new BlockPos(centerX + 2, y, z), Blocks.SPRUCE_LOG);
                    }
                    set(level, new BlockPos(centerX - 2, DECK_Y + 1, z), Blocks.SPRUCE_FENCE);
                    set(level, new BlockPos(centerX + 2, DECK_Y + 1, z), Blocks.SPRUCE_FENCE);
                }
            }
        }
    }

    private static void buildHarborShelter(ServerLevel level) {
        for (int x = -15; x <= -3; x++) {
            for (int z = 27; z <= 39; z++) set(level, new BlockPos(x, DECK_Y, z), Blocks.OAK_PLANKS);
        }
        int[][] posts = {{-14, 28}, {-4, 28}, {-14, 38}, {-4, 38}};
        for (int[] post : posts) pillar(level, post[0], post[1], DECK_Y + 1, DECK_Y + 6, Blocks.OAK_LOG);
        for (int x = -16; x <= -2; x++) {
            for (int z = 26; z <= 40; z++) set(level, new BlockPos(x, DECK_Y + 7, z), Blocks.SPRUCE_SLAB);
        }
        for (int x = -12; x <= -6; x += 3) set(level, new BlockPos(x, DECK_Y + 1, 34), Blocks.BARREL);
    }

    private static void buildLighthouse(ServerLevel level) {
        int cx = 30;
        int cz = 33;
        for (int y = DECK_Y + 1; y <= DECK_Y + 10; y++) {
            Block shell = ((y - DECK_Y) / 2) % 2 == 0 ? Blocks.CONCRETE.white() : Blocks.CONCRETE.red();
            for (int dx = -2; dx <= 2; dx++) {
                for (int dz = -2; dz <= 2; dz++) {
                    if (Math.abs(dx) != 2 && Math.abs(dz) != 2) continue;
                    set(level, new BlockPos(cx + dx, y, cz + dz), shell);
                }
            }
        }
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) set(level, new BlockPos(cx + dx, DECK_Y + 11, cz + dz), Blocks.DARK_OAK_SLAB);
        }
        set(level, new BlockPos(cx, DECK_Y + 12, cz), Blocks.SEA_LANTERN);
    }

    private static void buildHarborDetails(ServerLevel level) {
        int[][] lamps = {{-31, 20}, {-16, 20}, {16, 20}, {31, 20}, {-28, 36}, {22, 36}};
        for (int[] lamp : lamps) {
            set(level, new BlockPos(lamp[0], DECK_Y + 1, lamp[1]), Blocks.SPRUCE_FENCE);
            set(level, new BlockPos(lamp[0], DECK_Y + 2, lamp[1]), Blocks.SPRUCE_FENCE);
            set(level, new BlockPos(lamp[0], DECK_Y + 3, lamp[1]), Blocks.LANTERN);
        }
        int[][] crates = {{7, 28}, {9, 28}, {12, 30}, {15, 29}, {18, 31}};
        for (int[] crate : crates) set(level, new BlockPos(crate[0], DECK_Y + 1, crate[1]), Blocks.BARREL);
    }

    private static void buildDeepSea(ServerLevel level) {
        buildOffshoreDeck(level);
        buildDeepSeaArms(level);
        buildObservationTower(level);
        buildDeepSeaLights(level);
    }

    private static void buildOffshoreDeck(ServerLevel level) {
        for (int x = -12; x <= 12; x++) {
            for (int z = 8; z <= 25; z++) {
                Block deck = (Math.abs(x) == 12 || z == 8 || z == 25)
                        ? Blocks.POLISHED_BLACKSTONE_BRICKS : Blocks.DARK_PRISMARINE;
                set(level, new BlockPos(x, DECK_Y, z), deck);
            }
        }
        int[][] supports = {{-10, 10}, {10, 10}, {-10, 23}, {10, 23}};
        for (int[] support : supports) pillar(level, support[0], support[1], 48, DECK_Y - 1, Blocks.DARK_OAK_LOG);
    }

    private static void buildDeepSeaArms(ServerLevel level) {
        for (int z = -18; z <= 8; z++) {
            for (int x = -2; x <= 2; x++) set(level, new BlockPos(x, DECK_Y, z), Blocks.DARK_OAK_PLANKS);
            if ((z + 18) % 5 == 0) {
                pillar(level, -2, z, 48, DECK_Y - 1, Blocks.DARK_OAK_LOG);
                pillar(level, 2, z, 48, DECK_Y - 1, Blocks.DARK_OAK_LOG);
            }
        }
        for (int x = -32; x <= -12; x++) {
            for (int z = 15; z <= 19; z++) set(level, new BlockPos(x, DECK_Y, z), Blocks.DARK_OAK_PLANKS);
        }
        for (int x = 12; x <= 32; x++) {
            for (int z = 15; z <= 19; z++) set(level, new BlockPos(x, DECK_Y, z), Blocks.DARK_OAK_PLANKS);
        }
    }

    private static void buildObservationTower(ServerLevel level) {
        int[][] posts = {{-8, 20}, {8, 20}, {-8, 24}, {8, 24}};
        for (int[] post : posts) pillar(level, post[0], post[1], DECK_Y + 1, DECK_Y + 8, Blocks.IRON_BLOCK);
        for (int x = -9; x <= 9; x++) {
            for (int z = 19; z <= 25; z++) set(level, new BlockPos(x, DECK_Y + 9, z), Blocks.SMOOTH_STONE_SLAB);
        }
        for (int x = -4; x <= 4; x++) set(level, new BlockPos(x, DECK_Y + 6, 22), Blocks.IRON_BARS);
    }

    private static void buildDeepSeaLights(ServerLevel level) {
        int[][] lights = {{-11, 9}, {11, 9}, {-11, 24}, {11, 24}, {-31, 17}, {31, 17}, {0, -17}};
        for (int[] light : lights) {
            set(level, new BlockPos(light[0], DECK_Y + 1, light[1]), Blocks.IRON_BARS);
            set(level, new BlockPos(light[0], DECK_Y + 2, light[1]), Blocks.SEA_LANTERN);
        }
    }

    private static void pillar(ServerLevel level, int x, int z, int minY, int maxY, Block block) {
        for (int y = minY; y <= maxY; y++) set(level, new BlockPos(x, y, z), block);
    }

    private static void set(ServerLevel level, BlockPos pos, Block block) {
        level.setBlock(pos, block.defaultBlockState(), 2);
    }
}
