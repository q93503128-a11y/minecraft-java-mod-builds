package dev.moonseungjun.openworldrpg.combat.runtime;

import dev.moonseungjun.openworldrpg.combat.authority.ProjectImpactTransaction;
import dev.moonseungjun.openworldrpg.combat.state.BurningRuntimeState;
import dev.moonseungjun.openworldrpg.combat.state.CombatStateServices;
import dev.moonseungjun.openworldrpg.integration.actor.ExternalActorBindingRuntime;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

/** Server-owned Burning status for project magic/fire skills. */
public final class ProjectBurningRuntime {
    public static final int TICK_COUNT =
            (int) (BurningRuntimeState.DURATION_TICKS
                    / BurningRuntimeState.TICK_INTERVAL_TICKS);

    private static final ConcurrentHashMap<UUID, Entry> BURNING =
            new ConcurrentHashMap<>();

    private ProjectBurningRuntime() {
    }

    public static Application apply(
            ServerPlayer sourcePlayer,
            LivingEntity target,
            ProjectImpactTransaction.DamageSourceSnapshot sourceSnapshot,
            double totalActionCoefficient,
            long nowTick
    ) {
        Objects.requireNonNull(sourcePlayer, "sourcePlayer");
        Objects.requireNonNull(target, "target");
        Objects.requireNonNull(sourceSnapshot, "sourceSnapshot");
        if (sourcePlayer.level().isClientSide()
                || target.level() != sourcePlayer.level()
                || target == sourcePlayer
                || !Double.isFinite(totalActionCoefficient)
                || totalActionCoefficient <= 0.0
                || nowTick < 0L
                || !(target.level() instanceof ServerLevel level)) {
            return Application.rejected();
        }

        var targetSnapshot = ExternalActorBindingRuntime
                .projectTargetSnapshot(target, nowTick)
                .orElse(null);
        if (targetSnapshot == null) {
            return Application.rejected();
        }

        double coefficientPerTick =
                totalActionCoefficient / TICK_COUNT;
        double candidateTickDamage =
                ProjectImpactTransaction.resolveDirectDamage(
                        new ProjectImpactTransaction.DirectDamageRequest(
                                sourceSnapshot,
                                targetSnapshot,
                                ProjectImpactTransaction.DamageSchool.MAGIC,
                                coefficientPerTick,
                                1.0,
                                1.0
                        )
                ).finalDamage();
        if (candidateTickDamage <= 0.0) {
            return Application.rejected();
        }

        Entry entry = BURNING.compute(
                target.getUUID(),
                (ignored, current) -> {
                    if (current == null
                            || current.level() != level) {
                        return new Entry(
                                level,
                                sourcePlayer.getUUID(),
                                new BurningRuntimeState()
                        );
                    }
                    return current;
                }
        );
        var applied = entry.state().apply(
                candidateTickDamage,
                nowTick
        );
        if (applied.magnitudeReplaced()) {
            entry.setSourcePlayerId(
                    sourcePlayer.getUUID()
            );
        }

        return new Application(
                true,
                applied.magnitudeReplaced(),
                applied.tickDamage(),
                applied.nextTickAt(),
                applied.expiresAt(),
                entry.sourcePlayerId()
        );
    }

    public static void tick(MinecraftServer server) {
        Objects.requireNonNull(server, "server");
        BURNING.entrySet().removeIf(entry -> {
            Entry burning = entry.getValue();
            var entity = burning.level().getEntity(entry.getKey());
            if (!(entity instanceof LivingEntity target)
                    || !target.isAlive()) {
                return true;
            }

            long nowTick = burning.level().getGameTime();
            var snapshot = burning.state().snapshot(nowTick);
            if (!snapshot.active()) {
                return true;
            }

            var due = burning.state().pollTick(nowTick);
            if (!due.due()) {
                return false;
            }

            ServerPlayer source = server.getPlayerList()
                    .getPlayer(burning.sourcePlayerId());
            if (source == null
                    || !source.isAlive()
                    || source.level() != burning.level()) {
                return true;
            }

            if (ProjectMinecraftDamageApplicator.applyTimedMagic(
                    source,
                    target,
                    due.damage()
            )) {
                CombatStateServices.markCombatActivity(
                        source.getUUID(),
                        nowTick
                );
            }
            return due.finalTick()
                    || !target.isAlive();
        });
    }

    public static void removeSource(UUID playerId) {
        if (playerId == null) {
            return;
        }
        BURNING.entrySet().removeIf(
                entry -> entry.getValue()
                        .sourcePlayerId()
                        .equals(playerId)
        );
    }

    public static void clearTarget(UUID targetId) {
        if (targetId != null) {
            BURNING.remove(targetId);
        }
    }

    public static int activeCount() {
        return BURNING.size();
    }

    private static final class Entry {
        private final ServerLevel level;
        private UUID sourcePlayerId;
        private final BurningRuntimeState state;

        private Entry(
                ServerLevel level,
                UUID sourcePlayerId,
                BurningRuntimeState state
        ) {
            this.level = Objects.requireNonNull(level, "level");
            this.sourcePlayerId =
                    Objects.requireNonNull(
                            sourcePlayerId,
                            "sourcePlayerId"
                    );
            this.state = Objects.requireNonNull(state, "state");
        }

        private ServerLevel level() {
            return level;
        }

        private UUID sourcePlayerId() {
            return sourcePlayerId;
        }

        private void setSourcePlayerId(UUID sourcePlayerId) {
            this.sourcePlayerId = Objects.requireNonNull(
                    sourcePlayerId,
                    "sourcePlayerId"
            );
        }

        private BurningRuntimeState state() {
            return state;
        }
    }

    public record Application(
            boolean accepted,
            boolean magnitudeReplaced,
            double tickDamage,
            long nextTickAt,
            long expiresAt,
            UUID sourcePlayerId
    ) {
        public Application {
            if (!accepted
                    && (magnitudeReplaced
                    || tickDamage != 0.0
                    || nextTickAt != 0L
                    || expiresAt != 0L
                    || sourcePlayerId != null)) {
                throw new IllegalArgumentException(
                        "Rejected Burning application cannot carry state."
                );
            }
            if (accepted
                    && (!Double.isFinite(tickDamage)
                    || tickDamage <= 0.0
                    || nextTickAt < 0L
                    || expiresAt < nextTickAt
                    || sourcePlayerId == null)) {
                throw new IllegalArgumentException(
                        "Invalid Burning application."
                );
            }
        }

        public static Application rejected() {
            return new Application(
                    false,
                    false,
                    0.0,
                    0L,
                    0L,
                    null
            );
        }
    }
}
