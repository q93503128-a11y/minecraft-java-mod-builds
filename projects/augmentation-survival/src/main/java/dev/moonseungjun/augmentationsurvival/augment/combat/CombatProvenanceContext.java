package dev.moonseungjun.augmentationsurvival.augment.combat;

import java.util.Objects;
import java.util.function.Supplier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;

/**
 * Server-thread provenance boundary. Future derived attacks must enter through runAs/callAs so
 * ROOT-only counters and damage bonuses never recurse accidentally.
 */
public final class CombatProvenanceContext {
    private static final ThreadLocal<CombatProvenance> OVERRIDE = new ThreadLocal<>();

    private CombatProvenanceContext() {
    }

    public static CombatProvenance resolve(DamageSource source) {
        Objects.requireNonNull(source, "source");
        CombatProvenance overridden = OVERRIDE.get();
        if (overridden != null) {
            return overridden;
        }
        if (!(source.getEntity() instanceof ServerPlayer)) {
            return CombatProvenance.OTHER;
        }
        if (source.typeHolder().is(DamageTypeTags.IS_PROJECTILE)) {
            return CombatProvenance.ROOT_PROJECTILE;
        }
        if (source.typeHolder().is(DamageTypeTags.IS_PLAYER_ATTACK)
                || source.getDirectEntity() == source.getEntity()) {
            return CombatProvenance.ROOT_MELEE;
        }
        return CombatProvenance.OTHER;
    }

    public static void runAs(CombatProvenance provenance, Runnable action) {
        callAs(provenance, () -> {
            action.run();
            return null;
        });
    }

    public static <T> T callAs(CombatProvenance provenance, Supplier<T> action) {
        Objects.requireNonNull(provenance, "provenance");
        Objects.requireNonNull(action, "action");
        CombatProvenance previous = OVERRIDE.get();
        OVERRIDE.set(provenance);
        try {
            return action.get();
        } finally {
            if (previous == null) {
                OVERRIDE.remove();
            } else {
                OVERRIDE.set(previous);
            }
        }
    }
}
