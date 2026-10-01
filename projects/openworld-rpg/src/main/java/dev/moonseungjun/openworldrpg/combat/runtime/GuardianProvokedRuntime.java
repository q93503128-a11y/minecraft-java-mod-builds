package dev.moonseungjun.openworldrpg.combat.runtime;

import dev.moonseungjun.openworldrpg.combat.state.PlayerProgressionService;
import dev.moonseungjun.openworldrpg.combat.state.RootClass;
import dev.moonseungjun.openworldrpg.integration.actor.ExternalActorBindingRuntime;
import dev.moonseungjun.openworldrpg.integration.actor.ExternalActorCombatProfile;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;

/**
 * Server-owned Guardian Provoked state.
 *
 * <p>The status publishes target-weight multipliers rather than hard-locking donor AI. Current
 * authored encounter adapters may consume the multiplier; PvP and non-AI targets are rejected.</p>
 */
public final class GuardianProvokedRuntime {
    public static final long DURATION_TICKS = 100L;
    public static final double NORMAL_ELITE_WEIGHT = 4.0;
    public static final double MINIBOSS_BOSS_WEIGHT = 2.0;

    private static final ConcurrentHashMap<UUID, Entry> ENTRIES =
            new ConcurrentHashMap<>();

    private GuardianProvokedRuntime() {
    }

    public static Application apply(
            ServerPlayer guardian,
            LivingEntity target,
            long nowTick
    ) {
        Objects.requireNonNull(guardian, "guardian");
        Objects.requireNonNull(target, "target");
        if (nowTick < 0L
                || guardian.level().isClientSide()
                || guardian.level() != target.level()
                || !(target instanceof Mob)
                || !isGuardian(guardian)) {
            return Application.rejected();
        }
        var profile = ExternalActorBindingRuntime.combatProfile(target).orElse(null);
        if (profile == null) {
            return Application.rejected();
        }
        double weight = profile.combatRank()
                == ExternalActorCombatProfile.CombatRank.NORMAL_ELITE
                ? NORMAL_ELITE_WEIGHT
                : MINIBOSS_BOSS_WEIGHT;
        long expiresAt = Math.addExact(nowTick, DURATION_TICKS);
        ENTRIES.put(
                target.getUUID(),
                new Entry(
                        guardian.getUUID(),
                        (ServerLevel) target.level(),
                        expiresAt,
                        weight
                )
        );
        return new Application(true, expiresAt, weight);
    }

    public static boolean isProvokedToward(
            LivingEntity target,
            ServerPlayer guardian,
            long nowTick
    ) {
        Objects.requireNonNull(target, "target");
        Objects.requireNonNull(guardian, "guardian");
        Entry entry = liveEntry(target, nowTick);
        return entry != null
                && entry.guardianId().equals(guardian.getUUID())
                && guardian.level() == target.level()
                && guardian.isAlive();
    }

    public static double threatWeightMultiplier(
            LivingEntity target,
            UUID candidatePlayerId,
            long nowTick
    ) {
        Objects.requireNonNull(target, "target");
        Objects.requireNonNull(candidatePlayerId, "candidatePlayerId");
        Entry entry = liveEntry(target, nowTick);
        if (entry == null || !entry.guardianId().equals(candidatePlayerId)) {
            return 1.0;
        }
        return entry.weightMultiplier();
    }

    public static void tick(MinecraftServer server) {
        Objects.requireNonNull(server, "server");
        ENTRIES.entrySet().removeIf(entry -> {
            Entry state = entry.getValue();
            LivingEntity target = state.level().getEntity(entry.getKey())
                    instanceof LivingEntity living ? living : null;
            ServerPlayer guardian = server.getPlayerList()
                    .getPlayer(state.guardianId());
            long nowTick = state.level().getGameTime();
            return nowTick >= state.expiresAtTick()
                    || target == null
                    || !target.isAlive()
                    || guardian == null
                    || !guardian.isAlive()
                    || guardian.level() != state.level()
                    || !isGuardian(guardian);
        });
    }

    public static void clearTarget(UUID targetId) {
        if (targetId != null) {
            ENTRIES.remove(targetId);
        }
    }

    public static void clearGuardian(UUID guardianId) {
        if (guardianId != null) {
            ENTRIES.entrySet().removeIf(
                    entry -> entry.getValue().guardianId().equals(guardianId)
            );
        }
    }

    private static Entry liveEntry(
            LivingEntity target,
            long nowTick
    ) {
        if (nowTick < 0L) {
            throw new IllegalArgumentException(
                    "Provoked time must be non-negative."
            );
        }
        Entry entry = ENTRIES.get(target.getUUID());
        if (entry == null) {
            return null;
        }
        if (entry.level() != target.level()
                || nowTick >= entry.expiresAtTick()
                || !target.isAlive()) {
            ENTRIES.remove(target.getUUID(), entry);
            return null;
        }
        return entry;
    }

    private static boolean isGuardian(ServerPlayer player) {
        return PlayerProgressionService.state(player)
                .activeClass()
                .filter(RootClass.GUARDIAN::equals)
                .isPresent();
    }

    private record Entry(
            UUID guardianId,
            ServerLevel level,
            long expiresAtTick,
            double weightMultiplier
    ) {
        private Entry {
            Objects.requireNonNull(guardianId, "guardianId");
            Objects.requireNonNull(level, "level");
            if (expiresAtTick < 0L
                    || !Double.isFinite(weightMultiplier)
                    || weightMultiplier < 1.0) {
                throw new IllegalArgumentException(
                        "Invalid Provoked runtime entry."
                );
            }
        }
    }

    public record Application(
            boolean accepted,
            long expiresAtTick,
            double threatWeightMultiplier
    ) {
        public Application {
            if (expiresAtTick < 0L
                    || !Double.isFinite(threatWeightMultiplier)
                    || threatWeightMultiplier < 0.0) {
                throw new IllegalArgumentException(
                        "Invalid Provoked application."
                );
            }
            if (!accepted
                    && (expiresAtTick != 0L
                    || threatWeightMultiplier != 0.0)) {
                throw new IllegalArgumentException(
                        "Rejected Provoked application cannot carry state."
                );
            }
        }

        public static Application rejected() {
            return new Application(false, 0L, 0.0);
        }
    }
}
