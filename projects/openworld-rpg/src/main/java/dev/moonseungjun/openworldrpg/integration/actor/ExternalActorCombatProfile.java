package dev.moonseungjun.openworldrpg.integration.actor;

import dev.moonseungjun.openworldrpg.combat.authority.ProjectImpactTransaction;
import java.util.Objects;

public record ExternalActorCombatProfile(
        String entityId,
        int contentLevel,
        float maxHealth,
        double defense,
        double magicResistance,
        double poiseMax,
        CombatRank combatRank,
        ExternalActorReactionCapabilities reactionCapabilities,
        ExternalActorWeakPointProfile weakPointProfile,
        boolean meaningfulControlRewardEligible
) {
    public ExternalActorCombatProfile {
        Objects.requireNonNull(entityId, "entityId");
        Objects.requireNonNull(combatRank, "combatRank");
        Objects.requireNonNull(
                reactionCapabilities,
                "reactionCapabilities"
        );
        Objects.requireNonNull(
                weakPointProfile,
                "weakPointProfile"
        );
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

    public ExternalActorCombatProfile(
            String entityId,
            int contentLevel,
            float maxHealth,
            double defense,
            double magicResistance,
            double poiseMax,
            CombatRank combatRank,
            ExternalActorReactionCapabilities reactionCapabilities,
            ExternalActorWeakPointProfile weakPointProfile
    ) {
        this(
                entityId,
                contentLevel,
                maxHealth,
                defense,
                magicResistance,
                poiseMax,
                combatRank,
                reactionCapabilities,
                weakPointProfile,
                defaultMeaningfulControlRewardEligible(combatRank)
        );
    }

    public ExternalActorCombatProfile(
            String entityId,
            int contentLevel,
            float maxHealth,
            double defense,
            double magicResistance,
            double poiseMax
    ) {
        this(
                entityId,
                contentLevel,
                maxHealth,
                defense,
                magicResistance,
                poiseMax,
                CombatRank.NORMAL_ELITE,
                ExternalActorReactionCapabilities.none(),
                ExternalActorWeakPointProfile.none()
        );
    }

    public ExternalActorCombatProfile(
            String entityId,
            int contentLevel,
            float maxHealth,
            double defense,
            double magicResistance,
            double poiseMax,
            CombatRank combatRank
    ) {
        this(
                entityId,
                contentLevel,
                maxHealth,
                defense,
                magicResistance,
                poiseMax,
                combatRank,
                ExternalActorReactionCapabilities.none(),
                ExternalActorWeakPointProfile.none()
        );
    }

    public ExternalActorCombatProfile(
            String entityId,
            int contentLevel,
            float maxHealth,
            double defense,
            double magicResistance,
            double poiseMax,
            CombatRank combatRank,
            ExternalActorReactionCapabilities reactionCapabilities
    ) {
        this(
                entityId,
                contentLevel,
                maxHealth,
                defense,
                magicResistance,
                poiseMax,
                combatRank,
                reactionCapabilities,
                ExternalActorWeakPointProfile.none()
        );
    }

    private static boolean defaultMeaningfulControlRewardEligible(
            CombatRank combatRank
    ) {
        return combatRank != CombatRank.NORMAL_ELITE;
    }

    public enum CombatRank {
        NORMAL_ELITE,
        MINIBOSS,
        BOSS
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
                190.0,
                CombatRank.BOSS,
                ExternalActorReactionCapabilities.none()
        );
    }
}
