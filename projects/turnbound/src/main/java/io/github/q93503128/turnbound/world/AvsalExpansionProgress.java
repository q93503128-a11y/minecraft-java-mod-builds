package io.github.q93503128.turnbound.world;

import net.minecraft.server.level.ServerPlayer;

import java.util.Set;

/** Durable server-owned progression flags for the first Av'Sal production slice. */
final class AvsalExpansionProgress {
    static final String BRIEFED = "AVSAL_MQ_AV01_BRIEFED";
    static final String ROADSIDE_ECHO_SEEN = "AVSAL_ROADSIDE_ECHO_SEEN";
    static final String ROAD_PATROL_SEEN = "AVSAL_ROAD_PATROL_SEEN";
    static final String OUTSKIRTS_REACHED = "AVSAL_MQ_AV01_COMPLETE";
    static final String CLUE_SCAVENGER = "AVSAL_MQ_AV02_SCAVENGER";
    static final String CLUE_SURVIVOR = "AVSAL_MQ_AV02_SURVIVOR";
    static final String CLUE_RECORDS = "AVSAL_MQ_AV02_RECORDS";
    static final String INVESTIGATION_COMPLETE = "AVSAL_MQ_AV02_COMPLETE";
    private static final Set<String> INVESTIGATION_CLUES = Set.of(CLUE_SCAVENGER, CLUE_SURVIVOR, CLUE_RECORDS);

    enum Stage { LOCKED, BRIEFING, ROAD_EVENT, ROAD_PATROL, OUTSKIRTS, INVESTIGATE, NORTHBOUND }

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
        if (flags.contains(OUTSKIRTS_REACHED)) {
            return flags.contains(INVESTIGATION_COMPLETE) ? Stage.NORTHBOUND : Stage.INVESTIGATE;
        }
        if (!flags.contains(ROADSIDE_ECHO_SEEN)) return Stage.ROAD_EVENT;
        if (!flags.contains(ROAD_PATROL_SEEN)) return Stage.ROAD_PATROL;
        return Stage.OUTSKIRTS;
    }

    static int investigationCount(Set<String> flags) {
        if (flags == null || flags.isEmpty()) return 0;
        int count = 0;
        for (String clue : INVESTIGATION_CLUES) if (flags.contains(clue)) count++;
        return count;
    }

    static boolean investigationComplete(Set<String> flags) {
        return flags != null && (flags.contains(INVESTIGATION_COMPLETE) || investigationCount(flags) >= 2);
    }

    static boolean reconcileInvestigation(ServerPlayer player) {
        if (player == null || player.level().getServer() == null) return false;
        Set<String> flags = ExternalWorldSavedData.get(player.level().getServer()).onboardingFlags(player.getUUID());
        if (!flags.contains(OUTSKIRTS_REACHED) || flags.contains(INVESTIGATION_COMPLETE) || investigationCount(flags) < 2) return false;
        return mark(player, INVESTIGATION_COMPLETE);
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
