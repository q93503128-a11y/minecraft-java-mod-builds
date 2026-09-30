package io.github.q93503128.turnbound.world;

import net.minecraft.server.level.ServerPlayer;

import java.util.Set;

/** Durable server-owned progression flags for the first Av'Sal production slice. */
final class AvsalExpansionProgress {
    static final String BRIEFED = "AVSAL_MQ_AV01_BRIEFED";
    static final String ROADSIDE_ECHO_SEEN = "AVSAL_ROADSIDE_ECHO_SEEN";
    static final String ROAD_PATROL_SEEN = "AVSAL_ROAD_PATROL_SEEN";
    static final String OUTSKIRTS_REACHED = "AVSAL_MQ_AV01_COMPLETE";

    enum Stage { LOCKED, BRIEFING, ROAD_EVENT, ROAD_PATROL, OUTSKIRTS, INVESTIGATE }

    private AvsalExpansionProgress() {}

    static boolean briefingReady(Set<String> clears, Set<String> flags) {
        return clears != null && flags != null
                && DrabyelOpeningTutorial.patrolCleared(clears)
                && flags.contains(DrehmalContextualOnboarding.HUB_ROUTE_REVIEWED)
                && !flags.contains(BRIEFED);
    }

    static boolean briefed(Set<String> flags) {
        return flags != null && flags.contains(BRIEFED);
    }

    static Stage stage(Set<String> flags, Set<String> clears) {
        if (flags == null || clears == null || !DrabyelOpeningTutorial.patrolCleared(clears)) return Stage.LOCKED;
        if (!flags.contains(BRIEFED)) return Stage.BRIEFING;
        if (flags.contains(OUTSKIRTS_REACHED)) return Stage.INVESTIGATE;
        if (!flags.contains(ROADSIDE_ECHO_SEEN)) return Stage.ROAD_EVENT;
        if (!flags.contains(ROAD_PATROL_SEEN)) return Stage.ROAD_PATROL;
        return Stage.OUTSKIRTS;
    }

    static boolean mark(ServerPlayer player, String flag) {
        if (player == null || flag == null || flag.isBlank()) return false;
        var server = player.level().getServer();
        if (server == null) return false;
        ExternalWorldSavedData data = ExternalWorldSavedData.get(server);
        if (data.onboardingFlag(player.getUUID(), flag)) return false;
        data.markOnboardingFlag(player.getUUID(), flag);
        return true;
    }
}
