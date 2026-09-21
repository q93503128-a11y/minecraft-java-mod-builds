package dev.moonseungjun.openworldrpg.integration.actor;

import dev.moonseungjun.openworldrpg.combat.authority.ProjectImpactTransaction;
import java.util.Objects;

public record ExternalActorCombatProfile(
        String entityId,
        int contentLevel,
        float maxHealth,
        double defense,
        double magicResistance,
        double poiseMax
) {
    public ExternalActorCombatProfile {
        Objects.requireNonNull(entityId, "entityId");
        if (entityId.isBlank()
                || contentLevel < 1
                || !Float.isFinite(maxHealth)
                || maxHealth <= 0.0F
                || !Double.isFinite(defense)
                || defense < 0.0
                || !Double.isFinite(magicResistance)
                || magicResistance < 0.0
                || !Double.isFinite(poiseMax)
                || poiseMax < 0.0) {
            throw new IllegalArgumentException("Invalid external actor combat profile: " + entityId);
        }
    }

    public ProjectImpactTransaction.DamageTargetSnapshot projectTargetSnapshot() {
        return projectTargetSnapshot(1.0);
    }

    public ProjectImpactTransaction.DamageTargetSnapshot projectTargetSnapshot(
            double authoredDamageTakenMultiplier
    ) {
        return new ProjectImpactTransaction.DamageTargetSnapshot(
                defense,
                magicResistance,
                authoredDamageTakenMultiplier,
                0.0,
                poiseMax
        );
    }

    public static ExternalActorCombatProfile r01Earthloong() {
        return new ExternalActorCombatProfile(
                "threateningly_mobs:the_earthloong",
                8,
                4900.0F,
                45.0,
                35.0,
                190.0
        );
    }
}
