package dev.moonseungjun.openworldrpg.combat.runtime;

import dev.moonseungjun.openworldrpg.combat.state.CombatStateServices;
import dev.moonseungjun.openworldrpg.recovery.RecoveryUseRuntime;
import java.util.Objects;
import net.minecraft.server.level.ServerPlayer;

/** Server-authoritative application seam for authored player-poise pressure. */
public final class ProjectPlayerPoisePressureRuntime {
    private ProjectPlayerPoisePressureRuntime() {
    }

    public static Application applyAuthoredPressure(ServerPlayer target, double pressure) {
        Objects.requireNonNull(target, "target");
        if (!Double.isFinite(pressure) || pressure < 0.0) {
            throw new IllegalArgumentException("Player poise pressure must be finite and non-negative.");
        }

        var state = CombatStateServices.playerPoiseStates().state(target.getUUID());
        if (state.isEmpty()) {
            return Application.rejected(pressure);
        }

        long gameTick = target.level().getGameTime();
        double hyperarmorMultiplier =
                WarriorSkillRuntime.hyperarmorMultiplier(
                        target,
                        gameTick
                );
        double effectivePressure = pressure
                / Math.max(1.0, hyperarmorMultiplier);
        var result = state.orElseThrow().apply(
                effectivePressure,
                gameTick
        );
        if (result.breakTriggered()) {
            RecoveryUseRuntime.cancelPreResolution(target, gameTick);
        }
        return new Application(
                true,
                pressure,
                result.effectivePressure(),
                result.remainingPoise(),
                result.breakTriggered(),
                result.postBreakImmune()
        );
    }

    public record Application(
            boolean applied,
            double authoredPressure,
            double effectivePressure,
            double remainingPoise,
            boolean breakTriggered,
            boolean postBreakImmune
    ) {
        private static Application rejected(double authoredPressure) {
            return new Application(false, authoredPressure, 0.0, 0.0, false, false);
        }
    }
}
