package io.github.q93503128.turnbound.world;

import java.util.UUID;

/**
 * Drehmal physical-shop progression.
 *
 * <p>The open world does not hard-gate normal T1/T2 equipment by party level or story chapter.
 * Price, route danger and available Gold are the natural readiness checks. Legacy non-Drehmal chapter
 * progression remains unchanged.</p>
 */
final class DrehmalEquipmentProgression {
    private DrehmalEquipmentProgression() {}

    static int shopChapter(UUID playerId) {
        return 2;
    }

    static boolean tier2Unlocked(UUID playerId) {
        return true;
    }
}
