package dev.moonseungjun.openworldrpg.combat.runtime;

import dev.moonseungjun.openworldrpg.combat.authority.ProjectSpellSpec;
import dev.moonseungjun.openworldrpg.combat.state.CombatStateServices;
import dev.moonseungjun.openworldrpg.combat.state.PlayerNegativeStatusRuntimeState;
import dev.moonseungjun.openworldrpg.combat.state.PlayerProgressionService;
import dev.moonseungjun.openworldrpg.combat.state.RootClass;
import java.util.Objects;
import net.minecraft.server.level.ServerPlayer;

/**
 * Project-owned Cleric Mend resolution layered over the generic healing authority.
 */
public final class ClericMendRuntime {
    private ClericMendRuntime() {
    }

    public static Application apply(
            ServerPlayer caster,
            ServerPlayer target
    ) {
        Objects.requireNonNull(caster, "caster");
        Objects.requireNonNull(target, "target");

        if (caster.level() != target.level()
                || !target.isAlive()
                || PlayerProgressionService.state(caster)
                        .activeClass()
                        .filter(RootClass.CLERIC::equals)
                        .isEmpty()) {
            return Application.rejected();
        }

        long nowTick = caster.level().getGameTime();
        var combat = CombatStateServices.states()
                .getOrCreate(caster.getUUID(), nowTick);
        var grace = CombatStateServices.clericGraceStates()
                .getOrCreate(caster.getUUID());

        boolean empowered = grace.consumeForSpender(
                nowTick,
                combat.lastCombatActivityTick()
        );
        double coefficient = empowered
                ? ProjectSpellSpec.MEND_EMPOWERED_HEAL_COEFFICIENT
                : ProjectSpellSpec.MEND_HEAL_COEFFICIENT;

        ProjectHealingRuntime.Application healing =
                ProjectHealingRuntime.applySkillHeal(
                        caster,
                        target,
                        coefficient
                );
        if (!healing.accepted()) {
            return Application.rejected();
        }

        int cleansed = 0;
        if (empowered) {
            cleansed = CombatStateServices.negativeStatusStates()
                    .getOrCreate(target.getUUID())
                    .cleanseOneTagged(
                            PlayerNegativeStatusRuntimeState
                                    .MINOR_DISPELLABLE_TAG,
                            nowTick
                    );
        }

        var gain = grace.recordEffectiveHeal(
                target.getUUID(),
                healing.effectiveHealing(),
                target.getMaxHealth(),
                nowTick,
                combat.lastCombatActivityTick()
        );

        return new Application(
                true,
                empowered,
                coefficient,
                healing.requestedHealing(),
                healing.effectiveHealing(),
                cleansed,
                gain.currentPips()
        );
    }

    public record Application(
            boolean accepted,
            boolean empowered,
            double healCoefficient,
            double requestedHealing,
            double effectiveHealing,
            int cleansedStatuses,
            int gracePipsAfter
    ) {
        public Application {
            if (healCoefficient < 0.0
                    || requestedHealing < 0.0
                    || effectiveHealing < 0.0
                    || cleansedStatuses < 0
                    || cleansedStatuses > 1
                    || gracePipsAfter < 0
                    || gracePipsAfter > 3) {
                throw new IllegalArgumentException(
                        "Invalid Mend runtime result."
                );
            }
            if (!accepted
                    && (empowered
                    || healCoefficient != 0.0
                    || requestedHealing != 0.0
                    || effectiveHealing != 0.0
                    || cleansedStatuses != 0
                    || gracePipsAfter != 0)) {
                throw new IllegalArgumentException(
                        "Rejected Mend cannot carry applied state."
                );
            }
        }

        public static Application rejected() {
            return new Application(
                    false,
                    false,
                    0.0,
                    0.0,
                    0.0,
                    0,
                    0
            );
        }
    }
}
