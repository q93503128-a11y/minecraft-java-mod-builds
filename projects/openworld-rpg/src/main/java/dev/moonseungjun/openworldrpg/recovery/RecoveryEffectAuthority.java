package dev.moonseungjun.openworldrpg.recovery;

import java.util.Objects;

/** Pure server-authority resolution contract for the three R01 quick recovery consumables. */
public final class RecoveryEffectAuthority {
    public static final String MINOR_DISPELLABLE_TAG = "minor_dispellable";

    private RecoveryEffectAuthority() {
    }

    public static Effect resolve(
            RecoveryConsumable consumable,
            double maxHp,
            double maxMana
    ) {
        Objects.requireNonNull(consumable, "consumable");
        if (!Double.isFinite(maxHp) || maxHp <= 0.0) {
            throw new IllegalArgumentException("maxHp must be finite and positive.");
        }
        if (!Double.isFinite(maxMana) || maxMana <= 0.0) {
            throw new IllegalArgumentException("maxMana must be finite and positive.");
        }

        return switch (consumable) {
            case HEALING_POTION -> new Effect(
                    RecoveryActionRules.healingPotionAmount(maxHp),
                    0.0,
                    0.0,
                    0,
                    false,
                    0
            );
            case FOCUS_DRAUGHT -> new Effect(
                    0.0,
                    RecoveryActionRules.focusImmediateAmount(maxMana),
                    RecoveryActionRules.focusTailPerTick(maxMana),
                    RecoveryActionRules.FOCUS_DRAUGHT_TAIL_TICKS,
                    false,
                    0
            );
            case CLEANSING_TONIC -> new Effect(
                    0.0,
                    0.0,
                    0.0,
                    0,
                    true,
                    60
            );
        };
    }

    public record Effect(
            double hpRestore,
            double immediateManaRestore,
            double manaRestorePerTick,
            int manaTailTicks,
            boolean cleanseMinorDispellable,
            int negativeBuildupResistanceTicks
    ) {
        public Effect {
            if (!Double.isFinite(hpRestore) || hpRestore < 0.0) {
                throw new IllegalArgumentException("hpRestore must be finite and non-negative.");
            }
            if (!Double.isFinite(immediateManaRestore) || immediateManaRestore < 0.0) {
                throw new IllegalArgumentException(
                        "immediateManaRestore must be finite and non-negative."
                );
            }
            if (!Double.isFinite(manaRestorePerTick) || manaRestorePerTick < 0.0) {
                throw new IllegalArgumentException(
                        "manaRestorePerTick must be finite and non-negative."
                );
            }
            if (manaTailTicks < 0 || negativeBuildupResistanceTicks < 0) {
                throw new IllegalArgumentException("Effect durations must be non-negative.");
            }
        }
    }
}
