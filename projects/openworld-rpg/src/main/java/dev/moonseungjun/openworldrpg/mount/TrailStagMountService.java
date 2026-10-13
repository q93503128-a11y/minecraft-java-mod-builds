package dev.moonseungjun.openworldrpg.mount;

import java.util.Objects;
import net.minecraft.server.level.ServerPlayer;

/** Server-owned persistent Trail Stag progression seam for the R01 event and later summon runtime. */
public final class TrailStagMountService {
    private TrailStagMountService() {
    }

    public static TrailStagMountState state(ServerPlayer player) {
        Objects.requireNonNull(player, "player");
        return player.getAttachedOrSet(
                TrailStagMountAttachments.STATE,
                TrailStagMountState.initial()
        );
    }

    /** Idempotent completion seam for A Stag at the Ford. */
    public static TrailStagMountState unlockFromGreenwaterEvent(ServerPlayer player) {
        Objects.requireNonNull(player, "player");
        TrailStagMountState current = state(player);
        TrailStagMountState next = current.unlockTrailStag();
        if (!next.equals(current)) {
            player.setAttached(TrailStagMountAttachments.STATE, next);
        }
        return next;
    }

    /** Called only after the live Resolve runtime has actually reached zero. */
    public static TrailStagMountState recordResolveBreak(ServerPlayer player, long nowTick) {
        Objects.requireNonNull(player, "player");
        TrailStagMountState next = state(player).recordResolveBreak(nowTick);
        player.setAttached(TrailStagMountAttachments.STATE, next);
        return next;
    }

    /**
     * Gates the start and continuation of the one-second summon channel. Placement checks belong to
     * completion because the player may move during the channel and the server must probe the final
     * authoritative position rather than trusting an earlier candidate.
     */
    public static SummonEligibility evaluatePreChannel(
            ServerPlayer player,
            long nowTick,
            long ticksSinceHostileCombat,
            boolean presentationRuntimeReady
    ) {
        Objects.requireNonNull(player, "player");
        if (nowTick < 0L || ticksSinceHostileCombat < 0L) {
            throw new IllegalArgumentException("Mount summon timing values must be non-negative.");
        }
        TrailStagMountState state = state(player);
        if (!state.trailStagSelected()) return SummonEligibility.LOCKED;
        if (!presentationRuntimeReady) return SummonEligibility.PRESENTATION_NOT_READY;
        if (player.isSpectator() || !player.isAlive()) return SummonEligibility.INVALID_PLAYER;
        if (state.forcedCooldownTicksRemaining(nowTick) > 0L) return SummonEligibility.FORCED_COOLDOWN;
        if (ticksSinceHostileCombat < TrailStagMountRules.HOSTILE_COMBAT_QUIET_TICKS) {
            return SummonEligibility.COMBAT_LOCK;
        }
        return SummonEligibility.ALLOWED;
    }

    public static SummonEligibility evaluateSummon(
            ServerPlayer player,
            long nowTick,
            long ticksSinceHostileCombat,
            boolean forbiddenMountVolume,
            boolean hasLegalSpawnCandidate,
            boolean presentationRuntimeReady
    ) {
        SummonEligibility preChannel = evaluatePreChannel(
                player,
                nowTick,
                ticksSinceHostileCombat,
                presentationRuntimeReady
        );
        if (preChannel != SummonEligibility.ALLOWED) {
            return preChannel;
        }
        if (forbiddenMountVolume) return SummonEligibility.FORBIDDEN_VOLUME;
        if (!hasLegalSpawnCandidate) return SummonEligibility.NEED_OPEN_GROUND;
        return SummonEligibility.ALLOWED;
    }

    public enum SummonEligibility {
        ALLOWED,
        LOCKED,
        PRESENTATION_NOT_READY,
        INVALID_PLAYER,
        FORBIDDEN_VOLUME,
        FORCED_COOLDOWN,
        COMBAT_LOCK,
        NEED_OPEN_GROUND
    }
}
