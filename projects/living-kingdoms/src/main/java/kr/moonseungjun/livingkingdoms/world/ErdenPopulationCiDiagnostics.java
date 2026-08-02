package kr.moonseungjun.livingkingdoms.world;

import kr.moonseungjun.livingkingdoms.LivingKingdoms;
import kr.moonseungjun.livingkingdoms.worldgen.AuthoredContinentDensity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.HashSet;
import java.util.Set;

/** Emits a compact progress reason when the headless household sample is not materialised yet. */
public final class ErdenPopulationCiDiagnostics {
    private static final boolean ENABLED =
            "1".equals(System.getenv("LIVING_KINGDOMS_CI_REALM_TEST"));
    private static final int LOG_INTERVAL_TICKS = 200;

    private ErdenPopulationCiDiagnostics() {
    }

    public static void onServerTick(ServerTickEvent.Post event) {
        if (!ENABLED) return;
        ServerLevel level = event.getServer().getLevel(StarterRealmManager.REALM_KEY);
        if (level == null
                || level.getGameTime() % LOG_INTERVAL_TICKS != 0L
                || !RealmSitePlanner.isBuilt(level, "erden_kingdom")) return;
        ErdenPopulationSavedData population = level.getDataStorage()
                .computeIfAbsent(ErdenPopulationSavedData.TYPE);
        if (population.households().isEmpty()) return;
        ErdenPopulationSavedData.Household sample = population.households().getFirst();
        long key = ((long) sample.homeX() << 32) ^ (sample.homeZ() & 0xffffffffL);
        boolean homeLoaded = level.hasChunk(sample.homeX() >> 4, sample.homeZ() >> 4);
        boolean upperComplete = level.getDataStorage()
                .computeIfAbsent(ErdenUrbanLifeSavedData.TYPE)
                .isUpperFloorComplete(key, ErdenUrbanLifeManager.UPPER_FLOOR_REVISION);
        int doorY = findDoorY(level, sample.homeX(), sample.homeZ());
        Set<String> names = new HashSet<>();
        for (ErdenPopulationSavedData.Resident resident : sample.residents()) {
            names.add(resident.name());
        }
        AABB capital = new AABB(
                ErdenCapitalStreamingBuilder.WEST_WALL_X - 64,
                level.getMinY(),
                ErdenCapitalStreamingBuilder.NORTH_WALL_Z - 64,
                ErdenCapitalStreamingBuilder.EAST_WALL_X + 64,
                level.getMaxY(),
                ErdenCapitalStreamingBuilder.SOUTH_WALL_Z + 64);
        int spawned = level.getEntitiesOfClass(
                Villager.class, capital,
                villager -> names.contains(villager.getName().getString())).size();
        LivingKingdoms.LOGGER.info(
                "LK_ERDEN_POPULATION_CI_WAIT home={},{} loaded={} upper_complete={} door_y={} spawned_sample={} game_time={}",
                sample.homeX(), sample.homeZ(), homeLoaded, upperComplete, doorY,
                spawned, level.getGameTime());
    }

    private static int findDoorY(ServerLevel level, int x, int z) {
        if (!level.hasChunk(x >> 4, z >> 4)) return Integer.MIN_VALUE;
        int designed = (int) Math.round(AuthoredContinentDensity.surfaceHeight(x, z));
        int minimum = Math.max(level.getMinY(), designed - 8);
        int maximum = Math.min(level.getMaxY() - 1, designed + 64);
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int y = minimum; y <= maximum; y++) {
            cursor.set(x, y, z);
            if (level.getBlockState(cursor).getBlock() instanceof DoorBlock) return y;
        }
        return Integer.MIN_VALUE;
    }
}
