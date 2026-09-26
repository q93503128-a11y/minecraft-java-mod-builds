package dev.moonseungjun.openworldrpg.progression.r01;

import java.util.Objects;
import net.minecraft.server.level.ServerPlayer;

/**
 * Reconnect-safe semantic bridge for the R01 post-Earthloong chamber aftermath and Alderford
 * briefing. Presentation callers may play or skip dialogue, but only these committed server states
 * advance the story.
 */
public final class R01PostQuarryService {
    private R01PostQuarryService() {
    }

    /**
     * Closes the crash window after Earthloong first-clear state commits and before the return
     * objective becomes durable.
     */
    public static boolean reconcileImmediateAftermath(ServerPlayer player) {
        Objects.requireNonNull(player, "player");
        R01PlayerState current = R01PlayerStateService.state(player);
        if (!current.quarry().firstClear()
                || current.opening().mainStage().isAtLeast(
                        R01MainStage.POST_QUARRY_BRIEFING_PENDING
                )) {
            return false;
        }

        R01PlayerStateService.commitPostQuarryAftermath(player);
        return true;
    }

    /**
     * Semantic interaction used by the later chamber spatial adapter. If relay evidence was missed
     * before the boss, the post-boss damaged plate records it here without duplicating anything.
     */
    public static boolean recordChamberRelayInteraction(ServerPlayer player) {
        Objects.requireNonNull(player, "player");
        R01PlayerState before = R01PlayerStateService.state(player);
        if (!before.quarry().firstClear()) {
            return false;
        }

        reconcileImmediateAftermath(player);
        before = R01PlayerStateService.state(player);
        if (before.quarry().relayEvidenceSeen()) {
            return false;
        }
        R01PlayerStateService.markQuarryRelayEvidenceSeen(player);
        return true;
    }

    /**
     * Completing line-by-line presentation and whole-scene skip intentionally commit the exact same
     * progression result: both peer leads become known and Act I opens.
     */
    public static boolean completeOrSkipAlderfordBriefing(ServerPlayer player) {
        Objects.requireNonNull(player, "player");
        reconcileImmediateAftermath(player);
        R01PlayerState before = R01PlayerStateService.state(player);
        if (!before.quarry().firstClear()
                || !before.opening().mainStage().isAtLeast(
                        R01MainStage.POST_QUARRY_BRIEFING_PENDING
                )) {
            return false;
        }
        if (before.opening().postQuarryBriefingSeen()
                && before.opening().act1WesternRelayLeadKnown()
                && before.opening().act1WhitecrestStationLeadKnown()
                && before.opening().mainStage().isAtLeast(
                        R01MainStage.ACT1_LEADS_OPEN
                )) {
            return false;
        }

        R01PlayerStateService.completePostQuarryBriefing(player);
        return true;
    }
}
