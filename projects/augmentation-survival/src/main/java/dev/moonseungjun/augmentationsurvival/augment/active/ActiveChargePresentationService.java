package dev.moonseungjun.augmentationsurvival.augment.active;

import dev.moonseungjun.augmentationsurvival.AugmentationSurvival;
import dev.moonseungjun.augmentationsurvival.state.AugmentationPlayerState;
import java.util.Set;

/**
 * Converts the server-owned cooldown keys into a compact HUD snapshot.
 *
 * <p>The compatible roster mirrors the runtime services that actually implement the canonical
 * Double Charge contract. Toggle, death-resource and mythic actives are deliberately excluded.</p>
 */
public final class ActiveChargePresentationService {
    private static final String DOUBLE_CHARGE_ID = id("double_charge");
    private static final Set<String> DOUBLE_CHARGE_COMPATIBLE = Set.of(
            id("cleanse"), id("pull"), id("swap"), id("shield"), id("temporary_platform"),
            id("berserk"), id("healing_wave"), id("shield_throw"), id("rewind"),
            id("time_acceleration"), id("clone"), id("phase_shift"), id("domination"),
            id("gigantification"), id("area_acceleration"), id("revive")
    );

    private ActiveChargePresentationService() {
    }

    public static boolean supportsDoubleCharge(String abilityId) {
        return abilityId != null && DOUBLE_CHARGE_COMPATIBLE.contains(abilityId);
    }

    public static Snapshot snapshot(AugmentationPlayerState state, String abilityId, long now) {
        if (abilityId == null || abilityId.isBlank()) {
            return Snapshot.EMPTY;
        }
        if (state.level(DOUBLE_CHARGE_ID) > 0 && supportsDoubleCharge(abilityId)) {
            return new Snapshot(
                    2,
                    remainingTicks(state.cooldownReadyTick(chargeKey(abilityId, 0)), now),
                    remainingTicks(state.cooldownReadyTick(chargeKey(abilityId, 1)), now)
            );
        }
        return new Snapshot(1, remainingTicks(state.cooldownReadyTick(abilityId), now), 0);
    }

    private static int remainingTicks(long readyTick, long now) {
        long remaining = Math.max(0L, readyTick - now);
        return (int) Math.min(Integer.MAX_VALUE, remaining);
    }

    private static String chargeKey(String abilityId, int index) {
        return abilityId + "#charge" + index;
    }

    private static String id(String path) {
        return AugmentationSurvival.MOD_ID + ":" + path;
    }

    public record Snapshot(int maxCharges, int charge0Ticks, int charge1Ticks) {
        private static final Snapshot EMPTY = new Snapshot(0, 0, 0);

        public Snapshot {
            maxCharges = Math.max(0, Math.min(2, maxCharges));
            charge0Ticks = Math.max(0, charge0Ticks);
            charge1Ticks = Math.max(0, charge1Ticks);
        }

        public int readyCharges() {
            if (maxCharges <= 0) return 0;
            int ready = charge0Ticks <= 0 ? 1 : 0;
            if (maxCharges > 1 && charge1Ticks <= 0) ready++;
            return ready;
        }

        public int nextRechargeTicks() {
            int next = Integer.MAX_VALUE;
            if (charge0Ticks > 0) next = Math.min(next, charge0Ticks);
            if (maxCharges > 1 && charge1Ticks > 0) next = Math.min(next, charge1Ticks);
            return next == Integer.MAX_VALUE ? 0 : next;
        }
    }
}
