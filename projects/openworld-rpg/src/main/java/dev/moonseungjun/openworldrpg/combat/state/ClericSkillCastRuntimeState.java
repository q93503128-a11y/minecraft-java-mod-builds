package dev.moonseungjun.openworldrpg.combat.state;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Snapshots class-mechanic modifiers at server-accepted Cleric cast time.
 *
 * <p>Projectile impacts can arrive after cast completion, so Grace/Doctrine cannot be re-read at
 * impact time without letting a later skill change the earlier projectile's result.</p>
 */
public final class ClericSkillCastRuntimeState {
    public static final long PENDING_CAST_TTL_TICKS = 200L;

    private final Map<String, PendingCast> pending = new HashMap<>();

    public void begin(
            String spellId,
            boolean graceEmpowered,
            double outputMultiplier,
            long nowTick
    ) {
        requireSpellId(spellId);
        if (!Double.isFinite(outputMultiplier)
                || outputMultiplier < 1.0) {
            throw new IllegalArgumentException(
                    "Cleric cast output multiplier must be finite and >= 1."
            );
        }
        if (nowTick < 0L) {
            throw new IllegalArgumentException(
                    "Cleric cast time must be non-negative."
            );
        }
        refresh(nowTick);
        pending.put(
                spellId,
                new PendingCast(
                        spellId,
                        graceEmpowered,
                        outputMultiplier,
                        nowTick,
                        Math.addExact(
                                nowTick,
                                PENDING_CAST_TTL_TICKS
                        )
                )
        );
    }

    public PendingCast consume(String spellId, long nowTick) {
        requireSpellId(spellId);
        refresh(nowTick);
        return pending.remove(spellId);
    }

    public void reset() {
        pending.clear();
    }

    private void refresh(long nowTick) {
        if (nowTick < 0L) {
            throw new IllegalArgumentException(
                    "Cleric cast time must be non-negative."
            );
        }
        pending.entrySet().removeIf(
                entry -> nowTick >= entry.getValue().expiresAtTick()
        );
    }

    private static void requireSpellId(String spellId) {
        Objects.requireNonNull(spellId, "spellId");
        if (!spellId.startsWith("openworld_rpg:")) {
            throw new IllegalArgumentException(
                    "Cleric pending cast must be project-owned."
            );
        }
    }

    public record PendingCast(
            String spellId,
            boolean graceEmpowered,
            double outputMultiplier,
            long acceptedTick,
            long expiresAtTick
    ) {
        public PendingCast {
            requireSpellId(spellId);
            if (!Double.isFinite(outputMultiplier)
                    || outputMultiplier < 1.0
                    || acceptedTick < 0L
                    || expiresAtTick <= acceptedTick) {
                throw new IllegalArgumentException(
                        "Invalid pending Cleric cast snapshot."
                );
            }
        }
    }
}
