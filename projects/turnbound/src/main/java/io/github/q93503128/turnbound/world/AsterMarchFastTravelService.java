package io.github.q93503128.turnbound.world;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/** Server-authoritative projection for the six canonical Aster March relay destinations. */
public final class AsterMarchFastTravelService {
    private static final double CURRENT_RADIUS_SQR = 14.0 * 14.0;

    private AsterMarchFastTravelService() {}

    public static boolean canonicalDestination(String id) {
        return AsterMarchFastTravelRules.canonicalDestination(id);
    }

    public static boolean unlocked(ServerPlayer player, String destinationId) {
        if (player == null) return false;
        return AsterMarchFastTravelRules.unlocked(destinationId, progress(player));
    }

    public static String lockedReason(String destinationId) {
        return AsterMarchFastTravelRules.lockedReason(destinationId);
    }

    /**
     * Replaces stale per-region canonical FT entries with one authoritative six-anchor relay list while keeping
     * local non-relay travel choices such as START_VILLAGE intact.
     */
    public static FieldUiSnapshot project(ServerPlayer player, FieldUiSnapshot snapshot) {
        if (player == null || snapshot == null || !snapshot.active() || snapshot.mode() == FieldUiSnapshot.Mode.LOADING) {
            return snapshot;
        }

        List<FieldUiSnapshot.Travel> travels = new ArrayList<>();
        for (FieldUiSnapshot.Travel travel : snapshot.travels()) {
            if (!canonicalDestination(travel.id())) travels.add(travel);
        }

        AsterMarchFastTravelRules.Progress progress = progress(player);
        Vec3 playerPos = player.position();
        for (FieldTravelCatalog.Destination destination : FieldTravelCatalog.destinations()) {
            double dx = playerPos.x - destination.x();
            double dy = playerPos.y - destination.y();
            double dz = playerPos.z - destination.z();
            boolean current = dx * dx + dy * dy + dz * dz <= CURRENT_RADIUS_SQR;
            travels.add(new FieldUiSnapshot.Travel(
                    destination.id(), destination.label(),
                    AsterMarchFastTravelRules.unlocked(destination.id(), progress), current));
        }

        return new FieldUiSnapshot(
                snapshot.active(), snapshot.mode(), snapshot.patrolsCleared(), snapshot.patrolGoal(),
                snapshot.bossUnlocked(), snapshot.chapterCleared(), snapshot.earnedXp(), snapshot.earnedGold(),
                snapshot.objective(), snapshot.dialogue(), snapshot.reward(), snapshot.encounters(), travels,
                snapshot.loadingStage(), snapshot.loadingPercent());
    }

    private static AsterMarchFastTravelRules.Progress progress(ServerPlayer player) {
        CampaignProgressStore.Snapshot snapshot = CampaignProgressStore.snapshot(player.getUUID());
        return new AsterMarchFastTravelRules.Progress(
                snapshot.quests().completed(), snapshot.quests().unlockFlags(), snapshot.clearedEncounters());
    }
}
