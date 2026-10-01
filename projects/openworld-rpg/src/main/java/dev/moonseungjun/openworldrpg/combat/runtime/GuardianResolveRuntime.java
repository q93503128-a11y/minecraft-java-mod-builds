package dev.moonseungjun.openworldrpg.combat.runtime;

import dev.moonseungjun.openworldrpg.combat.authority.PlayerBarrierAuthority;
import dev.moonseungjun.openworldrpg.combat.state.CombatStateServices;
import dev.moonseungjun.openworldrpg.combat.state.PlayerProgressionService;
import dev.moonseungjun.openworldrpg.combat.state.RootClass;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.server.level.ServerPlayer;

public final class GuardianResolveRuntime {
    private static final String STAND_TOGETHER_SOURCE_ID =
            "openworld_rpg:guardian_stand_together";
    private static final ConcurrentHashMap<UUID, GuardianResolveRuntimeState>
            STATES = new ConcurrentHashMap<>();

    private GuardianResolveRuntime() {
    }

    public static void synchronize(ServerPlayer player) {
        Objects.requireNonNull(player, "player");
        if (!isActiveGuardian(player)) {
            STATES.remove(player.getUUID());
            return;
        }
        state(player.getUUID()).synchronizeExpiryBonusTicks(
                GuardianRootPassiveEffects
                        .resolveExpiryBonusTicks(player)
        );
    }

    public static GainApplication onPerfectGuard(
            ServerPlayer player,
            long nowTick
    ) {
        if (!isActiveGuardian(player)) {
            return GainApplication.rejected();
        }
        synchronize(player);
        var combat = CombatStateServices.states()
                .getOrCreate(player.getUUID(), nowTick);
        var gain = state(player.getUUID()).recordPerfectGuard(
                nowTick,
                combat.lastCombatActivityTick()
        );
        return applyGain(player, gain, nowTick);
    }

    public static GainApplication onCounterwallPerfectGuard(
            ServerPlayer player,
            long nowTick
    ) {
        if (!isActiveGuardian(player)) {
            return GainApplication.rejected();
        }
        synchronize(player);
        var combat = CombatStateServices.states()
                .getOrCreate(player.getUUID(), nowTick);
        var gain = state(player.getUUID()).recordCounterwallGuard(
                nowTick,
                combat.lastCombatActivityTick()
        );
        return applyGain(player, gain, nowTick);
    }

    public static GainApplication onOrdinaryGuardedHit(
            ServerPlayer player,
            double finalStaminaCost,
            long nowTick
    ) {
        if (!isActiveGuardian(player)) {
            return GainApplication.rejected();
        }
        synchronize(player);
        var combat = CombatStateServices.states()
                .getOrCreate(player.getUUID(), nowTick);
        var gain = state(player.getUUID()).recordGuardedHit(
                finalStaminaCost,
                nowTick,
                combat.lastCombatActivityTick()
        );
        return applyGain(player, gain, nowTick);
    }

    public static GainApplication onBarrierAbsorbed(
            ServerPlayer guardian,
            ServerPlayer recipient,
            double absorbedDamage,
            long nowTick
    ) {
        Objects.requireNonNull(recipient, "recipient");
        if (!isActiveGuardian(guardian)) {
            return GainApplication.rejected();
        }
        synchronize(guardian);
        var combat = CombatStateServices.states()
                .getOrCreate(guardian.getUUID(), nowTick);
        var gain = state(guardian.getUUID())
                .recordBarrierAbsorption(
                        recipient.getUUID(),
                        absorbedDamage,
                        recipient.getMaxHealth(),
                        nowTick,
                        combat.lastCombatActivityTick()
                );
        return applyGain(guardian, gain, nowTick);
    }

    public static int pips(ServerPlayer player, long nowTick) {
        if (!isActiveGuardian(player)) {
            return 0;
        }
        synchronize(player);
        var combat = CombatStateServices.states()
                .getOrCreate(player.getUUID(), nowTick);
        return state(player.getUUID()).pips(
                nowTick,
                combat.lastCombatActivityTick()
        );
    }

    public static int consumeAll(ServerPlayer player, long nowTick) {
        if (!isActiveGuardian(player)) {
            return 0;
        }
        synchronize(player);
        var combat = CombatStateServices.states()
                .getOrCreate(player.getUUID(), nowTick);
        return state(player.getUUID()).consumeAll(
                nowTick,
                combat.lastCombatActivityTick()
        );
    }

    public static void reset(UUID playerId) {
        if (playerId != null) {
            STATES.remove(playerId);
        }
    }

    public static void disconnect(UUID playerId) {
        reset(playerId);
    }

    private static GainApplication applyGain(
            ServerPlayer guardian,
            GuardianResolveRuntimeState.GainResult gain,
            long nowTick
    ) {
        boolean barrierTriggered = false;
        UUID barrierTargetId = null;
        double barrierApplied = 0.0;

        if (gain.reachedMaxNow()
                && GuardianRootPassiveEffects
                        .standTogetherEnabled(guardian)
                && state(guardian.getUUID())
                        .tryClaimStandTogether(nowTick)) {
            ServerPlayer target = nearestAlly(guardian);
            double fraction;
            if (target == null) {
                target = guardian;
                fraction = GuardianRootPassiveEffects
                        .STAND_TOGETHER_SOLO_BARRIER_MAX_HP_FRACTION;
            } else {
                fraction = GuardianRootPassiveEffects
                        .STAND_TOGETHER_ALLY_BARRIER_MAX_HP_FRACTION;
            }
            double requested = target.getMaxHealth()
                    * fraction
                    * (1.0 + GuardianRootPassiveEffects
                            .barrierOutputBonus(guardian));
            var grant = ProjectBarrierRuntime.applyFixedBarrier(
                    guardian,
                    target,
                    STAND_TOGETHER_SOURCE_ID,
                    requested,
                    PlayerBarrierAuthority
                            .DEFAULT_BARRIER_DURATION_TICKS,
                    false,
                    true
            );
            barrierTriggered = grant.accepted()
                    && grant.appliedAmount() > 0.0;
            barrierTargetId = target.getUUID();
            barrierApplied = grant.appliedAmount();
        }

        return new GainApplication(
                true,
                gain.thresholdQualified(),
                gain.pipAdded(),
                gain.icdBlocked(),
                gain.currentPips(),
                gain.reachedMaxNow(),
                barrierTriggered,
                barrierTargetId,
                barrierApplied
        );
    }

    private static ServerPlayer nearestAlly(ServerPlayer guardian) {
        var server = guardian.level().getServer();
        if (server == null) {
            return null;
        }
        double maxDistanceSq =
                GuardianRootPassiveEffects.STAND_TOGETHER_RANGE
                        * GuardianRootPassiveEffects.STAND_TOGETHER_RANGE;
        ServerPlayer nearest = null;
        double nearestDistanceSq = Double.POSITIVE_INFINITY;
        for (ServerPlayer candidate
                : server.getPlayerList().getPlayers()) {
            if (candidate == guardian
                    || candidate.level() != guardian.level()
                    || !candidate.isAlive()
                    || candidate.isSpectator()) {
                continue;
            }
            double distanceSq = guardian.distanceToSqr(candidate);
            if (distanceSq > maxDistanceSq) {
                continue;
            }
            if (distanceSq < nearestDistanceSq
                    || (distanceSq == nearestDistanceSq
                    && nearest != null
                    && candidate.getUUID().toString()
                            .compareTo(nearest.getUUID().toString()) < 0)) {
                nearest = candidate;
                nearestDistanceSq = distanceSq;
            }
        }
        return nearest;
    }

    private static GuardianResolveRuntimeState state(UUID playerId) {
        return STATES.computeIfAbsent(
                playerId,
                ignored -> new GuardianResolveRuntimeState()
        );
    }

    private static boolean isActiveGuardian(ServerPlayer player) {
        return player != null
                && !player.level().isClientSide()
                && PlayerProgressionService.state(player)
                        .activeClass()
                        .filter(RootClass.GUARDIAN::equals)
                        .isPresent();
    }

    public record GainApplication(
            boolean accepted,
            boolean thresholdQualified,
            boolean pipAdded,
            boolean icdBlocked,
            int currentPips,
            boolean reachedMaxNow,
            boolean standTogetherTriggered,
            UUID standTogetherTargetId,
            double standTogetherBarrierApplied
    ) {
        public GainApplication {
            if (currentPips < 0
                    || currentPips > GuardianResolveRuntimeState.MAX_PIPS
                    || !Double.isFinite(standTogetherBarrierApplied)
                    || standTogetherBarrierApplied < 0.0) {
                throw new IllegalArgumentException(
                        "Invalid Guardian Resolve application."
                );
            }
            if (!accepted
                    && (thresholdQualified
                    || pipAdded
                    || icdBlocked
                    || currentPips != 0
                    || reachedMaxNow
                    || standTogetherTriggered
                    || standTogetherTargetId != null
                    || standTogetherBarrierApplied != 0.0)) {
                throw new IllegalArgumentException(
                        "Rejected Guardian Resolve application cannot carry state."
                );
            }
            if (standTogetherTriggered
                    && standTogetherTargetId == null) {
                throw new IllegalArgumentException(
                        "Stand Together requires a barrier target."
                );
            }
        }

        public static GainApplication rejected() {
            return new GainApplication(
                    false,
                    false,
                    false,
                    false,
                    0,
                    false,
                    false,
                    null,
                    0.0
            );
        }
    }
}
