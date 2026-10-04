package dev.moonseungjun.openworldrpg.multiplayer;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/** Pure server timing state for one player's multiplayer Downed / revive lifecycle. */
public final class ProjectDownedRuntimeState {
    public static final int RESCUE_WINDOW_TICKS = 300;
    public static final int BASE_REVIVE_CHANNEL_TICKS = 50;
    public static final int RESCUE_FATIGUE_TICKS = 400;
    public static final double REVIVE_HP_FRACTION = 0.35;
    public static final double REVIVE_STAMINA_FRACTION = 0.50;
    public static final double REVIVE_MANA_FRACTION = 0.25;

    private boolean downed;
    private long rescueDeadlineTick = Long.MIN_VALUE / 4;
    private long rescueFatigueUntilTick = Long.MIN_VALUE / 4;
    private UUID reviverId;
    private long reviveCompleteAtTick = Long.MIN_VALUE / 4;

    public EnterStatus tryEnter(long nowTick) {
        requireTick(nowTick);
        if (downed) return EnterStatus.ALREADY_DOWNED;
        if (rescueFatigueActive(nowTick)) return EnterStatus.RESCUE_FATIGUE;
        downed = true;
        rescueDeadlineTick = Math.addExact(nowTick, RESCUE_WINDOW_TICKS);
        reviverId = null;
        reviveCompleteAtTick = Long.MIN_VALUE / 4;
        return EnterStatus.ENTERED;
    }

    public BeginReviveStatus tryBeginRevive(UUID candidateReviverId, long nowTick) {
        Objects.requireNonNull(candidateReviverId, "candidateReviverId");
        requireTick(nowTick);
        if (!downed) return BeginReviveStatus.NOT_DOWNED;
        if (rescueExpired(nowTick)) return BeginReviveStatus.RESCUE_EXPIRED;
        if (reviverId != null) {
            return reviverId.equals(candidateReviverId)
                    ? BeginReviveStatus.ALREADY_CHANNELING
                    : BeginReviveStatus.OTHER_REVIVER_CHANNELING;
        }
        long completion = Math.addExact(nowTick, BASE_REVIVE_CHANNEL_TICKS);
        if (completion >= rescueDeadlineTick) {
            return BeginReviveStatus.INSUFFICIENT_RESCUE_TIME;
        }
        reviverId = candidateReviverId;
        reviveCompleteAtTick = completion;
        return BeginReviveStatus.STARTED;
    }

    public boolean interruptRevive(UUID candidateReviverId) {
        Objects.requireNonNull(candidateReviverId, "candidateReviverId");
        if (!candidateReviverId.equals(reviverId)) return false;
        reviverId = null;
        reviveCompleteAtTick = Long.MIN_VALUE / 4;
        return true;
    }

    public CompleteReviveStatus tryCompleteRevive(UUID candidateReviverId, long nowTick) {
        Objects.requireNonNull(candidateReviverId, "candidateReviverId");
        requireTick(nowTick);
        if (!downed) return CompleteReviveStatus.NOT_DOWNED;
        if (rescueExpired(nowTick)) {
            reviverId = null;
            reviveCompleteAtTick = Long.MIN_VALUE / 4;
            return CompleteReviveStatus.RESCUE_EXPIRED;
        }
        if (reviverId == null || !reviverId.equals(candidateReviverId)) {
            return CompleteReviveStatus.NOT_CHANNEL_OWNER;
        }
        if (nowTick < reviveCompleteAtTick) return CompleteReviveStatus.TOO_EARLY;

        downed = false;
        rescueDeadlineTick = Long.MIN_VALUE / 4;
        reviverId = null;
        reviveCompleteAtTick = Long.MIN_VALUE / 4;
        rescueFatigueUntilTick = Math.addExact(nowTick, RESCUE_FATIGUE_TICKS);
        return CompleteReviveStatus.REVIVED;
    }

    public void clearAfterDefeat() {
        downed = false;
        rescueDeadlineTick = Long.MIN_VALUE / 4;
        rescueFatigueUntilTick = Long.MIN_VALUE / 4;
        reviverId = null;
        reviveCompleteAtTick = Long.MIN_VALUE / 4;
    }

    public boolean downed() {
        return downed;
    }

    public boolean rescueExpired(long nowTick) {
        requireTick(nowTick);
        return downed && nowTick >= rescueDeadlineTick;
    }

    public boolean rescueFatigueActive(long nowTick) {
        requireTick(nowTick);
        return nowTick < rescueFatigueUntilTick;
    }

    public Optional<UUID> reviverId() {
        return Optional.ofNullable(reviverId);
    }

    public Snapshot snapshot(long nowTick) {
        requireTick(nowTick);
        return new Snapshot(
                downed,
                downed && nowTick >= rescueDeadlineTick,
                downed ? Math.max(0L, rescueDeadlineTick - nowTick) : 0L,
                Optional.ofNullable(reviverId),
                reviverId != null ? Math.max(0L, reviveCompleteAtTick - nowTick) : 0L,
                Math.max(0L, rescueFatigueUntilTick - nowTick)
        );
    }

    private static void requireTick(long nowTick) {
        if (nowTick < 0L) throw new IllegalArgumentException("Downed runtime tick must be non-negative.");
    }

    public enum EnterStatus { ENTERED, ALREADY_DOWNED, RESCUE_FATIGUE }
    public enum BeginReviveStatus {
        STARTED, NOT_DOWNED, RESCUE_EXPIRED, ALREADY_CHANNELING,
        OTHER_REVIVER_CHANNELING, INSUFFICIENT_RESCUE_TIME
    }
    public enum CompleteReviveStatus {
        REVIVED, NOT_DOWNED, RESCUE_EXPIRED, NOT_CHANNEL_OWNER, TOO_EARLY
    }

    public record Snapshot(
            boolean downed,
            boolean rescueExpired,
            long rescueTicksRemaining,
            Optional<UUID> reviverId,
            long reviveChannelTicksRemaining,
            long rescueFatigueTicksRemaining
    ) {
        public Snapshot {
            reviverId = Objects.requireNonNull(reviverId, "reviverId");
            if (rescueTicksRemaining < 0L
                    || reviveChannelTicksRemaining < 0L
                    || rescueFatigueTicksRemaining < 0L) {
                throw new IllegalArgumentException("Downed snapshot timings cannot be negative.");
            }
            if (!downed && (rescueExpired || rescueTicksRemaining != 0L
                    || reviverId.isPresent() || reviveChannelTicksRemaining != 0L)) {
                throw new IllegalArgumentException("Non-downed snapshot cannot carry rescue state.");
            }
        }
    }
}
