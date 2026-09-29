package dev.moonseungjun.openworldrpg.combat.runtime;

import dev.moonseungjun.openworldrpg.combat.state.RebukedRuntimeState;
import dev.moonseungjun.openworldrpg.integration.actor.ExternalActorCombatProfile;
import dev.moonseungjun.openworldrpg.integration.actor.ExternalActorBindingRuntime;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.world.entity.LivingEntity;

/**
 * Server-owned negative combat states applied to project-owned hostiles.
 */
public final class ProjectHostileStatusRuntime {
    public static final double REBUKED_STANDARD_MULTIPLIER = 0.85;
    public static final double REBUKED_BOSS_MULTIPLIER = 0.92;
    public static final long REBUKED_STANDARD_TICKS = 80L;
    public static final long REBUKED_BOSS_TICKS = 60L;
    public static final long REBUKED_EMPOWERED_BONUS_TICKS = 20L;

    private static final ConcurrentHashMap<UUID, RebukedRuntimeState>
            REBUKED = new ConcurrentHashMap<>();

    private ProjectHostileStatusRuntime() {
    }

    public static Optional<Application> applyRebuked(
            LivingEntity target,
            boolean empowered,
            long nowTick
    ) {
        var profile = ExternalActorBindingRuntime
                .combatProfile(target)
                .orElse(null);
        if (profile == null) {
            return Optional.empty();
        }

        boolean bossRank = profile.combatRank()
                == ExternalActorCombatProfile.CombatRank.MINIBOSS_BOSS;
        double multiplier = bossRank
                ? REBUKED_BOSS_MULTIPLIER
                : REBUKED_STANDARD_MULTIPLIER;
        long baseDuration = bossRank
                ? REBUKED_BOSS_TICKS
                : REBUKED_STANDARD_TICKS;
        long duration = baseDuration
                + (empowered
                        ? REBUKED_EMPOWERED_BONUS_TICKS
                        : 0L);

        var state = REBUKED.computeIfAbsent(
                target.getUUID(),
                ignored -> new RebukedRuntimeState()
        );
        var applied = state.apply(
                multiplier,
                duration,
                nowTick
        );
        return Optional.of(
                new Application(
                        multiplier,
                        duration,
                        applied.expiresAtTick(),
                        bossRank
                )
        );
    }

    public static double outgoingDirectDamageMultiplier(
            LivingEntity attacker,
            long nowTick
    ) {
        var state = REBUKED.get(attacker.getUUID());
        if (state == null) {
            return 1.0;
        }
        double multiplier =
                state.outgoingDirectDamageMultiplier(nowTick);
        if (multiplier == 1.0) {
            REBUKED.remove(attacker.getUUID(), state);
        }
        return multiplier;
    }

    public static Optional<RebukedRuntimeState.Snapshot> rebukedSnapshot(
            LivingEntity target,
            long nowTick
    ) {
        var state = REBUKED.get(target.getUUID());
        if (state == null) {
            return Optional.empty();
        }
        var snapshot = state.snapshot(nowTick);
        if (!snapshot.active()) {
            REBUKED.remove(target.getUUID(), state);
            return Optional.empty();
        }
        return Optional.of(snapshot);
    }

    public static void clear(UUID entityId) {
        REBUKED.remove(entityId);
    }

    public record Application(
            double multiplier,
            long durationTicks,
            long expiresAtTick,
            boolean bossRank
    ) {
    }
}
