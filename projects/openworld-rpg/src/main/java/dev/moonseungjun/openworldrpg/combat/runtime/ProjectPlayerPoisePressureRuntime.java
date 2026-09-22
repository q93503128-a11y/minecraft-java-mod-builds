package dev.moonseungjun.openworldrpg.combat.runtime;

import java.util.Objects;
import net.minecraft.server.level.ServerPlayer;

/**
 * Explicit fail-closed seam for authored player-poise pressure.
 *
 * <p>COMBAT_BALANCE.md closes the max-poise formula and recovery rules, but the current persistent
 * equipment publisher does not yet publish the exact partial-armor ArmorPoise contribution. This
 * seam prevents authored pressure from being silently discarded or replaced with vanilla knockback.
 * It must become an accepting runtime only when that canonical snapshot exists.</p>
 */
public final class ProjectPlayerPoisePressureRuntime {
    private ProjectPlayerPoisePressureRuntime() {
    }

    public static Application applyAuthoredPressure(ServerPlayer target, double pressure) {
        Objects.requireNonNull(target, "target");
        if (!Double.isFinite(pressure) || pressure < 0.0) {
            throw new IllegalArgumentException("Player poise pressure must be finite and non-negative.");
        }
        return new Application(false, pressure);
    }

    public record Application(boolean applied, double authoredPressure) {
    }
}
