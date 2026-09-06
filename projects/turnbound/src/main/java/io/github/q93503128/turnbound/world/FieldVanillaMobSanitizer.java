package io.github.q93503128.turnbound.world;

import io.github.q93503128.turnbound.presentation.BattleActorEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.AABB;

import java.util.EnumMap;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * World/region scoped throttle for removing vanilla mobs from authored TURNBOUND field spaces.
 * Multiple players may tick the same region, but the physical-world scan must happen at most once per second.
 */
final class FieldVanillaMobSanitizer {
    enum Region { SOUTHGATE, GLOAMWOOD, AQUEDUCT, QUARRY, OLD_RELAY }

    static final long INTERVAL_TICKS = 20L;
    private static final Map<ServerLevel, EnumMap<Region, Long>> LAST_RUN = new WeakHashMap<>();

    private FieldVanillaMobSanitizer() {}

    static boolean clearIfDue(ServerLevel level, Region region, AABB area) {
        if (level == null || region == null || area == null) return false;
        long now = level.getGameTime();
        EnumMap<Region, Long> byRegion = LAST_RUN.computeIfAbsent(level, ignored -> new EnumMap<>(Region.class));
        Long last = byRegion.get(region);
        if (!due(last, now)) return false;
        byRegion.put(region, now);

        for (Mob mob : level.getEntitiesOfClass(Mob.class, area)) {
            if (!(mob instanceof BattleActorEntity)) mob.discard();
        }
        return true;
    }

    static boolean due(Long lastRun, long now) {
        return lastRun == null || now < lastRun || now - lastRun >= INTERVAL_TICKS;
    }

    static void clearForTests() { LAST_RUN.clear(); }
}
