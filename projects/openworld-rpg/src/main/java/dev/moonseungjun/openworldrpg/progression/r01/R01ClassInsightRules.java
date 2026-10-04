package dev.moonseungjun.openworldrpg.progression.r01;

import dev.moonseungjun.openworldrpg.integration.actor.R01ExternalActorCatalog;
import java.util.Objects;
import java.util.UUID;

/** Pure qualification rules for R01 class Insights whose evidence is already runtime-bound. */
public final class R01ClassInsightRules {
    private R01ClassInsightRules() {
    }

    public static boolean hunterCrownedMarkQualifies(
            String targetEntityId,
            boolean authoredSpawn,
            UUID targetId,
            UUID quarryAtCast,
            boolean fullFocusSpent,
            boolean authoredWeakPointHit
    ) {
        Objects.requireNonNull(targetEntityId, "targetEntityId");
        Objects.requireNonNull(targetId, "targetId");
        return R01ExternalActorCatalog.REGALHART.equals(targetEntityId)
                && authoredSpawn
                && fullFocusSpent
                && authoredWeakPointHit
                && targetId.equals(quarryAtCast);
    }
}
