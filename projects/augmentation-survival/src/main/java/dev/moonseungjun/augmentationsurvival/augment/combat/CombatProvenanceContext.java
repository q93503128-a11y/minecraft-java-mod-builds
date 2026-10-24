package dev.moonseungjun.augmentationsurvival.augment.combat;

import dev.moonseungjun.augmentationsurvival.augment.projectile.ProjectileAugmentService;
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
    private static final ThreadLocal<Context> OVERRIDE = new ThreadLocal<>();

    private CombatProvenanceContext() {
    }

    public static CombatProvenance resolve(DamageSource source) {
        Objects.requireNonNull(source, "source");
        Context overridden = OVERRIDE.get();
        if (overridden != null) {
            return overridden.provenance();
        }
        CombatProvenance projectile = ProjectileAugmentService.provenance(source);
        if (projectile != null) {
            return projectile;
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

    public static boolean isDelayedDamage() {
        Context context = OVERRIDE.get();
        return context != null && context.provenance() == CombatProvenance.DELAYED_DAMAGE;
    }

    public static float delayedDamageOr(float fallback) {
        Context context = OVERRIDE.get();
        if (context == null || context.provenance() != CombatProvenance.DELAYED_DAMAGE) {
            return fallback;
        }
        float amount = context.explicitDamage();
        return Float.isFinite(amount) && amount > 0.0F ? amount : fallback;
    }

    public static void runAs(CombatProvenance provenance, Runnable action) {
        callAs(provenance, () -> {
            action.run();
            return null;
        });
    }

    public static <T> T callAs(CombatProvenance provenance, Supplier<T> action) {
        return callWithContext(new Context(provenance, Float.NaN), action);
    }

    public static <T> T callAsDelayed(float amount, Supplier<T> action) {
        if (!(amount > 0.0F) || !Float.isFinite(amount)) {
            return action.get();
        }
        return callWithContext(new Context(CombatProvenance.DELAYED_DAMAGE, amount), action);
    }

    private static <T> T callWithContext(Context context, Supplier<T> action) {
        Objects.requireNonNull(context, "context");
        Objects.requireNonNull(action, "action");
        Context previous = OVERRIDE.get();
        OVERRIDE.set(context);
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

    private record Context(CombatProvenance provenance, float explicitDamage) {
        private Context {
            Objects.requireNonNull(provenance, "provenance");
        }
    }
}
