package dev.moonseungjun.openworldrpg.progression.r01;

import dev.moonseungjun.openworldrpg.integration.actor.ExternalActorBindingRuntime;
import dev.moonseungjun.openworldrpg.progression.ClassInsight;
import dev.moonseungjun.openworldrpg.progression.PlayerPassiveProgressService;
import java.util.Objects;
import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

/**
 * Server-owned bridge from already-confirmed R01 gameplay evidence to permanent Class Insight state.
 *
 * <p>This service does not infer success from proximity, post-hit Quarry state, donor damage or
 * presentation. Callers must provide the accepted-cast Focus/Quarry snapshot and the authored
 * weak-point result from the project hit authority.</p>
 */
public final class R01ClassInsightService {
    private R01ClassInsightService() {
    }

    public static boolean recordHunterCrownedMarkHit(
            ServerPlayer hunter,
            LivingEntity target,
            UUID quarryAtCast,
            boolean fullFocusSpent,
            boolean authoredWeakPointHit
    ) {
        Objects.requireNonNull(hunter, "hunter");
        Objects.requireNonNull(target, "target");
        if (hunter.level().isClientSide()
                || hunter.level() != target.level()) {
            return false;
        }

        var profile = ExternalActorBindingRuntime
                .combatProfile(target)
                .orElse(null);
        if (profile == null
                || !R01ClassInsightRules.hunterCrownedMarkQualifies(
                        profile.entityId(),
                        ExternalActorBindingRuntime.isAuthoredSpawn(target),
                        target.getUUID(),
                        quarryAtCast,
                        fullFocusSpent,
                        authoredWeakPointHit
                )) {
            return false;
        }

        return PlayerPassiveProgressService.completeInsight(
                hunter,
                ClassInsight.HUNTER_R01_CROWNED_MARK
        ).status() == PlayerPassiveProgressService
                .InsightStatus.COMPLETED;
    }
}
