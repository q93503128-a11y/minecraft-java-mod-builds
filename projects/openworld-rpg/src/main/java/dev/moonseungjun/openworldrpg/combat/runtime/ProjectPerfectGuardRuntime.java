package dev.moonseungjun.openworldrpg.combat.runtime;

import dev.moonseungjun.openworldrpg.integration.actor.ExternalActorBindingRuntime;
import dev.moonseungjun.openworldrpg.integration.actor.ExternalActorCombatProfile;
import java.util.Objects;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

public final class ProjectPerfectGuardRuntime {
    private ProjectPerfectGuardRuntime() {
    }

    public static Application onSuccessfulPerfectGuard(
            ServerPlayer defender,
            LivingEntity attacker,
            long nowTick
    ) {
        Objects.requireNonNull(defender, "defender");
        Objects.requireNonNull(attacker, "attacker");
        if (defender.level().isClientSide()
                || defender == attacker
                || defender.level() != attacker.level()) {
            return Application.rejected();
        }

        var profile = ExternalActorBindingRuntime
                .combatProfile(attacker)
                .orElse(null);
        if (profile == null) {
            return Application.rejected();
        }

        double authoredPoiseDamage = poiseDamageForRank(
                profile.combatRank(),
                profile.poiseMax(),
                GuardianRootPassiveEffects
                        .perfectGuardPoiseOutputMultiplier(defender)
        );
        var applied = ExternalActorBindingRuntime
                .applyProjectPoiseDamage(
                        attacker,
                        authoredPoiseDamage,
                        nowTick
                )
                .orElse(null);
        if (applied == null) {
            return Application.rejected();
        }
        return new Application(
                true,
                authoredPoiseDamage,
                applied.effectivePoiseDamage(),
                applied.remainingPoise(),
                applied.breakTriggered()
        );
    }

    public static double poiseDamageForRank(
            ExternalActorCombatProfile.CombatRank rank,
            double targetPoiseMax,
            double outputMultiplier
    ) {
        Objects.requireNonNull(rank, "rank");
        if (!Double.isFinite(targetPoiseMax)
                || targetPoiseMax <= 0.0
                || !Double.isFinite(outputMultiplier)
                || outputMultiplier <= 0.0) {
            throw new IllegalArgumentException(
                    "Perfect-guard poise inputs must be finite and positive."
            );
        }
        double fraction = rank
                == ExternalActorCombatProfile.CombatRank.BOSS
                ? 0.15
                : 0.20;
        return (8.0 + targetPoiseMax * fraction)
                * outputMultiplier;
    }

    public record Application(
            boolean applied,
            double authoredPoiseDamage,
            double effectivePoiseDamage,
            double remainingPoise,
            boolean breakTriggered
    ) {
        public Application {
            if (!Double.isFinite(authoredPoiseDamage)
                    || authoredPoiseDamage < 0.0
                    || !Double.isFinite(effectivePoiseDamage)
                    || effectivePoiseDamage < 0.0
                    || !Double.isFinite(remainingPoise)
                    || remainingPoise < 0.0) {
                throw new IllegalArgumentException(
                        "Invalid perfect-guard poise application."
                );
            }
        }

        public static Application rejected() {
            return new Application(
                    false,
                    0.0,
                    0.0,
                    0.0,
                    false
            );
        }
    }
}
